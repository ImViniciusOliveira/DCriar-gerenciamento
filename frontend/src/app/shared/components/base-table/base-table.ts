import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface TableColumn<T> {
  key: string;
  header: string;
  sortable?: boolean;
  sortKey?: string;
  cellTemplate: any;
}

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
export class BaseTable<T> implements OnChanges {
  @Input() items: T[] = [];
  @Input() columns: TableColumn<T>[] = [];
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 10;
  @Input() pageIndex: number = 0;

  @Output() pageChange = new EventEmitter<any>();
  @Output() sortChange = new EventEmitter<Sort>();

  dataSource: MatTableDataSource<T>;
  columnKeys: string[];

  paginatorAriaLabel: string = 'Selecione a página';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor() {
    this.dataSource = new MatTableDataSource(this.items);
    this.columnKeys = this.columns.map(c => c.key);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['items']) {
      this.dataSource.data = this.items;
    }
    if (changes['columns']) {
      this.columnKeys = this.columns.map(c => c.key);
    }
  }

  onPageChange(event: any): void {
    this.pageChange.emit(event);
  }

  onSortChange(sort: Sort): void {
    this.sortChange.emit(sort);
  }
}
