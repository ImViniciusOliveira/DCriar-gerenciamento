import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, of } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseLotes } from '../models/lote-materia-prima.model';

@Injectable({ providedIn: 'root' })
export class LoteMateriaPrimaService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  findAll(page = 0, size = 10, sort = 'id', order = 'asc'): Observable<ApiResponseLotes> {
    return this.endpoints$.pipe(
      take(1),
      switchMap(endpoints => {
        const url = endpoints._links?.['lotes-materia-prima']?.href;
        if (!url) {
          console.error('URL de lotes-materia-prima não encontrada na resposta da API raiz.');
          // Adicionado _links para satisfazer o tipo
          return of({ _embedded: { loteMateriaPrimaList: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } } as ApiResponseLotes);
        }

        const baseUrl = url.split('{')[0];
        const params = new HttpParams()
          .set('page', page.toString())
          .set('size', size.toString())
          .set('sort', `${sort},${order}`);

        return this.http.get<ApiResponseLotes>(baseUrl, { params });
      })
    );
  }
}
