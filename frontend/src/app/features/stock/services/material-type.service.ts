import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, map, switchMap, take, shareReplay, tap, combineLatest, of, catchError } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import {
  ApiResponseMaterialTypes,
  MaterialType,
  MaterialTypeRequest
} from '../models/material-type.model';

/**
 * Serviço responsável pelo gerenciamento de Tipos de Matéria-Prima.
 * Implementa uma arquitetura reativa para lidar com paginação, filtros e atualizações de dados,
 * centralizando a lógica de comunicação com a API para esta entidade.
 */
@Injectable({
  providedIn: 'root'
})
export class MaterialTypeService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  private readonly initialSearchParams = {
    page: 0,
    size: 10,
    sort: 'id,asc'
  };

  private readonly searchParams = signal<{
    page: number;
    size: number;
    sort: string;
    nome?: string;
    unidadeDeConsumo?: string;
  }>(this.initialSearchParams, {
    equal: (a, b) =>
      a.page === b.page &&
      a.size === b.size &&
      a.sort === b.sort &&
      a.nome === b.nome &&
      a.unidadeDeConsumo === b.unidadeDeConsumo
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  /**
   * Observable reativo que emite a lista de Tipos de Matéria-Prima.
   * É acionado sempre que os parâmetros de busca mudam ou um refresh manual é solicitado,
   * mantendo os componentes atualizados automaticamente.
   */
  readonly materialTypes$: Observable<ApiResponseMaterialTypes>;

  constructor() {
    this.materialTypes$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['tipos-materia-prima']?.href;
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

            if (params.nome) {
              httpParams = httpParams.set('nome', params.nome);
            }
            if (params.unidadeDeConsumo) {
              httpParams = httpParams.set('unidadeDeConsumo', params.unidadeDeConsumo);
            }

            return this.http.get<any>(baseUrl, { params: httpParams }).pipe(
              map(response => this.normalizeAndSortResponse(response, params)),
              catchError(() => of(this.createEmptyResponse()))
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  /**
   * Retorna o fluxo observável principal de Tipos de Matéria-Prima.
   */
  getMaterialTypes(): Observable<ApiResponseMaterialTypes> {
    return this.materialTypes$;
  }

  searchByProductType(params: {
    tipoProduto: 'CORTE' | 'CONSUMO';
    page: number;
    size: number;
    sort: string;
    nome?: string;
    unidadeDeConsumo?: string;
  }): Observable<ApiResponseMaterialTypes> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => {
        let httpParams = new HttpParams()
          .set('tipoProduto', params.tipoProduto)
          .set('page', params.page.toString())
          .set('size', params.size.toString())
          .set('sort', params.sort);

        if (params.nome) {
          httpParams = httpParams.set('nome', params.nome);
        }
        if (params.unidadeDeConsumo) {
          httpParams = httpParams.set('unidadeDeConsumo', params.unidadeDeConsumo);
        }

        return this.http.get<any>(baseUrl, { params: httpParams }).pipe(
          map(response => this.normalizeAndSortResponse(response, params)),
          catchError(() => of(this.createEmptyResponse()))
        );
      })
    );
  }

  /**
   * Atualiza os parâmetros de busca, o que dispara uma nova emissão no `materialTypes$`.
   */
  updateSearchParams(params: Partial<{
    page: number;
    size: number;
    sort: string;
    nome: string;
    unidadeDeConsumo: string;
  }>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  /**
   * Reseta os parâmetros de busca para o estado padrão.
   */
  resetSearchParams(): void {
    this.searchParams.set(this.initialSearchParams);
  }

  /**
   * Retorna um template HATEOAS para a criação de um novo Tipo de Matéria-Prima.
   */
  getNewTemplate(): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<MaterialType>(`${baseUrl}/new`))
    );
  }

  /**
   * Cria um novo Tipo de Matéria-Prima na API.
   * @param request O payload para a criação.
   * @param skipRefresh Se true, não dispara a atualização da lista (útil para operações em lote).
   */
  create(request: MaterialTypeRequest, skipRefresh = false): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<MaterialType>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Remove um Tipo de Matéria-Prima pela sua URL.
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  /**
   * Atualiza um Tipo de Matéria-Prima existente na API.
   * @param url A URL do recurso a ser atualizado.
   * @param request O payload com as alterações.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  update(url: string, request: MaterialTypeRequest, skipRefresh = false): Observable<MaterialType> {
    return this.http.patch<MaterialType>(url, request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }
  /**
   * Busca um Tipo de Matéria-Prima específico pelo seu ID.
   */
  findById(id: number): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<MaterialType>(`${baseUrl}/${id}`))
    );
  }

  private normalizeAndSortResponse(response: any, params: { sort: string, size: number }): ApiResponseMaterialTypes {
    const items = response?._embedded?.['tipos-materia-prima'] || response?._embedded?.['tipoMateriaPrimaModelList'] || [];
    const [sortField, sortOrder] = params.sort.split(',');

    items.sort((a: any, b: any) => {
      const getNestedValue = (obj: any, path: string) => path.split('.').reduce((o, key) => o && o[key], obj);
      const valueA = getNestedValue(a, sortField);
      const valueB = getNestedValue(b, sortField);

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      if (valueA < valueB) return sortOrder === 'asc' ? -1 : 1;
      if (valueA > valueB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    return {
      _embedded: { 'tipos-materia-prima': items },
      _links: response._links,
      page: {
        size: params.size,
        totalElements: response.page?.totalElements ?? items.length,
        totalPages: response.page?.totalPages ?? Math.ceil(items.length / params.size),
        number: response.page?.number ?? 0
      }
    };
  }

  private getBaseUrl(): Observable<string> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const url = endpoints._links?.['tipos-materia-prima']?.href;
        if (!url) {
          throw new Error('URL de tipos-materia-prima não encontrada na resposta da API raiz.');
        }
        return url.split('{')[0];
      })
    );
  }

  private createEmptyResponse(): ApiResponseMaterialTypes {
    return {
      _embedded: { 'tipos-materia-prima': [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }
}
