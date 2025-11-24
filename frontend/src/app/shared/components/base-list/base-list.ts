import { Directive, OnInit, WritableSignal, inject, signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import {PaginationHandler} from '../../services/pagination-handler';
import {EntityDialogService} from '../../services/entity-dialog';

@Directive() // Diretiva base para componentes de lista, não precisa de seletor.
export abstract class BaseList<T> implements OnInit {
  //-------------------
  // SERVIÇOS BASE
  //-------------------
  readonly pagination = inject(PaginationHandler);
  protected readonly entityDialog = inject(EntityDialogService);

  //-------------------
  // ESTADO BASE
  //-------------------
  items: WritableSignal<T[]> = signal([]);

  //-------------------
  // MÉTODOS ABSTRATOS (Obrigatórios para classes filhas)
  //-------------------

  /**
   * Contém a lógica específica para carregar os itens da API.
   * É chamado automaticamente no `ngOnInit` e após eventos de paginação/ordenação.
   */
  abstract loadItems(): void;

  //-------------------
  // CICLO DE VIDA ANGULAR
  //-------------------
  ngOnInit(): void {
    this.loadItems();
  }

  //-------------------
  // MANIPULADORES DE EVENTOS (Lógica Padrão)
  //-------------------

  /**
   * Manipula o evento de mudança de página do `mat-paginator`.
   * Delega para o `PaginationHandler` e recarrega os itens.
   */
  onPageChange(event: PageEvent): void {
    this.pagination.handlePageEvent(event);
    this.loadItems();
  }

  /**
   * Manipula o evento de mudança de ordenação do `mat-sort`.
   * Delega para o `PaginationHandler` e recarrega os itens.
   */
  onSortChange(sort: Sort): void {
    this.pagination.handleSortChange(sort);
    this.loadItems();
  }
}
