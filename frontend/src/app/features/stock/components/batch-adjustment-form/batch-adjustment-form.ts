import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Batch } from '../../models/batch.model';

interface AdjustmentOption {
  value: string;
  label: string;
}

interface AdjustmentMetric {
  label: string;
  value: string;
}

interface AdjustmentImpactItem {
  id: number;
  description: string;
  size: string;
  currentValue: string;
  nextValue: string;
  reason: string;
}

@Component({
  selector: 'app-batch-adjustment-form',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './batch-adjustment-form.html',
  styleUrls: ['./batch-adjustment-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchAdjustmentForm {
  readonly batch = input.required<Batch>();

  protected readonly showCalculationResult = signal(false);

  protected readonly operationOptions: AdjustmentOption[] = [
    { value: 'AJUSTE', label: 'Ajuste' },
    { value: 'PERDA_DESCARTE', label: 'Perda / Descarte' }
  ];

  protected readonly directionOptions: AdjustmentOption[] = [
    { value: 'ADICIONAR', label: 'Adicionar' },
    { value: 'RETIRAR', label: 'Retirar' }
  ];

  protected readonly currentLotMetrics = computed<AdjustmentMetric[]>(() => {
    const batch = this.batch();
    return [
      {
        label: 'Saldo do Lote',
        value: this.formatQuantity(batch.saldoEstoque, batch.unidadeSimbolo)
      },
      {
        label: 'Custo Total do Lote',
        value: this.formatCurrency(batch.custoTotalLote)
      },
      {
        label: 'Custo por Unidade do Lote',
        value: this.formatUnitCost(batch.custoTotalLote, batch.saldoEstoque, batch.unidadeSimbolo)
      }
    ];
  });

  protected readonly nextLotMetrics = computed<AdjustmentMetric[]>(() => {
    const batch = this.batch();
    const saldoAtual = batch.saldoEstoque ?? 0;
    const custoAtual = batch.custoTotalLote ?? 0;
    const saldoNovo = saldoAtual > 0 ? Math.max(saldoAtual - saldoAtual * 0.2, 0) : 0;
    const custoNovo = custoAtual > 0 ? Math.max(custoAtual - custoAtual * 0.2, 0) : 0;

    return [
      {
        label: 'Novo Saldo do Lote',
        value: this.formatQuantity(saldoNovo, batch.unidadeSimbolo)
      },
      {
        label: 'Novo Custo Total do Lote',
        value: this.formatCurrency(custoNovo)
      },
      {
        label: 'Novo Custo por Unidade',
        value: this.formatUnitCost(custoNovo, saldoNovo, batch.unidadeSimbolo)
      }
    ];
  });

  protected readonly impactedItems = computed<AdjustmentImpactItem[]>(() => {
    const batch = this.batch();
    const widthMm = Number(batch.atributos?.['larguraMm'] ?? 0);
    const widthCm = widthMm > 0 ? widthMm / 10 : 20;
    const fallbackUnit = batch.unidadeSimbolo || 'm²';

    return [
      {
        id: Number(batch.id) + 1 || 1,
        description: `Retalho direto do lote #${batch.id}`,
        size: `${this.formatNumber(widthCm)}cm x 20cm`,
        currentValue: this.formatCurrency((batch.custoTotalLote ?? 0) * 0.15),
        nextValue: this.formatCurrency((batch.custoTotalLote ?? 0) * 0.21),
        reason: `Pela mudança no ajuste, esse retalho herdaria o novo custo do lote base em ${fallbackUnit}.`
      },
      {
        id: Number(batch.id) + 2 || 2,
        description: `Retalho derivado do retalho #${Number(batch.id) + 1 || 1}`,
        size: `${this.formatNumber(Math.max(widthCm / 2, 5))}cm x 10cm`,
        currentValue: this.formatCurrency((batch.custoTotalLote ?? 0) * 0.08),
        nextValue: this.formatCurrency((batch.custoTotalLote ?? 0) * 0.11),
        reason: 'Se a cadeia de retalhos for recalculada, este item também pode ter o valor atualizado.'
      }
    ];
  });

  protected previewCalculation(): void {
    this.showCalculationResult.set(true);
  }

  protected resetPreview(): void {
    this.showCalculationResult.set(false);
  }

  private formatCurrency(value: number | undefined): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value ?? 0);
  }

  private formatQuantity(value: number | undefined, unit: string | undefined): string {
    if (typeof value !== 'number') {
      return `0${unit || ''}`;
    }

    return `${this.formatNumber(value)}${unit || ''}`;
  }

  private formatUnitCost(total: number | undefined, quantity: number | undefined, unit: string | undefined): string {
    if (!quantity || quantity <= 0) {
      return `R$ 0,00/${unit || 'un'}`;
    }

    const unitCost = (total ?? 0) / quantity;
    const currency = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(unitCost);

    return `${currency}/${unit || 'un'}`;
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 2
    }).format(value);
  }
}
