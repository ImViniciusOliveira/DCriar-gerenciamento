import { Component, DestroyRef, effect, inject, input, OnDestroy, OnInit, output, signal, WritableSignal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { of } from 'rxjs';
import { filter, debounceTime, distinctUntilChanged, catchError, take } from 'rxjs/operators';
import { ApiRoot } from '../../../core/services/api-root';
import { Product } from '../../../features/products/models/product.model';
import { ProductService } from '../../../features/products/services/product.service';

/**
 * Componente genérico para busca e seleção de Produtos com Estoque Disponível.
 * Utiliza Autocomplete para combinar busca e seleção em um único campo, otimizando o espaço.
 * Padrão idêntico a MaterialTypeSearch.
 */
@Component({
  selector: 'app-product-stock-search',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatAutocompleteModule
  ],
  templateUrl: './product-stock-search.html',
  styleUrls: ['./product-stock-search.scss'],
})
export class ProductStockSearch implements OnInit, OnDestroy {
  // --- Entradas e Saídas ---
  /** O FormControl do formulário pai que este componente irá controlar. */
  control = input.required<FormControl>();
  /** Flag para indicar se o componente está em modo de edição, para lidar com o valor inicial. */
  isEditMode = input(false);
  /** Emite o evento de seleção para o componente pai. */
  selectionChange = output<MatSelectChange>();

  // --- Injeção de Dependências ---
  private readonly productService = inject(ProductService);
  private readonly apiRoot = inject(ApiRoot);
  private readonly destroyRef = inject(DestroyRef);

  // --- Controles de Formulário Internos ---
  searchControl = new FormControl<string | Product | null>('');
  filterOperatorControl = new FormControl<'GTE' | 'LTE'>('GTE');
  filterValueControl = new FormControl<number | null>(null);

  // --- Estado Interno ---
  products: WritableSignal<Partial<Product>[]> = signal([]);
  isSearching = signal(false);
  totalElements = signal(0);

  // --- Paginação ---
  private currentPage = 0;
  private readonly pageSize = 20;

  constructor() {
    const getUrl = (link: string) =>
      this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];
    const productsSearchUrl = getUrl('produtos') ?? null;

    if (!productsSearchUrl) {
      console.error('URL para busca de produtos não pôde ser determinada.');
    }

    // Desabilita o controle se a URL da API não for encontrada.
    effect(() => {
      if (!productsSearchUrl) {
        this.control().disable();
        this.searchControl.disable();
      }
    });

    const productsResponse = toSignal(
      this.productService.getProductsSimple().pipe(catchError(() => of([])))
    );

    // Reage à resposta do serviço.
    effect(() => {
      this.isSearching.set(false);
      const response = productsResponse();
      if (response && Array.isArray(response)) {
        if (this.currentPage === 0) {
          this.products.set(response);
        } else {
          this.products.update(current => [...current, ...response]);
        }
      }
    });
  }

  ngOnInit(): void {
    const ctrl = this.control();

    // Sincroniza o valor inicial do pai com o input de busca
    if (ctrl.value) {
      this.searchControl.setValue(ctrl.value);
      this.products.set([ctrl.value]);
    } else if (this.isEditMode()) {
      ctrl.valueChanges.pipe(
        filter(value => !!value),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((initialValue: Product) => {
        this.searchControl.setValue(initialValue);
        this.products.update(currentProducts => {
          const exists = currentProducts.some(p => p.id === initialValue.id);
          return exists ? currentProducts : [initialValue, ...currentProducts];
        });
      });
    }

    // Busca ao digitar no input (apenas se for string, ou seja, usuário digitando)
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (typeof value === 'string') {
        this.performSearch();
      }
    });
  }

  ngOnDestroy(): void {
  }

  performSearch(): void {
    this.isSearching.set(true);
    this.currentPage = 0;
    // ProductService usa updateSearchParams(page, size, sort)
    this.productService.updateSearchParams(this.currentPage, this.pageSize, 'nome,asc');
  }

  displayFn(product: Product): string {
    return product && product.nome ? `${product.nome} (Estoque: ${product.estoqueFisicoTotal || 0})` : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as Product;
    this.control().setValue(selected);
    // Emite um evento compatível com MatSelectChange para manter compatibilidade
    this.selectionChange.emit({ source: null as any, value: selected });
  }

  /**
   * Toggle entre ≥ (GTE) e ≤ (LTE)
   */
  toggleFilterOperator(): void {
    const current = this.filterOperatorControl.value;
    this.filterOperatorControl.setValue(current === 'GTE' ? 'LTE' : 'GTE');
  }
}


