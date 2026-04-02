import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { STOCK_HISTORY_RESPONSE_MOCK } from './stock-home.mock';
import { DetailsDialog } from '../../../../shared/components/details-dialog/details-dialog';

type StockSectionKey = 'consultas' | 'ajustes' | 'historico';
type HistoryRangeKey = '1d' | '1m' | '6m' | '1a' | 'all';
type HistorySortField = 'data' | 'produto' | 'sku' | 'quantidade' | null;
type HistorySortDirection = 'asc' | 'desc';

interface StockSection {
  key: StockSectionKey;
  title: string;
  buttonLabel: string;
  subtitle: string;
}

interface HistoryRangeOption {
  key: HistoryRangeKey;
  label: string;
}

interface HistorySortOption {
  field: Exclude<HistorySortField, null>;
  label: string;
}

@Component({
  selector: 'app-stock-home',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './stock-home.html',
  styleUrl: './stock-home.scss',
})
export class StockHome {
  private readonly dialog = inject(MatDialog);

  protected readonly sections: StockSection[] = [
    {
      key: 'ajustes',
      title: 'Ajustes',
      buttonLabel: 'Ajustes',
      subtitle: 'Correcao do estoque fisico total e redistribuicao de saldo por canal'
    },
    {
      key: 'consultas',
      title: 'Consultas',
      buttonLabel: 'Consultas',
      subtitle: 'Consultas de estoque fisico, distribuicao por canal e visoes consolidadas'
    },
    {
      key: 'historico',
      title: 'Historico',
      buttonLabel: 'Historico',
      subtitle: 'Acompanhamento de mudancas, conferencias e auditoria do estoque'
    }
  ];

  protected readonly activeSection = signal<StockSection>(this.sections[0]);
  protected readonly historyResponse = STOCK_HISTORY_RESPONSE_MOCK;
  private readonly historyAnchorDate = new Date('2026-04-02T08:10:00');
  protected readonly isHistorySection = computed(() => this.activeSection().key === 'historico');
  protected readonly selectedRange = signal<HistoryRangeKey>('1m');
  protected readonly selectedSortField = signal<HistorySortField>(null);
  protected readonly selectedSortDirection = signal<HistorySortDirection>('desc');
  protected readonly rangeOptions: HistoryRangeOption[] = [
    { key: '1d', label: '1D' },
    { key: '1m', label: '1M' },
    { key: '6m', label: '6M' },
    { key: '1a', label: '1A' },
    { key: 'all', label: 'Todo período' }
  ];
  protected readonly sortOptions: HistorySortOption[] = [
    { field: 'data', label: 'Data' },
    { field: 'produto', label: 'Produto' },
    { field: 'sku', label: 'SKU' },
    { field: 'quantidade', label: 'Quantidade' }
  ];
  protected readonly filteredHistoryItems = computed(() => {
    const range = this.selectedRange();
    const sortField = this.selectedSortField();
    const sortDirection = this.selectedSortDirection();
    const cutoff = this.resolveCutoffDate(range);

    const filtered = this.historyResponse._embedded.movimentacoes.filter(item => {
      if (!cutoff) {
        return true;
      }

      return new Date(item.data).getTime() >= cutoff.getTime();
    });

    if (!sortField) {
      return [...filtered].sort((left, right) => new Date(right.data).getTime() - new Date(left.data).getTime());
    }

    const sorted = [...filtered].sort((left, right) => {
      switch (sortField) {
        case 'data':
          return new Date(left.data).getTime() - new Date(right.data).getTime();
        case 'produto':
          return left.produto.nome.localeCompare(right.produto.nome, 'pt-BR');
        case 'sku':
          return left.produto.sku.localeCompare(right.produto.sku, 'pt-BR');
        case 'quantidade':
          return left.quantidade - right.quantidade;
      }
    });

    return sortDirection === 'desc' ? sorted.reverse() : sorted;
  });

  protected setActiveSection(section: StockSection): void {
    this.activeSection.set(section);
  }

  protected setHistoryRange(range: HistoryRangeKey): void {
    this.selectedRange.set(range);
  }

  protected toggleHistorySort(field: Exclude<HistorySortField, null>): void {
    const currentField = this.selectedSortField();
    const currentDirection = this.selectedSortDirection();

    if (currentField !== field) {
      this.selectedSortField.set(field);
      this.selectedSortDirection.set(field === 'data' ? 'desc' : 'asc');
      return;
    }

    if (currentDirection === 'asc') {
      this.selectedSortDirection.set('desc');
      return;
    }

    if (field === 'data') {
      this.selectedSortField.set(null);
      this.selectedSortDirection.set('desc');
      return;
    }

    this.selectedSortDirection.set('asc');
  }

  protected formatMovementType(tipo: string): string {
    switch (tipo) {
      case 'ENTRADA_PRODUCAO':
        return 'Entrada de producao';
      case 'SAIDA_VENDA':
        return 'Saida de venda';
      case 'AJUSTE_MANUAL':
        return 'Ajuste manual';
      case 'ENTRADA_ESTORNO':
        return 'Entrada de estorno';
      case 'ESTORNO_PRODUCAO':
        return 'Estorno de producao';
      default:
        return tipo;
    }
  }

  protected formatSignedQuantity(quantidade: number): string {
    return `${quantidade > 0 ? '+' : '-'}${Math.abs(quantidade)}`;
  }

  protected isPositiveQuantity(quantidade: number): boolean {
    return quantidade >= 0;
  }

  protected getSortIcon(field: Exclude<HistorySortField, null>): string {
    if (this.selectedSortField() !== field) {
      return field === 'data' ? 'south' : 'unfold_more';
    }

    return this.selectedSortDirection() === 'asc' ? 'north' : 'south';
  }

  protected openReasonDetails(id: number, motivo: string): void {
    this.dialog.open(DetailsDialog, {
      data: {
        title: `Motivo da movimentação #${id}`,
        items: [{ value: motivo }],
        showLabels: false
      },
      width: '680px',
      maxWidth: '90vw',
      autoFocus: false
    });
  }

  protected getTruncatedReason(motivo: string): string {
    if (motivo.length <= 100) {
      return motivo;
    }

    return `${motivo.slice(0, 97)}...`;
  }

  private resolveCutoffDate(range: HistoryRangeKey): Date | null {
    const base = new Date(this.historyAnchorDate);

    switch (range) {
      case '1d':
        base.setDate(base.getDate() - 1);
        return base;
      case '1m':
        base.setMonth(base.getMonth() - 1);
        return base;
      case '6m':
        base.setMonth(base.getMonth() - 6);
        return base;
      case '1a':
        base.setFullYear(base.getFullYear() - 1);
        return base;
      case 'all':
        return null;
    }
  }
}
