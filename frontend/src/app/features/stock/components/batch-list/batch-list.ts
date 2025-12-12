import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { LoteMateriaPrima } from '../../models/lote-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeList } from '../material-type-list/material-type-list';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';
import { LoteMateriaPrimaForm, LoteMateriaPrimaFormData } from '../lote-materia-prima-form/lote-materia-prima-form';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

/**
 * Componente de listagem de Lotes de Matéria-Prima.
 *
 * Utiliza a estratégia `OnPush` e Signals para reagir automaticamente às mudanças
 * de estado no serviço `LoteMateriaPrimaService`.
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
  // Fornece uma instância local do PaginationHandler para esta lista.
  // Isso isola o estado da paginação (tamanho da página, etc.) de outras listas na aplicação.
  providers: [PaginationHandler]
})
export class BatchList extends BaseList<LoteMateriaPrima> implements AfterViewInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly loteService = inject(LoteMateriaPrimaService);
  private readonly dialog = inject(MatDialog);

  tableColumns: TableColumn<LoteMateriaPrima>[] = [];

  // Referências aos templates de célula definidos no HTML
  @ViewChild('tipoTemplate') tipoTemplate!: TemplateRef<any>;
  @ViewChild('saldoTemplate') saldoTemplate!: TemplateRef<any>;
  @ViewChild('unidadeTemplate') unidadeTemplate!: TemplateRef<any>;
  @ViewChild('atributosTemplate') atributosTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super();

    // Converte o Observable de lotes do serviço em um signal para consumo reativo.
    // O tratamento de erro é feito aqui para garantir que o signal sempre tenha um valor válido.
    const lotesResponse = toSignal(
      this.loteService.lotesMateriaPrima$.pipe(
        catchError((error) => {
          console.error('Erro ao carregar lotes:', error);
          this.entityDialog.showErrorSnackbar('Falha ao carregar a lista de lotes.');
          return of(undefined);
        })
      )
    );

    // Reage a novas emissões do serviço e atualiza o estado da lista.
    // Sincroniza os dados recebidos com o estado interno da BaseList e do PaginationHandler.
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
    // Configura as colunas da tabela.
    // Necessário fazer no AfterViewInit pois depende dos @ViewChild templates.
    this.tableColumns = [
      { key: 'nomeTipoMateriaPrima', header: 'Matéria-Prima', sortable: true, cellTemplate: this.tipoTemplate },
      { key: 'saldoEstoque', header: 'Saldo', sortable: true, cellTemplate: this.saldoTemplate },
      { key: 'unidadeDeEstoque', header: 'Unidade', sortable: true, cellTemplate: this.unidadeTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, cellTemplate: this.atributosTemplate },
      { key: 'acoes', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    // Marca para verificação pois alteramos dados que afetam a view após a inicialização
    this.cdr.detectChanges();
  }

  /**
   * Notifica o serviço sobre mudanças na paginação ou ordenação.
   * A atualização da lista ocorre reativamente através do `effect` no construtor.
   */
  override loadItems(): void {
    this.loteService.updateSearchParams({
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

  async onCreate(): Promise<void> {
    try {
      // Busca o template HATEOAS para criação
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
        // O serviço cuidará de atualizar a lista automaticamente após o delete bem-sucedido
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

  /**
   * Converte o objeto de atributos em um array para exibição na tabela.
   * Necessário porque o template itera sobre uma lista, mas os atributos vêm como um mapa.
   */
  getAtributosAsArray(atributos: { [key: string]: any }): { key: string, value: any }[] {
    if (!atributos) {
      return [];
    }
    return Object.entries(atributos).map(([key, value]) => ({ key, value }));
  }
}
