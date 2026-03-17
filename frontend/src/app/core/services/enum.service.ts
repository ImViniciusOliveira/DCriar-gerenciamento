import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {map, Observable, of} from 'rxjs';
import {catchError, shareReplay} from 'rxjs/operators';

/**
 * Representa uma opção de enum com seu valor (chave), texto de exibição e símbolo opcional.
 */
export interface EnumOption {
  value: string;
  viewValue: string;
  simbolo?: string;
  compatibleInputUnit?: string;
  compatibleInputFactor?: number;
}

/**
 * Interface para descrever um item individual na resposta da API de enums.
 */
interface EnumResponseItem {
  name: string;
  descricao: string;
  simbolo: string;
  unidadeCadastroCompativel?: string;
  fatorConversaoCadastroCompativel?: number;
  [key: string]: any; // Permite outras propriedades como 'simbolo' e '_links'.
}

/**
 * Interface genérica para a estrutura de resposta HATEOAS com `_embedded`.
 */
interface EmbeddedEnumResponse {
  _embedded: { [key: string]: EnumResponseItem[] };
}

/**
 * Serviço responsável por buscar e cachear opções de enums da API.
 * Ele utiliza `shareReplay` para evitar requisições HTTP repetidas para o mesmo enum.
 */
@Injectable({
  providedIn: 'root'
})
export class EnumService {
  private readonly http = inject(HttpClient);
  private readonly optionsCache = new Map<string, Observable<EnumOption[]>>();
  private readonly unitsMapCache = new Map<string, Observable<Map<string, EnumOption>>>();

  /**
   * Retorna um Observable com um mapa de unidades de consumo.
   * A requisição é cacheada por URL para evitar chamadas repetidas.
   */
  getConsumptionUnitsMap(url: string): Observable<Map<string, EnumOption>> {
    const cacheKey = `${url}::unidadesDeMedida::map`;
    const cached = this.unitsMapCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const request$ = this.getEnumOptions(url, 'unidadesDeMedida').pipe(
      map(options => new Map(options.map(opt => [opt.value, opt]))),
      shareReplay(1)
    );

    this.unitsMapCache.set(cacheKey, request$);
    return request$;
  }

  /**
   * Busca opções de enum de uma URL específica da API.
   * A requisição é cacheada por URL e chave `_embedded` para evitar chamadas repetidas.
   * @param url A URL do endpoint do enum.
   * @param embeddedKey A chave dentro do objeto `_embedded` onde as opções do enum estão localizadas.
   * @returns Um Observable que emite um array de `EnumOption`.
   */
  getEnumOptions(url: string, embeddedKey: string): Observable<EnumOption[]> {
    const cacheKey = `${url}::${embeddedKey}`;
    const cached = this.optionsCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const request$ = this.http.get<EmbeddedEnumResponse>(url).pipe(
      map(response => {
        const embedded = response?._embedded;
        if (!embedded) {
          return [];
        }

        const items = embedded[embeddedKey] || [];

        return items.map(item => ({
          value: item.name,
          viewValue: item.descricao,
          simbolo: item.simbolo,
          compatibleInputUnit: item.unidadeCadastroCompativel,
          compatibleInputFactor: item.fatorConversaoCadastroCompativel
        }));
      }),
      shareReplay(1),
      catchError(() => of([]))
    );

    this.optionsCache.set(cacheKey, request$);
    return request$;
  }
}
