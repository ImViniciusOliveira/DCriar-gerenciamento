import { Component, computed, effect, EventEmitter, input, Output, TemplateRef, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Configuração para uma coluna da tabela dinâmica.
 * @template T O tipo de dado do item da linha.
 */
export interface TableColumn<T> {
  key: string;
  header: string;
  sortable?: boolean;
  sortKey?: string;
  cellTemplate: TemplateRef<{ $implicit: T }>;
}

/**
 * Um componente de tabela genérico e reutilizável, construído sobre o Angular Material.
 * É projetado para funcionar com paginação e ordenação no servidor (server-side),
 * recebendo seus dados e estado de paginação via inputs.
 */
@Component({
  selector: 'app-base-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './base-table.html',
  styleUrls: ['./base-table.scss']
})
export class BaseTable<T> {
  // --- Entradas (Inputs) ---
  items = input.required<T[]>();
  columns = input.required<TableColumn<T>[]>();
  totalElements = input.required<number>();
  pageSize = input(10);
  pageIndex = input(0);

  // --- Saídas (Outputs) ---
  @Output() pageChange = new EventEmitter<PageEvent>();
  @Output() sortChange = new EventEmitter<Sort>();

  // --- Sinais Internos e Computados ---
  protected readonly dataSource: MatTableDataSource<T>;
  protected readonly columnKeys = computed(() => this.columns().map(c => c.key));

  // --- Referências de View (ViewChild) ---
  private readonly paginator = viewChild.required(MatPaginator);
  private readonly sort = viewChild.required(MatSort);

  constructor() {
    this.dataSource = new MatTableDataSource<T>([]);

    // Reage a mudanças nos dados de entrada e atualiza a tabela.
    effect(() => {
      this.dataSource.data = this.items();
    });

    // Reage a mudanças nos inputs de paginação e atualiza o MatPaginator.
    effect(() => {
      const currentPaginator = this.paginator();
      const currentSort = this.sort();

      if (currentPaginator) {
        // IMPORTANTE: Não conectamos o paginador ao dataSource (this.dataSource.paginator = currentPaginator)
        // porque estamos usando paginação no servidor. Se conectássemos, o MatTableDataSource
        // assumiria o controle e basearia o 'length' apenas nos dados da página atual, quebrando a navegação.
        currentPaginator.pageIndex = this.pageIndex();
        currentPaginator.pageSize = this.pageSize();
        currentPaginator.length = this.totalElements();
      }
      if (currentSort) {
        this.dataSource.sort = currentSort;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  onSortChange(sort: Sort): void {
    this.sortChange.emit(sort);
  }
}
