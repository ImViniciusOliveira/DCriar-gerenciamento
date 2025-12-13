import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
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
 * Componente de listagem de Lotes de Matéria-Prima.
 *
 * Utiliza a estratégia `OnPush` e Signals para reagir automaticamente às mudanças
 * de estado no serviço `BatchService`.
 */
@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
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
    editTitle: 'Editar Lote'
  };

  tableColumns: TableColumn<Batch>[] = [];

  // Referências aos templates de célula definidos no HTML
  @ViewChild('typeTemplate') typeTemplate!: TemplateRef<any>;
  @ViewChild('balanceTemplate') balanceTemplate!: TemplateRef<any>;
  @ViewChild('unitTemplate') unitTemplate!: TemplateRef<any>;
  @ViewChild('attributesTemplate') attributesTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  constructor() {
    super();

    // Converte o Observable de lotes do serviço em um signal para consumo reativo.
    const lotesResponse = toSignal(
      this.batchService.batches$.pipe(
        catchError((error) => {
          console.error('Erro ao carregar lotes:', error);
          this.entityDialog.showErrorSnackbar(BatchList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    // Reage a novas emissões do serviço e atualiza o estado da lista.
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
      { key: 'nomeTipoMateriaPrima', header: 'Matéria-Prima', sortable: true, cellTemplate: this.typeTemplate },
      { key: 'saldoEstoque', header: 'Saldo', sortable: true, cellTemplate: this.balanceTemplate },
      { key: 'unidadeDeEstoque', header: 'Unidade', sortable: true, cellTemplate: this.unitTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, cellTemplate: this.attributesTemplate },
      { key: 'acoes', header: 'Ações', cellTemplate: this.actionsTemplate }
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    this.batchService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: this.pagination.sortString()
    });
  }

  openMaterialTypeDialog(): void {
    this.dialog.open(MaterialTypeList, {
      width: '80vw',
      maxWidth: '900px',
      height: '80vh'
    });
  }

  async onCreate(): Promise<void> {
    try {
      const template = await lastValueFrom(this.batchService.getNewTemplate());
      this.openFormDialog({
        template,
        title: BatchList.Texts.createTitle
      }, BatchList.Texts.createSuccess);
    } catch (error) {
      console.error('Erro ao buscar template para novo lote:', error);
      this.entityDialog.showErrorSnackbar(BatchList.Texts.createError);
    }
  }

  onEdit(lote: Batch): void {
    const loteCopy = structuredClone(lote);
    this.openFormDialog({
      template: loteCopy,
      title: BatchList.Texts.editTitle
    }, BatchList.Texts.saveSuccess);
  }

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
      if (saved) {
        this.entityDialog.showSuccessSnackbar(successMessage);
      }
    });
  }

  getAttributesAsArray(atributos: { [key: string]: any }): { key: string, value: any }[] {
    if (!atributos) {
      return [];
    }
    return Object.entries(atributos).map(([key, value]) => ({ key, value }));
  }
}
