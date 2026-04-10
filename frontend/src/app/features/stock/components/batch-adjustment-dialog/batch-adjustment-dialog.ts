import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

import { Batch } from '../../models/batch.model';
import { BatchAdjustmentForm } from '../batch-adjustment-form/batch-adjustment-form';
import { BatchMovementsSection } from '../batch-movements-section/batch-movements-section';

export interface BatchAdjustmentDialogData {
  template: Batch;
  title: string;
}

@Component({
  selector: 'app-batch-adjustment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    BatchAdjustmentForm,
    BatchMovementsSection
  ],
  templateUrl: './batch-adjustment-dialog.html',
  styleUrls: ['./batch-adjustment-dialog.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchAdjustmentDialog {
  private readonly dialogRef = inject(MatDialogRef<BatchAdjustmentDialog>);

  readonly data: BatchAdjustmentDialogData = inject(MAT_DIALOG_DATA);
  readonly batch = signal(this.data.template);

  close(): void {
    this.dialogRef.close();
  }

  onAdjustmentApplied(updatedBatch: Batch): void {
    this.batch.set(updatedBatch);
  }
}
