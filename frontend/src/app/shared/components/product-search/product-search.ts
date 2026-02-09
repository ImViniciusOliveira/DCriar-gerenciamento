import { Component, inject, input, output, OnInit, signal, DestroyRef, effect } from '@angular/core';
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
 * Suporta filtragem por canal de venda para exibir estoque disponível.
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

  /** ID do canal de venda para filtrar estoque (opcional). */
  channelId = input<number>();

  /** Mensagem de erro customizada para exibir no campo (ex: "Estoque insuficiente"). */
  customError = input<string | null>(null);

  /** Mensagem de dica customizada para exibir no campo (ex: "Estoque: 10"). */
  customHint = input<string | null>(null);

  /** Evento emitido quando um produto é selecionado. */
  productSelected = output<Product>();

  // --- Injeção ---
  private readonly productService = inject(ProductService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Estado ---
  // Aceita Partial<Product> pois a busca pode retornar DTOs de resumo
  filteredProducts = signal<Partial<Product>[]>([]);
  isLoading = signal(false);

  constructor() {
    // Reage a mudanças no channelId para limpar a busca se o canal mudar
    effect(() => {
      const id = this.channelId();
      // Se o canal mudar, poderíamos limpar o input ou refazer a busca.
      // Por enquanto, vamos manter simples: a próxima digitação usará o novo ID.
    });
  }

  ngOnInit(): void {
    this.searchControl().valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(value => typeof value === 'string'), // Ignora se o valor for um objeto (após seleção)
      tap(() => this.isLoading.set(true)),
      switchMap(value => {
        // Permite busca vazia se tiver canal selecionado (para listar tudo do canal)
        // Se não tiver canal, exige pelo menos 2 caracteres
        const currentChannelId = this.channelId();
        const minLength = currentChannelId ? 0 : 2;

        if (value === null || (typeof value === 'string' && value.length < minLength)) {
          return of([]);
        }

        // Passa o channelId para o serviço
        return this.productService.searchProducts(value || '', currentChannelId).pipe(
          catchError(() => of([]))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(products => {
      this.filteredProducts.set(products);
      this.isLoading.set(false);
    });
  }

  displayFn(product: Partial<Product>): string {
    return product && product.nome ? product.nome : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    // Fazemos o cast para Product porque sabemos que o ID e Nome (essenciais) estão lá.
    // O consumidor (SalesForm) deve lidar com o fato de que alguns campos podem faltar.
    const product = event.option.value as Product;
    this.productSelected.emit(product);
  }

  /**
   * Retorna a quantidade de estoque para o canal atual.
   */
  getStockForChannel(product: Partial<Product>): number | undefined {
    const currentChannelId = this.channelId();
    if (currentChannelId && product.estoquePorCanal) {
      return product.estoquePorCanal[currentChannelId];
    }
    return undefined;
  }
}
