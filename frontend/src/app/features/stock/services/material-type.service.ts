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
 *
 * Implementa o padrão de arquitetura reativa com Signals para gerenciamento de estado
 * de paginação e filtros, além de otimização de requisições (skipRefresh).
 *
 * Padroniza a resposta da API, tratando nomes de propriedades com hífen (ex: 'tipos-materia-prima')
 * e garantindo uma ordenação consistente no lado do cliente.
 */
@Injectable({
  providedIn: 'root'
})
export class MaterialTypeService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  private readonly searchParams = signal<{
    page: number;
    size: number;
    sort: string;
    nome?: string;
    unidadeDeConsumo?: string;
  }>({
    page: 0,
    size: 10,
    sort: 'id,asc'
  }, {
    // Evita disparar nova busca se os parâmetros forem idênticos
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
   * Observable reativo que emite a lista de tipos de matéria-prima.
   * Atualiza automaticamente quando os parâmetros mudam ou o refresh é acionado.
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
              catchError(err => {
                console.error('Erro ao buscar tipos de matéria-prima', err);
                return of(this.createEmptyResponse());
              })
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  getMaterialTypes(): Observable<ApiResponseMaterialTypes> {
    return this.materialTypes$;
  }

  /**
   * Atualiza os parâmetros de busca, disparando uma nova requisição automaticamente.
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

  getNewTemplate(): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<MaterialType>(`${baseUrl}/new`))
    );
  }

  /**
   * Cria um novo tipo de matéria-prima.
   * @param request
   * @param skipRefresh Se true, não atualiza a lista automaticamente (útil para operações em lote).
   */
  create(request: MaterialTypeRequest, skipRefresh = false): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<MaterialType>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  /**
   * Atualiza um registro existente.
   * @param url
   * @param request
   * @param skipRefresh Se true, não atualiza a lista automaticamente.
   */
  update(url: string, request: MaterialTypeRequest, skipRefresh = false): Observable<MaterialType> {
    return this.http.patch<MaterialType>(url, request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  findByUrl(url: string): Observable<MaterialType> {
    return this.http.get<MaterialType>(url);
  }

  findById(id: number): Observable<MaterialType> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<MaterialType>(`${baseUrl}/${id}`))
    );
  }

  // --- Métodos Auxiliares Privados ---

  /**
   * Normaliza a resposta da API e aplica ordenação no cliente.
   * Suporta propriedades aninhadas (ex: 'categoria.nome').
   */
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
