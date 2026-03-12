import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, filter, switchMap, shareReplay, combineLatest, of, catchError, tap, throwError, take } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { ApiResponseProduction } from '../models/production.model';
import { SimulationResult, SimulationCutResult } from '../models/simulation.model';

/**
 * Representa o payload enviado para a simulação.
 */
export interface SimulationRequest {
  produtoId: number;
  quantidade: number;
  loteId?: number;
  lotesConsumidosIds?: number[];
}

/**
 * Representa o payload enviado para a verificação de layout.
 */
export interface VerificationRequest {
  produtoId: number;
  loteId: number;
  quantidade: number;
  modoCalculo: string;
  margens?: {
    superior: number | null;
    inferior: number | null;
    esquerda: number | null;
    direita: number | null;
  };
  larguraFinalCm?: number | null;
  comprimentoFinalCm?: number | null;
  larguraBlocoProdutosCm?: number | null;
  comprimentoBlocoProdutosCm?: number | null;
}

/**
 * Representa o payload para criar uma ordem de produção por corte.
 */
export interface CreateCutOrderRequest {
  produtoId: number;
  lotePrincipalId: number;
  quantidadeProduzida: number;
  modoCalculo: string;
  larguraFinalCm: number;
  comprimentoFinalCm: number;
  canalVendaDestinoId?: number | null;
  motivo?: string | null;
  margens?: {
    superior: number | null;
    inferior: number | null;
    esquerda: number | null;
    direita: number | null;
  };
}

/**
 * Serviço para gerenciamento de Ordens de Produção.
 *
 * Implementa uma arquitetura reativa com Signals para gerenciar o estado da busca
 * e atualiza a lista de ordens automaticamente.
 */
@Injectable({
  providedIn: 'root'
})
export class ProductionService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  private readonly searchParams = signal<{
    page: number;
    size: number;
    sort: string;
  }>({
    page: 0,
    size: 10,
    sort: 'dataCriacao,desc'
  }, {
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly searchParams$ = toObservable(this.searchParams);

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
    shareReplay(1)
  );

  /**
   * Observable reativo que emite a lista de ordens de produção.
   * Atualiza automaticamente quando os parâmetros de busca mudam ou um refresh é acionado.
   */
  readonly productionOrders$: Observable<ApiResponseProduction>;

  constructor() {
    this.productionOrders$ = this.endpoints$.pipe(
      switchMap(endpoints => {
        const url = endpoints._links?.['ordens-de-producao']?.href;
        if (!url) {
          return of(this.createEmptyResponse());
        }
        const baseUrl = url.split('{')[0];

        return combineLatest([
          this.searchParams$,
          this.refresh$
        ]).pipe(
          switchMap(([params, _]) => {
            const httpParams = new HttpParams()
              .set('page', params.page.toString())
              .set('size', params.size.toString())
              .set('sort', params.sort);

            return this.http.get<ApiResponseProduction>(baseUrl, { params: httpParams }).pipe(
              catchError(() => of(this.createEmptyResponse()))
            );
          })
        );
      }),
      shareReplay(1)
    );
  }

  /**
   * Executa a simulação de produção no backend.
   * @param url A URL completa para o endpoint de simulação (descoberta via HATEOAS).
   * @param payload Os dados para a simulação (produtoId e quantidade).
   * @returns Um Observable com a resposta da simulação.
   */
  simulateProduction(url: string, payload: SimulationRequest): Observable<SimulationResult> {
    return this.http.post<SimulationResult>(url, payload);
  }

  /**
   * Executa a verificação de layout de corte no backend.
   * @param payload Os dados editados para verificação.
   * @returns Um Observable com o novo resultado da simulação.
   */
  verifyCutLayout(payload: VerificationRequest): Observable<SimulationCutResult> {
    // Como o endpoint de verificação não está no ApiRoot (é um sub-recurso),
    // construímos a URL a partir do base path de ordens de produção.
    return this.endpoints$.pipe(
      take(1),
      switchMap((endpoints: any) => {
        const url = endpoints._links?.['ordens-de-producao']?.href;
        if (!url) return throwError(() => new Error('Endpoint de ordens de produção não encontrado.'));
        const baseUrl = url.split('{')[0];
        return this.http.post<SimulationCutResult>(`${baseUrl}/verificar-corte`, payload);
      })
    );
  }

  /**
   * Cria uma nova ordem de produção por corte.
   * @param url A URL completa para o endpoint de criação (descoberta via HATEOAS).
   * @param payload Os dados da ordem de produção.
   * @returns Um Observable com a resposta da criação.
   */
  createCutOrder(url: string, payload: CreateCutOrderRequest): Observable<any> {
    return this.http.post<any>(url, payload).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  /**
   * Atualiza os parâmetros de busca, disparando uma nova requisição.
   */
  updateSearchParams(params: Partial<{ page: number; size: number; sort: string; }>): void {
    this.searchParams.update(current => ({ ...current, ...params }));
  }

  /**
   * Remove uma ordem de produção.
   * Trata o erro 404 (Not Found) como sucesso (idempotência).
   */
  delete(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(undefined);
        }
        return throwError(() => error);
      }),
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  private createEmptyResponse(): ApiResponseProduction {
    return {
      _embedded: { ordensDeProducao: [] },
      _links: {},
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 }
    };
  }
}
