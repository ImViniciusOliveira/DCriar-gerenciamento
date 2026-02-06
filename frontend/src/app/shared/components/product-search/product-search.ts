import { Component, inject, input, output, OnInit, signal, DestroyRef } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, tap, catchError, filter } from 'rxjs/operators';
import { of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ProductService } from '../../../features/products/services/product.service';
import { Product } from '../../../features/products/models/product.model';

/**
 * Componente de busca de produtos com Autocomplete.
 * Permite buscar produtos por nome ou SKU e selecionar um item da lista.
 */
@Component({
  selector: 'app-product-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './product-search.html',
  styleUrls: ['./product-search.scss']
})
export class ProductSearch implements OnInit {
  // --- Inputs e Outputs ---
  /** FormControl para o texto de busca (exibição). */
  searchControl = input.required<FormControl>();

  /** Evento emitido quando um produto é selecionado. */
  productSelected = output<Product>();

  // --- Injeção ---
  private readonly productService = inject(ProductService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Estado ---
  filteredProducts = signal<Product[]>([]);
  isLoading = signal(false);

  constructor() {
    // Configura a lógica de busca reativa
    // Nota: A subscrição é feita no ngOnInit para garantir que o input searchControl esteja disponível
  }

  ngOnInit(): void {
    this.searchControl().valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(value => typeof value === 'string'), // Ignora se o valor for um objeto (após seleção)
      tap(() => this.isLoading.set(true)),
      switchMap(value => {
        if (!value || value.length < 2) {
          return of([]); // Retorna array vazio se texto curto
        }

        return this.productService.searchProducts(value).pipe(
          catchError(() => of([]))
        );
      }),
      takeUntilDestroyed(this.destroyRef) // Passando o DestroyRef explicitamente
    ).subscribe(products => {
      this.filteredProducts.set(products);
      this.isLoading.set(false);
    });
  }

  displayFn(product: Product): string {
    return product && product.nome ? product.nome : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const product: Product = event.option.value;
    this.productSelected.emit(product);
  }
}
