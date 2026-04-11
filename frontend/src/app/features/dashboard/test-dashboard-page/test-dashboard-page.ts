import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import {
  DASHBOARD_MOCK,
  DASHBOARD_PERIOD_OPTIONS,
  DashboardAlertItem,
  DashboardQuickAction,
  DashboardSalesSeriesPoint,
  DashboardSalesPeriod
} from './test-dashboard-page.mock';

interface DashboardAlertViewModel extends DashboardAlertItem {
  progressPercent: number;
  shortageAmount: number;
  tone: 'critical' | 'warning';
}

interface DashboardSalesSeriesViewModel extends DashboardSalesSeriesPoint {
  revenuePercent: number;
}

type DashboardPeriodSelection = DashboardSalesPeriod | 'custom';

@Component({
  selector: 'app-test-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './test-dashboard-page.html',
  styleUrl: './test-dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TestDashboardPage {
  protected readonly periodOptions = DASHBOARD_PERIOD_OPTIONS;
  protected readonly quickActions: readonly DashboardQuickAction[] = DASHBOARD_MOCK.quickActions;
  protected readonly selectedPeriod = signal<DashboardPeriodSelection>('30d');
  protected readonly customRangeOpen = signal(false);
  protected readonly customStartDate = signal('');
  protected readonly customEndDate = signal('');

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
      return 'O período personalizado pode ter no máximo 1 ano.';
    }

    return null;
  });

  protected readonly customRangeLabel = computed(() => {
    if (this.selectedPeriod() !== 'custom') {
      return 'Período personalizado';
    }

    const startValue = this.customStartDate();
    const endValue = this.customEndDate();
    if (!startValue || !endValue) {
      return 'Período personalizado';
    }

    return `${this.formatDateLabel(this.parseDateInput(startValue))} - ${this.formatDateLabel(this.parseDateInput(endValue))}`;
  });

  protected readonly customSalesSnapshot = computed(() => {
    const startValue = this.customStartDate();
    const endValue = this.customEndDate();

    if (!startValue || !endValue || this.customRangeError()) {
      return null;
    }

    const startDate = this.parseDateInput(startValue);
    const endDate = this.parseDateInput(endValue);
    if (!startDate || !endDate) {
      return null;
    }

    return this.buildCustomSnapshot(startDate, endDate);
  });

  protected readonly salesSnapshot = computed(() => {
    const selected = this.selectedPeriod();
    if (selected === 'custom') {
      return this.customSalesSnapshot() ?? DASHBOARD_MOCK.sales.byPeriod['30d'];
    }

    return DASHBOARD_MOCK.sales.byPeriod[selected];
  });
  protected readonly chartGridTemplate = computed(() => {
    const columns = this.salesSnapshot().series.length;
    return `repeat(${columns}, minmax(0, 1fr))`;
  });

  protected readonly salesKpis = computed(() => {
    const snapshot = this.salesSnapshot();

    return [
      {
        label: 'Faturamento total',
        value: snapshot.revenueTotal,
        type: 'currency' as const,
        helper: 'Receita do período'
      },
      {
        label: 'Total de vendas',
        value: snapshot.orderCount,
        type: 'number' as const,
        helper: 'Pedidos fechados no período'
      },
      {
        label: 'Canal líder',
        value: snapshot.leadingChannelName,
        type: 'text' as const,
        helper: snapshot.leadingChannelHelper
      }
    ];
  });

  protected readonly salesSeries = computed<DashboardSalesSeriesViewModel[]>(() => {
    const snapshot = this.salesSnapshot();
    const maxRevenue = Math.max(...snapshot.series.map(point => point.revenue), 0);

    return snapshot.series.map(point => ({
      label: point.label,
      helperLabel: point.helperLabel,
      revenue: point.revenue,
      orderCount: point.orderCount,
      revenuePercent: maxRevenue === 0 ? 0 : Math.max((point.revenue / maxRevenue) * 100, 12)
    }));
  });

  protected readonly productAlerts = computed(() => this.decorateAlerts(DASHBOARD_MOCK.lowStockProducts));
  protected readonly materialAlerts = computed(() => this.decorateAlerts(DASHBOARD_MOCK.criticalMaterials));

  protected setSelectedPeriod(period: DashboardSalesPeriod): void {
    this.selectedPeriod.set(period);
    this.customRangeOpen.set(false);
  }

  protected isSelectedPeriod(period: DashboardSalesPeriod): boolean {
    return this.selectedPeriod() === period;
  }

  protected isCustomRangeSelected(): boolean {
    return this.selectedPeriod() === 'custom';
  }

  protected toggleCustomRange(): void {
    this.customRangeOpen.update(current => !current);
  }

  protected setCustomStartDate(value: string): void {
    this.customStartDate.set(value);
  }

  protected setCustomEndDate(value: string): void {
    this.customEndDate.set(value);
  }

  protected applyCustomRange(): void {
    if (this.customSalesSnapshot()) {
      this.selectedPeriod.set('custom');
      this.customRangeOpen.set(false);
    }
  }

  protected clearCustomRange(): void {
    this.customStartDate.set('');
    this.customEndDate.set('');
    this.customRangeOpen.set(false);

    if (this.selectedPeriod() === 'custom') {
      this.selectedPeriod.set('30d');
    }
  }

  protected isPositiveDelta(delta: number): boolean {
    return delta >= 0;
  }

  private decorateAlerts(items: readonly DashboardAlertItem[]): DashboardAlertViewModel[] {
    return items.map(item => {
      const progressPercent = item.minimumAmount <= 0
        ? 100
        : Math.min((item.currentAmount / item.minimumAmount) * 100, 100);
      const shortageAmount = Math.max(item.minimumAmount - item.currentAmount, 0);
      const ratio = item.minimumAmount <= 0 ? 1 : item.currentAmount / item.minimumAmount;

      return {
        ...item,
        progressPercent,
        shortageAmount,
        tone: ratio <= 0.45 ? 'critical' : 'warning'
      };
    });
  }

  private parseDateInput(value: string): Date | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(`${value}T00:00:00`);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  private getInclusiveDayCount(startDate: Date, endDate: Date): number {
    const millisecondsPerDay = 1000 * 60 * 60 * 24;
    return Math.floor((endDate.getTime() - startDate.getTime()) / millisecondsPerDay) + 1;
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

  private buildCustomSnapshot(startDate: Date, endDate: Date) {
    const totalDays = this.getInclusiveDayCount(startDate, endDate);
    const averageDailyRevenue = 2580;
    const averageDailyOrders = 6.2;
    const revenueTotal = Number((totalDays * averageDailyRevenue).toFixed(2));
    const orderCount = Math.max(Math.round(totalDays * averageDailyOrders), 1);

    return {
      revenueTotal,
      orderCount,
      leadingChannelName: totalDays > 45 ? 'Shopee' : 'Instagram',
      leadingChannelHelper: totalDays > 45 ? 'Maior volume no período' : 'Melhor conversão no período',
      comparisonDeltaPercent: totalDays > 90 ? 11.4 : totalDays > 30 ? 8.9 : 6.7,
      trendLabel: 'Receita no período selecionado',
      series: this.buildCustomSeries(startDate, endDate),
      topProducts: this.buildCustomTopProducts(revenueTotal)
    };
  }

  private buildCustomSeries(startDate: Date, endDate: Date): DashboardSalesSeriesPoint[] {
    const totalDays = this.getInclusiveDayCount(startDate, endDate);
    let segmentCount = 6;

    if (totalDays <= 7) {
      segmentCount = totalDays;
    } else if (totalDays <= 31) {
      segmentCount = 4;
    } else if (totalDays <= 120) {
      segmentCount = 5;
    }

    const series: DashboardSalesSeriesPoint[] = [];
    const currentStart = new Date(startDate.getTime());
    const baseRevenue = 4300;
    const baseOrders = 10;

    for (let index = 0; index < segmentCount; index += 1) {
      const remainingDays = this.getInclusiveDayCount(currentStart, endDate);
      const remainingSegments = segmentCount - index;
      const bucketDays = Math.ceil(remainingDays / remainingSegments);
      const currentEnd = new Date(currentStart.getTime());
      currentEnd.setDate(currentEnd.getDate() + bucketDays - 1);
      if (currentEnd.getTime() > endDate.getTime()) {
        currentEnd.setTime(endDate.getTime());
      }

      const label = segmentCount <= 6
        ? this.formatShortDate(currentStart)
        : `Faixa ${index + 1}`;
      const helperLabel = currentStart.getTime() === currentEnd.getTime()
        ? this.formatShortDate(currentStart)
        : `${this.formatShortDate(currentStart)} a ${this.formatShortDate(currentEnd)}`;
      const revenueMultiplier = 1 + (index * 0.08);
      const orderMultiplier = 1 + (index * 0.05);

      series.push({
        label,
        helperLabel,
        revenue: Number((baseRevenue * bucketDays * revenueMultiplier).toFixed(2)),
        orderCount: Math.max(Math.round(baseOrders * (bucketDays / 7) * orderMultiplier), 1)
      });

      currentStart.setTime(currentEnd.getTime());
      currentStart.setDate(currentStart.getDate() + 1);
    }

    return series;
  }

  private buildCustomTopProducts(revenueTotal: number) {
    const baseProducts = [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', share: 0.24, unitsFactor: 0.0038 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', share: 0.19, unitsFactor: 0.0062 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', share: 0.16, unitsFactor: 0.0017 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', share: 0.14, unitsFactor: 0.0044 },
      { productId: 17, name: 'Kit de Tags Premium', sku: 'KTP-001', share: 0.11, unitsFactor: 0.0025 }
    ];

    return baseProducts.map(product => ({
      productId: product.productId,
      name: product.name,
      sku: product.sku,
      revenue: Number((revenueTotal * product.share).toFixed(2)),
      unitsSold: Math.max(Math.round(revenueTotal * product.unitsFactor), 1)
    }));
  }

  private formatShortDate(date: Date): string {
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}/${month}`;
  }
}
