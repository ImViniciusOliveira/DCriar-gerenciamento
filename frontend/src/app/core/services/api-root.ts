import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Hateoas } from '../models/hateoas.model';
import { Observable, tap, shareReplay, take, catchError, of } from 'rxjs';
import { environment } from './environment';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class ApiRoot {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  private readonly API_URL = `${environment.apiUrl}/api/v1`;

  endpoints = signal<Hateoas | undefined>(undefined);

  endpoints$: Observable<Hateoas>;

  constructor() {
    this.endpoints$ = this.loadEndpoints();
    // A subscrição inicial garante que os endpoints sejam carregados na inicialização da aplicação.
    // O take(1) finaliza o observable após a primeira emissão, evitando memory leaks (vazamentos de memória).
    this.endpoints$.pipe(take(1)).subscribe();
  }

  loadEndpoints(): Observable<Hateoas> {
    return this.http
      .get<Hateoas>(this.API_URL)
      .pipe(
        catchError(() => {
          // Mensagem amigável para o usuário
          this.snackBar.open('Não foi possível conectar ao servidor. Tente novamente mais tarde.', 'Fechar', {
            duration: 7000,
          });

          return of({} as Hateoas);
        }),
        tap(endpoints => this.endpoints.set(endpoints)),
        shareReplay(1) // Cacheia o resultado para evitar múltiplas chamadas à raiz da API.
      );
  }
}
