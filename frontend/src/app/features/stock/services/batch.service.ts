import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseBatches, Batch, BatchRequest } from '../models/batch.model';

/**
 * Serviço responsável pelo gerenciamento de Lotes de Matéria-Prima.
 * Implementa uma arquitetura reativa para lidar com paginação, filtros e atualizações de dados,
 * centralizando a lógica de comunicação com a API para esta entidade.
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
    tipoMateriaPrimaId: number | null;
    nome: string | null;
  }>({
    page: 0,
    size: 10,
    sort: 'id,asc',
    tipoMateriaPrimaId: null,
    nome: null,
  }, {
    equal: (a, b) =>
      a.page === b.page &&
      a.size === b.size &&
      a.sort === b.sort &&
      a.tipoMateriaPrimaId === b.tipoMateriaPrimaId &&
      a.nome === b.nome
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  /**
   * Observable reativo que emite a lista de Lotes de Matéria-Prima.
   * É acionado sempre que os parâmetros de busca mudam ou um refresh manual é solicitado,
   * mantendo os componentes atualizados automaticamente.
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
            let httpParams = new HttpParams()
              .set('page', params.page.toString())
              .set('size', params.size.toString())
              .set('sort', params.sort);

            // Adicionar os novos filtros se existirem
            if (params.tipoMateriaPrimaId) {
              httpParams = httpParams.set('tipoMateriaPrimaId', params.tipoMateriaPrimaId.toString());
            }
            if (params.nome) {
              httpParams = httpParams.set('nome', params.nome);
            }

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
   * Atualiza os parâmetros de busca, o que dispara uma nova emissão no `batches$`.
   */
  updateSearchParams(params: Partial<{ page: number; size: number; sort: string; tipoMateriaPrimaId: number | null; nome: string | null; }>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  /**
   * Retorna um template HATEOAS para a criação de um novo Lote de Matéria-Prima.
   */
  getNewTemplate(): Observable<Batch> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<Batch>(`${baseUrl}/new`))
    );
  }

  /**
   * Busca um Lote de Matéria-Prima específico pela sua URL completa.
   */
  findByUrl(url: string): Observable<Batch> {
    return this.http.get<Batch>(url);
  }

  /**
   * Cria um novo Lote de Matéria-Prima na API.
   * @param request O payload para a criação.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  create(request: BatchRequest, skipRefresh = false): Observable<Batch> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<Batch>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Atualiza um Lote de Matéria-Prima existente na API.
   * @param url A URL do recurso a ser atualizado.
   * @param request O payload com as alterações.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  update(url: string, request: BatchRequest, skipRefresh = false): Observable<Batch> {
    return this.http.put<Batch>(url, request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Remove um Lote de Matéria-Prima pela sua URL.
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
