import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, effect, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom, catchError, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
import { ProductFormComponent, ProductFormData } from '../product-form/product-form';
import { FilterStockPipe } from './filter-stock.pipe';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DetailsPopover } from '../../../../shared/components/details-popover/details-popover';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

/**
 * Componente de listagem para Produtos.
 * Gerencia a exibição de dados em tabela, paginação e ações de CRUD (criar, editar, deletar).
 */
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
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  @ViewChild('priceTemplate') priceTemplate!: TemplateRef<any>;
  @ViewChild('createdAtTemplate') createdAtTemplate!: TemplateRef<any>;
  @ViewChild('detalhesTemplate') detalhesTemplate!: TemplateRef<any>;
  @ViewChild('estoquePorCanalTemplate') estoquePorCanalTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  constructor() {
    super('products', { active: 'nome', direction: 'asc' });
    const productsResponse = toSignal(
      this.productService.getProducts().pipe(
        catchError(() => {
          this.entityDialog.showErrorSnackbar(ProductList.Texts.loadError);
          return of(undefined);
        })
      )
    );

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
      { key: 'nome', header: 'Produto', sortable: true, className: 'col-wide', cellTemplate: this.nomeTemplate },
      { key: 'sku', header: 'SKU', sortable: true, cellTemplate: this.skuTemplate },
      { key: 'precoComercial', header: 'Preço', sortable: true, className: 'col-price', cellTemplate: this.priceTemplate },
      { key: 'dataCriacao', header: 'Criado em', sortable: true, className: 'col-created', cellTemplate: this.createdAtTemplate },
      { key: 'ativo', header: 'Ativo', sortable: true, className: 'col-active', cellTemplate: this.ativoTemplate },
      { key: 'estoque', header: 'Estoque Total', sortable: true, sortKey: 'estoqueFisicoTotal', cellTemplate: this.estoqueTemplate },
      { key: 'detalhes', header: 'Detalhes', sortable: false, className: 'col-fit-hover-left', cellTemplate: this.detalhesTemplate },
      { key: 'estoquePorCanal', header: 'Canais', sortable: false, className: 'col-fit-hover-left', cellTemplate: this.estoquePorCanalTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, className: 'col-actions', cellTemplate: this.acoesTemplate },
    ];
    this.cdr.detectChanges();
  }

  /**
   * Notifica o serviço sobre mudanças de paginação ou ordenação.
   * A UI é atualizada reativamente pelo `effect` no construtor.
   */
  override loadItems(): void {
    this.productService.updateSearchParams(
      this.pagination.pageIndex(),
      this.pagination.pageSize(),
      this.pagination.sortString()
    );
  }

  /**
   * Solicita confirmação e remove o produto selecionado.
   */
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

  /**
   * Abre o formulário em modo de visualização para o produto selecionado.
   */
  onView(product: Product): void {
    const dialogData: ProductFormData = { product, isEditMode: false, title: 'Detalhes do Produto' };
    this.openProductDialog(dialogData);
  }

  /**
   * Abre o formulário de edição para o produto selecionado.
   */
  onEdit(product: Product): void {
    const productCopy = structuredClone(product);
    this.openProductDialog({ product: productCopy, isEditMode: true, title: 'Editar Produto' }, ProductList.Texts.saveSuccess);
  }

  /**
   * Abre o formulário para a criação de um novo Produto.
   */
  async onCreate(): Promise<void> {
    try {
      const newProductTemplate = await lastValueFrom(this.productService.getNewProductTemplate());
      this.openProductDialog({
        product: newProductTemplate,
        isEditMode: false,
        isCreationMode: true,
        title: 'Cadastrar Produto'
      }, ProductList.Texts.createSuccess);
    } catch {
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

  /**
   * Formata os detalhes específicos do produto para exibição no popover.
   */
  getProductDetails(product: Product): { key: string, value: string }[] {
    const details: { key: string, value: string }[] = [];
    if (product.tipoProduto === 'CORTE') {
      if (product.dimensoes) {
        details.push({ key: '', value: `${product.dimensoes.larguraCm} x ${product.dimensoes.comprimentoCm} cm` });
      }
      if (product.cor) {
        details.push({ key: '', value: product.cor });
      }
    } else if (product.tipoProduto === 'CONSUMO') {
      if (product.codigoFabricante) {
        details.push({ key: '', value: product.codigoFabricante });
      }
      for (const [key, value] of Object.entries(product.especificacoes || {})) {
        details.push({ key: '', value: String(value) });
      }
    }
    return details;
  }
}
