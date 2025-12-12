import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, take, map, combineLatest, of, catchError, tap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseVendas, Venda, VendaRequest } from '../models/sales.model';

@Injectable({
  providedIn: 'root'
})
export class SalesService {
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
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  readonly sales$: Observable<ApiResponseVendas>;

  constructor() {
    this.sales$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['vendas']?.href;
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

            return this.http.get<ApiResponseVendas>(baseUrl, { params: httpParams }).pipe(
              catchError(err => {
                console.error('Erro ao buscar vendas', err);
                return of(this.createEmptyResponse());
              })
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  updateSearchParams(params: Partial<{ page: number; size: number; sort: string; }>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  getNewTemplate(): Observable<Venda> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.get<Venda>(`${baseUrl}/new`))
    );
  }

  findByUrl(url: string): Observable<Venda> {
    return this.http.get<Venda>(url);
  }

  create(request: VendaRequest, skipRefresh = false): Observable<Venda> {
    return this.getBaseUrl().pipe(
      switchMap(baseUrl => this.http.post<Venda>(baseUrl, request)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  update(url: string, request: VendaRequest, skipRefresh = false): Observable<Venda> {
    return this.http.put<Venda>(url, request).pipe(
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  private getBaseUrl(): Observable<string> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const url = endpoints._links?.['vendas']?.href;
        if (!url) {
          throw new Error('URL de vendas não encontrada na resposta da API raiz.');
        }
        return url.split('{')[0];
      })
    );
  }

  private createEmptyResponse(): ApiResponseVendas {
    return {
      _embedded: { vendaModelList: [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }
}
