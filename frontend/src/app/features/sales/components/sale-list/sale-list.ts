import { Component, inject, signal, ViewChild, TemplateRef, AfterViewInit, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';

// Nossos componentes e serviços reutilizáveis
import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';

// Coisas específicas de Vendas
import { Sale } from '../../models/sale.model';
import { SaleService } from '../../services/sale.service';
import { SaleFormComponent } from '../sale-form/sale-form';

@Component({
  selector: 'app-sale-list',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    DatePipe,
    MatIconModule,
    MatButtonModule,
    BaseTable,
  ],
  templateUrl: './sale-list.html',
  styleUrls: ['./sale-list.scss']
})
export class SaleListComponent implements OnInit, AfterViewInit {
  readonly pagination = inject(PaginationHandler);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly saleService = inject(SaleService);
  private readonly cdr = inject(ChangeDetectorRef);

  sales = signal<Sale[]>([]);
  tableColumns: TableColumn<Sale>[] = [];

  // Referências aos templates do HTML
  @ViewChild('idTemplate') idTemplate!: TemplateRef<any>;
  @ViewChild('channelTemplate') channelTemplate!: TemplateRef<any>;
  @ViewChild('itemsTemplate') itemsTemplate!: TemplateRef<any>;
  @ViewChild('amountTemplate') amountTemplate!: TemplateRef<any>;
  @ViewChild('dateTemplate') dateTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  ngOnInit(): void {
    this.loadSales();
  }

  ngAfterViewInit(): void {
    // Define as colunas com os novos campos
    this.tableColumns = [
      { key: 'id', header: 'ID', sortable: true, cellTemplate: this.idTemplate },
      { key: 'nomeCanalVenda', header: 'Canal de Venda', sortable: true, cellTemplate: this.channelTemplate },
      { key: 'itens', header: 'Itens', sortable: false, cellTemplate: this.itemsTemplate },
      { key: 'valorTotal', header: 'Valor Total', sortable: true, cellTemplate: this.amountTemplate },
      { key: 'dataCriacao', header: 'Data', sortable: true, cellTemplate: this.dateTemplate },
      { key: 'actions', header: 'Ações', sortable: false, cellTemplate: this.actionsTemplate },
    ];
    this.cdr.detectChanges();
  }

  loadSales(): void {
    this.saleService.getSales(
      this.pagination.pageIndex(),
      this.pagination.pageSize(),
      this.pagination.sortString()
    ).subscribe(response => {
      // Chave alterada para "vendas"
      this.sales.set(response._embedded?.vendas ?? []);
      this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
    });
  }

  onPageChange(event: PageEvent): void {
    this.pagination.handlePageEvent(event);
    this.loadSales();
  }

  onSortChange(sort: Sort): void {
    this.pagination.handleSortChange(sort);
    this.loadSales();
  }

  onDelete(sale: Sale): void {
    // Usando o ID da venda para a mensagem de confirmação
    this.entityDialog.openConfirmDeleteDialog(`Venda #${sale.id}`).subscribe((confirmed: boolean) => {
      if (confirmed) {
        const deleteUrl = sale._links?.['self']?.href; // Usando o link 'self' como exemplo
        if (!deleteUrl) {
          this.entityDialog.showErrorSnackbar('URL para exclusão não encontrada.');
          return;
        }
        this.saleService.deleteSale(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar('Venda excluída com sucesso!');
            this.loadSales();
          },
          error: () => this.entityDialog.showErrorSnackbar('Falha ao excluir a venda.')
        });
      }
    });
  }

  onEdit(sale: Sale): void {
    this.openFormDialog({ sale });
  }

  onCreate(): void {
    this.openFormDialog({ sale: {} });
  }

  private openFormDialog(formData: { sale: Partial<Sale> }): void {
    this.entityDialog.openFormDialog({
      component: SaleFormComponent,
      formData,
      title: formData.sale.id ? `Editar Venda #${formData.sale.id}` : 'Nova Venda'
    }).subscribe((saved: boolean) => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar('Venda salva com sucesso!');
        this.loadSales();
      }
    });
  }
}
