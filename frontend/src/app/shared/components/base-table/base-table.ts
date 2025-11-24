import { Component, Input, Output, EventEmitter, ViewChild, OnChanges, SimpleChanges, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface TableColumn<T> {
  key: string;
  header: string;
  cellTemplate: TemplateRef<any>;
  sortable?: boolean;
  sortKey?: string;
}

@Component({
  selector: 'app-base-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './base-table.html',
  styleUrls: ['./base-table.scss']
})
export class BaseTable<T> implements OnChanges {
  @Input() title: string = 'Itens';
  @Input() subtitle: string = 'Gerenciamento de itens';
  @Input() addButtonText: string = 'Adicionar Item';
  @Input() paginatorAriaLabel: string = 'Selecione a página de itens';

  @Input() items: T[] = [];
  @Input() columns: TableColumn<T>[] = [];

  @Input() totalElements: number = 0;
  @Input() pageSize: number = 10;
  @Input() pageIndex: number = 0;

  @Output() add = new EventEmitter<void>();
  @Output() pageChange = new EventEmitter<PageEvent>();
  @Output() sortChange = new EventEmitter<Sort>();

  dataSource = new MatTableDataSource<T>();
  columnKeys: string[] = [];

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['items']) {
      this.dataSource.data = this.items;
    }
    if (changes['columns']) {
      this.columnKeys = this.columns.map(c => c.key);
    }
  }

  onAdd(): void {
    this.add.emit();
  }

  onPageChange(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  onSortChange(sort: Sort): void {
    this.paginator.pageIndex = 0;
    this.sortChange.emit(sort);
  }
}
