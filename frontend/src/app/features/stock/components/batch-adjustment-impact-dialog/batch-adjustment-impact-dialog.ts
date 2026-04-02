import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { BatchAdjustmentImpactItem } from '../../models/batch.model';

export interface BatchAdjustmentImpactDialogData {
  items: BatchAdjustmentImpactItem[];
  selectedIds: number[];
}

export interface BatchAdjustmentImpactDialogResult {
  selectedIds: number[];
}

@Component({
  selector: 'app-batch-adjustment-impact-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatCheckboxModule],
  templateUrl: './batch-adjustment-impact-dialog.html',
  styleUrl: './batch-adjustment-impact-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchAdjustmentImpactDialog {
  readonly dialogRef = inject(MatDialogRef<BatchAdjustmentImpactDialog, BatchAdjustmentImpactDialogResult | false>);
  readonly data = inject<BatchAdjustmentImpactDialogData>(MAT_DIALOG_DATA);

  readonly selectedIds = signal<number[]>(this.data.selectedIds);
  readonly selectedCount = computed(() => this.selectedIds().length);

  isSelected(itemId: number): boolean {
    return this.selectedIds().includes(itemId);
  }

  onToggle(itemId: number, event: MatCheckboxChange): void {
    this.selectedIds.update(current =>
      event.checked
        ? Array.from(new Set([...current, itemId]))
        : current.filter(id => id !== itemId)
    );
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close({
      selectedIds: this.selectedIds()
    });
  }
}
