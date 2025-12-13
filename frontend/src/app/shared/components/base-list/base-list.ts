import { Directive, WritableSignal, effect, inject, signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { PaginationHandler } from '../../services/pagination-handler';
import { EntityDialogService } from '../../services/entity-dialog';

/**
 * Classe base abstrata para componentes de lista.
 *
 * Centraliza a lógica de estado de paginação e reatividade.
 * As classes filhas devem passar um identificador único (`listId`) no construtor
 * para permitir a persistência do estado da paginação.
 */
@Directive()
export abstract class BaseList<T> {
  // --- SERVIÇOS BASE ---
  readonly pagination = inject(PaginationHandler);
  protected readonly entityDialog = inject(EntityDialogService);

  // --- ESTADO BASE ---
  items: WritableSignal<T[]> = signal([]);

  /**
   * Contém a lógica específica para carregar os itens da API.
   */
  abstract loadItems(): void;

  /**
   * @param listId Identificador único para a lista (ex: 'products'), usado para persistir o estado.
   */
  protected constructor(protected readonly listId: string) {
    // Inicializa o handler de paginação com a chave única da lista.
    // Isso restaura o estado salvo (se houver) para esta lista específica.
    this.pagination.initialize(this.listId);

    // Este `effect` reage a mudanças nos sinais de paginação/ordenação.
    effect(() => {
      // A simples leitura dos sinais cria a dependência.
      this.pagination.pageIndex();
      this.pagination.pageSize();
      this.pagination.sortString();

      // Dispara o recarregamento dos dados.
      this.loadItems();
    });
  }

  /**
   * Manipula o evento de mudança de página do paginador.
   */
  onPageChange(event: PageEvent): void {
    this.pagination.handlePageEvent(event);
  }

  /**
   * Manipula o evento de mudança de ordenação da tabela.
   */
  onSortChange(sort: Sort): void {
    this.pagination.handleSortChange(sort);
  }
}
