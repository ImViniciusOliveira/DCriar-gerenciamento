import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { ProductionOrder } from '../../models/production.model';

export interface ProductionFormData {
  order?: ProductionOrder;
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
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatCheckboxModule
  ],
  templateUrl: './production-form.html',
  styleUrls: ['./production-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProductionForm>);
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isSaving = signal(false);
  isViewMode = signal(false);

  constructor() {
    this.isViewMode.set(!!this.data.isViewMode);

    this.form = this.fb.group({
      produtoNome: [{ value: '', disabled: true }],
      quantidadeProduzida: [{ value: '', disabled: true }],
      modoCalculo: [{ value: '', disabled: true }],
      dimensoes: [{ value: '', disabled: true }],
      motivo: [{ value: '', disabled: true }],
      dataCriacao: [{ value: '', disabled: true }],
      rotacionado: [{ value: false, disabled: true }],
      margemSuperior: [{ value: '', disabled: true }],
      margemInferior: [{ value: '', disabled: true }],
      margemEsquerda: [{ value: '', disabled: true }],
      margemDireita: [{ value: '', disabled: true }]
    });
  }

  ngOnInit(): void {
    if (this.data.order) {
      this.initializeForm(this.data.order);
    }
  }

  private initializeForm(order: ProductionOrder): void {
    let dimensoesStr = '';
    if (order.larguraFinalCm && order.comprimentoFinalCm) {
      dimensoesStr = `${order.larguraFinalCm} x ${order.comprimentoFinalCm}`;
    }

    this.form.patchValue({
      produtoNome: order.nomeProduto,
      quantidadeProduzida: order.quantidadeProduzida,
      modoCalculo: order.modoCalculo,
      dimensoes: dimensoesStr,
      motivo: order.motivo,
      dataCriacao: new Date(order.dataCriacao).toLocaleString(),
      rotacionado: order.rotacionado,
      margemSuperior: order.margens?.superior,
      margemInferior: order.margens?.inferior,
      margemEsquerda: order.margens?.esquerda,
      margemDireita: order.margens?.direita
    });

    if (this.isViewMode()) {
      this.form.disable();
    }
  }

  onSave(): void {
    if (this.form.invalid) return;
    this.isSaving.set(true);
    // Lógica de salvar será implementada na etapa de criação
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
