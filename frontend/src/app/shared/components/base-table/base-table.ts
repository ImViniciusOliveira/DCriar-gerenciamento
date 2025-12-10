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
  // A tipagem correta para o contexto do ngTemplateOutlet.
  // Isso informa ao TypeScript que o template espera um objeto de contexto
  // com uma propriedade `$implicit` do tipo T, resolvendo o warning de 'T' não utilizado.
  cellTemplate: TemplateRef<{ $implicit: T }>;
}

/**
 * Um componente de tabela genérico e reutilizável, construído sobre o Angular Material.
 * Ele é configurado através de inputs e emite eventos para paginação e ordenação,
 * delegando a lógica de busca de dados para o componente pai.
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

    // Um `effect` é usado para reagir a mudanças nos sinais e executar
    // efeitos colaterais, substituindo a necessidade do `ngOnChanges`.
    effect(() => {
      // Quando o sinal `items` mudar, atualiza os dados da tabela.
      this.dataSource.data = this.items();
    });

    effect(() => {
      // Quando os sinais de `paginator` e `sort` estiverem disponíveis,
      // conecta-os à fonte de dados da tabela.
      const currentPaginator = this.paginator();
      const currentSort = this.sort();

      if (currentPaginator) {
        this.dataSource.paginator = currentPaginator;
        // Força o paginador a atualizar seu estado visual
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
