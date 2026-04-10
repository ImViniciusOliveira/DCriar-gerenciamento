import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { lastValueFrom } from 'rxjs';

import { Batch, BatchMovement } from '../../models/batch.model';
import { BatchService } from '../../services/batch.service';

@Component({
  selector: 'app-batch-movements-section',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule
  ],
  templateUrl: './batch-movements-section.html',
  styleUrls: ['./batch-movements-section.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchMovementsSection {
  private readonly batchService = inject(BatchService);

  readonly batch = input.required<Batch>();
  readonly title = input('Movimentações do Lote');
  readonly description = input('Use este histórico para acompanhar o impacto do ajuste no lote selecionado.');

  readonly movements = signal<BatchMovement[]>([]);
  readonly totalMovements = signal(0);
  readonly isLoadingMovements = signal(false);
  readonly showMovements = signal(false);
  readonly toggleLabel = computed(() => this.showMovements() ? 'Esconder movimentações' : 'Mostrar movimentações');
  readonly shouldUseScrollableList = computed(() => this.movements().length > 5);
  readonly totalLabel = computed(() => {
    if (this.isLoadingMovements()) {
      return 'Carregando total de movimentações...';
    }

    const total = this.totalMovements();
    if (total === 0) {
      return 'Nenhuma movimentação registrada';
    }

    if (total === 1) {
      return '1 movimentação registrada';
    }

    return `${total} movimentações registradas`;
  });

  constructor() {
    effect(() => {
      const batch = this.batch();
      void this.loadMovements(batch);
    });
  }

  toggleMovements(): void {
    this.showMovements.update(current => !current);
  }

  movementLabel(tipo: string): string {
    switch (tipo) {
      case 'ENTRADA_COMPRA':
        return 'Entrada de compra';
      case 'SAIDA_PRODUCAO':
        return 'Saída para produção';
      case 'AJUSTE_INVENTARIO':
        return 'Ajuste de inventário';
      case 'PERDA_DESCARTE':
        return 'Perda / Descarte';
      case 'ENTRADA_SOBRA':
        return 'Entrada de retalho';
      default:
        return tipo.replaceAll('_', ' ').toLowerCase().replace(/^\w/, char => char.toUpperCase());
    }
  }

  formatMovementQuantity(movement: BatchMovement): string {
    const unit = this.batch().unidadeSimbolo ?? 'un';
    const prefix = movement.quantidade > 0 ? '+' : '';
    const quantity = new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 4
    }).format(movement.quantidade);
    return `${prefix}${quantity}${unit}`;
  }

  private async loadMovements(batch: Batch): Promise<void> {
    const movementsUrl = batch._links?.['movimentacoes']?.href;
    if (!movementsUrl) {
      this.movements.set([]);
      this.totalMovements.set(0);
      return;
    }

    this.isLoadingMovements.set(true);

    try {
      const response = await lastValueFrom(this.batchService.findMovements(movementsUrl));
      this.movements.set(response._embedded?.movimentacoes ?? []);
      this.totalMovements.set(response.totalMovimentacoes ?? response._embedded?.movimentacoes?.length ?? 0);
    } catch {
      this.movements.set([]);
      this.totalMovements.set(0);
    } finally {
      this.isLoadingMovements.set(false);
    }
  }
}
