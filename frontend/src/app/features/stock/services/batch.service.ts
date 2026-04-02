import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import {
  ApiResponseBatchMovements,
  ApiResponseBatches,
  Batch,
  BatchAdjustmentApplyRequest,
  BatchAdjustmentCalculateRequest,
  BatchAdjustmentCalculateResponse,
  BatchRequest
} from '../models/batch.model';

type BatchSearchParams = {
  page: number;
  size: number;
  sort: string;
  tipoMateriaPrimaId: number | null;
  nome: string | null;
};

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

  private readonly initialSearchParams: BatchSearchParams = {
    page: 0,
    size: 10,
    sort: 'tipoMateriaPrima.nome,asc',
    tipoMateriaPrimaId: null,
    nome: null,
  };

  private readonly searchParams = signal<BatchSearchParams>(this.initialSearchParams, {
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
    this.batches$ = this.createBatchesObservable(this.searchParams$);
  }

  /**
   * Atualiza os parâmetros de busca, o que dispara uma nova emissão no `batches$`.
   */
  updateSearchParams(params: Partial<BatchSearchParams>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  resetSearchParams(): void {
    this.searchParams.set(this.initialSearchParams);
  }

  search(params: Partial<BatchSearchParams>): Observable<ApiResponseBatches> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => {
        const resolvedParams: BatchSearchParams = {
          ...this.initialSearchParams,
          ...params
        };

        let httpParams = new HttpParams()
          .set('page', resolvedParams.page.toString())
          .set('size', resolvedParams.size.toString())
          .set('sort', resolvedParams.sort);

        if (resolvedParams.tipoMateriaPrimaId) {
          httpParams = httpParams.set('tipoMateriaPrimaId', resolvedParams.tipoMateriaPrimaId.toString());
        }
        if (resolvedParams.nome) {
          httpParams = httpParams.set('nome', resolvedParams.nome);
        }

        return this.http.get<ApiResponseBatches>(baseUrl, { params: httpParams }).pipe(
          catchError(() => of(this.createEmptyResponse()))
        );
      })
    );
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
    return this.http.get<Batch>(this.normalizeUrl(url));
  }

  findById(batchId: number | string): Observable<Batch> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<Batch>(`${baseUrl}/${batchId}`))
    );
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
    return this.http.put<Batch>(this.normalizeUrl(url), request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  calculateAdjustment(url: string, request: BatchAdjustmentCalculateRequest): Observable<BatchAdjustmentCalculateResponse> {
    return this.http.post<BatchAdjustmentCalculateResponse>(this.normalizeUrl(url), request);
  }

  applyAdjustment(url: string, request: BatchAdjustmentApplyRequest, skipRefresh = false): Observable<Batch> {
    return this.http.post<Batch>(this.normalizeUrl(url), request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  findMovements(url: string): Observable<ApiResponseBatchMovements> {
    return this.http.get<ApiResponseBatchMovements>(this.normalizeUrl(url));
  }

  /**
   * Remove um Lote de Matéria-Prima pela sua URL.
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(this.normalizeUrl(url)).pipe(
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

  private createBatchesObservable(params$: Observable<BatchSearchParams>): Observable<ApiResponseBatches> {
    return this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['lotes-materia-prima']?.href;
        if (!url) {
          return of(this.createEmptyResponse());
        }

        const baseUrl = url.split('{')[0];

        return combineLatest([
          params$,
          this.refresh$
        ]).pipe(
          switchMap(([params, _]) => {
            let httpParams = new HttpParams()
              .set('page', params.page.toString())
              .set('size', params.size.toString())
              .set('sort', params.sort);

            if (params.tipoMateriaPrimaId) {
              httpParams = httpParams.set('tipoMateriaPrimaId', params.tipoMateriaPrimaId.toString());
            }
            if (params.nome) {
              httpParams = httpParams.set('nome', params.nome);
            }

            return this.http.get<ApiResponseBatches>(baseUrl, { params: httpParams }).pipe(
              catchError(() => of(this.createEmptyResponse()))
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  private normalizeUrl(url: string): string {
    return url.split('{')[0];
  }
}
