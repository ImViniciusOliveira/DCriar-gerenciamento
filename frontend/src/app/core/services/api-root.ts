import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Hateoas } from '../models/hateoas.model';
import { shareReplay, catchError, of } from 'rxjs';
import { environment } from './environment';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toSignal } from '@angular/core/rxjs-interop';

/**
 * Serviço responsável por carregar e gerenciar os endpoints da API raiz (root).
 * Ele utiliza o padrão HATEOAS para descobrir dinamicamente as URLs dos recursos.
 */
@Injectable({
  providedIn: 'root',
})
export class ApiRoot {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  private readonly API_URL = `${environment.apiUrl}/api/v1`;

  // Observable que carrega os endpoints da API.
  // O `shareReplay(1)` garante que a requisição seja feita apenas uma vez e o resultado cacheado.
  private readonly _endpoints$ = this.http.get<Hateoas>(this.API_URL).pipe(
    catchError(() => {
      this.snackBar.open('Não foi possível conectar ao servidor. Tente novamente mais tarde.', 'Fechar', {
        duration: 7000,
      });
      return of({} as Hateoas); // Retorna um objeto Hateoas vazio em caso de erro.
    }),
    shareReplay(1)
  );

  // Sinal que armazena os endpoints da API, convertido do Observable `_endpoints$`.
  // `toSignal` lida com a subscrição e o ciclo de vida, e o `initialValue`
  // garante que o sinal sempre tenha um valor inicial.
  readonly endpoints = toSignal(this._endpoints$, { initialValue: {} as Hateoas });

  constructor() {}
}
