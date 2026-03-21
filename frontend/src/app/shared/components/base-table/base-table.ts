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
  className?: string;
  cellTemplate: TemplateRef<{ $implicit: T }>;
}

/**
 * Um componente de tabela genérico e reutilizável, construído sobre o Angular Material.
 * É projetado para funcionar com paginação e ordenação no servidor (server-side),
 * recebendo seus dados e estado de paginação via inputs e emitindo eventos de mudança.
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

    // Reage a mudanças nos dados de entrada e atualiza a fonte de dados da tabela.
    effect(() => {
      this.dataSource.data = this.items();
    });

    // Sincroniza o estado do paginador e do sort com os inputs do componente.
    // Esta abordagem desacoplada é crucial para a paginação/ordenação no servidor.
    effect(() => {
      const currentPaginator = this.paginator();
      const currentSort = this.sort();

      if (currentPaginator) {
        // IMPORTANTE: Não conectamos o paginador diretamente ao dataSource (ex: this.dataSource.paginator = currentPaginator).
        // Se fizéssemos isso, o MatTableDataSource assumiria o controle da paginação,
        // baseando o 'length' apenas nos dados da página atual, o que quebraria a navegação
        // e a contagem total de elementos vinda do servidor.
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
