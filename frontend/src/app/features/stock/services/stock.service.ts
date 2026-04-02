import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { catchError, filter, of, shareReplay, switchMap, Observable } from 'rxjs';

import { Hateoas } from '../../../core/models/hateoas.model';
import { ApiRoot } from '../../../core/services/api-root';
import { environment } from '../../../core/services/environment';
import { ApiResponseStockHistory, ApiResponseStockMovementTypes, StockMovementTypeOption } from '../models/stock-history.model';

type StockHistoryPeriod = '1d' | '1m' | '6m' | '1a' | 'all';

type StockHistorySearchParams = {
  page: number;
  size: number;
  sort: string;
  periodo: StockHistoryPeriod;
  nomeProduto: string;
  tipoMovimentacao: string;
};

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly initialHistorySearchParams: StockHistorySearchParams = {
    page: 0,
    size: 10,
    sort: 'data,desc',
    periodo: '1d',
    nomeProduto: '',
    tipoMovimentacao: ''
  };

  private readonly historySearchParams = signal<StockHistorySearchParams>(this.initialHistorySearchParams, {
    equal: (a, b) =>
      a.page === b.page &&
      a.size === b.size &&
      a.sort === b.sort &&
      a.periodo === b.periodo &&
      a.nomeProduto === b.nomeProduto &&
      a.tipoMovimentacao === b.tipoMovimentacao
  });
  private readonly historyRefreshVersion = signal(0);

  private readonly historySearchParams$ = toObservable(this.historySearchParams);
  private readonly historyRefreshVersion$ = toObservable(this.historyRefreshVersion);
  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is Hateoas => !!endpoints),
    shareReplay(1)
  );

  readonly movementTypes$: Observable<StockMovementTypeOption[]> = this.http
    .get<ApiResponseStockMovementTypes>(`${environment.apiUrl}/api/v1/enums/product/tipos-movimentacao-produto`)
    .pipe(
      switchMap(response => of(response._embedded?.tiposMovimentacaoProduto ?? [])),
      catchError(() => of([])),
      shareReplay(1)
    );

  readonly history$: Observable<ApiResponseStockHistory> = this.endpoints$.pipe(
    switchMap(endpoints => {
      const stockRootUrl = endpoints._links?.['estoques']?.href;
      if (!stockRootUrl) {
        return of(this.createEmptyHistoryResponse());
      }

      return this.http.get<Hateoas>(this.normalizeUrl(stockRootUrl)).pipe(
        switchMap(stockRoot => {
          const historyUrl = stockRoot._links?.['historico']?.href;
          if (!historyUrl) {
            return of(this.createEmptyHistoryResponse());
          }

          const baseUrl = this.normalizeUrl(historyUrl);

          return this.historyRefreshVersion$.pipe(
            switchMap(() =>
              this.historySearchParams$.pipe(
                switchMap(params => {
                  const httpParams = new HttpParams()
                    .set('page', params.page.toString())
                    .set('size', params.size.toString())
                    .set('sort', params.sort)
                    .set('periodo', params.periodo)
                    .set('nomeProduto', params.nomeProduto)
                    .set('tipoMovimentacao', params.tipoMovimentacao);

                  return this.http.get<ApiResponseStockHistory>(baseUrl, { params: httpParams }).pipe(
                    catchError(() => of(this.createEmptyHistoryResponse()))
                  );
                })
              )
            )
          );
        }),
        catchError(() => of(this.createEmptyHistoryResponse()))
      );
    }),
    shareReplay(1)
  );

  getHistory() {
    return this.history$;
  }

  getMovementTypes() {
    return this.movementTypes$;
  }

  updateHistorySearchParams(params: Partial<StockHistorySearchParams>): void {
    this.historySearchParams.update(current => ({ ...current, ...params }));
  }

  refreshHistory(): void {
    this.historyRefreshVersion.update(current => current + 1);
  }

  private createEmptyHistoryResponse(): ApiResponseStockHistory {
    return {
      _embedded: {
        historicoEstoqueConsolidadoResponseDTOList: []
      },
      _links: {},
      page: {
        size: 0,
        totalElements: 0,
        totalPages: 0,
        number: 0
      }
    };
  }

  private normalizeUrl(url: string): string {
    return url.split('{')[0];
  }
}
