import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface DetailsDialogItem {
  label?: string;
  value: string | number;
}

export interface DetailsDialogData {
  title: string;
  items: DetailsDialogItem[];
  showLabels?: boolean;
}

@Component({
  selector: 'app-details-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  templateUrl: './details-dialog.html',
  styleUrl: './details-dialog.scss'
})
export class DetailsDialog {
  readonly dialogRef = inject(MatDialogRef<DetailsDialog>);
  readonly data = inject<DetailsDialogData>(MAT_DIALOG_DATA);

  close(): void {
    this.dialogRef.close();
  }
}
