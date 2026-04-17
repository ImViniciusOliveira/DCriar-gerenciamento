import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, forkJoin, map, Observable, shareReplay, switchMap, take } from 'rxjs';

import { Hateoas } from '../../../core/models/hateoas.model';
import { ApiRoot } from '../../../core/services/api-root';
import { environment } from '../../../core/services/environment';
import {
  DashboardDateRange,
  DashboardSalesSummary,
  SalesAnalysisByChannelResponse,
  SalesAnalysisByProductResponse,
  SalesAnalysisSeriesResponse,
  SalesAnalysisTotalsResponse
} from '../models/dashboard-sales.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardSalesService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is Hateoas => !!endpoints),
    shareReplay(1)
  );

  loadSummary(range: DashboardDateRange): Observable<DashboardSalesSummary> {
    return this.getSalesAnalysisBaseUrl().pipe(
      switchMap(baseUrl => {
        const dateParams = this.buildDateParams(range);

        return forkJoin({
          totals: this.http.get<SalesAnalysisTotalsResponse>(`${baseUrl}/totais`, {
            params: dateParams
          }),
          leadingChannel: this.http.get<SalesAnalysisByChannelResponse>(`${baseUrl}/por-canal`, {
            params: dateParams
              .set('sort', 'receita,desc')
              .set('page', '0')
              .set('size', '1')
          }),
          topProducts: this.http.get<SalesAnalysisByProductResponse>(`${baseUrl}/por-produto`, {
            params: dateParams
              .set('sort', 'receita,desc')
              .set('page', '0')
              .set('size', '5')
          }),
          series: this.http.get<SalesAnalysisSeriesResponse>(`${baseUrl}/serie-temporal`, {
            params: dateParams
          })
        });
      }),
      map(({ totals, leadingChannel, topProducts, series }) => {
        const firstChannel = leadingChannel._embedded?.['vendas-analise-por-canal']?.[0] ?? null;
        const topProductItems = topProducts._embedded?.['vendas-analise-por-produto'] ?? [];

        return {
          revenueTotal: totals.receita ?? 0,
          orderCount: totals.totalPedidos ?? 0,
          revenuePeriodAnterior: totals.receitaPeriodoAnterior ?? 0,
          leadingChannelName: firstChannel?.nomeCanal ?? 'Sem vendas',
          leadingChannelHelper: firstChannel
            ? `${firstChannel.totalPedidos ?? 0} pedido(s) no período`
            : 'Nenhuma venda no período selecionado.',
          comparisonDeltaPercent: totals.deltaPercent ?? null,
          trendLabel: series.trendLabel,
          series: (series.serie ?? []).map(point => ({
            label: point.label,
            helperLabel: point.helperLabel ?? null,
            revenue: point.receita ?? 0,
            orderCount: point.totalPedidos ?? 0
          })),
          topProducts: topProductItems.map(item => ({
            productId: item.produtoId,
            name: item.nomeProduto,
            sku: item.skuProduto,
            revenue: item.receita ?? 0,
            unitsSold: item.unidadesVendidas ?? 0
          }))
        };
      })
    );
  }

  private getSalesAnalysisBaseUrl(): Observable<string> {
    return this.endpoints$.pipe(
      take(1),
      map(endpoints => {
        const salesUrl = endpoints._links?.['vendas']?.href;
        if (!salesUrl) {
          return `${environment.apiVersionPath}/vendas/analise`;
        }

        return `${this.normalizeUrl(salesUrl)}/analise`;
      })
    );
  }

  private buildDateParams(range: DashboardDateRange): HttpParams {
    return new HttpParams()
      .set('dataInicio', range.dataInicio)
      .set('dataFim', range.dataFim);
  }

  private normalizeUrl(url: string): string {
    return url.split('{')[0].replace(/\/$/, '');
  }
}
