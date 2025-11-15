import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, filter, map, switchMap, tap, shareReplay, take, catchError, of } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { Hateoas } from '../../../core/models/hateoas.model';
import { ApiResponseProducts, Product } from '../models/products.model';
import { Channel, ProductChannelStock } from '../../stock/models/channel-stock.model';
import { SalesChannelService } from './sales-channel.service';

@Injectable({
  providedIn: 'root',
})
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);
  private readonly salesChannelService = inject(SalesChannelService);

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
        return this.http.get<ApiResponseProducts>(finalUrl);
      }),
      catchError(err => {
        console.error(`Falha ao buscar produtos na página ${page}, tamanho ${size}`, err);
        // Em caso de erro, retorna uma resposta vazia para não quebrar a UI.
        return of({ _embedded: { produtos: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } } as ApiResponseProducts);
      })
    );
  }

  getNewProductTemplate(): Observable<Product> {
    return this.endpoints$.pipe(
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.get<Product>(`${baseUrl}/new`)),
      take(1)
    );
  }

  getProductById(id: number): Observable<Product> {
    return this.endpoints$.pipe(
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.get<Product>(`${baseUrl}/${id}`)),
      take(1)
    );
  }

  deleteProduct(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refresh$.next())
    );
  }

  updateProduct(url: string, product: Product): Observable<Product> {
    return this.http.put<Product>(url, product);
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
    if (!product.id) {
      delete product.id;
    }

    return this.endpoints$.pipe(
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.patch<Product>(`${baseUrl}/${productId}`, product)),
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
   * Busca o estoque por canal para um produto específico.
   */
  getChannelStock(product: Product): Observable<{ [key: string]: number }> {
    const stockUrl = product._links?.['estoque-por-canal']?.href;
    if (!stockUrl) {
      return of({});
    }

    return this.http.get<ProductChannelStock>(stockUrl).pipe(
      map(response => {
        const stockEntries = response?.canais || [];
        if (stockEntries.length === 0) {
          return {};
        }
        return this.createChannelMap(stockEntries);
      }),
      catchError(err => {
        console.error(`Erro ao buscar estoque para o produto ID ${product.id}:`, err);
        return of({});
      })
    );
  }

  private getProductBaseUrl(endpoints: Hateoas): string {
    const url = endpoints?._links?.['produtos']?.href;
    if (!url) {
      throw new Error('URL de produtos não encontrada na resposta da API');
    }
    return url.split('{')[0];
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

    if (payload.dimensoes) {
      payload.dimensoesUnitarias = payload.dimensoes;
      delete payload.dimensoes;
    }

    return payload;
  }

  private createChannelMap(channels: Channel[]): { [key: string]: number } {
    const finalMap = channels.reduce((acc, channel) => {
      acc[channel.canalNome] = channel.quantidade;
      return acc;
    }, {} as { [key: string]: number });

    return finalMap;
  }
}
