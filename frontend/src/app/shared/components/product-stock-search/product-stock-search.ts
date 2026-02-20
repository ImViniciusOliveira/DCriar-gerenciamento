import { Component, DestroyRef, inject, input, OnDestroy, OnInit, output, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
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
  /**
   * O estado de 'buscando' agora é um Signal de leitura (Signal<boolean>)
   * que reflete diretamente o estado do serviço, garantindo uma única fonte da verdade.
   */
  isSearching: Signal<boolean>;

  constructor() {
    // Conecta o estado de busca local com o estado do serviço.
    this.isSearching = this.productService.isSearchingByStock;

    // Apenas a digitação no campo de busca principal dispara uma nova busca.
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.triggerSearchNow();
    });
  }

  ngOnInit(): void {
    // Preenche o campo se houver valor inicial (modo de edição)
    const initialValue = this.control().value;
    if (initialValue) {
      this.searchControl.setValue(initialValue);
    }

    // Gatilho 1: Dispara a busca inicial ao carregar o componente.
    this.triggerSearchNow();
  }

  ngOnDestroy(): void {}

  /**
   * Gatilho 2: Dispara a busca ao abrir o painel do autocomplete.
   */
  onAutocompleteOpened(): void {
    this.triggerSearchNow();
  }

  /**
   * Centraliza a lógica de busca, lendo os valores atuais dos controles
   * e enviando-os para o serviço. Este é o único local que chama o serviço.
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
   * Importante: Apenas muda o valor do controle, não dispara uma nova busca.
   */
  toggleFilterOperator(): void {
    const current = this.filterOperatorControl.value;
    this.filterOperatorControl.setValue(current === 'GTE' ? 'LTE' : 'GTE');
  }
}
