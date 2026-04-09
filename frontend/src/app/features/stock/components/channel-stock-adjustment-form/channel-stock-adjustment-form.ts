import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdjustmentChannelSummary } from '../../models/stock-adjustment.model';
import { ChannelStockAdjustmentRequest, StockAdjustmentDirection, StockService } from '../../services/stock.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { clearApiFieldErrors, applyApiFieldErrors, extractApiErrorPayload, resolveApiErrorMessage } from '../../../../shared/utils/api-errors';
import { clearControlError, setControlError } from '../../../../shared/utils/control-errors';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';
import { stockApiErrorOptions } from '../../utils/stock-api-errors';

interface ChannelAdjustmentOption {
  value: StockAdjustmentDirection;
  label: string;
}

export interface ChannelStockAdjustmentFormData {
  template: AdjustmentChannelSummary;
  title: string;
}

@Component({
  selector: 'app-channel-stock-adjustment-form',
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
  templateUrl: './channel-stock-adjustment-form.html',
  styleUrls: ['./channel-stock-adjustment-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChannelStockAdjustmentForm {
  private static readonly BACKEND_FIELD_MAP: Record<string, string> = {
    quantidade: 'quantidade'
  };

  private static readonly BACKEND_ERROR_FIELDS = Object.values(ChannelStockAdjustmentForm.BACKEND_FIELD_MAP);

  private static readonly Texts = {
    saveError: 'Não foi possível ajustar o estoque do canal.',
    validationError: 'Revise os campos destacados.'
  };

  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly dialogRef = inject(MatDialogRef<ChannelStockAdjustmentForm>);
  private readonly stockService = inject(StockService);
  private readonly entityDialog = inject(EntityDialogService);
  readonly data: ChannelStockAdjustmentFormData = inject(MAT_DIALOG_DATA);

  readonly matcher = new InstantErrorStateMatcher();
  readonly isSaving = signal(false);
  readonly backendMaxQuantity = signal<number | null>(null);
  readonly options: ChannelAdjustmentOption[] = [
    { value: 'ADICIONAR', label: 'Adicionar ao canal' },
    { value: 'RETIRAR', label: 'Retirar do canal' }
  ];

  readonly form = this.fb.group({
    direcao: this.fb.control<StockAdjustmentDirection>('ADICIONAR', { validators: [Validators.required] }),
    quantidade: this.fb.control('', [Validators.required, Validators.pattern(/^[1-9]\d*$/)])
  });

  readonly selectedDirectionLockReason = computed(() => this.getDirectionLockReason(this.form.controls.direcao.getRawValue()));
  readonly maxAllowedQuantity = computed(() => this.resolveEffectiveMaxQuantity());

  readonly hasAvailableDirection = computed(() => this.options.some(option => !this.isDirectionLocked(option.value)));

  constructor() {
    ChannelStockAdjustmentForm.BACKEND_ERROR_FIELDS.forEach(controlPath => {
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

    const firstAvailable = this.options.find(option => !this.isDirectionLocked(option.value))?.value ?? 'ADICIONAR';
    this.form.controls.direcao.setValue(firstAvailable);
    this.updateQuantityValidators();
  }

  onSave(): void {
    if (this.isSaving()) {
      return;
    }

    if (this.form.invalid || !this.hasAvailableDirection()) {
      this.form.markAllAsTouched();
      this.entityDialog.showErrorSnackbar(ChannelStockAdjustmentForm.Texts.validationError);
      return;
    }

    const direction = this.form.controls.direcao.getRawValue();
    if (this.isDirectionLocked(direction)) {
      setControlError(this.form.controls.quantidade, 'backend', this.getDirectionLockReason(direction) ?? ChannelStockAdjustmentForm.Texts.saveError);
      this.form.controls.quantidade.markAsTouched();
      this.entityDialog.showErrorSnackbar(ChannelStockAdjustmentForm.Texts.validationError);
      return;
    }

    const request: ChannelStockAdjustmentRequest = {
      produtoId: this.data.template.produtoId,
      canalVendaId: this.data.template.canalVendaId,
      direcao: direction,
      quantidade: this.toAbsoluteQuantity()
    };

    clearApiFieldErrors(this.form, ChannelStockAdjustmentForm.BACKEND_ERROR_FIELDS);
    this.isSaving.set(true);

    this.stockService.adjustChannelStock(request).subscribe({
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

  isDirectionLocked(direction: StockAdjustmentDirection): boolean {
    const field = direction === 'ADICIONAR' ? 'ajusteCanalPositivo' : 'ajusteCanalNegativo';
    return hasLockedField(this.data.template, field);
  }

  getDirectionLockReason(direction: StockAdjustmentDirection): string | null {
    return direction === 'ADICIONAR'
      ? getLockedFieldReason(this.data.template, 'ajusteCanalPositivo')
      : getLockedFieldReason(this.data.template, 'ajusteCanalNegativo');
  }

  private toAbsoluteQuantity(): number {
    return Number(this.form.controls.quantidade.getRawValue());
  }

  private handleApiError(err: unknown): void {
    clearApiFieldErrors(this.form, ChannelStockAdjustmentForm.BACKEND_ERROR_FIELDS);
    this.applyBackendQuantityLimit(err);
    const hasFieldErrors = applyApiFieldErrors(this.form, err, {
      fieldMap: ChannelStockAdjustmentForm.BACKEND_FIELD_MAP,
      ...stockApiErrorOptions
    });

    const payload = extractApiErrorPayload(err);
    if ((payload.status === 400 || payload.status === 409) && !hasFieldErrors) {
      setControlError(
        this.form.controls.quantidade,
        'backend',
        resolveApiErrorMessage(err, ChannelStockAdjustmentForm.Texts.saveError, stockApiErrorOptions)
      );
      this.form.controls.quantidade.markAsTouched();
      this.entityDialog.showErrorSnackbar(ChannelStockAdjustmentForm.Texts.validationError);
      return;
    }

    if (hasFieldErrors) {
      this.entityDialog.showErrorSnackbar(ChannelStockAdjustmentForm.Texts.validationError);
      return;
    }

    this.entityDialog.showApiErrorSnackbar(err, ChannelStockAdjustmentForm.Texts.saveError, stockApiErrorOptions);
  }

  private resolveEffectiveMaxQuantity(): number | null {
    const backendMax = this.backendMaxQuantity();
    if (backendMax != null) {
      return backendMax;
    }

    const direction = this.form.controls.direcao.getRawValue();
    if (direction === 'ADICIONAR') {
      return Math.max(this.data.template.estoqueDisponivelParaAlocar ?? 0, 0);
    }

    return Math.max(this.data.template.quantidadeNoCanal ?? 0, 0);
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
}
