import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdjustmentProductSummary } from '../../models/stock-adjustment.model';
import { ProductPhysicalAdjustmentRequest, StockService } from '../../services/stock.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { clearApiFieldErrors, applyApiFieldErrors, extractApiErrorPayload, resolveApiErrorMessage } from '../../../../shared/utils/api-errors';
import { clearControlError, setControlError } from '../../../../shared/utils/control-errors';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';
import { stockApiErrorOptions } from '../../utils/stock-api-errors';

type ProductAdjustmentDirection = 'ADICIONAR' | 'RETIRAR';

interface ProductAdjustmentOption {
  value: ProductAdjustmentDirection;
  label: string;
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
  readonly options: ProductAdjustmentOption[] = [
    { value: 'ADICIONAR', label: 'Adicionar' },
    { value: 'RETIRAR', label: 'Retirar' }
  ];

  readonly form = this.fb.group({
    direcao: this.fb.control<ProductAdjustmentDirection>('ADICIONAR', { validators: [Validators.required] }),
    quantidade: this.fb.control('', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]),
    motivo: this.fb.control('', [Validators.required, Validators.maxLength(100)])
  });

  readonly selectedDirectionLockReason = computed(() => this.getDirectionLockReason(this.form.controls.direcao.getRawValue()));

  constructor() {
    ProductStockAdjustmentForm.BACKEND_ERROR_FIELDS.forEach(controlPath => {
      this.form.get(controlPath)?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => clearControlError(this.form.get(controlPath), 'backend'));
    });

    if (this.isDirectionLocked('RETIRAR')) {
      this.form.controls.direcao.setValue('ADICIONAR');
    }
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
      quantidade: this.toSignedQuantity(direction),
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

  formatInteger(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 0
    }).format(value);
  }

  isDirectionLocked(direction: ProductAdjustmentDirection): boolean {
    return direction === 'RETIRAR' && hasLockedField(this.data.template, 'ajusteFisicoNegativo');
  }

  getDirectionLockReason(direction: ProductAdjustmentDirection): string | null {
    if (direction !== 'RETIRAR') {
      return null;
    }

    return getLockedFieldReason(this.data.template, 'ajusteFisicoNegativo');
  }

  private toSignedQuantity(direction: ProductAdjustmentDirection): number {
    const absoluteQuantity = Number(this.form.controls.quantidade.getRawValue());
    return direction === 'RETIRAR' ? absoluteQuantity * -1 : absoluteQuantity;
  }

  private handleApiError(err: unknown): void {
    clearApiFieldErrors(this.form, ProductStockAdjustmentForm.BACKEND_ERROR_FIELDS);
    const hasFieldErrors = applyApiFieldErrors(this.form, err, {
      fieldMap: ProductStockAdjustmentForm.BACKEND_FIELD_MAP,
      ...stockApiErrorOptions
    });

    const payload = extractApiErrorPayload(err);
    if ((payload.status === 400 || payload.status === 409) && !hasFieldErrors) {
      setControlError(
        this.form.controls.quantidade,
        'backend',
        resolveApiErrorMessage(err, ProductStockAdjustmentForm.Texts.saveError, stockApiErrorOptions)
      );
      this.form.controls.quantidade.markAsTouched();
      this.entityDialog.showErrorSnackbar(ProductStockAdjustmentForm.Texts.validationError);
      return;
    }

    if (hasFieldErrors) {
      this.entityDialog.showErrorSnackbar(ProductStockAdjustmentForm.Texts.validationError);
      return;
    }

    this.entityDialog.showApiErrorSnackbar(err, ProductStockAdjustmentForm.Texts.saveError, stockApiErrorOptions);
  }
}
