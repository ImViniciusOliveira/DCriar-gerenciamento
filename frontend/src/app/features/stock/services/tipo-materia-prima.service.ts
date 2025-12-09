import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, map, switchMap, take, shareReplay } from 'rxjs';
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

  findAll(
    page: number,
    size: number,
    sort: string,
    order: string,
    nome?: string,
    unidadeDeConsumo?: string
  ): Observable<ApiResponseTipoMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => {
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

  getNewTemplate(): Observable<TipoMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<TipoMateriaPrima>(`${baseUrl}/new`))
    );
  }

  create(request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<TipoMateriaPrima>(baseUrl, request))
    );
  }

  delete(url: string): Observable<void> {
    return this.http.delete<void>(url);
  }

  update(url: string, request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    return this.http.patch<TipoMateriaPrima>(url, request);
  }

  findByUrl(url: string): Observable<TipoMateriaPrima> {
    return this.http.get<TipoMateriaPrima>(url);
  }

  findById(id: number): Observable<TipoMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<TipoMateriaPrima>(`${baseUrl}/${id}`))
    );
  }
}
