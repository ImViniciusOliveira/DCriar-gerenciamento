import { Injectable, signal, computed } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';

/**
 * Serviço reutilizável para gerenciar o estado de paginação e ordenação de tabelas e listas.
 *
 * Centraliza a lógica de estado usando Sinais do Angular, permitindo que múltiplos
 * componentes compartilhem e reajam a mudanças de página, tamanho da página e ordenação.
 */
@Injectable({
  providedIn: 'root'
})
export class PaginationHandler {
  // --- Estado da Paginação ---
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly totalElements = signal(0);

  // --- Estado da Ordenação ---
  readonly sortActive = signal('id'); // Coluna de ordenação padrão.
  readonly sortDirection = signal<Sort['direction']>('asc');

  /**
   * Sinal computado que gera a string de ordenação para a API (ex: "nome,asc").
   * Reage automaticamente a qualquer mudança nos sinais `sortActive` ou `sortDirection`,
   * garantindo que a string para a API esteja sempre atualizada.
   */
  readonly sortString = computed(() => {
    const active = this.sortActive();
    const direction = this.sortDirection();
    // Se a direção não estiver definida, a API pode esperar apenas o nome do campo.
    return direction ? `${active},${direction}` : active;
  });

  /**
   * Atualiza o estado de paginação a partir de um evento do MatPaginator.
   * Este método serve como uma ponte entre o componente de UI e o estado do serviço.
   */
  handlePageEvent(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
  }

  /**
   * Atualiza o estado de ordenação a partir de um evento do MatSort.
   */
  handleSortChange(sort: Sort): void {
    // Se a direção for removida (cliques sucessivos), volta para a ordenação padrão.
    this.sortActive.set(sort.direction ? sort.active : 'id');
    this.sortDirection.set(sort.direction || 'asc');

    // Efeito colateral crucial: Ao reordenar, sempre volta para a primeira página
    // para evitar a visualização de uma página que pode não existir com a nova ordem.
    this.pageIndex.set(0);
  }

  /**
   * Atualiza o número total de elementos, geralmente com o valor vindo da resposta da API.
   * Essencial para que o paginador saiba quantas páginas exibir.
   */
  updateTotalElements(total: number): void {
    this.totalElements.set(total);
  }
}
