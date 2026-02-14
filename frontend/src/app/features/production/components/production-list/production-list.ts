import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { ProductionOrder } from '../../models/production.model';
import { ProductionService } from '../../services/production.service';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { ProductionForm, ProductionFormData } from '../production-form/production-form';
import { ProductionOrderForm, ProductionOrderFormData } from '../production-order-form/production-order-form';

/**
 * Componente de listagem para Ordens de Produção.
 * Gerencia a exibição de dados em tabela, paginação e ações de visualização/exclusão.
 */
@Component({
  selector: 'app-production-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    BaseTable
  ],
  templateUrl: './production-list.html',
  styleUrls: ['./production-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class ProductionList extends BaseList<ProductionOrder> implements AfterViewInit {
  private readonly productionService = inject(ProductionService);
  private readonly cdr = inject(ChangeDetectorRef);

  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteSuccess: 'Ordem de produção excluída com sucesso!',
    deleteError: 'Falha ao excluir a ordem de produção.',
    loadError: 'Falha ao carregar a lista de ordens de produção.',
    resourceError: 'Não foi possível encontrar o recurso.',
    createTitle: 'Nova Produção',
    detailsTitle: 'Detalhes da Ordem'
  };

  tableColumns: TableColumn<ProductionOrder>[] = [];

  @ViewChild('produtoTemplate') produtoTemplate!: TemplateRef<any>;
  @ViewChild('quantidadeTemplate') quantidadeTemplate!: TemplateRef<any>;
  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    // Ordenação padrão por data de criação decrescente
    super('production-orders', { active: 'dataCriacao', direction: 'desc' });

    const productionResponse = toSignal(
      this.productionService.productionOrders$.pipe(
        catchError((error) => {
          console.error('Erro ao carregar ordens de produção:', error);
          this.entityDialog.showErrorSnackbar(ProductionList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    effect(() => {
      const response = productionResponse();
      if (response) {
        const items = response._embedded?.ordensDeProducao ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(items);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nomeProduto', header: 'Produto', sortable: true, cellTemplate: this.produtoTemplate },
      { key: 'quantidadeProduzida', header: 'Qtd', sortable: true, cellTemplate: this.quantidadeTemplate },
      { key: 'dataCriacao', header: 'Data', sortable: true, cellTemplate: this.dataTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, cellTemplate: this.acoesTemplate },
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    this.productionService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: this.pagination.sortString()
    });
  }

  onCreate(): void {
    const dialogData: ProductionFormData = {
      title: ProductionList.Texts.createTitle,
      isViewMode: false
    };

    this.entityDialog.openFormDialog({
      component: ProductionForm,
      formData: dialogData,
      title: dialogData.title,
      width: '900px'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar('Ordem de produção criada com sucesso!');
        this.productionService.updateSearchParams({});
      }
    });
  }

  /**
   * Abre a tela de detalhes para a ordem selecionada.
   */
  onView(order: ProductionOrder): void {
    const orderCopy = structuredClone(order);
    this.openFormDialog({
      template: orderCopy,
      title: ProductionList.Texts.detailsTitle,
      isViewMode: true
    });
  }

  onDelete(order: ProductionOrder): void {
    const deleteUrl = order._links?.['deletar-ordem-de-producao']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar(ProductionList.Texts.resourceError);
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `Ordem #${order.id} - ${order.nomeProduto}`,
      ProductionList.Texts.deleteConfirmTitle
    ).subscribe(confirmed => {
      if (confirmed) {
        this.productionService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar(ProductionList.Texts.deleteSuccess);
          },
          error: () => {
            this.entityDialog.showErrorSnackbar(ProductionList.Texts.deleteError);
          }
        });
      }
    });
  }

  private openFormDialog(dialogData: ProductionOrderFormData): void {
    this.entityDialog.openFormDialog({
      component: ProductionOrderForm,
      formData: dialogData,
      title: dialogData.title,
      width: '700px'
    }).subscribe();
  }
}
