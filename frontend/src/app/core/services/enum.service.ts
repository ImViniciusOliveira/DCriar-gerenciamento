import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {map, Observable, of} from 'rxjs';
import {catchError, shareReplay} from 'rxjs/operators';

/**
 * Representa uma opção de enum com seu valor (chave), texto de exibição e símbolo opcional.
 */
export interface EnumOption {
  value: string;
  viewValue: string;
  pluralViewValue?: string;
  simbolo?: string;
  displayQuantityWithSymbol?: boolean;
  compatibleInputUnit?: string;
  compatibleInputFactor?: number;
}

/**
 * Interface para descrever um item individual na resposta da API de enums.
 */
interface EnumResponseItem {
  name: string;
  descricao: string;
  descricaoPlural?: string;
  simbolo: string;
  exibirQuantidadeComSimbolo?: boolean;
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

  getMeasurementUnitsByProductType(url: string, tipoProduto: 'CORTE' | 'CONSUMO'): Observable<EnumOption[]> {
    const cacheKey = `${url}::unidadesDeMedida::${tipoProduto}`;
    const cached = this.optionsCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const request$ = this.http.get<EmbeddedEnumResponse>(url, {
      params: new HttpParams().set('tipoProduto', tipoProduto)
    }).pipe(
      map(response => {
        const embedded = response?._embedded;
        if (!embedded) {
          return [];
        }

        const items = embedded['unidadesDeMedida'] || [];

        return items.map(item => ({
          value: item.name,
          viewValue: item.descricao,
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
          pluralViewValue: item.descricaoPlural,
          simbolo: item.simbolo,
          displayQuantityWithSymbol: item.exibirQuantidadeComSimbolo,
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

  formatQuantityWithUnit(
    amount: number | string | null | undefined,
    unit?: Pick<EnumOption, 'viewValue' | 'pluralViewValue' | 'simbolo' | 'displayQuantityWithSymbol'> | null
  ): string {
    const normalizedAmount = amount ?? 0;
    const amountLabel = typeof normalizedAmount === 'number' ? this.formatAmount(normalizedAmount) : String(normalizedAmount);

    if (!unit) {
      return amountLabel;
    }

    if (unit.displayQuantityWithSymbol && unit.simbolo) {
      return `${amountLabel}${unit.simbolo}`;
    }

    if (!unit.viewValue) {
      return amountLabel;
    }

    const unitLabel = Number(normalizedAmount) === 1 ? unit.viewValue : (unit.pluralViewValue ?? unit.viewValue);
    return `${amountLabel} ${unitLabel}`;
  }

  buildFallbackUnitOption(
    viewValue?: string | null,
    pluralViewValue?: string | null,
    simbolo?: string | null,
    displayQuantityWithSymbol?: boolean | null
  ): EnumOption | null {
    if (!viewValue && !simbolo) {
      return null;
    }

    return {
      value: '',
      viewValue: viewValue ?? '',
      pluralViewValue: pluralViewValue ?? undefined,
      simbolo: simbolo ?? undefined,
      displayQuantityWithSymbol: displayQuantityWithSymbol ?? false
    };
  }

  private formatAmount(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 3
    }).format(value);
  }
}
