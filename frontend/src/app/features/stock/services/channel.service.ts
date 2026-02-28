import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay, switchMap, of, catchError } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseChannels, Channel } from '../models/channel.model';

/**
 * Serviço responsável pelo gerenciamento de Canais de Venda.
 * Fornece métodos para buscar a lista de canais disponíveis.
 */
@Injectable({
  providedIn: 'root'
})
export class ChannelService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    shareReplay(1)
  );

  /**
   * Busca todos os canais de venda disponíveis.
   * O resultado é cacheado (shareReplay) para evitar requisições desnecessárias,
   * já que a lista de canais muda com pouca frequência.
   */
  getAllChannels(): Observable<Channel[]> {
    return this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints?._links?.['canais-venda']?.href;
        if (!url) {
          return of([]);
        }

        const cleanUrl = url.split('{')[0];

        return this.http.get<ApiResponseChannels>(cleanUrl).pipe(
          map(response => response._embedded?.['canais-venda'] || []),
          catchError(() => of([]))
        );
      }),
      shareReplay(1)
    );
  }
}
