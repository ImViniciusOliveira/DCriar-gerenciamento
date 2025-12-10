import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, filter, map, switchMap, tap, shareReplay, take, catchError, of, combineLatest } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { Hateoas } from '../../../core/models/hateoas.model';
import { ApiResponseProducts, Product } from '../models/product.model';
import { Channel } from '../../stock/models/channel-stock.model';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  /**
   * Signal para forçar a atualização da lista.
   * Configurado com `equal: () => false` para disparar sempre que setado, mesmo com o mesmo valor.
   */
  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  /**
   * Signal que armazena os parâmetros atuais de paginação e ordenação.
   * Possui uma função de igualdade personalizada para evitar disparos se os valores forem idênticos.
   */
  private readonly productSearchParams = signal<{ page: number; size: number; sort: string }>({
    page: 0,
    size: 10,
    sort: 'id,asc'
  }, {
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly productSearchParams$ = toObservable(this.productSearchParams);

  /**
   * Observable reativo que emite a lista de produtos combinada com o estoque.
   * Atualiza automaticamente quando os parâmetros de busca mudam ou o refresh é acionado.
   */
  readonly products$: Observable<ApiResponseProducts>;

  constructor() {
    this.products$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        if (!endpoints || !endpoints._links?.['produtos']?.href) {
          return of(this.createEmptyResponse());
        }

        return combineLatest([
          this.productSearchParams$,
          this.refresh$
        ]).pipe(
          switchMap(([params, _]) => {
            const productsRootUrl = endpoints._links?.['produtos']?.href;
            if (!productsRootUrl) {
               return of(this.createEmptyResponse());
            }

            const baseUrl = productsRootUrl.split('{')[0];
            const finalUrl = `${baseUrl}?page=${params.page}&size=${params.size}&sort=${params.sort}`;

            return this.http.get<any>(finalUrl).pipe(
              switchMap(productsApiResponse => this.enrichProductsWithStock(productsApiResponse)),
              map(responseWithMergedStocks => this.sortAndPaginateClientSide(responseWithMergedStocks, params)),
              catchError(err => {
                console.error(`Falha ao buscar produtos na página ${params.page}, tamanho ${params.size}`, err);
                return of(this.createEmptyResponse());
              })
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    shareReplay(1)
  );

  getProducts(): Observable<ApiResponseProducts> {
    return this.products$;
  }

  updateSearchParams(page: number, size: number, sort: string): void {
    this.productSearchParams.set({ page, size, sort });
  }

  getProductByUrl(url: string): Observable<Product> {
    return this.http.get<Product>(url);
  }

  getNewProductTemplate(): Observable<Product> {
    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.get<Product>(`${baseUrl}/new`)),
      take(1)
    );
  }

  deleteProduct(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  createProduct(product: Partial<Product>, skipRefresh = false): Observable<Product> {
    const payload = this.mapToPayload(product);
    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      take(1),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(url => this.http.post<Product>(url, payload)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  patchProduct(productId: number, product: Partial<Product>, skipRefresh = false): Observable<Product> {
    delete product.id;
    const payload = this.mapToPayload(product);

    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.patch<Product>(`${baseUrl}/${productId}`, payload)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); }),
      take(1)
    );
  }

  uploadProductPhoto(uploadUrl: string, file: File, skipRefresh = false): Observable<Product> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Product>(uploadUrl, formData)
      .pipe(tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); }));
  }

  getStocksForProducts(productIds: number[], stockUrl: string): Observable<{ [productId: string]: { [channelKey: string]: number } }> {
    if (productIds.length === 0) {
      return of({});
    }

    const baseUrl = stockUrl.split('{')[0];
    const url = new URL(baseUrl);
    url.searchParams.set('produtoIds', productIds.join(','));

    return this.http.get<any>(url.toString()).pipe(
      map(response => {
        const stockMap: { [productId: string]: { [channelKey: string]: number } } = {};
        const productStocks = response?._embedded?.produtoEstoqueResponseDTOList || [];
        for (const item of productStocks) {
          stockMap[item.produtoId] = this.createChannelMap(item.canais);
        }
        return stockMap;
      })
    );
  }

  private enrichProductsWithStock(productsApiResponse: any): Observable<ApiResponseProducts> {
    const productsFromApi = [
      ...(productsApiResponse?._embedded?.produtoDeCorteModelList || []),
      ...(productsApiResponse?._embedded?.produtoDeConsumoDiretoModelList || [])
    ];
    const stockUrl = productsApiResponse?._links?.['estoques-por-produtos']?.href;

    if (productsFromApi.length === 0 || !stockUrl) {
      return of({
        ...productsApiResponse,
        _embedded: { produtos: productsFromApi }
      });
    }

    const productIds = productsFromApi.map((p: Product) => p.id);
    return this.getStocksForProducts(productIds, stockUrl).pipe(
      map(allStocks => {
        const mergedProducts = productsFromApi.map((product: Product) => ({
          ...product,
          estoquePorCanal: allStocks[product.id] || {},
        }));
        return {
          ...productsApiResponse,
          _embedded: { produtos: mergedProducts }
        };
      }),
      catchError(() => {
        console.warn('Falha ao buscar estoque para produtos, retornando produtos sem estoque por canal.');
        return of({
          ...productsApiResponse,
          _embedded: { produtos: productsFromApi }
        });
      })
    );
  }

  private sortAndPaginateClientSide(response: ApiResponseProducts, params: { size: number, sort: string }): ApiResponseProducts {
    const products = response._embedded.produtos;
    const [sortField, sortOrder] = params.sort.split(',');

    products.sort((a: any, b: any) => {
      const getNestedValue = (obj: any, path: string) => path.split('.').reduce((o, key) => o && o[key], obj);
      const valueA = getNestedValue(a, sortField);
      const valueB = getNestedValue(b, sortField);

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      if (valueA < valueB) return sortOrder === 'asc' ? -1 : 1;
      if (valueA > valueB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    const totalElementsCombined = products.length;
    const totalPagesCombined = Math.ceil(totalElementsCombined / params.size);

    return {
      _embedded: { produtos: products },
      _links: response._links,
      page: {
        size: params.size,
        totalElements: totalElementsCombined,
        totalPages: totalPagesCombined,
        number: response.page?.number ?? 0
      }
    } as ApiResponseProducts;
  }

  private mapToPayload(product: Partial<Product>): any {
    const payload: any = { ...product };
    if (payload.materiaPrima) {
      payload.tipoMateriaPrimaId = payload.materiaPrima.id;
      delete payload.materiaPrima;
    }
    return payload;
  }

  private createChannelMap(channels: Channel[]): { [key: string]: number } {
    return Object.fromEntries(channels.map(channel => [channel.canalNome, channel.quantidade]));
  }

  private getProductBaseUrl(endpoints: Hateoas): string {
    const url = endpoints?._links?.['produtos']?.href;
    if (!url) {
      throw new Error('URL de produtos não encontrada na resposta da API');
    }
    return url.split('{')[0];
  }

  private createEmptyResponse(): ApiResponseProducts {
    return { _embedded: { produtos: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } };
  }
}
