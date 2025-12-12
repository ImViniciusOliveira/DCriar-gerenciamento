import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product';
import { ProductFormComponent, ProductFormData } from '../product-form/product-form';
import { FilterStockPipe } from './filter-stock.pipe';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    FilterStockPipe,
    BaseTable,
    DetailsPopover
  ],
  templateUrl: './product-list.html',
  styleUrls: ['./product-list.scss'],
  // Fornece uma instância local do PaginationHandler para esta lista.
  // Isso isola o estado da paginação (tamanho da página, etc.) de outras listas na aplicação.
  providers: [PaginationHandler]
})
export class ProductList extends BaseList<Product> implements AfterViewInit {
  private readonly productService = inject(ProductService);
  private readonly cdr = inject(ChangeDetectorRef);

  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteSuccess: 'Produto excluído com sucesso!',
    saveSuccess: 'Produto salvo com sucesso!',
    createSuccess: 'Produto cadastrado com sucesso!',
    deleteError: 'Falha ao excluir o produto.',
    loadError: 'Falha ao carregar a lista de produtos.',
    createError: 'Não foi possível iniciar o cadastro de um novo produto.',
  };

  tableColumns: TableColumn<Product>[] = [];

  @ViewChild('skuTemplate') skuTemplate!: TemplateRef<any>;
  @ViewChild('nomeTemplate') nomeTemplate!: TemplateRef<any>;
  @ViewChild('ativoTemplate') ativoTemplate!: TemplateRef<any>;
  @ViewChild('estoqueTemplate') estoqueTemplate!: TemplateRef<any>;
  @ViewChild('detalhesTemplate') detalhesTemplate!: TemplateRef<any>;
  @ViewChild('estoquePorCanalTemplate') estoquePorCanalTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super();
    // Converte o Observable de produtos do serviço em um signal para consumo reativo.
    const productsResponse = toSignal(
      this.productService.getProducts().pipe(
        catchError((error) => {
          console.error('Erro ao carregar produtos:', error);
          this.entityDialog.showErrorSnackbar(ProductList.Texts.loadError);
          return of(undefined);
        })
      )
    );

    // Reage a novas emissões do serviço e atualiza o estado da lista.
    effect(() => {
      const response = productsResponse();
      if (response) {
        const products = response._embedded?.produtos ?? [];
        this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
        this.items.set(products);
      }
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'sku', header: 'SKU', sortable: true, cellTemplate: this.skuTemplate },
      { key: 'nome', header: 'Produto', sortable: true, cellTemplate: this.nomeTemplate },
      { key: 'ativo', header: 'Ativo', sortable: true, cellTemplate: this.ativoTemplate },
      { key: 'estoque', header: 'Estoque Total', sortable: true, sortKey: 'estoqueFisicoTotal', cellTemplate: this.estoqueTemplate },
      { key: 'detalhes', header: 'Detalhes', sortable: false, cellTemplate: this.detalhesTemplate },
      { key: 'estoquePorCanal', header: 'Canais', sortable: false, cellTemplate: this.estoquePorCanalTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, cellTemplate: this.acoesTemplate },
    ];
    this.cdr.detectChanges();
  }

  /**
   * Notifica o serviço sobre mudanças na paginação ou ordenação.
   * A atualização da lista ocorre reativamente através do `effect` no construtor.
   */
  override loadItems(): void {
    this.productService.updateSearchParams(
      this.pagination.pageIndex(),
      this.pagination.pageSize(),
      this.pagination.sortString()
    );
  }

  onDelete(product: Product): void {
    this.entityDialog.openConfirmDeleteDialog(product.nome, ProductList.Texts.deleteConfirmTitle)
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;

        const deleteUrl = product._links?.['deletar-produto']?.href;
        if (!deleteUrl) {
          this.entityDialog.showErrorSnackbar(ProductList.Texts.deleteError);
          return;
        }
        this.productService.deleteProduct(deleteUrl).subscribe({
          next: () => this.entityDialog.showSuccessSnackbar(ProductList.Texts.deleteSuccess),
          error: () => this.entityDialog.showErrorSnackbar(ProductList.Texts.deleteError)
        });
      });
  }

  onView(product: Product): void {
    const dialogData: ProductFormData = { product, isEditMode: false, title: 'Detalhes do Produto' };
    this.openProductDialog(dialogData);
  }

  onEdit(product: Product): void {
    const productCopy = structuredClone(product);
    this.openProductDialog({ product: productCopy, isEditMode: true, title: 'Editar Produto' }, ProductList.Texts.saveSuccess);
  }

  async onCreate(): Promise<void> {
    try {
      const newProductTemplate = await lastValueFrom(this.productService.getNewProductTemplate());
      this.openProductDialog({
        product: newProductTemplate,
        isEditMode: false,
        isCreationMode: true,
        title: 'Cadastrar Produto'
      }, ProductList.Texts.createSuccess);
    } catch (error) {
      console.error('Erro ao buscar template para novo produto:', error);
      this.entityDialog.showErrorSnackbar(ProductList.Texts.createError);
    }
  }

  private openProductDialog(dialogData: ProductFormData, successMessage?: string): void {
    this.entityDialog.openFormDialog({
      component: ProductFormComponent,
      formData: dialogData,
      title: dialogData.title,
      width: '90vw',
      maxWidth: '900px',
    }).subscribe((saved: boolean) => {
      if (saved && successMessage) {
        this.entityDialog.showSuccessSnackbar(successMessage);
      }
    });
  }

  getProductDetails(product: Product): { key: string, value: string }[] {
    const details: { key: string, value: string }[] = [];
    if (product.tipoProduto === 'CORTE') {
      if (product.dimensoes) {
        details.push({ key: 'Dimensões', value: `${product.dimensoes.larguraCm} x ${product.dimensoes.comprimentoCm} cm` });
      }
      if (product.cor) {
        details.push({ key: 'Cor', value: product.cor });
      }
    } else if (product.tipoProduto === 'CONSUMO_DIRETO') {
      if (product.codigoFabricante) {
        details.push({ key: 'Cód. Fab.', value: product.codigoFabricante });
      }
      for (const [key, value] of Object.entries(product.especificacoes || {})) {
        details.push({ key, value });
      }
    }
    return details;
  }
}
