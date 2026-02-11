import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProductionOrder } from '../../models/production.model';
import { ProductionService } from '../../services/production.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';

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
    MatProgressSpinnerModule
  ],
  templateUrl: './production-form.html',
  styleUrls: ['./production-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProductionForm>);
  private readonly productionService = inject(ProductionService);
  private readonly entityDialog = inject(EntityDialogService);
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
      larguraFinalCm: [{ value: '', disabled: true }],
      comprimentoFinalCm: [{ value: '', disabled: true }],
      motivo: [{ value: '', disabled: true }],
      dataCriacao: [{ value: '', disabled: true }]
    });
  }

  ngOnInit(): void {
    if (this.data.order) {
      this.initializeForm(this.data.order);
    }
  }

  private initializeForm(order: ProductionOrder): void {
    this.form.patchValue({
      produtoNome: order.nomeProduto,
      quantidadeProduzida: order.quantidadeProduzida,
      modoCalculo: order.modoCalculo,
      larguraFinalCm: order.larguraFinalCm,
      comprimentoFinalCm: order.comprimentoFinalCm,
      motivo: order.motivo,
      dataCriacao: new Date(order.dataCriacao).toLocaleString()
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
