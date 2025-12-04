import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {EntityDialogService} from './entity-dialog';

export interface CrudOperationFeedback<T> {
  /** A chamada de API (Observable) a ser executada. */
  request: Observable<T>;
  /** Mensagem a ser exibida em caso de sucesso. */
  successMessage: string;
  /** Mensagem a ser exibida em caso de erro. */
  errorMessage: string;
  /** Callback a ser executado após o sucesso (geralmente para recarregar a lista). */
  onSuccess: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class CrudHelperService {
  private readonly entityDialog = inject(EntityDialogService);

  /**
   * Executa uma operação de CRUD e gerencia o feedback ao usuário (snackbars)
   * e o recarregamento da lista de forma padronizada.
   *
   * @returns Um Observable que pode ser "subscribe" com segurança.
   */
  handleRequest<T>({ request, successMessage, errorMessage, onSuccess }: CrudOperationFeedback<T>): Observable<T> {
    return request.pipe(
      tap({
        next: () => {
          this.entityDialog.showSuccessSnackbar(successMessage);
          onSuccess();
        },
        error: (err) => {
          // No futuro, podemos adicionar um serviço de logging aqui.
          console.error(`CRUD Helper Error: ${errorMessage}`, err);
          this.entityDialog.showErrorSnackbar(errorMessage);
        }
      })
    );
  }
}
