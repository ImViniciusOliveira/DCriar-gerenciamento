import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';

import { ProductionOrder } from '../../models/production.model';
import { Product } from '../../../products/models/product.model';
import { ProductStockSearch } from '../../../../shared/components/product-stock-search/product-stock-search';

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
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ProductStockSearch
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
  produto = signal<Product | null>(null);

  constructor() {
    this.form = this.fb.group({
      tipoProducao: [null, Validators.required],
      produtoId: [null],
      quantidade: [null, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.cdr.markForCheck();
  }

  /**
   * Getter para o FormControl de tipo de produção
   */
  get tipoProducaoControl(): FormControl {
    return this.form.get('tipoProducao') as FormControl;
  }

  /**
   * Getter para o FormControl de produto, seguindo padrão de MaterialTypeSearch
   */
  get produtoControl(): FormControl {
    return this.form.get('produtoId') as FormControl;
  }

  /**
   * Callback quando um produto é selecionado
   */
  onProdutoChange(event: MatSelectChange): void {
    const produto = event.value as Product;
    this.produto.set(produto);
    this.form.patchValue({
      produtoId: produto.id
    });
  }


  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();
    const payload = {
      produtoId: formValue.produtoId,
      quantidade: Number(formValue.quantidade)
    };
    console.log('📋 DEBUG: Salvando ordem de produção', payload);

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










