import { CommonModule } from '@angular/common';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, computed, inject, signal, Signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import {
  DASHBOARD_PERIOD_OPTIONS,
  DashboardDateRange,
  DashboardPeriodOption,
  DashboardSalesPeriod,
  DashboardSalesSeriesPoint,
  DashboardSalesSummary,
  DashboardTopProduct,
  EMPTY_DASHBOARD_SALES_SUMMARY
} from '../models/dashboard-sales.model';
import { DashboardSalesService } from '../services/dashboard-sales.service';
import { StockService } from '../../stock/services/stock.service';
import { MaterialStockAnalysisSummary } from '../../stock/models/material-stock-analysis.model';
import { ProductStockAnalysisSummary } from '../../stock/models/product-stock-analysis.model';

interface DashboardQuickAction {
  label: string;
  icon: string;
  route: string;
  queryParams?: Record<string, string>;
}

interface DashboardAlertItem {
  id: number;
  name: string;
  currentAmount: number;
  minimumAmount: number;
  unitLabel: string;
  stockLevelPercent: number;
  helperText: string;
  actionLabel: string;
  route: string;
  queryParams?: Record<string, string>;
  tone: 'warning' | 'critical';
}

interface DashboardEmptyState {
  title: string;
  description?: string;
  tone: 'positive';
}

interface DashboardKpiCard {
  label: string;
  value: number | string;
  type: 'currency' | 'number' | 'text';
  helper: string;
}

interface DashboardSalesSeriesViewModel extends DashboardSalesSeriesPoint {
  revenuePercent: number;
}

type DashboardPeriodSelection = DashboardSalesPeriod | 'custom';

type SalesSectionState =
  | { status: 'loading'; range: DashboardDateRange; summary: null; message: null }
  | { status: 'ready'; range: DashboardDateRange; summary: DashboardSalesSummary; message: null }
  | { status: 'error'; range: DashboardDateRange; summary: null; message: string };

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPage {
  private readonly stockService = inject(StockService);
  private readonly dashboardSalesService = inject(DashboardSalesService);
  private readonly initialSalesSectionState: SalesSectionState = {
    status: 'loading',
    range: this.buildPresetRange('30d'),
    summary: null,
    message: null
  };

  protected readonly periodOptions: readonly DashboardPeriodOption[] = DASHBOARD_PERIOD_OPTIONS;
  protected readonly quickActions: DashboardQuickAction[] = [
    { label: 'Nova venda', icon: 'point_of_sale', route: '/vendas', queryParams: { action: 'create' } },
    { label: 'Novo lote', icon: 'inventory_2', route: '/lotes-materia-prima', queryParams: { action: 'create' } },
    { label: 'Nova produção', icon: 'precision_manufacturing', route: '/ordens-de-producao', queryParams: { action: 'create' } },
    { label: 'Ajustes', icon: 'tune', route: '/estoques' }
  ];

  protected readonly selectedPeriod = signal<DashboardPeriodSelection>('30d');
  protected readonly customRangeOpen = signal(false);
  protected readonly customStartDate = signal('');
  protected readonly customEndDate = signal('');
  protected readonly appliedRange = signal<DashboardDateRange>(this.buildPresetRange('30d'));

  protected readonly maxDateForCustomRange = this.formatDateInput(new Date());
  protected readonly minDateForCustomRange = (() => {
    const minDate = new Date();
    minDate.setFullYear(minDate.getFullYear() - 1);
    return this.formatDateInput(minDate);
  })();

  protected readonly customRangeError = computed(() => {
    const startValue = this.customStartDate();
    const endValue = this.customEndDate();

    if (!startValue && !endValue) {
      return null;
    }

    if (!startValue || !endValue) {
      return 'Selecione a data inicial e final.';
    }

    const startDate = this.parseDateInput(startValue);
    const endDate = this.parseDateInput(endValue);
    if (!startDate || !endDate) {
      return 'Informe um período válido.';
    }

    if (startDate.getTime() > endDate.getTime()) {
      return 'A data final deve ser maior ou igual à inicial.';
    }

    if (this.getInclusiveDayCount(startDate, endDate) > 366) {
      return 'O período personalizado pode ter no máximo 366 dias.';
    }

    return null;
  });

  protected readonly customRangeLabel = computed(() => {
    if (this.selectedPeriod() !== 'custom') {
      return 'Período personalizado';
    }

    const range = this.appliedRange();
    return `${this.formatDateLabel(this.parseDateInput(range.dataInicio))} - ${this.formatDateLabel(this.parseDateInput(range.dataFim))}`;
  });

  private readonly salesSectionState$ = toObservable(this.appliedRange).pipe(
    switchMap(range =>
      this.dashboardSalesService.loadSummary(range).pipe(
        map(summary => ({
          status: 'ready' as const,
          range,
          summary,
          message: null
        })),
        startWith({
          status: 'loading' as const,
          range,
          summary: null,
          message: null
        }),
        catchError(() =>
          of({
            status: 'error' as const,
            range,
            summary: null,
            message: 'Não foi possível carregar a análise de vendas. Tente novamente em instantes.'
          })
        )
      )
    )
  );

  protected readonly salesSectionState = toSignal(
    this.salesSectionState$,
    {
      initialValue: this.initialSalesSectionState
    }
  ) as Signal<SalesSectionState>;

  protected readonly salesSnapshot = computed(() => this.salesSectionState().summary ?? EMPTY_DASHBOARD_SALES_SUMMARY);
  protected readonly salesKpis = computed<DashboardKpiCard[]>(() => {
    const snapshot = this.salesSnapshot();

    return [
      {
        label: 'Faturamento total',
        value: snapshot.revenueTotal,
        type: 'currency',
        helper: 'Receita do período selecionado'
      },
      {
        label: 'Total de vendas',
        value: snapshot.orderCount,
        type: 'number',
        helper: 'Pedidos fechados no período'
      },
      {
        label: 'Canal líder',
        value: snapshot.leadingChannelName,
        type: 'text',
        helper: snapshot.leadingChannelHelper
      }
    ];
  });

  protected readonly summaryTitle = computed(() => {
    const period = this.selectedPeriod();
    if (period === 'custom') {
      return 'Resumo do período personalizado';
    }

    const option = this.periodOptions.find(p => p.key === period);
    if (!option) {
      return 'Resumo do período';
    }

    // Transforma 'Hoje' em 'de hoje', '7 dias' em 'da semana', etc.
    const periodName = option.label
      .replace('Hoje', 'de hoje')
      .replace('7 dias', 'da semana')
      .replace('30 dias', 'do mês')
      .replace('3 meses', 'do trimestre')
      .replace('6 meses', 'do semestre');

    return `Resumo ${periodName}`;
  });

  protected readonly formattedComparison = computed(() => {
    const snapshot = this.salesSnapshot();
    const delta = snapshot.comparisonDeltaPercent;

    if (delta === null) {
      return {
        text: 'Sem base comparativa',
        tooltip: 'Não há dados no período anterior para comparação.'
      };
    }

    const revenueCurrent = snapshot.revenueTotal;
    const revenuePrevious = snapshot.revenuePeriodAnterior;
    const tooltip = `Período atual: ${revenueCurrent.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} | Período anterior: ${revenuePrevious.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}`;

    if (delta < 0) {
      if (delta === -100) {
        return { text: '0x', tooltip };
      }
      return { text: `${delta.toFixed(1)}% menos que o período anterior`, tooltip };
    }

    if (delta <= 100) {
      return { text: `+${delta.toFixed(1)}% mais que o período anterior`, tooltip };
    }

    // delta > 100
    const multiplier = revenueCurrent / revenuePrevious;
    return { text: `${multiplier.toFixed(1)}x mais que período anterior`, tooltip };
  });

  protected readonly salesSeries = computed<DashboardSalesSeriesViewModel[]>(() => {
    const points = this.salesSnapshot().series;
    const maxRevenue = Math.max(...points.map(point => point.revenue), 0);

    return points.map(point => ({
      ...point,
      revenuePercent: maxRevenue === 0 ? 0 : (point.revenue / maxRevenue) * 100
    }));
  });
  protected readonly topProducts = computed<DashboardTopProduct[]>(() => this.salesSnapshot().topProducts);
  protected readonly isSalesLoading = computed(() => this.salesSectionState().status === 'loading');
  protected readonly hasSalesError = computed(() => this.salesSectionState().status === 'error');
  protected readonly salesErrorMessage = computed(() => this.salesSectionState().message);
  protected readonly hasSalesData = computed(() => this.salesSnapshot().orderCount > 0 || this.salesSnapshot().revenueTotal > 0);
  protected readonly hasTopProducts = computed(() => this.topProducts().length > 0);
  protected readonly chartGridTemplate = computed(() => `repeat(${Math.max(this.salesSeries().length, 1)}, minmax(0, 1fr))`);
  protected readonly salesEmptyState: DashboardEmptyState = {
    title: 'Nenhuma venda no período selecionado',
    description: 'Ajuste o intervalo para visualizar faturamento, pedidos e produtos líderes.',
    tone: 'positive'
  };

  private readonly productStockAnalysisResult = toSignal(
    this.stockService.searchProductStockAnalysis({
      page: 0,
      size: 10,
      sort: 'percentualRisco,desc',
      statusAnalise: 'CRITICO'
    }),
    { initialValue: { items: [] as ProductStockAnalysisSummary[], total: 0 } }
  );

  protected readonly productAlerts = computed<DashboardAlertItem[]>(() =>
    this.productStockAnalysisResult().items
      .sort((left, right) => (right.percentualRisco ?? 0) - (left.percentualRisco ?? 0))
      .map(item => this.toProductAlertItem(item))
  );
  protected readonly hasProductAlerts = computed(() => this.productAlerts().length > 0);
  protected readonly productEmptyState: DashboardEmptyState = {
    title: 'Nenhum produto com saldo baixo',
    tone: 'positive'
  };

  private readonly materialStockAnalysisResult = toSignal(
    this.stockService.searchMaterialStockAnalysis({
      page: 0,
      size: 10,
      sort: 'percentualRisco,desc',
      statusAnalise: 'CRITICO',
      politicaSaldoRetalho: 'TODOS'
    }),
    { initialValue: { items: [] as MaterialStockAnalysisSummary[], total: 0 } }
  );

  protected readonly materialAlerts = computed<DashboardAlertItem[]>(() =>
    this.materialStockAnalysisResult().items
      .sort((left, right) => (right.percentualRisco ?? 0) - (left.percentualRisco ?? 0))
      .map(item => this.toMaterialAlertItem(item))
  );
  protected readonly hasMaterialAlerts = computed(() => this.materialAlerts().length > 0);
  protected readonly materialEmptyState: DashboardEmptyState = {
    title: 'Nenhuma matéria-prima com estoque baixo',
    tone: 'positive'
  };

  protected setSelectedPeriod(period: DashboardSalesPeriod): void {
    const range = this.buildPresetRange(period);
    this.selectedPeriod.set(period);
    this.appliedRange.set(range);
    this.customRangeOpen.set(false);
  }

  protected isSelectedPeriod(period: DashboardSalesPeriod): boolean {
    return this.selectedPeriod() === period;
  }

  protected isCustomRangeSelected(): boolean {
    return this.selectedPeriod() === 'custom';
  }

  protected toggleCustomRange(): void {
    if (!this.customRangeOpen()) {
      const today = this.formatDateInput(new Date());
      this.customStartDate.set(today);
      this.customEndDate.set(today);
    }

    this.customRangeOpen.update(current => !current);
  }

  protected setCustomStartDate(value: string): void {
    this.customStartDate.set(value);
  }

  protected setCustomEndDate(value: string): void {
    this.customEndDate.set(value);
  }

  protected applyCustomRange(): void {
    if (this.customRangeError()) {
      return;
    }

    const dataInicio = this.customStartDate();
    const dataFim = this.customEndDate();
    if (!dataInicio || !dataFim) {
      return;
    }

    this.selectedPeriod.set('custom');
    this.appliedRange.set({ dataInicio, dataFim });
    this.customRangeOpen.set(false);
  }

  protected clearCustomRange(): void {
    this.customStartDate.set('');
    this.customEndDate.set('');
    this.customRangeOpen.set(false);

    if (this.selectedPeriod() === 'custom') {
      this.setSelectedPeriod('30d');
    }
  }

  protected isPositiveDelta(delta: number | null): boolean {
    return (delta ?? 0) >= 0;
  }

  private buildPresetRange(period: DashboardSalesPeriod): DashboardDateRange {
    const endDate = this.startOfDay(new Date());
    const startDate = this.startOfDay(new Date(endDate.getTime()));

    switch (period) {
      case '1d':
        break;
      case '7d':
        startDate.setDate(startDate.getDate() - 6);
        break;
      case '30d':
        startDate.setDate(startDate.getDate() - 29);
        break;
      case '90d':
        startDate.setDate(1);
        startDate.setMonth(startDate.getMonth() - 2);
        break;
      case '180d':
        startDate.setDate(1);
        startDate.setMonth(startDate.getMonth() - 5);
        break;
    }

    return {
      dataInicio: this.formatDateInput(startDate),
      dataFim: this.formatDateInput(endDate)
    };
  }

  private startOfDay(date: Date): Date {
    const normalized = new Date(date.getTime());
    normalized.setHours(0, 0, 0, 0);
    return normalized;
  }

  private parseDateInput(value: string): Date | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(`${value}T00:00:00`);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  private formatDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDateLabel(date: Date | null): string {
    if (!date) {
      return '-';
    }

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  private getInclusiveDayCount(startDate: Date, endDate: Date): number {
    const millisecondsPerDay = 1000 * 60 * 60 * 24;
    return Math.floor((endDate.getTime() - startDate.getTime()) / millisecondsPerDay) + 1;
  }

  private toProductAlertItem(item: ProductStockAnalysisSummary): DashboardAlertItem {
    const currentAmount = item.saldoConsiderado ?? 0;
    const minimumAmount = item.estoqueCritico ?? 0;
    const riskPercent = Math.max(0, Math.min(100, item.percentualRisco ?? 0));
    const stockLevelPercent = Math.max(0, 100 - riskPercent);
    const shortageAmount = Math.max(minimumAmount - currentAmount, 0);

    return {
      id: item.produtoId,
      name: item.nomeProduto,
      currentAmount,
      minimumAmount,
      unitLabel: 'un',
      stockLevelPercent,
      helperText: shortageAmount > 0
        ? `Faltam ${shortageAmount.toFixed(0)} un para voltar ao mínimo.`
        : 'Saldo no limite crítico. Avalie nova produção.',
      actionLabel: 'Adicionar saldo em produto',
      route: '/ordens-de-producao',
      queryParams: { action: 'create', produtoId: String(item.produtoId) },
      tone: item.statusAnalise === 'CRITICO' ? 'critical' : 'warning'
    };
  }

  private toMaterialAlertItem(item: MaterialStockAnalysisSummary): DashboardAlertItem {
    const currentAmount = item.saldoConsiderado ?? 0;
    const minimumAmount = item.estoqueCritico ?? 0;
    const unitLabel = item.unidadeSimbolo || item.unidadeDescricao || item.unidadeDeConsumo;
    const riskPercent = Math.max(0, Math.min(100, item.percentualRisco ?? 0));
    const stockLevelPercent = Math.max(0, 100 - riskPercent);
    const shortageAmount = Math.max(minimumAmount - currentAmount, 0);

    return {
      id: item.tipoMateriaPrimaId,
      name: item.nomeTipoMateriaPrima,
      currentAmount,
      minimumAmount,
      unitLabel,
      stockLevelPercent,
      helperText: shortageAmount > 0
        ? `Faltam ${shortageAmount.toFixed(0)} ${unitLabel} para sair da faixa crítica.`
        : 'Saldo no limite crítico. Avalie reposição imediata.',
      actionLabel: 'Adicionar lote de matéria-prima',
      route: '/lotes-materia-prima',
      queryParams: {
        action: 'create',
        tipoMateriaPrimaId: String(item.tipoMateriaPrimaId),
        tipoProduto: item.tipoProdutoCompativel
      },
      tone: item.statusAnalise === 'CRITICO' ? 'critical' : 'warning'
    };
  }
}
