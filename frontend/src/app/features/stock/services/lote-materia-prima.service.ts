import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take } from 'rxjs';
import { map } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseLotes, LoteMateriaPrima, LoteMateriaPrimaRequest } from '../models/lote-materia-prima.model';

@Injectable({ providedIn: 'root' })
export class LoteMateriaPrimaService {
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
        const url = endpoints._links?.['lotes-materia-prima']?.href;
        if (!url) {
          throw new Error('URL de lotes-materia-prima não encontrada na resposta da API raiz.');
        }
        return url.split('{')[0];
      })
    );
  }

  findAll(page = 0, size = 10, sort = 'id', order = 'asc'): Observable<ApiResponseLotes> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => {
        const params = new HttpParams()
          .set('page', page.toString())
          .set('size', size.toString())
          .set('sort', `${sort},${order}`);

        return this.http.get<ApiResponseLotes>(baseUrl, { params });
      })
    );
  }

  getNewTemplate(): Observable<LoteMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<LoteMateriaPrima>(`${baseUrl}/new`))
    );
  }

  findByUrl(url: string): Observable<LoteMateriaPrima> {
    return this.http.get<LoteMateriaPrima>(url);
  }

  create(request: LoteMateriaPrimaRequest): Observable<LoteMateriaPrima> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<LoteMateriaPrima>(baseUrl, request))
    );
  }

  update(url: string, request: LoteMateriaPrimaRequest): Observable<LoteMateriaPrima> {
    return this.http.put<LoteMateriaPrima>(url, request);
  }

  delete(url: string): Observable<void> {
    return this.http.delete<void>(url);
  }
}
