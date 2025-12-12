import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { TipoMateriaPrima } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { MaterialTypeForm, MaterialTypeFormData } from '../material-type-form/material-type-form';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

/**
 * Componente de listagem de Tipos de Matéria-Prima.
 *
 * Utiliza a estratégia `OnPush` e Signals para reagir automaticamente às mudanças
 * de estado no serviço `MaterialTypeService`.
 */
@Component({
  selector: 'app-material-type-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule, BaseTable],
  templateUrl: './material-type-list.html',
  styleUrls: ['./material-type-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler, MaterialTypeService]
})
export class MaterialTypeList extends BaseList<TipoMateriaPrima> implements AfterViewInit {
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly cdr = inject(ChangeDetectorRef);

  tableColumns: TableColumn<TipoMateriaPrima>[] = [];

  // Referências aos templates de célula definidos no HTML
  @ViewChild('nameTemplate') nameTemplate!: TemplateRef<any>;
  @ViewChild('unitTemplate') unitTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  constructor() {
    super();

    // Converte o fluxo de dados do serviço em um Signal de leitura.
    // Isso permite que o componente reaja a atualizações (filtros, paginação, refresh) automaticamente.
    const materialTypesResponse = toSignal(
      this.materialTypeService.getTiposMateriaPrima().pipe(
        catchError((error) => {
          console.error('Erro ao carregar tipos de matéria-prima:', error);
          this.entityDialog.showErrorSnackbar('Falha ao carregar a lista.');
          return of(undefined);
        })
      )
    );

    // Efeito colateral que sincroniza o estado do Signal com a BaseList.
    // Atualiza a lista de itens e os metadados de paginação sempre que o serviço emite novos dados.
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
    // Configura as colunas da tabela.
    // Necessário fazer no AfterViewInit pois depende dos @ViewChild templates.
    this.tableColumns = [
      { key: 'nome', header: 'Nome', sortable: true, cellTemplate: this.nameTemplate },
      { key: 'unidadeDeConsumo', header: 'Unidade', sortable: false, cellTemplate: this.unitTemplate },
      { key: 'actions', header: 'Ações', cellTemplate: this.actionsTemplate }
    ];
    // Marca para verificação pois alteramos dados que afetam a view após a inicialização
    this.cdr.detectChanges();
  }

  /**
   * Sobrescreve o método da BaseList.
   * Em vez de fazer a requisição manualmente, apenas atualiza os parâmetros no serviço.
   * O Signal no construtor cuidará de receber os novos dados.
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

  async onCreate(): Promise<void> {
    try {
      // Busca o template HATEOAS para criação
      const template = await lastValueFrom(this.materialTypeService.getNewTemplate());
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
      const itemToEdit = await lastValueFrom(this.materialTypeService.findByUrl(selfUrl));
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
        // O serviço cuidará de atualizar a lista automaticamente após o delete bem-sucedido
        this.materialTypeService.delete(deleteUrl).subscribe({
          next: () => {
            this.entityDialog.showSuccessSnackbar('Matéria-prima excluída com sucesso!');
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
      }
    });
  }
}
