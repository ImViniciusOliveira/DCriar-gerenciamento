import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseBatches, Batch, BatchRequest } from '../models/batch.model';

/**
 * Serviço para gerenciamento de Lotes de Matéria-Prima.
 *
 * Implementa uma arquitetura reativa com Signals para gerenciar o estado da busca
 * (filtros, paginação) e atualiza a lista de lotes automaticamente.
 */
@Injectable({ providedIn: 'root' })
export class BatchService {
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
    sort: 'id,asc'
  }, {
    // Evita disparar nova busca se os parâmetros forem idênticos
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  /**
   * Observable reativo que emite a lista de lotes.
   * Atualiza automaticamente quando os parâmetros de busca mudam ou um refresh é acionado.
   */
  readonly batches$: Observable<ApiResponseBatches>;

  constructor() {
    this.batches$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['lotes-materia-prima']?.href;
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

            return this.http.get<ApiResponseBatches>(baseUrl, { params: httpParams }).pipe(
              catchError(err => {
                console.error('Erro ao buscar lotes de matéria-prima', err);
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

  getNewTemplate(): Observable<Batch> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<Batch>(`${baseUrl}/new`))
    );
  }

  findByUrl(url: string): Observable<Batch> {
    return this.http.get<Batch>(url);
  }

  /**
   * Cria um novo lote.
   * @param request
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  create(request: BatchRequest, skipRefresh = false): Observable<Batch> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<Batch>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Atualiza um lote existente.
   * @param url
   * @param request
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  update(url: string, request: BatchRequest, skipRefresh = false): Observable<Batch> {
    return this.http.put<Batch>(url, request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Remove um lote.
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  private getBaseUrl(): Observable<string> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const url = endpoints._links?.['lotes-materia-prima']?.href;
        if (!url) {
          throw new Error('URL de lotes-materia-prima não encontrada na resposta da API raiz.');
        }
        return url.split('{')[0];
      })
    );
  }

  private createEmptyResponse(): ApiResponseBatches {
    return {
      _embedded: { 'lotes-materia-prima': [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }
}
