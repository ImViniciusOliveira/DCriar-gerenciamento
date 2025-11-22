import { Component, OnInit, ViewChild, inject, signal, TemplateRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Product } from '../../models/products.model';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProductsService } from '../../services/products';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog/confirm-dialog';
import { filter, of, lastValueFrom, map, switchMap, catchError } from 'rxjs';
import { ProductFormComponent, ProductFormData } from '../product-form/product-form';
import { ApiRoot } from '../../../../core/services/api-root';
import { FilterStockPipe } from './filter-stock.pipe';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';
import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatMenuModule,
    MatTooltipModule,
    FilterStockPipe,
    BaseTable,
  ],
  templateUrl: './product-list.html',
  styleUrls: ['./product-list.scss'],
})
export class ProductList implements OnInit, AfterViewInit {
  private static readonly Texts = {
    deleteConfirmTitle: 'Confirmar Exclusão',
    deleteConfirmMessage: (name: string) => `Tem certeza que deseja excluir o produto "${name}"?`,
    deleteSuccess: 'Produto excluído com sucesso!',
    saveSuccess: 'Produto salvo com sucesso!',
    createSuccess: 'Produto cadastrado com sucesso!',
    deleteError: 'Falha ao excluir o produto.',
    loadError: 'Falha ao carregar a lista de produtos. Tente novamente mais tarde.',
  };
  private readonly productsService = inject(ProductsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);

  private loadingTimer: any;

  products = signal<Product[]>([]);
  isLoading = signal(false);
  tableColumns: TableColumn<Product>[] = [];

  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  sortActive = signal('nome');
  sortDirection = signal<Sort['direction']>('asc');

  readonly consumptionUnitsMap = signal(new Map<string, EnumOption>());

  @ViewChild('skuTemplate') skuTemplate!: TemplateRef<any>;
  @ViewChild('nomeTemplate') nomeTemplate!: TemplateRef<any>;
  @ViewChild('ativoTemplate') ativoTemplate!: TemplateRef<any>;
  @ViewChild('estoqueTemplate') estoqueTemplate!: TemplateRef<any>;
  @ViewChild('detalhesTemplate') detalhesTemplate!: TemplateRef<any>;
  @ViewChild('estoquePorCanalTemplate') estoquePorCanalTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  ngOnInit(): void {
    this.loadProducts();
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
  }

  loadProducts(): void {
    this.loadingTimer = setTimeout(() => this.isLoading.set(true), 300);

    const sortString = `${this.sortActive()},${this.sortDirection()}`;
    this.productsService.getProducts(this.pageIndex(), this.pageSize(), sortString).pipe(
      switchMap(productsResponse => {
        const products = productsResponse?._embedded?.produtos || [];
        const stockUrl = productsResponse._links?.['estoques-por-produtos']?.href;
        this.totalElements.set(productsResponse.page?.totalElements || 0);

        if (products.length === 0 || !stockUrl) {
          return of([]);
        }

        const productIds = products.map(p => p.id);
        return this.productsService.getStocksForProducts(productIds, stockUrl).pipe(
          map(allStocks => this.mergeStockData(products, allStocks)),
          catchError(() => of(products))
        );
      })
    ).subscribe({
      next: (finalProducts) => {
        clearTimeout(this.loadingTimer);
        this.products.set(finalProducts);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Erro ao carregar produtos:', error);
        clearTimeout(this.loadingTimer);
        this.snackBar.open(ProductList.Texts.loadError, 'Fechar', { duration: 5000 });
        this.isLoading.set(false);
      }
    });
  }

  private mergeStockData(products: Product[], allStocks: { [productId: string]: { [channelKey: string]: number } }): Product[] {
    return products.map(product => ({
      ...product,
      estoquePorCanal: allStocks[product.id] || {},
    }));
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadProducts();
  }

  onSortChange(sort: Sort) {
    this.sortActive.set(sort.direction ? sort.active : 'nome');
    this.sortDirection.set(sort.direction || 'asc');
    this.pageIndex.set(0);
    this.loadProducts();
  }

  async onDelete(product: Product): Promise<void> {
    const dialogData: ConfirmDialogData = {
      title: ProductList.Texts.deleteConfirmTitle,
      message: ProductList.Texts.deleteConfirmMessage(product.nome),
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      try {
        const deleteUrl = product._links?.['deletar-produto']?.href;
        if (!deleteUrl) throw new Error('URL de exclusão não encontrada.');
        await lastValueFrom(this.productsService.deleteProduct(deleteUrl));
        this.snackBar.open(ProductList.Texts.deleteSuccess, 'Fechar', { duration: 3000 });
        this.loadProducts();
      } catch (error) {
        console.error('Erro ao excluir produto:', error);
        this.snackBar.open(ProductList.Texts.deleteError, 'Fechar', { duration: 3000 });
      }
    }
  }

  async onView(product: Product): Promise<void> {
    const dialogData: ProductFormData = { product, isEditMode: false, title: 'Detalhes do Produto' };
    this.openProductDialog(dialogData, '');
  }

  async onEdit(product: Product): Promise<void> {
    const productCopy = structuredClone(product);
    this.openProductDialog({ product: productCopy, isEditMode: true, title: 'Editar Produto' }, ProductList.Texts.saveSuccess);
  }

  async onCreate(): Promise<void> {
    try {
      await lastValueFrom(this.apiRoot.endpoints$);
      const newProductTemplate = await lastValueFrom(this.productsService.getNewProductTemplate());
      this.openProductDialog({
        product: newProductTemplate,
        isEditMode: false,
        isCreationMode: true,
        title: 'Cadastrar Produto'
      }, ProductList.Texts.createSuccess);
    } catch (error) {
      console.error('Erro ao buscar template para novo produto:', error);
      this.snackBar.open('Não foi possível iniciar o cadastro de um novo produto.', 'Fechar', { duration: 3000 });
    }
  }

  private openProductDialog(dialogData: ProductFormData, successMessage: string): void {
    const dialogRef = this.dialog.open(ProductFormComponent, {
      data: dialogData,
      width: '90vw',
      maxWidth: '900px',
      autoFocus: false,
    });

    dialogRef.afterClosed().pipe(filter(result => result === true)).subscribe(() => {
      if (successMessage) {
        this.snackBar.open(successMessage, 'Fechar', { duration: 3000 });
      }
      this.loadProducts();
    });
  }

  getChannelDisplayName(channelKey: string): string {
    return channelKey;
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
      Object.entries(product.especificacoes || {}).forEach(([key, value]) => details.push({ key, value }));
    }
    return details;
  }
}
