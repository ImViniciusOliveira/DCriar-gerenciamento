import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { ProductionOrder } from '../../models/production.model';

export interface ProductionOrderFormData {
  template: ProductionOrder;
  title: string;
  isViewMode?: boolean;
}

/**
 * Formulário para visualização de Ordens de Produção.
 * Por enquanto, apenas modo de visualização (detalhes).
 */
@Component({
  selector: 'app-production-order-form',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule
  ],
  templateUrl: './production-order-form.html',
  styleUrl: './production-order-form.scss'
})
export class ProductionOrderForm {
  private readonly dialogRef = inject(MatDialogRef<ProductionOrderForm>);
  public readonly data: ProductionOrderFormData = inject(MAT_DIALOG_DATA);

  isViewMode = signal(true);

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getFormattedDimensions(): string {
    const order = this.data.template;
    if (order.larguraFinalCm && order.comprimentoFinalCm) {
      return `${order.larguraFinalCm} x ${order.comprimentoFinalCm} cm`;
    }
    return 'N/A';
  }

  getCalculationModeText(): string {
    const mode = this.data.template.modoCalculo;
    return mode === 'AUTOMATICO' ? 'Automático' : mode === 'MANUAL' ? 'Manual' : 'N/A';
  }
}
