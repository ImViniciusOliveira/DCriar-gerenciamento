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
import { SalesForm, SalesFormData } from '../sales-form/sales-form';

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

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('canalTemplate') canalTemplate!: TemplateRef<any>;
  @ViewChild('valorTemplate') valorTemplate!: TemplateRef<any>;
  @ViewChild('itensTemplate') itensTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super('sales'); // Chave única para persistência de paginação

    // Define a ordenação padrão por data decrescente se estiver no padrão inicial (id, asc)
    if (this.pagination.sortActive() === 'id' && this.pagination.sortDirection() === 'asc') {
      this.pagination.handleSortChange({ active: 'dataCriacao', direction: 'desc' });
    }

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
        const items = response._embedded?.vendas ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(items);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'dataCriacao', header: 'Data', sortable: true, cellTemplate: this.dataTemplate },
      { key: 'valorTotal', header: 'Total', sortable: true, cellTemplate: this.valorTemplate },
      { key: 'nomeCanalVenda', header: 'Canal', sortable: false, cellTemplate: this.canalTemplate },
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
    try {
      // Busca o template HATEOAS para nova venda (opcional, mas boa prática se o backend fornecer defaults)
      const template = await lastValueFrom(this.salesService.getNewTemplate());

      this.openSalesDialog({
        template,
        title: 'Nova Venda'
      }, SalesList.Texts.createSuccess);

    } catch (error) {
      console.error('Erro ao buscar template para nova venda:', error);
      // Mesmo com erro no template, tentamos abrir o formulário vazio
      this.openSalesDialog({
        title: 'Nova Venda'
      }, SalesList.Texts.createSuccess);
    }
  }

  private openSalesDialog(dialogData: SalesFormData, successMessage: string): void {
    this.entityDialog.openFormDialog({
      component: SalesForm,
      formData: dialogData,
      title: dialogData.title,
      width: '90vw',
      maxWidth: '1000px' // Formulário de venda precisa de espaço
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar(successMessage);
        // O BaseList já cuida de recarregar a lista via effect quando o signal muda,
        // mas aqui garantimos o refresh manual se necessário.
        this.salesService.updateSearchParams({});
      }
    });
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
