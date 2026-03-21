import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { Batch } from '../../models/batch.model';
import { BatchService } from '../../services/batch.service';
import { MaterialTypeList } from '../material-type-list/material-type-list';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';
import { BatchForm, BatchFormData } from '../batch-form/batch-form';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

/**
 * Componente de listagem para Lotes de Matéria-Prima.
 * Gerencia a exibição de dados em tabela, paginação e ações de CRUD (criar, editar, deletar).
 */
@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatTooltipModule,
    BaseTable,
    DetailsPopover
  ],
  templateUrl: './batch-list.html',
  styleUrls: ['./batch-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class BatchList extends BaseList<Batch> implements AfterViewInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly batchService = inject(BatchService);
  private readonly dialog = inject(MatDialog);

  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteSuccess: 'Lote excluído com sucesso!',
    saveSuccess: 'Lote atualizado com sucesso!',
    createSuccess: 'Lote registrado com sucesso!',
    deleteError: 'Falha ao excluir o lote.',
    loadError: 'Falha ao carregar a lista de lotes.',
    createError: 'Não foi possível iniciar o registro do lote.',
    resourceError: 'Não foi possível encontrar o recurso.',
    createTitle: 'Registrar Entrada de Lote',
    editTitle: 'Editar Lote',
    viewTitle: 'Detalhes do Lote' // Novo título
  };

  tableColumns: TableColumn<Batch>[] = [];

  @ViewChild('typeTemplate') typeTemplate!: TemplateRef<any>;
  @ViewChild('balanceTemplate') balanceTemplate!: TemplateRef<any>;
  @ViewChild('costTemplate') costTemplate!: TemplateRef<any>;
  @ViewChild('createdAtTemplate') createdAtTemplate!: TemplateRef<any>;
  @ViewChild('attributesTemplate') attributesTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  constructor() {
    super('batches', { active: 'tipoMateriaPrima.nome', direction: 'asc' });

    const lotesResponse = toSignal(
      this.batchService.batches$.pipe(
        catchError(() => {
          this.entityDialog.showErrorSnackbar(BatchList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    effect(() => {
      const response = lotesResponse();
      if (response) {
        const items = response._embedded?.['lotes-materia-prima'] ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(items);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'tipoMateriaPrima.nome', header: 'Matéria-Prima', sortable: true, cellTemplate: this.typeTemplate },
      { key: 'saldoEstoque', header: 'Quantidade', sortable: false, cellTemplate: this.balanceTemplate },
      { key: 'custoTotalLote', header: 'Custo do Lote', sortable: true, cellTemplate: this.costTemplate },
      { key: 'dataCriacao', header: 'Criado em', sortable: true, cellTemplate: this.createdAtTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, cellTemplate: this.attributesTemplate },
      { key: 'acoes', header: 'Ações', cellTemplate: this.actionsTemplate }
    ];
    this.cdr.detectChanges();
  }

  /**
   * Notifica o serviço sobre mudanças de paginação ou ordenação.
   * A UI é atualizada reativamente pelo `effect` no construtor.
   */
  override loadItems(): void {
    this.batchService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: this.pagination.sortString()
    });
  }

  /**
   * Abre o diálogo para gerenciamento de Tipos de Matéria-Prima.
   */
  openMaterialTypeDialog(): void {
    this.dialog.open(MaterialTypeList, {
      width: '80vw',
      maxWidth: '900px',
      height: '80vh'
    });
  }

  /**
   * Abre o formulário para a criação de um novo Lote.
   */
  async onCreate(): Promise<void> {
    try {
      const template = await lastValueFrom(this.batchService.getNewTemplate());
      this.openFormDialog({
        template,
        title: BatchList.Texts.createTitle
      }, BatchList.Texts.createSuccess);
    } catch {
      this.entityDialog.showErrorSnackbar(BatchList.Texts.createError);
    }
  }

  /**
   * Abre o formulário de edição para o lote selecionado.
   */
  onEdit(lote: Batch): void {
    const loteCopy = structuredClone(lote);
    this.openFormDialog({
      template: loteCopy,
      title: BatchList.Texts.editTitle
    }, BatchList.Texts.saveSuccess);
  }

  /**
   * Abre a tela de detalhes para o lote selecionado.
   */
  onViewDetails(lote: Batch): void {
    const loteCopy = structuredClone(lote);
    this.openFormDialog({
      template: loteCopy,
      title: BatchList.Texts.viewTitle,
      isViewMode: true
    }, ''); // Não mostra mensagem de sucesso no modo de visualização
  }

  /**
   * Solicita confirmação e remove o lote selecionado.
   */
  onDelete(lote: Batch): void {
    const deleteUrl = lote._links?.['delete']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar(BatchList.Texts.resourceError);
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `Lote '${lote.id}'`,
      BatchList.Texts.deleteConfirmTitle
    ).subscribe(confirmed => {
      if (confirmed) {
        this.batchService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar(BatchList.Texts.deleteSuccess);
          },
          error: () => {
            this.entityDialog.showErrorSnackbar(BatchList.Texts.deleteError);
          }
        });
      }
    });
  }

  private openFormDialog(dialogData: BatchFormData, successMessage: string): void {
    this.entityDialog.openFormDialog({
      component: BatchForm,
      formData: dialogData,
      title: dialogData.title,
      width: '800px'
    }).subscribe(saved => {
      if (saved && successMessage) { // Só mostra a mensagem se ela for fornecida
        this.entityDialog.showSuccessSnackbar(successMessage);
      }
    });
  }

  /**
   * Converte o objeto de atributos em um array para exibição no popover.
   */
  getAttributesAsArray(atributos: { [key: string]: any }): { key: string, value: any }[] {
    if (!atributos) {
      return [];
    }
    return Object.entries(atributos).map(([key, value]) => ({ key, value }));
  }
}
