import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap, throwError } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseProduction, ProductionOrder } from '../models/production.model';

/**
 * Serviço para gerenciamento de Ordens de Produção.
 *
 * Implementa uma arquitetura reativa com Signals para gerenciar o estado da busca
 * e atualiza a lista de ordens automaticamente.
 */
@Injectable({
  providedIn: 'root'
})
export class ProductionService {
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
   * Observable reativo que emite a lista de ordens de produção.
   * Atualiza automaticamente quando os parâmetros de busca mudam ou um refresh é acionado.
   */
  readonly productionOrders$: Observable<ApiResponseProduction>;

  constructor() {
    this.productionOrders$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        // O link 'ordens-de-producao' deve estar presente na raiz da API
        // Conforme visto no OrdemDeProducaoModelAssembler: .withRel("ordens-de-producao")
        const url = endpoints._links?.['ordens-de-producao']?.href;
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

            return this.http.get<ApiResponseProduction>(baseUrl, { params: httpParams }).pipe(
              catchError(err => {
                console.error('Erro ao buscar ordens de produção', err);
                return of(this.createEmptyResponse());
              })
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

  /**
   * Remove uma ordem de produção.
   * Trata o erro 404 (Not Found) como sucesso (idempotência).
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(undefined);
        }
        return throwError(() => error);
      }),
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  // TODO: Implementar métodos de criação (createCorte, createConsumoDireto) e simulação
  // quando formos implementar o formulário. Por enquanto, focamos na listagem.

  private createEmptyResponse(): ApiResponseProduction {
    return {
      _embedded: { ordensDeProducao: [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }
}
