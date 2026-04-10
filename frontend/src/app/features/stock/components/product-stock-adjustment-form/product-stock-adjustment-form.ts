import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { AdjustmentProductSummary } from '../../models/stock-adjustment.model';
import { ProductPhysicalAdjustmentRequest, StockAdjustmentDirection, StockService } from '../../services/stock.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { clearApiFieldErrors, applyApiFieldErrors, extractApiErrorPayload } from '../../../../shared/utils/api-errors';
import { clearControlError, setControlError } from '../../../../shared/utils/control-errors';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';
import { stockApiErrorOptions } from '../../utils/stock-api-errors';

interface ProductAdjustmentOption {
  value: StockAdjustmentDirection;
  label: string;
}

interface AdjustmentMetric {
  label: string;
  value: string;
}

interface ProductAdjustmentLayoutMetrics {
  identity: AdjustmentMetric[];
  stock: AdjustmentMetric[];
}

export interface ProductStockAdjustmentFormData {
  template: AdjustmentProductSummary;
  title: string;
}

@Component({
  selector: 'app-product-stock-adjustment-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './product-stock-adjustment-form.html',
  styleUrls: ['./product-stock-adjustment-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductStockAdjustmentForm {
  protected readonly quantityInputMaxLength = 10;

  private static readonly BACKEND_FIELD_MAP: Record<string, string> = {
    quantidade: 'quantidade',
    motivo: 'motivo'
  };

  private static readonly BACKEND_ERROR_FIELDS = Object.values(ProductStockAdjustmentForm.BACKEND_FIELD_MAP);

  private static readonly Texts = {
    saveError: 'Não foi possível ajustar o estoque físico do produto.',
    validationError: 'Revise os campos destacados.'
  };

  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly dialogRef = inject(MatDialogRef<ProductStockAdjustmentForm>);
  private readonly stockService = inject(StockService);
  private readonly entityDialog = inject(EntityDialogService);
  readonly data: ProductStockAdjustmentFormData = inject(MAT_DIALOG_DATA);

  readonly matcher = new InstantErrorStateMatcher();
  readonly isSaving = signal(false);
  readonly showExplanation = signal(false);
  readonly backendMaxQuantity = signal<number | null>(null);
  readonly options: ProductAdjustmentOption[] = [
    { value: 'ADICIONAR', label: 'Adicionar' },
    { value: 'RETIRAR', label: 'Retirar' }
  ];

  readonly form = this.fb.group({
    direcao: this.fb.control<StockAdjustmentDirection>('ADICIONAR', { validators: [Validators.required] }),
    quantidade: this.fb.control('', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]),
    motivo: this.fb.control('', [Validators.required, Validators.maxLength(100)])
  });

  private readonly selectedDirection = toSignal(this.form.controls.direcao.valueChanges, {
    initialValue: this.form.controls.direcao.getRawValue()
  });

  readonly selectedDirectionLockReason = computed(() => this.getDirectionLockReason(this.selectedDirection()));
  readonly maxAllowedQuantity = computed(() => this.resolveEffectiveMaxQuantity());
  readonly currentProductMetrics = computed<ProductAdjustmentLayoutMetrics>(() => ({
    identity: [
      {
        label: 'Produto',
        value: this.data.template.nomeProduto
      },
      {
        label: 'SKU',
        value: this.data.template.skuProduto
      }
    ],
    stock: [
      {
        label: 'Físico atual',
        value: this.formatInteger(this.data.template.estoqueFisicoTotal)
      },
      {
        label: 'Distribuído',
        value: this.formatInteger(this.data.template.estoqueDistribuidoTotal)
      },
      {
        label: 'Disponível',
        value: this.formatInteger(this.data.template.estoqueDisponivelParaAlocar)
      }
    ]
  }));

  constructor() {
    ProductStockAdjustmentForm.BACKEND_ERROR_FIELDS.forEach(controlPath => {
      this.form.get(controlPath)?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => clearControlError(this.form.get(controlPath), 'backend'));
    });

    this.form.controls.direcao.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.backendMaxQuantity.set(null);
        this.updateQuantityValidators();
      });

    if (this.isDirectionLocked('RETIRAR')) {
      this.form.controls.direcao.setValue('ADICIONAR');
    }

    this.updateQuantityValidators();
  }

  onSave(): void {
    if (this.isSaving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.entityDialog.showErrorSnackbar(ProductStockAdjustmentForm.Texts.validationError);
      return;
    }

    const direction = this.form.controls.direcao.getRawValue();
    if (this.isDirectionLocked(direction)) {
      setControlError(this.form.controls.quantidade, 'backend', this.getDirectionLockReason(direction) ?? ProductStockAdjustmentForm.Texts.saveError);
      this.form.controls.quantidade.markAsTouched();
      this.entityDialog.showErrorSnackbar(ProductStockAdjustmentForm.Texts.validationError);
      return;
    }

    const request: ProductPhysicalAdjustmentRequest = {
      produtoId: this.data.template.produtoId,
      direcao: direction,
      quantidade: this.toAbsoluteQuantity(),
      motivo: this.form.controls.motivo.getRawValue().trim()
    };

    if (!request.motivo) {
      this.form.controls.motivo.setErrors({ required: true });
      this.form.controls.motivo.markAsTouched();
      this.entityDialog.showErrorSnackbar(ProductStockAdjustmentForm.Texts.validationError);
      return;
    }

    clearApiFieldErrors(this.form, ProductStockAdjustmentForm.BACKEND_ERROR_FIELDS);
    this.isSaving.set(true);

    this.stockService.adjustPhysicalStock(request).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.dialogRef.close(true);
      },
      error: err => {
        this.isSaving.set(false);
        this.handleApiError(err);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  toggleExplanation(): void {
    this.showExplanation.update(current => !current);
  }

  formatInteger(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 0
    }).format(value);
  }

  isDirectionLocked(direction: StockAdjustmentDirection): boolean {
    return direction === 'RETIRAR' && hasLockedField(this.data.template, 'ajusteFisicoNegativo');
  }

  getDirectionLockReason(direction: StockAdjustmentDirection): string | null {
    if (direction !== 'RETIRAR') {
      return null;
    }

    return getLockedFieldReason(this.data.template, 'ajusteFisicoNegativo');
  }

  private toAbsoluteQuantity(): number {
    return Number(this.form.controls.quantidade.getRawValue().trim());
  }

  private handleApiError(err: unknown): void {
    clearApiFieldErrors(this.form, ProductStockAdjustmentForm.BACKEND_ERROR_FIELDS);
    this.applyBackendQuantityLimit(err);
    const hasFieldErrors = applyApiFieldErrors(this.form, err, {
      fieldMap: ProductStockAdjustmentForm.BACKEND_FIELD_MAP,
      ...stockApiErrorOptions
    });

    const payload = extractApiErrorPayload(err);
    if ((payload.status === 400 || payload.status === 409) && !hasFieldErrors) {
      setControlError(
        this.form.controls.quantidade,
        'backend',
        this.buildQuantityBackendMessage()
      );
      this.form.controls.quantidade.markAsTouched();
      this.entityDialog.showApiErrorSnackbar(err, ProductStockAdjustmentForm.Texts.saveError, stockApiErrorOptions);
      return;
    }

    if (hasFieldErrors) {
      this.entityDialog.showApiErrorSnackbar(err, ProductStockAdjustmentForm.Texts.saveError, stockApiErrorOptions);
      return;
    }

    this.entityDialog.showApiErrorSnackbar(err, ProductStockAdjustmentForm.Texts.saveError, stockApiErrorOptions);
  }

  private resolveEffectiveMaxQuantity(): number | null {
    const backendMax = this.backendMaxQuantity();
    if (backendMax != null) {
      return backendMax;
    }

    const direction = this.selectedDirection();
    if (direction === 'RETIRAR') {
      return Math.max(this.data.template.estoqueFisicoTotal ?? 0, 0);
    }

    return null;
  }

  private updateQuantityValidators(): void {
    const validators = [Validators.required, Validators.pattern(/^[1-9]\d*$/)];
    const maxAllowed = this.resolveEffectiveMaxQuantity();
    if (maxAllowed != null) {
      validators.push(Validators.max(maxAllowed));
    }

    this.form.controls.quantidade.setValidators(validators);
    this.form.controls.quantidade.updateValueAndValidity({ emitEvent: false });
  }

  private applyBackendQuantityLimit(err: unknown): void {
    const payload = extractApiErrorPayload(err);
    const maxAllowed = Number(payload.details?.['quantidadeMaximaPermitida']);
    if (!Number.isFinite(maxAllowed)) {
      return;
    }

    this.backendMaxQuantity.set(maxAllowed);
    this.updateQuantityValidators();
  }

  private buildQuantityBackendMessage(): string {
    const maxAllowed = this.maxAllowedQuantity();
    if (maxAllowed != null) {
      return `O máximo permitido para esta operação é ${this.formatInteger(maxAllowed)}.`;
    }

    return 'Revise a quantidade informada para o ajuste.';
  }
}
