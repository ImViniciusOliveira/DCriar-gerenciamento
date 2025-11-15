import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Product } from '../../models/products.model';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Sort, MatSortModule } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProductsService } from '../../services/products';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog/confirm-dialog';
import { filter, of, lastValueFrom, map, switchMap, catchError } from 'rxjs';
import { ProductFormComponent, ProductFormData } from '../product-form/product-form';
import { MatCardModule } from '@angular/material/card';
import { ApiRoot } from '../../../../core/services/api-root';
import { FilterStockPipe } from './filter-stock.pipe';
import { MatMenuModule } from '@angular/material/menu';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDialogModule,
    MatCardModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    FilterStockPipe,
    MatMenuModule,
    MatSnackBarModule,
  ],
  templateUrl: './product-list.html',
  styleUrls: ['./product-list.scss'],
})
export class ProductList implements OnInit {
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
  displayedColumns: string[] = ['sku', 'nome', 'cor', 'dimensoes', 'ativo', 'estoque', 'estoquePorCanal', 'acoes'];

  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  sortActive = signal('nome');
  sortDirection = signal<Sort['direction']>('asc');

  readonly consumptionUnitsMap = signal(new Map<string, EnumOption>());

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    // Exibe o spinner apenas se a operação demorar mais de 300ms.
    this.loadingTimer = setTimeout(() => this.isLoading.set(true), 300);

    const sortString = `${this.sortActive()},${this.sortDirection()}`;
    this.productsService.getProducts(this.pageIndex(), this.pageSize(), sortString).pipe(
      switchMap(productsResponse => {
        const products = productsResponse?._embedded?.produtos || [];
        const stockUrl = productsResponse._links?.['estoques-por-produtos']?.href;
        this.totalElements.set(productsResponse.page?.totalElements || 0);

        if (products.length === 0 || !stockUrl) {
          console.warn('[ProductList] URL de estoque não encontrada na resposta. Exibindo produtos sem dados de estoque por canal.');
          return of([]);
        }

        const productIds = products.map(p => p.id);
        return this.productsService.getStocksForProducts(productIds, stockUrl).pipe(
          map(allStocks => this.mergeStockData(products, allStocks)),
          catchError(() => {
            console.error('[ProductList] Falha ao buscar estoques. Exibindo produtos sem dados de estoque.');
            return of(products); // Em caso de erro, retorna apenas os produtos.
          })
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

  /**
   * Combina a lista de produtos com os dados de estoque.
   * Garante que cada produto tenha um objeto `estoquePorCanal` com todos os canais de venda,
   * preenchendo com 0 para os canais onde o produto não tem estoque.
   */
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

  sortData(sort: Sort) {
    this.sortActive.set(sort.direction ? sort.active : 'nome');
    this.sortDirection.set(sort.direction || 'asc');

    if (this.paginator && this.paginator.pageIndex !== 0) {
      this.paginator.firstPage();
    } else {
      this.loadProducts();
    }
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
        const deleteUrl = product._links['deletar-produto']?.href;
        if (!deleteUrl) {
          throw new Error('URL de exclusão não encontrada.');
        }
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
    try {
      const dialogData: ProductFormData = { product, isEditMode: false, title: 'Detalhes do Produto' };
      this.openProductDialog(dialogData, '');
    } catch (error) {
      console.error('Erro ao buscar detalhes do produto para visualização:', error);
      this.snackBar.open('Não foi possível carregar os dados para visualização.', 'Fechar', { duration: 3000 });
    }
  }

  async onEdit(product: Product): Promise<void> {
    try {
      const productCopy = structuredClone(product);
      this.openProductDialog({ product: productCopy, isEditMode: true, title: 'Editar Produto' }, ProductList.Texts.saveSuccess);
    } catch (error) {
      console.error('Erro ao buscar detalhes do produto para edição:', error);
      this.snackBar.open('Não foi possível carregar os dados para edição.', 'Fechar', { duration: 3000 });
    }
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
      width: '90vw', // Usa 90% da largura da tela
      maxWidth: '900px', // Mas não passa de 900px
      autoFocus: false,
    });

    dialogRef.afterClosed().pipe(filter(result => result === true)).subscribe(() => {
      this.snackBar.open(successMessage, 'Fechar', { duration: 3000 });
      this.loadProducts();
    });
  }

  getConsumptionUnitViewValue(key: string): string {
    return this.consumptionUnitsMap().get(key)?.viewValue ?? key;
  }

  getChannelDisplayName(channelKey: string): string {
    return channelKey;
  }

  trackByProductId(index: number, product: Product): number {
    return product.id;
  }
}
