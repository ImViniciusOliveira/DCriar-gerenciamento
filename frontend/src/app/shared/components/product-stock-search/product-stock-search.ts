import { Component, DestroyRef, effect, inject, input, OnDestroy, OnInit, output, signal, Signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { debounceTime, distinctUntilChanged, filter, merge, mergeMap, combineLatest, startWith, skip } from 'rxjs';
import { Product } from '../../../features/products/models/product.model';
import { ProductService } from '../../../features/products/services/product.service';
import { EnumService, EnumOption } from '../../../core/services/enum.service';

/**
 * Componente genérico para busca e seleção de Produtos com Estoque Disponível.
 * Utiliza Autocomplete para combinar busca e seleção em um único campo, otimizando o espaço.
 */
@Component({
  selector: 'app-product-stock-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatAutocompleteModule
  ],
  templateUrl: './product-stock-search.html',
  styleUrls: ['./product-stock-search.scss'],
})
export class ProductStockSearch implements OnInit, OnDestroy {
  // --- Entradas e Saídas ---
  control = input.required<FormControl>();
  isEditMode = input(false);
  productType = input<string | null>(null);
  selectionChange = output<MatSelectChange>();

  // --- Referências de Template ---
  @ViewChild(MatAutocompleteTrigger) autocompleteTrigger!: MatAutocompleteTrigger;

  // --- Injeção de Dependências ---
  private readonly productService = inject(ProductService);
  private readonly enumService = inject(EnumService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Controles de Formulário Internos ---
  searchControl = new FormControl<string | Product | null>('');
  filterOperatorControl = new FormControl<'GTE' | 'LTE'>('GTE');
  filterValueControl = new FormControl<number | null>(null);

  // --- Estado Interno ---
  products = toSignal(this.productService.getProductsByStock(), { initialValue: [] });
  isSearching: Signal<boolean>;
  /**
   * Controla se os filtros (tipo, estoque) foram alterados manualmente pelo usuário,
   * para decidir se o campo de busca deve ser limpo no próximo clique.
   */
  private filtersAreDirty = signal(false);
  private readonly unitsUrl = signal<string | null>(null);
  readonly consumptionUnitsMap: Signal<Map<string, EnumOption>> = toSignal(
    toObservable(this.unitsUrl).pipe(
      distinctUntilChanged(),
      filter((url): url is string => !!url),
      mergeMap(url => this.enumService.getConsumptionUnitsMap(url))
    ),
    { initialValue: new Map<string, EnumOption>() }
  );

  constructor() {
    this.isSearching = this.productService.isSearchingByStock;
    this.setupSearchTrigger();

    effect(() => {
      const firstProduct = this.products()[0];
      const url = firstProduct?._links?.['unidades-de-medida']?.href ?? null;

      if (url && this.unitsUrl() !== url) {
        this.unitsUrl.set(url);
      }
    });
  }

  ngOnInit(): void {
    const initialValue = this.control().value;
    if (initialValue) {
      this.searchControl.setValue(initialValue);
    }
  }

  ngOnDestroy(): void {}

  /**
   * Configura os gatilhos reativos para acionar a busca de produtos.
   * Combina um fluxo "lento" (com debounce para digitação) e um fluxo "rápido" (para cliques)
   * para criar uma experiência de usuário responsiva e eficiente.
   */
  private setupSearchTrigger(): void {
    const searchControl$ = this.searchControl.valueChanges.pipe(
      startWith(this.searchControl.value),
      filter(value => typeof value === 'string')
    );

    const slow$ = combineLatest([
      searchControl$,
      this.filterValueControl.valueChanges.pipe(startWith(this.filterValueControl.value))
    ]).pipe(
      debounceTime(300)
    );

    const fast$ = this.filterOperatorControl.valueChanges;

    merge(slow$, fast$).pipe(
      distinctUntilChanged(),
      skip(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.autocompleteTrigger?.openPanel();
      this.triggerSearchNow();
    });
  }

  /**
   * Verifica se o FormControl externo, passado para o componente, é obrigatório.
   */
  get isRequired(): boolean {
    return this.control().hasValidator(Validators.required);
  }

  /**
   * Dispara a busca ao abrir o painel do autocomplete e limpa o campo se os filtros mudaram.
   */
  onAutocompleteOpened(): void {
    if (this.filtersAreDirty()) {
      this.searchControl.setValue('');
      this.filtersAreDirty.set(false);
    }
    this.triggerSearchNow();
  }

  /**
   * Abre o painel de autocomplete quando o usuário foca no input de estoque.
   */
  onStockInputFocus(): void {
    this.autocompleteTrigger?.openPanel();
  }

  /**
   * Método público para que o componente pai possa notificar que um filtro externo
   * foi alterado manualmente pelo usuário.
   */
  public markFiltersAsDirty(): void {
    this.filtersAreDirty.set(true);
  }

  /**
   * Centraliza a lógica de busca, lendo os valores atuais dos controles
   * e enviando-os para o serviço.
   */
  private triggerSearchNow(): void {
    const searchTerm = typeof this.searchControl.value === 'string' ? this.searchControl.value : '';

    // Se o valor do filtro for null, busca produtos com estoque >= 0
    const estoqueValor = this.filterValueControl.value == null ? 0 : this.filterValueControl.value;

    this.productService.updateProductByStockSearchParams({
      nome: searchTerm,
      tipoProduto: this.productType(),
      estoqueValor,
      estoqueOperador: this.filterOperatorControl.value ?? 'GTE',
    });
  }

  /**
   * Formata como o nome do produto é exibido no input após a seleção.
   */
  displayFn = (product: Partial<Product> | null): string => {
    if (!product) return '';
    return this.buildProductLabel(product);
  };

  buildProductLabel(product: Partial<Product>): string {
    const nome = product.nome?.trim();
    if (!nome) return '';

    const detalhe = this.getProductDetail(product);
    const estoque = product.estoqueFisicoTotal || 0;

    return detalhe
      ? `${nome} (${detalhe}) (Estoque: ${estoque})`
      : `${nome} (Estoque: ${estoque})`;
  }

  private getProductDetail(product: Partial<Product>): string | null {
    if (product.dimensoes?.larguraCm != null && product.dimensoes?.comprimentoCm != null) {
      return `${this.formatNumber(product.dimensoes.larguraCm)}x${this.formatNumber(product.dimensoes.comprimentoCm)}cm`;
    }

    if (product.tipoProduto === 'CONSUMO' && product.unidadesPorProduto != null && product.materiaPrima?.unidadeDeConsumo) {
      return this.formatConsumptionAmount(product.unidadesPorProduto, product.materiaPrima.unidadeDeConsumo);
    }

    return null;
  }

  private formatConsumptionAmount(amount: number, unit: string): string {
    const normalizedUnit = unit.toUpperCase();
    const unitMeta = this.consumptionUnitsMap().get(normalizedUnit);
    const symbol = unitMeta?.simbolo?.trim();
    const description = unitMeta?.viewValue?.trim().toLowerCase();

    if (symbol && !this.isCountableUnit(normalizedUnit)) {
      return `${amount}${symbol}`;
    }

    if (description) {
      return `${amount} ${this.pluralizeUnit(normalizedUnit, description, amount)}`;
    }

    return `${amount} ${unit.toLowerCase()}`;
  }

  private isCountableUnit(unit: string): boolean {
    return unit === 'UNIDADE' || unit === 'FOLHA';
  }

  private pluralizeUnit(unit: string, description: string, amount: number): string {
    if (amount === 1) {
      return description;
    }

    const pluralMap: Record<string, string> = {
      UNIDADE: 'unidades',
      FOLHA: 'folhas'
    };

    return pluralMap[unit] ?? description;
  }

  private formatNumber(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 3
    }).format(value);
  }

  /**
   * Chamado quando uma opção é selecionada no autocomplete.
   */
  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as Product;
    this.control().setValue(selected);
    this.selectionChange.emit({ source: null as any, value: selected });
  }

  /**
   * Alterna o operador de filtro de estoque entre 'GTE' (≥) e 'LTE' (≤).
   */
  toggleFilterOperator(): void {
    const current = this.filterOperatorControl.value;
    this.filterOperatorControl.setValue(current === 'GTE' ? 'LTE' : 'GTE');
  }
}
