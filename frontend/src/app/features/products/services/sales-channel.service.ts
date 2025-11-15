import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, filter, map, shareReplay, switchMap, take } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';
import { ApiRoot } from '../../../core/services/api-root';
import { Hateoas } from '../../../core/models/hateoas.model';

export interface SalesChannel {
  id: number;
  nome: string;
  ativo: boolean;
  _links?: any;
}

interface ApiResponseSalesChannels extends Hateoas {
  _embedded: {
    'canais-venda': SalesChannel[];
  };
}

@Injectable({
  providedIn: 'root',
})
export class SalesChannelService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  // Um fluxo único e cacheado de todos os canais de venda da API.
  private readonly allChannels$: Observable<SalesChannel[]> = this.fetchAllSalesChannels().pipe(
    shareReplay(1)
  );

  // Expõe um mapa reativo de Nome do Canal -> Nome do Canal.
  readonly channelNameMap$: Observable<Map<string, string>> = this.allChannels$.pipe(
    map(channels => new Map(channels.map(c => [c.nome, c.nome])))
  );

  // Expõe uma lista reativa com os nomes de todos os canais.
  readonly channelKeys$: Observable<string[]> = this.allChannels$.pipe(
    map(channels => channels.map(c => c.nome))
  );

  private fetchAllSalesChannels(): Observable<SalesChannel[]> {
    return toObservable(this.apiRoot.endpoints).pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      take(1),
      map(endpoints => {
        const url = endpoints._links?.['canais-venda']?.href;
        if (!url) {
          throw new Error('URL de canais-venda não encontrada na resposta da API');
        }
        return url;
      }),
      switchMap(url => this.http.get<ApiResponseSalesChannels>(url)),
      map(response => response._embedded?.['canais-venda'] || [])
    );
  }
}
