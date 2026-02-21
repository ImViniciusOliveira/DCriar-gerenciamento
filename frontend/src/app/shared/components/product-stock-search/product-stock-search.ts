import { Component, DestroyRef, effect, inject, input, OnDestroy, OnInit, output, signal, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { debounceTime, distinctUntilChanged, skip } from 'rxjs/operators';
import { Product } from '../../../features/products/models/product.model';
import { ProductService } from '../../../features/products/services/product.service';

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
    MatProgressSpinnerModule,
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

  // --- Injeção de Dependências ---
  private readonly productService = inject(ProductService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Controles de Formulário Internos ---
  searchControl = new FormControl<string | Product | null>('');
  filterOperatorControl = new FormControl<'GTE' | 'LTE'>('GTE');
  filterValueControl = new FormControl<number | null>(1);

  // --- Estado Interno ---
  products = toSignal(this.productService.getProductsByStock(), { initialValue: [] });
  isSearching: Signal<boolean>;
  /**
   * Controla se os filtros (tipo, estoque) foram alterados manualmente pelo usuário,
   * para decidir se o campo de busca deve ser limpo no próximo clique.
   */
  private filtersAreDirty = signal(false);

  constructor() {
    this.isSearching = this.productService.isSearchingByStock;

    // Gatilho para busca ao digitar no campo principal.
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.triggerSearchNow();
    });

    // Reage a mudanças manuais nos filtros internos para marcar a busca como "suja".
    this.filterOperatorControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.markFiltersAsDirty());
    this.filterValueControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.markFiltersAsDirty());

    // Reage a mudanças no `productType` (vindo do pai) para disparar uma nova busca.
    effect(() => {
      this.productType();
      this.triggerSearchNow();
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
   * Verifica se o FormControl externo, passado para o componente, é obrigatório.
   * Isso é usado para exibir o asterisco (*) no mat-form-field interno.
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

    this.productService.updateProductByStockSearchParams({
      nome: searchTerm,
      tipoProduto: this.productType(),
      estoqueValor: this.filterValueControl.value ?? 1,
      estoqueOperador: this.filterOperatorControl.value ?? 'GTE',
    });
  }

  /**
   * Formata como o nome do produto é exibido no input após a seleção.
   */
  displayFn(product: Product): string {
    return product?.nome ? `${product.nome} (Estoque: ${product.estoqueFisicoTotal || 0})` : '';
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
