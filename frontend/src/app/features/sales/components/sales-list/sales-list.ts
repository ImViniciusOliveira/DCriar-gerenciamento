import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { Sale } from '../../models/sales.model';
import { SalesService } from '../../services/sales.service';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { SalesForm, SalesFormData } from '../sales-form/sales-form';
import { DetailsDialog, DetailsDialogData } from '../../../../shared/components/details-dialog/details-dialog';
import { ChannelForm, ChannelFormData } from '../../../stock/components/channel-form/channel-form';

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
    BaseTable
  ],
  templateUrl: './sales-list.html',
  styleUrls: ['./sales-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class SalesList extends BaseList<Sale> implements AfterViewInit {
  private readonly salesService = inject(SalesService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);

  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteSuccess: 'Venda excluída com sucesso!',
    saveSuccess: 'Venda atualizada com sucesso!',
    createSuccess: 'Venda registrada com sucesso!',
    deleteError: 'Falha ao excluir a venda.',
    loadError: 'Falha ao carregar a lista de vendas.',
    createError: 'Não foi possível iniciar o registro de uma nova venda.',
    createChannelSuccess: 'Canal de venda cadastrado com sucesso!',
    resourceError: 'Não foi possível encontrar o recurso.',
    createTitle: 'Nova Venda',
    editTitle: 'Editar Venda',
    createChannelTitle: 'Novo Canal de Venda'
  };

  tableColumns: TableColumn<Sale>[] = [];

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('nomeTemplate') nomeTemplate!: TemplateRef<any>;
  @ViewChild('apelidoTemplate') apelidoTemplate!: TemplateRef<any>;
  @ViewChild('cidadeEstadoTemplate') cidadeEstadoTemplate!: TemplateRef<any>;
  @ViewChild('valorTemplate') valorTemplate!: TemplateRef<any>;
  @ViewChild('itensTemplate') itensTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    // Passa a ordenação padrão correta para o BaseList
    super('sales', { active: 'dataCriacao', direction: 'desc' });

    const salesResponse = toSignal(
      this.salesService.sales$.pipe(
        catchError(() => {
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
      { key: 'dataCriacao', header: 'Criado em', sortable: true, className: 'col-created', cellTemplate: this.dataTemplate },
      { key: 'nomeCompleto', header: 'Nome', sortable: false, className: 'col-customer-name', widthPx: 200, cellTemplate: this.nomeTemplate },
      { key: 'apelido', header: 'Apelido', sortable: false, className: 'col-customer-nickname', widthPx: 180, cellTemplate: this.apelidoTemplate },
      { key: 'cidadeEstado', header: 'Cidade / Estado', sortable: false, className: 'col-customer-city-state', widthPx: 220, cellTemplate: this.cidadeEstadoTemplate },
      { key: 'valorTotal', header: 'Total', sortable: true, className: 'col-price', widthPx: 200, cellTemplate: this.valorTemplate },
      { key: 'itens', header: 'Itens', sortable: false, className: 'col-trigger col-fit-center', widthPx: 150, cellTemplate: this.itensTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, className: 'col-actions col-actions-main', widthPx: 150, cellTemplate: this.acoesTemplate },
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
      const template = await lastValueFrom(this.salesService.getNewTemplate());

      this.openSalesDialog({
        template,
        title: SalesList.Texts.createTitle
      }, SalesList.Texts.createSuccess);

    } catch {
      this.openSalesDialog({
        title: SalesList.Texts.createTitle
      }, SalesList.Texts.createSuccess);
    }
  }

  onCreateChannel(): void {
    const dialogData: ChannelFormData = {
      title: SalesList.Texts.createChannelTitle
    };

    this.entityDialog.openFormDialog({
      component: ChannelForm,
      formData: dialogData,
      title: dialogData.title,
      width: '460px',
      maxWidth: '95vw'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar(SalesList.Texts.createChannelSuccess);
      }
    });
  }

  /**
   * Abre o formulário de edição para a venda selecionada.
   */
  async onEdit(sale: Sale): Promise<void> {
    const fullSale = await this.loadSaleDetails(sale);
    this.openSalesDialog({
      template: fullSale,
      title: SalesList.Texts.editTitle
    }, SalesList.Texts.saveSuccess);
  }

  /**
   * Abre a tela de detalhes para a venda selecionada.
   */
  async onViewDetails(sale: Sale): Promise<void> {
    const fullSale = await this.loadSaleDetails(sale);
    this.openSalesDialog({
      template: fullSale,
      title: 'Detalhes da Venda',
      isViewMode: true
    }, ''); // Não mostra mensagem de sucesso no modo de visualização
  }

  /**
   * Solicita confirmação e remove a venda selecionada.
   */
  onDelete(sale: Sale): void {
    const deleteUrl = sale._links?.['delete']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar(SalesList.Texts.resourceError);
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `Venda #${sale.id}`,
      SalesList.Texts.deleteConfirmTitle
    ).subscribe(confirmed => {
      if (confirmed) {
        this.salesService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar(SalesList.Texts.deleteSuccess);
          },
          error: () => {
            this.entityDialog.showErrorSnackbar(SalesList.Texts.deleteError);
          }
        });
      }
    });
  }

  private openSalesDialog(dialogData: SalesFormData, successMessage: string): void {
    this.entityDialog.openFormDialog({
      component: SalesForm,
      formData: dialogData,
      title: dialogData.title,
      width: '90vw',
      maxWidth: '1000px'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar(successMessage);
        this.salesService.updateSearchParams({});
      }
    });
  }

  async openItems(sale: Sale): Promise<void> {
    const fullSale = await this.loadSaleDetails(sale);
    const items = fullSale.itens ?? [];

    const dialogData: DetailsDialogData = {
      title: `Itens da venda #${fullSale.id}`,
      items: items.length > 0
        ? items.map(item => ({
            label: `${item.quantidade}x ${item.nomeProduto}`,
            value: `R$ ${item.precoTotal.toFixed(2)}`
          }))
        : [{
            label: 'Itens',
            value: 'Nenhum item registrado'
          }],
      showLabels: true
    };

    this.dialog.open(DetailsDialog, {
      data: dialogData,
      width: '680px',
      maxWidth: '90vw',
      autoFocus: false
    });
  }

  canOpenItems(sale: Sale): boolean {
    return !!sale._links?.['self']?.href;
  }

  displaySummaryValue(value?: string | null): string {
    return value?.trim() ? value : '-';
  }

  displayCityState(sale: Sale): string {
    const cidade = sale.cidade?.trim();
    const estado = sale.estado?.trim();

    if (cidade && estado) {
      return `${cidade} / ${estado}`;
    }

    return cidade || estado || '-';
  }

  private async loadSaleDetails(sale: Sale): Promise<Sale> {
    const selfUrl = sale._links?.['self']?.href;
    if (!selfUrl) {
      return structuredClone(sale);
    }

    try {
      return await lastValueFrom(this.salesService.findByUrl(selfUrl));
    } catch {
      this.entityDialog.showErrorSnackbar(SalesList.Texts.resourceError);
      return structuredClone(sale);
    }
  }
}
