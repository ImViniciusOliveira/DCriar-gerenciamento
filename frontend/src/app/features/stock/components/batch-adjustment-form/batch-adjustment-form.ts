import { ChangeDetectionStrategy, Component, ElementRef, ViewChild, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, NonNullableFormBuilder } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  Batch,
  BatchAdjustmentApplyRequest,
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
}

@Component({
  selector: 'app-batch-adjustment-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
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
    MISSING_APPLY_LINK: 'O lote não expõe o link de aplicação do ajuste.',
    CALCULATE_ERROR: 'Não foi possível calcular o ajuste do lote.',
    APPLY_SUCCESS: 'Operação aplicada com sucesso.',
    APPLY_ERROR: 'Não foi possível aplicar a operação no lote.',
    ADJUSTMENT_OPERATION_MESSAGE: 'Ajuste corrige divergências de registro no lote. O valor total é mantido e o custo unitário é recalculado com base na nova quantidade informada.'
  };

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly batchService = inject(BatchService);
  private readonly entityDialog = inject(EntityDialogService);

  readonly batch = input.required<Batch>();
  readonly operationApplied = output<Batch>();

  @ViewChild('resultCard') private resultCard?: ElementRef<HTMLElement>;

  readonly form = this.fb.group({
    tipoOperacao: this.fb.control<BatchAdjustmentOperation>('AJUSTE'),
    direcao: this.fb.control<BatchAdjustmentDirection | null>('RETIRAR'),
    quantidade: this.fb.control(''),
    motivo: this.fb.control('')
  });

  protected readonly isCalculating = signal(false);
  protected readonly isApplying = signal(false);
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

  protected readonly movementQuantityLabel = computed(() => {
    const result = this.calculationResult();
    if (!result) {
      return '';
    }

    return this.formatSignedQuantity(result.quantidadeMovimentacaoGerada, result.unidadeSimbolo);
  });

  protected readonly impactedItems = computed<BatchAdjustmentImpactItem[]>(() => this.calculationResult()?.itensImpactados ?? []);
  protected readonly hasImpactedItems = computed(() => this.impactedItems().length > 0);

  protected readonly resultMessage = computed<AdjustmentResultMessage | null>(() => {
    const result = this.calculationResult();
    if (!result) {
      return null;
    }

    if (result.tipoOperacao === 'PERDA_DESCARTE') {
      return {
        movementLabel: 'Perda / Descarte',
        operationMessage: 'Perda ou descarte reduz o valor total do lote e mantém o custo unitário, porque o material perdido já fazia parte do custo pago pelo lote.'
      };
    }

    switch (result.contextoItensImpactados) {
      case 'MATERIA_PRIMA_NAO_GERA_RETALHO':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE
        };
      case 'SEM_RETALHOS_COM_SALDO':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE
        };
      case 'SEM_RETALHOS_VINCULADOS':
        return {
          movementLabel: 'Ajuste de inventário',
          operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE
        };
    }

    return {
      movementLabel: 'Ajuste de inventário',
      operationMessage: BatchAdjustmentForm.Texts.ADJUSTMENT_OPERATION_MESSAGE
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
        this.scrollToResult();
      },
      error: err => {
        this.isCalculating.set(false);
        this.entityDialog.showErrorSnackbar(err?.error?.detail || err?.error?.message || BatchAdjustmentForm.Texts.CALCULATE_ERROR);
      }
    });
  }

  protected applyOperation(): void {
    const applyUrl = this.batch()._links?.['aplicar-ajuste']?.href;
    if (!applyUrl) {
      this.entityDialog.showErrorSnackbar(BatchAdjustmentForm.Texts.MISSING_APPLY_LINK);
      return;
    }

    const payload: BatchAdjustmentApplyRequest = {
      tipoOperacao: this.form.controls.tipoOperacao.getRawValue(),
      direcao: this.shouldShowDirection() ? this.form.controls.direcao.getRawValue() : null,
      quantidade: this.parseQuantity(this.form.controls.quantidade.getRawValue()),
      motivo: this.form.controls.motivo.getRawValue().trim()
    };

    this.isApplying.set(true);

    this.batchService.applyAdjustment(applyUrl, payload).subscribe({
      next: updatedBatch => {
        this.isApplying.set(false);
        this.resetPreview();
        this.operationApplied.emit(updatedBatch);
        this.entityDialog.showSuccessSnackbar(BatchAdjustmentForm.Texts.APPLY_SUCCESS);
      },
      error: err => {
        this.isApplying.set(false);
        this.entityDialog.showErrorSnackbar(err?.error?.detail || err?.error?.message || BatchAdjustmentForm.Texts.APPLY_ERROR);
      }
    });
  }

  protected resetPreview(): void {
    this.calculationResult.set(null);
    this.selectedImpactedIds.set([]);
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

  private formatSignedQuantity(value: number | undefined, unit: string | undefined): string {
    if (typeof value !== 'number') {
      return `0${unit || ''}`;
    }

    const prefix = value > 0 ? '+' : '';
    return `${prefix}${this.formatNumber(value)}${unit || ''}`;
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

  private scrollToResult(): void {
    setTimeout(() => {
      this.resultCard?.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'end'
      });
    });
  }
}
