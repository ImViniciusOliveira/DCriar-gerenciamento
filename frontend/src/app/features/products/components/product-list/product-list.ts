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
import { filter, of, lastValueFrom, map, take, switchMap, catchError, forkJoin } from 'rxjs';
import { ProductFormComponent, ProductFormData } from '../product-form/product-form';
import { MatCardModule } from '@angular/material/card';
import { ApiRoot } from '../../../../core/services/api-root';
import { SalesChannelService } from '../../services/sales-channel.service';
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
  private readonly salesChannelService = inject(SalesChannelService);
  private readonly enumService = inject(EnumService);

  private channelNameMap = new Map<string, string>(); // Pode ser convertido para signal se houver necessidade de reatividade

  products = signal<Product[]>([]);
  isLoading = signal(false);
  displayedColumns: string[] = ['sku', 'nome', 'cor', 'dimensoes', 'ativo', 'estoque', 'estoquePorCanal', 'acoes'];

  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  // Controla a ordenação
  sortActive = signal('nome');
  sortDirection = signal<Sort['direction']>('asc');

  // Usar um signal para o mapa de unidades de consumo para consistência e reatividade
  readonly consumptionUnitsMap = signal(new Map<string, EnumOption>());

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnInit(): void {
    this.loadProducts();
    this.salesChannelService.channelNameMap$.pipe(take(1)).subscribe((mapData) => {
      this.channelNameMap = mapData;
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);

    this.salesChannelService.channelKeys$.pipe(
      take(1),
      switchMap(() => {
        const sortString = `${this.sortActive()},${this.sortDirection()}`;
        return this.productsService.getProducts(this.pageIndex(), this.pageSize(), sortString);
      }),
      switchMap(productsResponse => {
        const products = productsResponse?._embedded?.produtos || [];
        this.totalElements.set(productsResponse.page?.totalElements || 0);

        if (products.length === 0) {
          return of([]); // Retorna um array vazio para o subscribe final.
        }

        const stockObservables = products.map(product =>
          this.productsService.getChannelStock(product).pipe(
            map(stock => ({ productId: product.id, stock })),
            catchError(() => of({ productId: product.id, stock: {} })) // Em caso de erro, não quebra a cadeia.
          )
        );

        return forkJoin(stockObservables).pipe(
          map(stocks => this.mergeStockData(products, stocks))
        );
      })
    ).subscribe({
      next: (finalProducts) => {
        this.products.set(finalProducts);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Erro ao carregar produtos:', error);
        this.snackBar.open(ProductList.Texts.loadError, 'Fechar', { duration: 5000 });
        this.isLoading.set(false);
      }
    });
  }

  private mergeStockData(products: Product[], stocks: { productId: number; stock: { [key: string]: number } }[]): Product[] {
    const stockMap = new Map(stocks.map(s => [s.productId, s.stock]));
    const channelKeys = Array.from(this.channelNameMap.keys());
    const baseChannelStock = Object.fromEntries(channelKeys.map(key => [key, 0]));

    return products.map(product => {
      const productSpecificStock = stockMap.get(product.id) || {};
      const finalStock = { ...baseChannelStock, ...productSpecificStock };
      return { ...product, estoquePorCanal: finalStock };
    });
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
    } finally {
      this.isLoading.set(false);
    }
  }

  async onEdit(product: Product): Promise<void> {
    try {
      this.isLoading.set(true);
      const productCopy = structuredClone(product);
      this.openProductDialog({ product: productCopy, isEditMode: true, title: 'Editar Produto' }, ProductList.Texts.saveSuccess);
    } catch (error) {
      console.error('Erro ao buscar detalhes do produto para edição:', error);
      this.snackBar.open('Não foi possível carregar os dados para edição.', 'Fechar', { duration: 3000 });
    } finally {
      this.isLoading.set(false);
    }
  }

  async onCreate(): Promise<void> {
    try {
      this.isLoading.set(true);
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
    } finally {
      this.isLoading.set(false);
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
    return this.consumptionUnitsMap().get(key)?.viewValue ?? key; // Acessa o valor do signal
  }

  getChannelDisplayName(channelKey: string): string {
    return this.channelNameMap.get(channelKey) || channelKey;
  }

  trackByProductId(index: number, product: Product): number {
    return product.id;
  }
}

function compare(a: number | string, b: number | string, isAsc: boolean) {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}
