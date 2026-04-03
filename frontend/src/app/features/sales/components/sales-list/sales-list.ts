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
    resourceError: 'Não foi possível encontrar o recurso.',
    createTitle: 'Nova Venda',
    editTitle: 'Editar Venda'
  };

  tableColumns: TableColumn<Sale>[] = [];

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('canalTemplate') canalTemplate!: TemplateRef<any>;
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
      { key: 'valorTotal', header: 'Total', sortable: true, className: 'col-price', widthPx: 110, cellTemplate: this.valorTemplate },
      { key: 'nomeCanalVenda', header: 'Canal', sortable: false, className: 'col-sales-channel', widthPx: 170, cellTemplate: this.canalTemplate },
      { key: 'itens', header: 'Itens', sortable: false, className: 'col-trigger', widthPx: 117, cellTemplate: this.itensTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, className: 'col-actions col-actions-main', widthPx: 125, cellTemplate: this.acoesTemplate },
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

  /**
   * Abre o formulário de edição para a venda selecionada.
   */
  onEdit(sale: Sale): void {
    const saleCopy = structuredClone(sale);
    this.openSalesDialog({
      template: saleCopy,
      title: SalesList.Texts.editTitle
    }, SalesList.Texts.saveSuccess);
  }

  /**
   * Abre a tela de detalhes para a venda selecionada.
   */
  onViewDetails(sale: Sale): void {
    const saleCopy = structuredClone(sale);
    this.openSalesDialog({
      template: saleCopy,
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

  openItems(sale: Sale): void {
    const dialogData: DetailsDialogData = {
      title: `Itens da venda #${sale.id}`,
      items: sale.itens.map(item => ({
        label: `${item.quantidade}x ${item.nomeProduto}`,
        value: `R$ ${item.precoTotal.toFixed(2)}`
      })),
      showLabels: true
    };

    this.dialog.open(DetailsDialog, {
      data: dialogData,
      width: '680px',
      maxWidth: '90vw',
      autoFocus: false
    });
  }

  hasItems(sale: Sale): boolean {
    return sale.itens.length > 0;
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
