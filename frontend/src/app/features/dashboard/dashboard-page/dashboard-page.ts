import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { StockService } from '../../stock/services/stock.service';
import { MaterialStockAnalysisSummary } from '../../stock/models/material-stock-analysis.model';

interface DashboardQuickAction {
  label: string;
  icon: string;
  route: string;
}

interface DashboardMaterialAlertItem {
  id: number;
  name: string;
  currentAmount: number;
  minimumAmount: number;
  unitLabel: string;
  stockLevelPercent: number;
  helperText: string;
  actionLabel: string;
  route: string;
  tone: 'warning' | 'critical';
}

interface DashboardMaterialEmptyState {
  title: string;
  tone: 'positive';
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [RouterLink, MatIconModule, DecimalPipe],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPage {
  private readonly stockService = inject(StockService);

  protected readonly quickActions: DashboardQuickAction[] = [
    { label: 'Nova venda', icon: 'point_of_sale', route: '/vendas' },
    { label: 'Matérias-primas', icon: 'inventory_2', route: '/tipos-materia-prima' },
    { label: 'Nova produção', icon: 'precision_manufacturing', route: '/ordens-de-producao' },
    { label: 'Ajustes', icon: 'tune', route: '/estoques' }
  ];

  protected readonly productAlerts = [];

  private readonly materialStockAnalysisResult = toSignal(
    this.stockService.searchMaterialStockAnalysis({
      page: 0,
      size: 50,
      sort: 'percentualRisco,desc',
      statusAnalise: 'CRITICO',
      politicaSaldoRetalho: 'TODOS'
    }).pipe(
      map(result => result.items)
    ),
    { initialValue: [] as MaterialStockAnalysisSummary[] }
  );

  protected readonly materialAlerts = computed<DashboardMaterialAlertItem[]>(() =>
    this.materialStockAnalysisResult()
      .sort((left, right) => (right.percentualRisco ?? 0) - (left.percentualRisco ?? 0))
      .map(item => this.toMaterialAlertItem(item))
  );

  protected readonly hasMaterialAlerts = computed(() => this.materialAlerts().length > 0);
  protected readonly materialEmptyState: DashboardMaterialEmptyState = {
    title: 'Nenhuma matéria-prima com estoque baixo',
    tone: 'positive'
  };

  private toMaterialAlertItem(item: MaterialStockAnalysisSummary): DashboardMaterialAlertItem {
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
      tone: 'critical'
    };
  }
}
