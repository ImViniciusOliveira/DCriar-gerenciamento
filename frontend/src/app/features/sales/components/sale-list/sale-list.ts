import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

// Nossos componentes e serviços reutilizáveis
import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';

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
export class SaleListComponent extends BaseList<Sale> implements AfterViewInit {
  // Serviços específicos de Vendas
  private readonly saleService = inject(SaleService);
  private readonly cdr = inject(ChangeDetectorRef);

  // Estado específico de Vendas
  tableColumns: TableColumn<Sale>[] = [];

  // Referências aos templates do HTML
  @ViewChild('idTemplate') idTemplate!: TemplateRef<any>;
  @ViewChild('channelTemplate') channelTemplate!: TemplateRef<any>;
  @ViewChild('itemsTemplate') itemsTemplate!: TemplateRef<any>;
  @ViewChild('amountTemplate') amountTemplate!: TemplateRef<any>;
  @ViewChild('dateTemplate') dateTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  ngAfterViewInit(): void {
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

  // Implementação do método abstrato da classe base
  override loadItems(): void {
    this.saleService.getSales(
      this.pagination.pageIndex(),
      this.pagination.pageSize(),
      this.pagination.sortString()
    ).subscribe(response => {
      this.items.set(response._embedded?.vendas ?? []); // Usa a propriedade 'items' da classe base
      this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
    });
  }

  // Métodos de CRUD específicos de Vendas
  onDelete(sale: Sale): void {
    this.entityDialog.openConfirmDeleteDialog(`Venda #${sale.id}`).subscribe((confirmed: boolean) => {
      if (confirmed) {
        const deleteUrl = sale._links?.['self']?.href;
        if (!deleteUrl) {
          this.entityDialog.showErrorSnackbar('URL para exclusão não encontrada.');
          return;
        }
        this.saleService.deleteSale(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar('Venda excluída com sucesso!');
            this.loadItems();
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
        this.loadItems();
      }
    });
  }
}
