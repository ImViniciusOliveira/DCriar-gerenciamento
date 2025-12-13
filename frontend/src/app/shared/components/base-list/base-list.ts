import { Directive, WritableSignal, effect, inject, signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { PaginationHandler } from '../../services/pagination-handler';
import { EntityDialogService } from '../../services/entity-dialog';

/**
 * Uma classe base abstrata para componentes de lista que usam paginação e ordenação.
 *
 * Esta classe adota um padrão reativo: ela usa um `effect` para observar
 * mudanças nos sinais de paginação/ordenação e dispara automaticamente o
 * recarregamento dos dados, eliminando a necessidade de chamadas manuais
 * em múltiplos lugares.
 */
@Directive()
export abstract class BaseList<T> {
  // --- SERVIÇOS BASE ---
  readonly pagination = inject(PaginationHandler);
  protected readonly entityDialog = inject(EntityDialogService);

  // --- ESTADO BASE ---
  items: WritableSignal<T[]> = signal([]);

  // --- MÉTODO ABSTRATO (Obrigatório para classes filhas) ---

  /**
   * Contém a lógica específica para carregar os itens da API.
   * A classe filha deve implementar este método para buscar os dados
   * usando os sinais do `PaginationHandler`.
   */
  abstract loadItems(): void;

  protected constructor() {
    // --- LÓGICA REATIVA ---
    // Este `effect` é o coração da classe base. Ele cria uma dependência
    // com os sinais de paginação e ordenação.
    effect(() => {
      // 1. Lê os sinais. Qualquer mudança em um deles fará o `effect` ser executado novamente.
      this.pagination.pageIndex();
      this.pagination.pageSize();
      this.pagination.sortString(); // Este é um `computed` que depende da ordenação.

      // 2. Dispara o recarregamento.
      // Como o `effect` roda uma vez na inicialização, ele substitui a necessidade
      // de chamar `loadItems()` no `ngOnInit`.
      this.loadItems();
    });
  }

  // --- MANIPULADORES DE EVENTOS (Lógica Simplificada) ---

  /**
   * Manipula o evento de mudança de página.
   * Sua única responsabilidade agora é atualizar o estado no `PaginationHandler`.
   * O `effect` cuidará do recarregamento dos dados.
   */
  onPageChange(event: PageEvent): void {
    this.pagination.handlePageEvent(event);
  }

  /**
   * Manipula o evento de mudança de ordenação.
   * Sua única responsabilidade agora é atualizar o estado no `PaginationHandler`.
   * O `effect` cuidará do recarregamento dos dados.
   */
  onSortChange(sort: Sort): void {
    this.pagination.handleSortChange(sort);
  }
}
