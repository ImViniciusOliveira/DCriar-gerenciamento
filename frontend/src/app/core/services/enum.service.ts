import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {map, Observable, of} from 'rxjs';
import {catchError, shareReplay} from 'rxjs/operators';

export interface EnumOption {
  value: string;
  viewValue: string;
  simbolo?: string;
}

// Interface para descrever um item individual na resposta da API de enums.
interface EnumResponseItem {
  name: string;
  descricao: string;
  simbolo: string;
  [key: string]: any; // Permite outras propriedades como 'simbolo' e '_links'.
}

// Interface genérica para a estrutura de resposta HATEOAS com _embedded.
interface EmbeddedEnumResponse {
  _embedded: { [key: string]: EnumResponseItem[] };
}

@Injectable({
  providedIn: 'root'
})
export class EnumService {
  private readonly http = inject(HttpClient);
  private cache: { [key: string]: Observable<any> } = {};

  /**
   * Retorna um Observable com um mapa de unidades de consumo.
   * O mapa é cacheado por URL para evitar requisições repetidas.
   */
  getConsumptionUnitsMap(url: string): Observable<Map<string, EnumOption>> {
    const cacheKey = `map_${url}`;
    if (!this.cache[cacheKey]) {
      this.cache[cacheKey] = this.getEnumOptions(url, 'unidadesDeMedida').pipe(
        map(options => new Map(options.map(opt => [opt.value, opt]))),
        shareReplay(1)
      );
    }
    return this.cache[cacheKey];
  }

  getEnumOptions(url: string, embeddedKey: string): Observable<EnumOption[]> {
    const cacheKey = `${url}_${embeddedKey}`;
    if (!this.cache[cacheKey]) {
      this.cache[cacheKey] = this.http.get<EmbeddedEnumResponse>(url).pipe(
        map(response => {
          const embedded = response?._embedded;
          if (!embedded) {
            return [];
          }

          const items = embedded[embeddedKey] || [];

          return items.map(item => ({
            value: item.name,
            viewValue: item.descricao,
            simbolo: item.simbolo
          }));
        }),
        shareReplay(1),
        catchError(err => {
          console.error(`Falha ao buscar enum da URL: ${url}`, err);
          return of([]);
        })
      );
    }
    return this.cache[cacheKey];
  }
}
