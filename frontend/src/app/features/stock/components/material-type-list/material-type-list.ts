import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { MaterialType } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { MaterialTypeForm, MaterialTypeFormData } from '../material-type-form/material-type-form';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

/**
 * Componente de listagem para Tipos de Matéria-Prima.
 * Gerencia a exibição de dados em tabela, paginação e ações de CRUD (criar, editar, deletar).
 */
@Component({
  selector: 'app-material-type-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule, BaseTable],
  templateUrl: './material-type-list.html',
  styleUrls: ['./material-type-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class MaterialTypeList extends BaseList<MaterialType> implements AfterViewInit, OnInit {
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.materialTypeService.resetSearchParams();
  }

  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteSuccess: 'Matéria-prima excluída com sucesso!',
    saveSuccess: 'Matéria-prima atualizada com sucesso!',
    createSuccess: 'Matéria-prima cadastrada com sucesso!',
    deleteError: 'Falha ao excluir a matéria-prima.',
    loadError: 'Falha ao carregar a lista.',
    createError: 'Não foi possível iniciar o cadastro.',
    resourceError: 'Não foi possível encontrar o recurso.',
    createTitle: 'Cadastrar Matéria-Prima',
    editTitle: 'Editar Matéria-Prima'
  };

  tableColumns: TableColumn<MaterialType>[] = [];

  @ViewChild('nameTemplate') nameTemplate!: TemplateRef<any>;
  @ViewChild('unitTemplate') unitTemplate!: TemplateRef<any>;
  @ViewChild('createdAtTemplate') createdAtTemplate!: TemplateRef<any>;
  @ViewChild('updatedAtTemplate') updatedAtTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  constructor() {
    super('material-types', { active: 'nome', direction: 'asc' });

    const materialTypesResponse = toSignal(
      this.materialTypeService.getMaterialTypes().pipe(
        catchError(() => {
          this.entityDialog.showErrorSnackbar(MaterialTypeList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    effect(() => {
      const response = materialTypesResponse();
      if (response) {
        const items = response._embedded?.['tipos-materia-prima'] ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(items);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nome', header: 'Nome', sortable: true, sortType: 'text', cellTemplate: this.nameTemplate },
      { key: 'unidadeDescricao', header: 'Unidade', sortable: false, widthPx: 150, cellTemplate: this.unitTemplate },
      { key: 'dataCriacao', header: 'Criado em', sortable: true, className: 'col-created', widthPx: 140, cellTemplate: this.createdAtTemplate },
      { key: 'dataAtualizacao', header: 'Atualizado em', sortable: true, className: 'col-created', widthPx: 140, cellTemplate: this.updatedAtTemplate },
      { key: 'actions', header: 'Ações', className: 'col-actions', widthPx: 100, cellTemplate: this.actionsTemplate }
    ];
    this.cdr.detectChanges();
  }

  /**
   * Notifica o serviço sobre mudanças de paginação ou ordenação.
   * A UI é atualizada reativamente pelo `effect` no construtor.
   */
  override loadItems(): void {
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.materialTypeService.updateSearchParams({
      page: this.pagination.pageIndex(),
      size: this.pagination.pageSize(),
      sort: `${sort},${order}`
    });
  }

  /**
   * Abre o formulário para a criação de um novo Tipo de Matéria-Prima.
   */
  async onCreate(): Promise<void> {
    try {
      const template = await lastValueFrom(this.materialTypeService.getNewTemplate());
      this.openFormDialog({
        template,
        title: MaterialTypeList.Texts.createTitle
      }, MaterialTypeList.Texts.createSuccess);
    } catch {
      this.entityDialog.showErrorSnackbar(MaterialTypeList.Texts.createError);
    }
  }

  /**
   * Abre o formulário de edição para o item selecionado.
   */
  onEdit(item: MaterialType): void {
    const itemCopy = structuredClone(item);
    this.openFormDialog({
      template: itemCopy,
      title: MaterialTypeList.Texts.editTitle
    }, MaterialTypeList.Texts.saveSuccess);
  }

  /**
   * Solicita confirmação e remove o item selecionado.
   */
  onDelete(item: MaterialType): void {
    const deleteUrl = item._links?.['delete']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar(MaterialTypeList.Texts.resourceError);
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `'${item.nome}'`,
      MaterialTypeList.Texts.deleteConfirmTitle
    ).subscribe(confirmed => {
      if (confirmed) {
        this.materialTypeService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar(MaterialTypeList.Texts.deleteSuccess);
          },
          error: () => {
            this.entityDialog.showErrorSnackbar(MaterialTypeList.Texts.deleteError);
          }
        });
      }
    });
  }

  private openFormDialog(dialogData: MaterialTypeFormData, successMessage: string): void {
    this.entityDialog.openFormDialog({
      component: MaterialTypeForm,
      formData: dialogData,
      title: dialogData.title,
      width: '500px'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar(successMessage);
      }
    });
  }
}
