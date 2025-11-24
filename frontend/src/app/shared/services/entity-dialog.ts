import { Injectable, inject, Type } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ConfirmDialog, ConfirmDialogData } from '../components/confirm-dialog/confirm-dialog';

export interface FormDialogData<T> {
  component: Type<any>;
  formData: T;
  title: string;
  width?: string;
  maxWidth?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EntityDialogService {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  openConfirmDeleteDialog(itemName: string, title: string = 'Confirmar Exclusão'): Observable<boolean> {
    const dialogData: ConfirmDialogData = {
      title,
      message: `Tem certeza que deseja excluir "${itemName}"?`,
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    return dialogRef.afterClosed().pipe(filter(result => result === true));
  }

  openFormDialog<T>(dialogConfig: FormDialogData<T>): Observable<boolean> {
    const dialogRef = this.dialog.open(dialogConfig.component, {
      data: dialogConfig.formData,
      width: dialogConfig.width || '90vw',
      maxWidth: dialogConfig.maxWidth || '900px',
      autoFocus: false,
    });

    return dialogRef.afterClosed().pipe(filter(result => result === true));
  }

  showSuccessSnackbar(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 3000 });
  }

  showErrorSnackbar(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 5000 });
  }
}
