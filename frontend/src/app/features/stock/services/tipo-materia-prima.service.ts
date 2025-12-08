import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, map, switchMap, shareReplay, take, of } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseTipoMateriaPrima, TipoMateriaPrima, TipoMateriaPrimaRequest } from '../models/tipo-materia-prima.model';

@Injectable({
  providedIn: 'root'
})
export class TipoMateriaPrimaService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  findAll(
    page: number,
    size: number,
    sort: string,
    order: string,
    nome?: string,
    unidadeDeConsumo?: string
  ): Observable<ApiResponseTipoMateriaPrima> {
    return this.endpoints$.pipe(
      take(1),
      switchMap(endpoints => {
        const url = endpoints._links?.['tipos-materia-prima']?.href; // Corrigido para kebab-case
        if (!url) {
          console.error('URL de tipos-materia-prima não encontrada na resposta da API raiz.');
          // Adicionado _links para satisfazer o tipo
          return of({ _embedded: { tiposMateriaPrima: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } } as ApiResponseTipoMateriaPrima);
        }

        const baseUrl = url.split('{')[0];
        let params = new HttpParams()
          .set('page', page.toString())
          .set('size', size.toString())
          .set('sort', `${sort},${order}`);

        if (nome) {
          params = params.set('nome', nome);
        }
        if (unidadeDeConsumo) {
          params = params.set('unidadeDeConsumo', unidadeDeConsumo);
        }

        return this.http.get<ApiResponseTipoMateriaPrima>(baseUrl, { params });
      })
    );
  }

  // NOTE: The methods below still use a hardcoded path.
  // They should also be updated to use the HATEOAS links from the response
  // when their functionality is fully implemented.

  findById(id: number): Observable<TipoMateriaPrima> {
    const apiUrl = '/api/v1/tipos-materia-prima'; // Placeholder
    return this.http.get<TipoMateriaPrima>(`${apiUrl}/${id}`);
  }

  create(request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    const apiUrl = '/api/v1/tipos-materia-prima'; // Placeholder
    return this.http.post<TipoMateriaPrima>(apiUrl, request);
  }

  update(id: number, request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    const apiUrl = '/api/v1/tipos-materia-prima'; // Placeholder
    return this.http.put<TipoMateriaPrima>(`${apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    const apiUrl = '/api/v1/tipos-materia-prima'; // Placeholder
    return this.http.delete<void>(`${apiUrl}/${id}`);
  }
}
