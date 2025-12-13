import { Injectable, signal, computed, inject } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { PaginationState, PaginationStateService } from './pagination-state.service';

/**
 * Serviço para gerenciar o estado de paginação e ordenação de uma tabela/lista específica.
 *
 * Esta classe é projetada para ser fornecida no nível do componente (`providers: [PaginationHandler]`),
 * garantindo que cada lista tenha sua própria instância e estado isolado.
 *
 * Ele se comunica com o `PaginationStateService` para persistir seu estado em memória
 * durante a sessão do usuário, associado a uma chave única.
 */
@Injectable()
export class PaginationHandler {
  // --- Injeção de Dependências ---
  private readonly stateService = inject(PaginationStateService);

  // --- Estado Interno ---
  private listId: string | null = null;

  // --- Estado da Paginação ---
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly totalElements = signal(0);

  // --- Estado da Ordenação ---
  readonly sortActive = signal('id');
  readonly sortDirection = signal<Sort['direction']>('asc');

  /**
   * Sinal computado que gera a string de ordenação para a API (ex: "nome,asc").
   */
  readonly sortString = computed(() => {
    const active = this.sortActive();
    const direction = this.sortDirection();
    return direction ? `${active},${direction}` : active;
  });

  /**
   * Inicializa o handler com uma chave única e restaura o estado salvo, se existir.
   * Este método deve ser chamado no construtor do componente de lista.
   * @param key A chave única que identifica a lista (ex: 'products', 'batches').
   */
  initialize(key: string): void {
    this.listId = key;
    const savedState = this.stateService.getState(key);

    if (savedState) {
      // Restaura o estado salvo do serviço global.
      this.pageSize.set(savedState.pageSize);
      this.pageIndex.set(savedState.pageIndex);
      this.sortActive.set(savedState.sort.active);
      this.sortDirection.set(savedState.sort.direction);
    }
  }

  /**
   * Atualiza o estado de paginação e o salva no serviço de persistência.
   */
  handlePageEvent(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
    this.saveState();
  }

  /**
   * Atualiza o estado de ordenação, reseta para a primeira página e salva o estado.
   */
  handleSortChange(sort: Sort): void {
    this.sortActive.set(sort.direction ? sort.active : 'id');
    this.sortDirection.set(sort.direction || 'asc');
    this.pageIndex.set(0); // Sempre volta para a primeira página ao reordenar.
    this.saveState();
  }

  updateTotalElements(total: number): void {
    this.totalElements.set(total);
  }

  /**
   * Salva o estado atual da paginação e ordenação no serviço de persistência.
   */
  private saveState(): void {
    if (!this.listId) {
      // Evita salvar o estado se o handler não foi inicializado com uma chave.
      return;
    }

    const currentState: PaginationState = {
      pageSize: this.pageSize(),
      pageIndex: this.pageIndex(),
      sort: {
        active: this.sortActive(),
        direction: this.sortDirection()
      }
    };
    this.stateService.setState(this.listId, currentState);
  }
}
