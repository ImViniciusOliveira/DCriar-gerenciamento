import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap, throwError } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseSales, Sale, SaleLocationConfig, SaleRequest } from '../models/sales.model';

/**
 * Serviço para gerenciamento de Vendas.
 *
 * Implementa uma arquitetura reativa com Signals para gerenciar o estado da busca
 * (filtros, paginação) e atualiza a lista de vendas automaticamente.
 */
@Injectable({
  providedIn: 'root'
})
export class SalesService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  private readonly searchParams = signal<{
    page: number;
    size: number;
    sort: string;
  }>({
    page: 0,
    size: 10,
    sort: 'dataCriacao,desc' // Ordenação padrão por data decrescente
  }, {
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  /**
   * Observable reativo que emite a lista de vendas.
   * Atualiza automaticamente quando os parâmetros de busca mudam ou um refresh é acionado.
   */
  readonly sales$: Observable<ApiResponseSales>;

  constructor() {
    this.sales$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['vendas']?.href;
        if (!url) {
          return of(this.createEmptyResponse());
        }
        const baseUrl = url.split('{')[0];

        return combineLatest([
          this.searchParams$,
          this.refresh$
        ]).pipe(
          switchMap(([params, _]) => {
            const httpParams = new HttpParams()
              .set('page', params.page.toString())
              .set('size', params.size.toString())
              .set('sort', params.sort);

            return this.http.get<ApiResponseSales>(baseUrl, { params: httpParams }).pipe(
              catchError(() => of(this.createEmptyResponse()))
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  /**
   * Atualiza os parâmetros de busca, disparando uma nova requisição.
   */
  updateSearchParams(params: Partial<{ page: number; size: number; sort: string; }>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  getNewTemplate(): Observable<Sale> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<Sale>(`${baseUrl}/new`))
    );
  }

  getLocationConfig(country?: string | null): Observable<SaleLocationConfig> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => {
        const pais = String(country ?? '').trim();
        const params = pais ? new HttpParams().set('pais', pais) : undefined;
        return this.http.get<SaleLocationConfig>(`${baseUrl}/localidade-config`, { params });
      })
    );
  }

  findByUrl(url: string): Observable<Sale> {
    return this.http.get<Sale>(this.normalizeUrl(url));
  }

  /**
   * Cria uma nova venda.
   * @param request
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  create(request: SaleRequest, skipRefresh = false): Observable<Sale> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<Sale>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Atualiza uma venda existente.
   * @param url
   * @param request
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  update(url: string, request: SaleRequest, skipRefresh = false): Observable<Sale> {
    return this.http.put<Sale>(this.normalizeUrl(url), request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Remove uma venda.
   * Trata o erro 404 (Not Found) como sucesso, pois o objetivo é que o recurso não exista.
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(this.normalizeUrl(url)).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          // Se já não existe, consideramos sucesso.
          return of(undefined);
        }
        return throwError(() => error);
      }),
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  private getBaseUrl(): Observable<string> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const url = endpoints._links?.['vendas']?.href;
        if (!url) {
          throw new Error('URL de vendas não encontrada na resposta da API raiz.');
        }
        return url.split('{')[0];
      })
    );
  }

  private createEmptyResponse(): ApiResponseSales {
    return {
      _embedded: { vendas: [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }

  private normalizeUrl(url: string): string {
    return url.split('{')[0];
  }
}
