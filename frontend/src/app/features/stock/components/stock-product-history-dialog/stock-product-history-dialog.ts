import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, TemplateRef, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { StockHistoryItem } from '../../models/stock-history.model';
import { StockService } from '../../services/stock.service';

export interface StockProductHistoryDialogData {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
}

@Component({
  selector: 'app-stock-product-history-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    BaseTable
  ],
  templateUrl: './stock-product-history-dialog.html',
  styleUrl: './stock-product-history-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockProductHistoryDialog {
  private readonly stockService = inject(StockService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dialogRef = inject(MatDialogRef<StockProductHistoryDialog>);
  readonly data = inject<StockProductHistoryDialogData>(MAT_DIALOG_DATA);

  readonly items = signal<StockHistoryItem[]>([]);
  readonly totalElements = signal(0);
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly sortActive = signal('data');
  readonly sortDirection = signal<Sort['direction']>('desc');
  readonly isLoading = signal(true);

  tableColumns: TableColumn<StockHistoryItem>[] = [];

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('movementTemplate') movementTemplate!: TemplateRef<any>;
  @ViewChild('quantityTemplate') quantityTemplate!: TemplateRef<any>;
  @ViewChild('reasonTemplate') reasonTemplate!: TemplateRef<any>;

  readonly title = computed(() => `Histórico de ${this.data.nomeProduto}`);
  readonly subtitle = computed(() => this.data.skuProduto || 'SKU não informado');

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'data', header: 'Data da movimentação', sortable: true, className: 'col-created', widthPx: 150, cellTemplate: this.dataTemplate },
      { key: 'tipo', header: 'Movimentação', sortable: false, className: 'col-movement-history', widthPx: 180, cellTemplate: this.movementTemplate },
      { key: 'quantidade', header: 'Quantidade', sortable: true, className: 'col-quantity-history', widthPx: 140, cellTemplate: this.quantityTemplate },
      { key: 'motivo', header: 'Motivo', sortable: false, className: 'col-reason-history', cellTemplate: this.reasonTemplate }
    ];

    this.load();
  }

  onPageChange(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
    this.load();
  }

  onSortChange(sort: Sort): void {
    this.sortActive.set(sort.direction ? sort.active : 'data');
    this.sortDirection.set(sort.direction || 'desc');
    this.pageIndex.set(0);
    this.load();
  }

  close(): void {
    this.dialogRef.close();
  }

  formatSignedQuantity(quantidade: number): string {
    return `${quantidade > 0 ? '+' : '-'}${Math.abs(quantidade)}`;
  }

  isPositiveQuantity(quantidade: number): boolean {
    return quantidade >= 0;
  }

  private load(): void {
    this.isLoading.set(true);
    const sort = this.sortDirection() ? `${this.sortActive()},${this.sortDirection()}` : this.sortActive();

    this.stockService.searchHistoryByProduct({
      produtoId: this.data.produtoId,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort,
      periodo: 'all'
    }).pipe(
      catchError(() => of(undefined)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(response => {
      this.items.set(response?._embedded?.historicoEstoqueConsolidadoResponseDTOList ?? []);
      this.totalElements.set(response?.page?.totalElements ?? 0);
      this.isLoading.set(false);
    });
  }
}
