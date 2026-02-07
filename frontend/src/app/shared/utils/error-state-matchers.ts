import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';

/**
 * Matcher customizado para exibir erros imediatamente quando o controle é inválido e foi alterado (dirty),
 * sem esperar que o usuário saia do campo (touched).
 * Útil para validações em tempo real, como verificação de estoque.
 */
export class InstantErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
