import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

import { Channel, ChannelRequest } from '../../models/channel.model';
import { ChannelService } from '../../services/channel.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';

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
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ChannelForm>);
  private readonly channelService = inject(ChannelService);
  private readonly entityDialog = inject(EntityDialogService);
  public readonly data: ChannelFormData = inject(MAT_DIALOG_DATA);

  private static readonly Texts = {
    saveError: 'Falha ao cadastrar o canal. Verifique os dados e tente novamente.'
  };

  readonly matcher = new InstantErrorStateMatcher();
  readonly isSaving = signal(false);

  readonly form = this.fb.group({
    nome: [this.data.template?.nome || '', [Validators.required, Validators.maxLength(150)]]
  });

  onSave(): void {
    if (this.form.invalid || this.isSaving()) {
      this.form.markAllAsTouched();
      return;
    }

    const request: ChannelRequest = {
      nome: this.form.getRawValue().nome?.trim() || ''
    };

    if (!request.nome) {
      this.form.controls.nome.setErrors({ required: true });
      return;
    }

    this.isSaving.set(true);
    this.channelService.create(request).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.dialogRef.close(true);
      },
      error: () => {
        this.isSaving.set(false);
        this.entityDialog.showErrorSnackbar(ChannelForm.Texts.saveError);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
