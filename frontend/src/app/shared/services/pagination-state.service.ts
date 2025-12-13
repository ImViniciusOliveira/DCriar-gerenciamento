import { Injectable } from '@angular/core';
import { Sort } from '@angular/material/sort';

/**
 * Define a estrutura do estado de paginação que será salvo.
 */
export interface PaginationState {
  pageIndex: number;
  pageSize: number;
  sort: Sort;
}

/**
 * Serviço singleton (`providedIn: 'root'`) que armazena em memória o estado
 * da paginação de múltiplas tabelas durante a sessão do usuário.
 *
 * Funciona como um mapa chave-valor, onde a chave é um identificador único
 * para cada lista (ex: 'products', 'batches').
 */
@Injectable({
  providedIn: 'root'
})
export class PaginationStateService {
  // O Map privado que armazena o estado.
  private stateMap = new Map<string, PaginationState>();

  /**
   * Salva ou atualiza o estado de paginação para uma lista específica.
   * @param key A chave única que identifica a lista.
   * @param state O objeto de estado a ser salvo.
   */
  setState(key: string, state: PaginationState): void {
    this.stateMap.set(key, state);
  }

  /**
   * Recupera o estado de paginação salvo para uma lista específica.
   * @param key A chave única que identifica a lista.
   * @returns O estado salvo ou `undefined` se não houver estado para a chave.
   */
  getState(key: string): PaginationState | undefined {
    return this.stateMap.get(key);
  }

  /**
   * Limpa o estado de uma lista específica. (Opcional, pode ser útil para debug)
   */
  clearState(key: string): void {
    this.stateMap.delete(key);
  }

  /**
   * Limpa todos os estados de paginação salvos.
   */
  clearAllStates(): void {
    this.stateMap.clear();
  }
}
