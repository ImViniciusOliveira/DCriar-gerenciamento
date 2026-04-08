import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';

/**
 * Define a estrutura dos dados necessários para o diálogo de confirmação.
 */
export interface ConfirmDialogData {
  title: string;
  message?: string;
  messageHtml?: string;
}

/**
 * Um diálogo genérico e reutilizável para ações de confirmação (ex: "Tem certeza?").
 * Ele retorna `true` se o usuário confirmar e `false` se cancelar.
 */
@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss', // `styleUrl` (singular) é usado para um único arquivo de estilo.
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class ConfirmDialog {
  // Injeção de dependência moderna usando a função inject(), em vez do construtor.
  readonly dialogRef = inject(MatDialogRef<ConfirmDialog>);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  /**
   * Fecha o diálogo e retorna `false` para indicar que a ação foi cancelada.
   */
  onCancel(): void {
    this.dialogRef.close(false);
  }

  /**
   * Fecha o diálogo e retorna `true` para indicar que a ação foi confirmada.
   */
  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
