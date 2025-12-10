import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, filter, map, switchMap, tap, shareReplay, take, catchError, of } from 'rxjs';
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

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  getProducts(page: number = 0, size: number = 10, sort: string = 'nome,ASC'): Observable<ApiResponseProducts> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const productsRootUrl = endpoints._links?.['produtos']?.href;
        if (!productsRootUrl) {
          throw new Error('URL de produtos não encontrada na resposta da API raiz.');
        }
        return productsRootUrl.split('{')[0];
      }),
      switchMap(baseUrl => {
        const finalUrl = `${baseUrl}?page=${page}&size=${size}&sort=${sort}`;
        // A API agora retorna duas listas de produtos, então precisamos ajustar o tipo de retorno esperado.
        return this.http.get<any>(finalUrl).pipe(
          map(response => {
            const embedded = response._embedded;
            const products = [
              ...(embedded?.produtoDeCorteModelList || []),
              ...(embedded?.produtoDeConsumoDiretoModelList || [])
            ];

            // Ordena a lista combinada no lado do cliente para garantir a consistência.
            const [sortField, sortOrder] = sort.split(',');
            products.sort((a, b) => {
              // Função auxiliar para acessar propriedades aninhadas (ex: 'dimensoes.larguraCm')
              const getNestedValue = (obj: any, path: string) => path.split('.').reduce((o, key) => o && o[key], obj);

              const valueA = getNestedValue(a, sortField);
              const valueB = getNestedValue(b, sortField);

              // Lida com valores nulos ou indefinidos para que fiquem no final
              if (valueA == null) return 1;
              if (valueB == null) return -1;

              if (valueA < valueB) {
                return sortOrder === 'asc' ? -1 : 1;
              }
              if (valueA > valueB) {
                return sortOrder === 'asc' ? 1 : -1;
              }
              return 0;
            });

            // Recalcula os dados de paginação com base na lista combinada.
            const totalElementsCombined = products.length;
            const totalPagesCombined = Math.ceil(totalElementsCombined / size);

            // Remontamos a resposta no formato que a aplicação espera (ApiResponseProducts)
            // e atualizamos os dados de paginação.
            return {
              _embedded: { produtos: products },
              _links: response._links,
              page: {
                size: size,
                totalElements: totalElementsCombined,
                totalPages: totalPagesCombined,
                number: page
              }
            } as ApiResponseProducts;
          })
        );
      }),
      catchError(err => {
        console.error(`Falha ao buscar produtos na página ${page}, tamanho ${size}`, err);
        // Em caso de erro, retorna uma resposta vazia para não quebrar a UI.
        return of({ _embedded: { produtos: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } } as ApiResponseProducts);
      })
    );
  }

  getProductByUrl(url: string): Observable<Product> {
    return this.http.get<Product>(url);
  }

  getNewProductTemplate(): Observable<Product> {
    return this.endpoints$.pipe(
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.get<Product>(`${baseUrl}/new`)),
      take(1)
    );
  }
  deleteProduct(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refresh$.next())
    );
  }
  createProduct(product: Partial<Product>): Observable<Product> {
    const payload = this.mapToPayload(product);
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(url => this.http.post<Product>(url, payload)),
      tap(() => this.refresh$.next())
    );
  }

  patchProduct(productId: number, product: Partial<Product>): Observable<Product> {
    delete product.id;
    const payload = this.mapToPayload(product);

    return this.endpoints$.pipe(
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.patch<Product>(`${baseUrl}/${productId}`, payload)),
      tap(() => this.refresh$.next()),
      take(1)
    );
  }

  uploadProductPhoto(uploadUrl: string, file: File): Observable<Product> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Product>(uploadUrl, formData)
      .pipe(tap(() => this.refresh$.next()));
  }

  /**
   * Busca o estoque por canal para uma lista de IDs de produtos.
   * @param productIds Um array com os IDs dos produtos.
   * @param stockUrl A URL completa do endpoint para buscar os estoques.
   * @returns Um Observable com um mapa de [productId] para seu mapa de estoque por canal.
   */
  getStocksForProducts(productIds: number[], stockUrl: string): Observable<{ [productId: string]: { [channelKey: string]: number } }> {
    if (productIds.length === 0) {
      return of({});
    }

    // Remove a parte do template da URL, que pode ou não existir.
    const baseUrl = stockUrl.split('{')[0];
    // Usa o objeto URL para adicionar os parâmetros de forma segura,
    // evitando duplicatas de '?'
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

  /**
   * Mapeia um objeto de produto para o payload esperado pela API.
   */
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
}
