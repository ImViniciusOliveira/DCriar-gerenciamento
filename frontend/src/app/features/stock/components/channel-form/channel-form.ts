import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Channel, ChannelRequest } from '../../models/channel.model';
import { ChannelService } from '../../services/channel.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { applyApiFieldErrors, clearApiFieldErrors } from '../../../../shared/utils/api-errors';
import { clearControlError } from '../../../../shared/utils/control-errors';
import { stockApiErrorOptions } from '../../utils/stock-api-errors';

export interface ChannelFormData {
  template?: Partial<Channel>;
  title: string;
}

@Component({
  selector: 'app-channel-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './channel-form.html',
  styleUrls: ['./channel-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChannelForm {
  private static readonly BACKEND_FIELD_MAP: Record<string, string> = {
    nome: 'nome'
  };

  private static readonly BACKEND_ERROR_FIELDS = Object.values(ChannelForm.BACKEND_FIELD_MAP);

  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ChannelForm>);
  private readonly channelService = inject(ChannelService);
  private readonly entityDialog = inject(EntityDialogService);
  public readonly data: ChannelFormData = inject(MAT_DIALOG_DATA);

  private static readonly Texts = {
    saveError: 'Falha ao cadastrar o canal. Verifique os dados e tente novamente.',
    formValidationError: 'Revise os campos destacados.'
  };

  readonly matcher = new InstantErrorStateMatcher();
  readonly isSaving = signal(false);

  readonly form = this.fb.group({
    nome: [this.data.template?.nome || '', [Validators.required, Validators.maxLength(150)]]
  });

  constructor() {
    ChannelForm.BACKEND_ERROR_FIELDS.forEach(controlPath => {
      this.form.get(controlPath)?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => clearControlError(this.form.get(controlPath), 'backend'));
    });
  }

  onSave(): void {
    if (this.isSaving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.entityDialog.showErrorSnackbar(ChannelForm.Texts.formValidationError);
      return;
    }

    const request: ChannelRequest = {
      nome: this.form.getRawValue().nome?.trim() || ''
    };

    if (!request.nome) {
      this.form.controls.nome.setErrors({ required: true });
      this.form.controls.nome.markAsTouched();
      this.entityDialog.showErrorSnackbar(ChannelForm.Texts.formValidationError);
      return;
    }

    this.isSaving.set(true);
    this.channelService.create(request).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.isSaving.set(false);
        clearApiFieldErrors(this.form, ChannelForm.BACKEND_ERROR_FIELDS);
        const hasFieldErrors = applyApiFieldErrors(this.form, err, {
          fieldMap: ChannelForm.BACKEND_FIELD_MAP,
          ...stockApiErrorOptions
        });
        if (hasFieldErrors) {
          this.entityDialog.showErrorSnackbar('Revise os campos destacados.');
        } else {
          this.entityDialog.showApiErrorSnackbar(err, ChannelForm.Texts.saveError, stockApiErrorOptions);
        }
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
