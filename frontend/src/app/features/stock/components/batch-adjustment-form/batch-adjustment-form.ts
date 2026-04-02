import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, NonNullableFormBuilder } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  Batch,
  BatchAdjustmentCalculateRequest,
  BatchAdjustmentCalculateResponse,
  BatchAdjustmentDirection,
  BatchAdjustmentImpactItem,
  BatchAdjustmentOperation
} from '../../models/batch.model';
import { BatchService } from '../../services/batch.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';

interface AdjustmentOption<T extends string> {
  value: T;
  label: string;
}

interface AdjustmentMetric {
  label: string;
  value: string;
}

interface AdjustmentResultMessage {
  movementLabel: string;
  operationMessage: string;
  emptyImpactTitle: string;
  emptyImpactMessage: string;
}

@Component({
  selector: 'app-batch-adjustment-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
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
  private static readonly Texts = {
    MISSING_LINK: 'O lote não expõe o link de cálculo de ajuste.',
    CALCULATE_ERROR: 'Não foi possível calcular o ajuste do lote.',
    ADJUSTMENT_OPERATION_MESSAGE: 'Ajuste corrige divergências de registro no lote. O valor total é mantido e o custo unitário é recalculado com base na nova quantidade informada.'
  };

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly batchService = inject(BatchService);
  private readonly entityDialog = inject(EntityDialogService);

  readonly batch = input.required<Batch>();

  readonly form = this.fb.group({
    tipoOperacao: this.fb.control<BatchAdjustmentOperation>('AJUSTE'),
    direcao: this.fb.control<BatchAdjustmentDirection | null>('RETIRAR'),
    quantidade: this.fb.control(''),
    motivo: this.fb.control('')
  });

  protected readonly isCalculating = signal(false);
  protected readonly calculationResult = signal<BatchAdjustmentCalculateResponse | null>(null);
  protected readonly selectedImpactedIds = signal<number[]>([]);
  protected readonly selectedOperation = toSignal(this.form.controls.tipoOperacao.valueChanges, {
    initialValue: this.form.controls.tipoOperacao.getRawValue()
  });

  protected readonly operationOptions: AdjustmentOption<BatchAdjustmentOperation>[] = [
    { value: 'AJUSTE', label: 'Ajuste' },
    { value: 'PERDA_DESCARTE', label: 'Perda / Descarte' }
  ];

  protected readonly directionOptions: AdjustmentOption<BatchAdjustmentDirection>[] = [
    { value: 'ADICIONAR', label: 'Adicionar' },
    { value: 'RETIRAR', label: 'Retirar' }
  ];

  protected readonly shouldShowDirection = computed(() => this.selectedOperation() === 'AJUSTE');
  protected readonly adjustmentUnitSymbol = computed(() => this.batch().unidadeSimbolo || 'un');

  protected readonly currentLotMetrics = computed<AdjustmentMetric[]>(() => {
    const batch = this.batch();
    const currentValue = batch.valorAtualLote ?? batch.custoTotalLote;
    const currentUnitCost = batch.custoUnitarioAtual
      ?? this.calculateUnitCost(currentValue, batch.saldoEstoque);

    return [
      {
        label: 'Saldo do Lote',
        value: this.formatQuantity(batch.saldoEstoque, batch.unidadeSimbolo)
      },
      {
        label: 'Valor Atual do Lote',
        value: this.formatCurrency(currentValue)
      },
      {
        label: 'Custo Unitário Atual',
        value: this.formatCurrencyPerUnit(currentUnitCost, batch.unidadeSimbolo)
      }
    ];
  });

  protected readonly projectedLotMetrics = computed<AdjustmentMetric[]>(() => {
    const result = this.calculationResult();
    if (!result) {
      return [];
    }

    return [
      {
        label: 'Saldo Projetado',
        value: this.formatQuantity(result.saldoProjetado, result.unidadeSimbolo)
      },
      {
        label: 'Valor Projetado do Lote',
        value: this.formatCurrency(result.valorProjetadoLote)
      },
      {
        label: 'Custo Unitário Projetado',
        value: this.formatCurrencyPerUnit(this.calculateUnitCost(result.valorProjetadoLote, result.saldoProjetado), result.unidadeSimbolo)
      }
    ];
  });

  protected readonly impactedItems = computed<BatchAdjustmentImpactItem[]>(() => this.calculationResult()?.itensImpactados ?? []);

  protected readonly resultMessage = computed<AdjustmentResultMessage | null>(() => {
    const result = this.calculationResult();
    if (!result) {
      return null;
    }

    if (result.tipoOperacao === 'PERDA_DESCARTE') {
      return {
        movementLabel: 'Perda / Descarte',
        operationMessage: 'Perda ou descarte reduz o valor total do lote e mantém o custo unitário, porque o material perdido já fazia parte do custo pago pelo lote.',
        emptyImpactTitle: 'Esta operação afeta apenas o lote informado.',
        emptyImpactMessage: 'Perda ou descarte não recalcula lotes derivados.'
      };
    }

    switch (result.contextoItensImpactados) {
      case 'MATERIA_PRIMA_NAO_GERA_RETALHO':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE,
          emptyImpactTitle: 'Esta matéria-prima não gera retalhos.',
          emptyImpactMessage: 'O ajuste afetará apenas este lote, sem recalcular itens derivados.'
        };
      case 'SEM_RETALHOS_COM_SALDO':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE,
          emptyImpactTitle: 'Não há retalhos com saldo disponível para recalcular.',
          emptyImpactMessage: 'Os retalhos vinculados a este lote já foram consumidos ou zerados.'
        };
      case 'SEM_RETALHOS_VINCULADOS':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE,
          emptyImpactTitle: 'Este lote ainda não possui retalhos vinculados.',
          emptyImpactMessage: 'O ajuste afetará apenas este lote.'
        };
    }

    return {
      movementLabel: 'Ajuste de inventário',
      operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE,
      emptyImpactTitle: 'Nenhum item derivado impactado.',
      emptyImpactMessage: 'Este lote não possui retalhos derivados para recalcular, então o ajuste afetará apenas este lote.'
    };
  });

  constructor() {
    this.form.controls.tipoOperacao.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(tipoOperacao => {
        const directionControl = this.form.controls.direcao;
        if (tipoOperacao === 'PERDA_DESCARTE') {
          directionControl.setValue(null, { emitEvent: false });
        } else {
          if (!directionControl.value) {
            directionControl.setValue('RETIRAR', { emitEvent: false });
          }
        }
      });

    this.form.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.calculationResult()) {
          this.calculationResult.set(null);
          this.selectedImpactedIds.set([]);
        }
      });
  }

  protected calculateAdjustment(): void {
    const calculateUrl = this.batch()._links?.['calcular-ajuste']?.href;
    if (!calculateUrl) {
      this.entityDialog.showErrorSnackbar(BatchAdjustmentForm.Texts.MISSING_LINK);
      return;
    }

    const payload: BatchAdjustmentCalculateRequest = {
      tipoOperacao: this.form.controls.tipoOperacao.getRawValue(),
      direcao: this.shouldShowDirection() ? this.form.controls.direcao.getRawValue() : null,
      quantidade: this.parseQuantity(this.form.controls.quantidade.getRawValue()),
      motivo: this.form.controls.motivo.getRawValue().trim()
    };

    this.isCalculating.set(true);

    this.batchService.calculateAdjustment(calculateUrl, payload).subscribe({
      next: result => {
        this.calculationResult.set(result);
        this.selectedImpactedIds.set(
          result.itensImpactados
            .filter(item => item.selecionadoPorPadrao)
            .map(item => item.id)
        );
        this.isCalculating.set(false);
      },
      error: err => {
        this.isCalculating.set(false);
        this.entityDialog.showErrorSnackbar(err?.error?.detail || err?.error?.message || BatchAdjustmentForm.Texts.CALCULATE_ERROR);
      }
    });
  }

  protected resetPreview(): void {
    this.calculationResult.set(null);
    this.selectedImpactedIds.set([]);
  }

  protected onImpactedItemToggle(itemId: number, event: MatCheckboxChange): void {
    this.selectedImpactedIds.update(current =>
      event.checked
        ? Array.from(new Set([...current, itemId]))
        : current.filter(id => id !== itemId)
    );
  }

  protected isImpactedItemSelected(itemId: number): boolean {
    return this.selectedImpactedIds().includes(itemId);
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

  private calculateUnitCost(total: number | undefined, quantity: number | undefined): number {
    if (!quantity || quantity <= 0) {
      return 0;
    }

    return (total ?? 0) / quantity;
  }

  private formatCurrencyPerUnit(value: number | undefined, unit: string | undefined): string {
    const currency = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value ?? 0);

    return `${currency}/${unit || 'un'}`;
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 4
    }).format(value);
  }

  private parseQuantity(value: string): number {
    return Number(value.replace(',', '.'));
  }
}
