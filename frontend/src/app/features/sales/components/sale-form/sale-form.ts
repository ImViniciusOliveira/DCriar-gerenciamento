import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-sale-form',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Formulário de Venda</h2>
    <mat-dialog-content>
      <p>Aqui vai o formulário de venda...</p>
      <p>ID: {{ data.sale?.id || 'Nova Venda' }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-raised-button color="primary" [mat-dialog-close]="true">Salvar</button>
    </mat-dialog-actions>
  `,
})
export class SaleFormComponent {
  constructor(
    public dialogRef: MatDialogRef<SaleFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { sale: any }
  ) {}
}
