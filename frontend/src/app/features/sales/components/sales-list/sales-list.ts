import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { Sale } from '../../models/sales.model';
import { SalesService } from '../../services/sales.service';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';

/**
 * Componente de listagem para Vendas.
 * Gerencia a exibição de dados em tabela, paginação e ações de visualização.
 */
@Component({
  selector: 'app-sales-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    BaseTable,
    DetailsPopover
  ],
  templateUrl: './sales-list.html',
  styleUrls: ['./sales-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class SalesList extends BaseList<Sale> implements AfterViewInit {
  private readonly salesService = inject(SalesService);
  private readonly cdr = inject(ChangeDetectorRef);

  private static readonly Texts = {
    loadError: 'Falha ao carregar a lista de vendas.',
    createError: 'Não foi possível iniciar o registro de uma nova venda.',
    createSuccess: 'Venda registrada com sucesso!'
  };

  tableColumns: TableColumn<Sale>[] = [];

  @ViewChild('idTemplate') idTemplate!: TemplateRef<any>;
  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('canalTemplate') canalTemplate!: TemplateRef<any>;
  @ViewChild('valorTemplate') valorTemplate!: TemplateRef<any>;
  @ViewChild('itensTemplate') itensTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super('sales'); // Chave única para persistência de paginação

    const salesResponse = toSignal(
      this.salesService.sales$.pipe(
        catchError((error) => {
          console.error('Erro ao carregar vendas:', error);
          this.entityDialog.showErrorSnackbar(SalesList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    effect(() => {
      const response = salesResponse();
      if (response) {
        const sales = response._embedded?.vendas ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(sales);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'id', header: 'ID', sortable: true, cellTemplate: this.idTemplate },
      { key: 'dataCriacao', header: 'Data', sortable: true, cellTemplate: this.dataTemplate },
      { key: 'nomeCanalVenda', header: 'Canal', sortable: false, cellTemplate: this.canalTemplate },
      { key: 'valorTotal', header: 'Total', sortable: true, cellTemplate: this.valorTemplate },
      { key: 'itens', header: 'Itens', sortable: false, cellTemplate: this.itensTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, cellTemplate: this.acoesTemplate },
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    this.salesService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: this.pagination.sortString()
    });
  }

  /**
   * Abre o formulário para registrar uma nova venda.
   */
  async onCreate(): Promise<void> {
    // TODO: Implementar abertura do SalesFormComponent quando ele estiver pronto
    console.log('Abrir formulário de nova venda');
  }

  /**
   * Formata os detalhes dos itens para exibição no popover.
   */
  getSaleItemsDetails(sale: Sale): { key: string, value: string }[] {
    return sale.itens.map(item => ({
      key: `${item.quantidade}x ${item.nomeProduto}`,
      value: `R$ ${item.precoTotal.toFixed(2)}`
    }));
  }
}
