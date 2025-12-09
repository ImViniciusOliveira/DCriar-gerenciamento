import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { lastValueFrom } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';
import { TipoMateriaPrimaService } from '../../services/tipo-materia-prima.service';
import { MaterialTypeForm, MaterialTypeFormData } from '../material-type-form/material-type-form';

@Component({
  selector: 'app-material-type-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule, BaseTable],
  templateUrl: './material-type-list.html',
  styleUrl: './material-type-list.scss'
})
export class MaterialTypeList extends BaseList<TipoMateriaPrima> implements AfterViewInit {
  private readonly tipoService = inject(TipoMateriaPrimaService);
  private readonly cdr = inject(ChangeDetectorRef);

  tableColumns: TableColumn<TipoMateriaPrima>[] = [];

  @ViewChild('materiaNomeTemplate') materiaNomeTemplate!: TemplateRef<any>;
  @ViewChild('materiaUnidadeTemplate') materiaUnidadeTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nome', header: 'Nome', sortable: true, cellTemplate: this.materiaNomeTemplate },
      { key: 'unidadeDeConsumo', header: 'Unidade', sortable: false, cellTemplate: this.materiaUnidadeTemplate },
      { key: 'actions', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    const page = this.pagination.pageIndex();
    const size = this.pagination.pageSize();
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.tipoService.findAll(page, size, sort, order).subscribe((response: any) => {
      this.items.set(response._embedded?.['tipos-materia-prima'] || []);
      this.pagination.updateTotalElements(response.page?.totalElements || 0);
    });
  }

  async onCreate(): Promise<void> {
    try {
      const template = await lastValueFrom(this.tipoService.getNewTemplate());
      this.openFormDialog({
        template,
        title: 'Cadastrar Matéria-Prima'
      }, 'Matéria-prima cadastrada com sucesso!');
    } catch (error) {
      console.error('Erro ao buscar template para nova matéria-prima:', error);
      this.entityDialog.showErrorSnackbar('Não foi possível iniciar o cadastro.');
    }
  }

  async editItem(item: TipoMateriaPrima): Promise<void> {
    const selfUrl = item._links?.['self']?.href;
    if (!selfUrl) {
      this.entityDialog.showErrorSnackbar('Não foi possível encontrar o recurso.');
      return;
    }

    try {
      const itemToEdit = await lastValueFrom(this.tipoService.findByUrl(selfUrl));
      this.openFormDialog({
        template: itemToEdit,
        title: 'Editar Matéria-Prima'
      }, 'Matéria-prima atualizada com sucesso!');
    } catch (error) {
      console.error('Erro ao buscar dados para edição:', error);
      this.entityDialog.showErrorSnackbar('Falha ao carregar dados para edição.');
    }
  }

  deleteItem(item: TipoMateriaPrima): void {
    const deleteUrl = item._links?.['delete']?.href;
    if (!deleteUrl) {
      this.entityDialog.showErrorSnackbar('Não foi possível encontrar a ação de exclusão.');
      return;
    }

    this.entityDialog.openConfirmDeleteDialog(
      `'${item.nome}'`,
      'Confirmar Exclusão'
    ).subscribe(confirmed => {
      if (confirmed) {
        this.tipoService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar('Matéria-prima excluída com sucesso!');
            this.loadItems();
          },
          error: () => {
            this.entityDialog.showErrorSnackbar('Falha ao excluir a matéria-prima.');
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
        this.loadItems();
      }
    });
  }
}
