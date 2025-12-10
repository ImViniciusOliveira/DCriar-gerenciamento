import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { lastValueFrom, catchError, of } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { LoteMateriaPrima } from '../../models/lote-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeList } from '../material-type-list/material-type-list';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';
import { LoteMateriaPrimaForm, LoteMateriaPrimaFormData } from '../lote-materia-prima-form/lote-materia-prima-form';

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
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchList extends BaseList<LoteMateriaPrima> implements AfterViewInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly loteService = inject(LoteMateriaPrimaService);
  private readonly dialog = inject(MatDialog);

  tableColumns: TableColumn<LoteMateriaPrima>[] = [];

  @ViewChild('tipoTemplate') tipoTemplate!: TemplateRef<any>;
  @ViewChild('saldoTemplate') saldoTemplate!: TemplateRef<any>;
  @ViewChild('unidadeTemplate') unidadeTemplate!: TemplateRef<any>;
  @ViewChild('atributosTemplate') atributosTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super();

    const lotesResponse = toSignal(
      this.loteService.lotesMateriaPrima$.pipe(
        catchError((error) => {
          console.error('Erro ao carregar lotes:', error);
          this.entityDialog.showErrorSnackbar('Falha ao carregar a lista de lotes.');
          return of(undefined);
        })
      )
    );

    effect(() => {
      const response = lotesResponse();
      if (response && response._embedded && response.page) {
        const items = response._embedded['lotes-materia-prima'] ?? [];
        this.pagination.updateTotalElements(response.page.totalElements);
        this.items.set(items);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nomeTipoMateriaPrima', header: 'Matéria-Prima', sortable: true, cellTemplate: this.tipoTemplate },
      { key: 'saldoEstoque', header: 'Saldo', sortable: true, cellTemplate: this.saldoTemplate },
      { key: 'unidadeDeEstoque', header: 'Unidade', sortable: true, cellTemplate: this.unidadeTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, cellTemplate: this.atributosTemplate },
      { key: 'acoes', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.loteService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: `${sort},${order}`
    });
  }

  openMaterialTypeDialog(): void {
    this.dialog.open(MaterialTypeList, {
      width: '80vw',
      maxWidth: '900px',
      height: '80vh' // Revertido para o valor original
    });
  }

  async onCreate(): Promise<void> {
    try {
      const template = await lastValueFrom(this.loteService.getNewTemplate());
      this.openFormDialog({
        template,
        title: 'Registrar Entrada de Lote'
      }, 'Lote registrado com sucesso!');
    } catch (error) {
      console.error('Erro ao buscar template para novo lote:', error);
      this.entityDialog.showErrorSnackbar('Não foi possível iniciar o registro do lote.');
    }
  }

  async editLote(lote: LoteMateriaPrima): Promise<void> {
    const selfUrl = lote._links?.['self']?.href;
    if (!selfUrl) {
      this.entityDialog.showErrorSnackbar('Não foi possível encontrar o recurso.');
      return;
    }

    try {
      const itemToEdit = await lastValueFrom(this.loteService.findByUrl(selfUrl));
      this.openFormDialog({
        template: itemToEdit,
        title: 'Editar Lote'
      }, 'Lote atualizado com sucesso!');
    } catch (error) {
      console.error('Erro ao buscar dados para edição:', error);
      this.entityDialog.showErrorSnackbar('Falha ao carregar dados para edição.');
    }
  }

  deleteLote(lote: LoteMateriaPrima): void {
    const deleteUrl = lote._links?.['delete']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar('Não foi possível encontrar a ação de exclusão.');
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `Lote '${lote.id}'`,
      'Confirmar Exclusão'
    ).subscribe(confirmed => {
      if (confirmed) {
        this.loteService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar('Lote excluído com sucesso!');
          },
          error: () => {
            this.entityDialog.showErrorSnackbar('Falha ao excluir o lote.');
          }
        });
      }
    });
  }

  private openFormDialog(dialogData: LoteMateriaPrimaFormData, successMessage: string): void {
    this.entityDialog.openFormDialog({
      component: LoteMateriaPrimaForm,
      formData: dialogData,
      title: dialogData.title,
      width: '800px'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar(successMessage);
      }
    });
  }

  getAtributosAsArray(atributos: { [key: string]: any }): { key: string, value: any }[] {
    if (!atributos) {
      return [];
    }
    return Object.entries(atributos).map(([key, value]) => ({ key, value }));
  }
}
