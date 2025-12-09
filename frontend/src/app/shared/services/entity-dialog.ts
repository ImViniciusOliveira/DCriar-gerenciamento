import { Injectable, inject, Type } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ConfirmDialog, ConfirmDialogData } from '../components/confirm-dialog/confirm-dialog';

/**
 * Interface para configurar um diálogo de formulário genérico.
 * @template T O tipo de dados que o formulário manipula.
 */
export interface FormDialogData<T> {
  /** O componente que será renderizado dentro do diálogo. */
  component: Type<any>;
  /** Os dados a serem passados para o componente do formulário (ex: uma entidade para edição). */
  formData: T;
  /** O título a ser exibido no cabeçalho do diálogo. */
  title: string;
  width?: string;
  maxWidth?: string;
}

/**
 * Serviço Facade para centralizar e padronizar o uso de MatDialog e MatSnackBar.
 *
 * O objetivo é simplificar a abertura de diálogos comuns (confirmação, formulários)
 * e garantir uma experiência de usuário consistente para notificações (snackbars)
 * em toda a aplicação.
 */
@Injectable({
  providedIn: 'root'
})
export class EntityDialogService {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  /**
   * Abre um diálogo de confirmação de exclusão padrão.
   * @returns Um Observable que emite `true` **apenas se** o usuário confirmar a exclusão.
   *          Ele não emite nada se o usuário cancelar, simplificando o código que o chama.
   */
  openConfirmDeleteDialog(itemName: string, title: string = 'Confirmar Exclusão'): Observable<boolean> {
    const dialogData: ConfirmDialogData = {
      title,
      message: `Tem certeza que deseja excluir "${itemName}"?`,
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });

    // O uso de `filter` é a chave aqui: ele garante que o `subscribe` no código chamador
    // só será executado se o resultado for `true` (confirmação).
    return dialogRef.afterClosed().pipe(filter(result => result === true));
  }

  /**
   * Abre um diálogo de formulário genérico, renderizando o componente fornecido.
   * @param dialogConfig A configuração do diálogo, incluindo o componente e os dados.
   * @returns Um Observable que emite `true` **apenas se** o diálogo for fechado com um resultado positivo
   *          (ex: o formulário foi salvo com sucesso).
   */
  openFormDialog<T>(dialogConfig: FormDialogData<T>): Observable<boolean> {
    const dialogRef = this.dialog.open(dialogConfig.component, {
      data: dialogConfig.formData,
      width: dialogConfig.width || '90vw',
      maxWidth: dialogConfig.maxWidth || '900px',
      autoFocus: false,
    });

    // Assim como no diálogo de confirmação, o `filter` simplifica a lógica de "salvo com sucesso".
    return dialogRef.afterClosed().pipe(filter(result => result === true));
  }

  /**
   * Exibe uma notificação de sucesso padronizada.
   */
  showSuccessSnackbar(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 3000 });
  }

  /**
   * Exibe uma notificação de erro padronizada.
   */
  showErrorSnackbar(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 5000 });
  }
}
