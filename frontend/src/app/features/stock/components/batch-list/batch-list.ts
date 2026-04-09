import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy, OnInit } from '@angular/core';
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
import { BatchForm, BatchFormData } from '../batch-form/batch-form';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { DetailsDialog, DetailsDialogData } from '../../../../shared/components/details-dialog/details-dialog';

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
    BaseTable
  ],
  templateUrl: './batch-list.html',
  styleUrls: ['./batch-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class BatchList extends BaseList<Batch> implements AfterViewInit, OnInit {
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
  @ViewChild('structureTemplate') structureTemplate!: TemplateRef<any>;
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

  ngOnInit(): void {
    this.batchService.resetSearchParams();
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'tipoMateriaPrima.nome', header: 'Matéria-Prima', sortable: true, sortType: 'text', className: 'col-batch-name', cellTemplate: this.typeTemplate },
      { key: 'tipoEstrutural', header: 'Tipo', sortable: false, className: 'col-batch-type', widthPx: 250, cellTemplate: this.structureTemplate },
      { key: 'saldoEstoque', header: 'Quantidade', sortable: false, className: 'col-batch-quantity', widthPx: 200, cellTemplate: this.balanceTemplate },
      { key: 'custoTotalLote', header: 'Custo do Lote', sortable: true, className: 'col-batch-cost', widthPx: 200, cellTemplate: this.costTemplate },
      { key: 'dataCriacao', header: 'Criado em', sortable: true, className: 'col-created', widthPx: 200, cellTemplate: this.createdAtTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, className: 'col-trigger col-fit-center', widthPx: 150, cellTemplate: this.attributesTemplate },
      { key: 'acoes', header: 'Ações', className: 'col-actions col-actions-main', widthPx: 150, cellTemplate: this.actionsTemplate }
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
          error: (err) => {
            this.entityDialog.showApiErrorSnackbar(err, BatchList.Texts.deleteError);
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

  openAttributes(lote: Batch): void {
    const dialogData: DetailsDialogData = {
      title: `Atributos do lote ${lote.id}`,
      items: this.getAttributesAsArray(lote.atributos ?? {}).map(item => ({
        label: item.key,
        value: String(item.value)
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

  hasAttributes(lote: Batch): boolean {
    return this.getAttributesAsArray(lote.atributos ?? {}).length > 0;
  }

  getStructureLabel(lote: Batch): string {
    return lote.identificadorPublico ?? String(lote.id);
  }

  getStructureOriginLabel(lote: Batch): string | null {
    if (lote.tipoEstrutural === 'RETALHO' && lote.identificadorOrigemPublico) {
      return `Origem: ${lote.identificadorOrigemPublico}`;
    }

    return null;
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
