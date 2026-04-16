import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

import { StockService } from '../../stock/services/stock.service';
import { MaterialStockAnalysisSummary } from '../../stock/models/material-stock-analysis.model';
import { ProductStockAnalysisSummary } from '../../stock/models/product-stock-analysis.model';

interface DashboardQuickAction {
  label: string;
  icon: string;
  route: string;
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
  tone: 'warning' | 'critical';
}

interface DashboardEmptyState {
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
      tone: item.statusAnalise === 'CRITICO' ? 'critical' : 'warning'
    };
  }
}
