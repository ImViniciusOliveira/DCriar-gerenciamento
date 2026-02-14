import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProductionOrder } from '../../models/production.model';

export interface ProductionFormData {
  template?: ProductionOrder;
  title: string;
  isViewMode?: boolean;
}

@Component({
  selector: 'app-production-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatOptionModule
  ],
  templateUrl: './production-form.html',
  styleUrls: ['./production-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProductionForm>);
  private readonly cdr = inject(ChangeDetectorRef);
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isSaving = signal(false);
  filterOperator = signal<'GTE' | 'LTE'>('GTE'); // ≥ ou ≤

  constructor() {
    this.form = this.fb.group({
      produtoId: [null],
      filtroEstoque: [null],
      quantidade: [null, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.cdr.markForCheck();
  }

  /**
   * Toggle entre ≥ (GTE) e ≤ (LTE)
   */
  toggleFilterOperator(): void {
    this.filterOperator.set(
      this.filterOperator() === 'GTE' ? 'LTE' : 'GTE'
    );
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();
    console.log('📋 DEBUG: Salvando ordem de produção', formValue);

    setTimeout(() => {
      this.isSaving.set(false);
      this.dialogRef.close(true);
      this.cdr.markForCheck();
    }, 500);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}



