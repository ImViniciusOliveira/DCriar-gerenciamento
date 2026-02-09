import { Component, inject, input, output, OnInit, signal, DestroyRef, effect } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, tap, catchError, filter, map } from 'rxjs/operators';
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

  /**
   * Mapa de ajustes de estoque (ID Produto -> Quantidade a adicionar).
   * Usado na edição para considerar o estoque que já pertence à venda como disponível.
   */
  stockAdjustments = input<Map<number, number>>(new Map());

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
      filter(value => typeof value === 'string'),
      tap(() => this.isLoading.set(true)),
      switchMap(value => {
        // Permite busca vazia se tiver canal selecionado (para listar tudo do canal)
        // Se não tiver canal, exige pelo menos 2 caracteres
        const currentChannelId = this.channelId();
        const minLength = currentChannelId ? 0 : 2;

        if (value === null || (typeof value === 'string' && value.length < minLength)) {
          return of([]);
        }

        // Busca SEMPRE incluindo zerados (true), pois vamos filtrar no frontend
        // baseando-se no estoque físico + ajuste virtual.
        return this.productService.searchProducts(value || '', currentChannelId, true).pipe(
          map(products => this.processProductsStock(products)),
          catchError(() => of([]))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(products => {
      this.filteredProducts.set(products);
      this.isLoading.set(false);
    });
  }

  /**
   * Processa os produtos retornados:
   * 1. Calcula o estoque efetivo (Físico + Ajuste).
   * 2. Filtra produtos que continuam zerados mesmo com o ajuste.
   */
  private processProductsStock(products: Partial<Product>[]): Partial<Product>[] {
    const adjustments = this.stockAdjustments();
    const currentChannelId = this.channelId();

    return products
      .map(product => {
        // Obtém estoque físico do canal
        let physicalStock = 0;
        if (currentChannelId && product.estoquePorCanal) {
          physicalStock = product.estoquePorCanal[currentChannelId] || 0;
        }

        // Obtém ajuste (quantidade original da venda)
        const adjustment = adjustments.get(product.id!) || 0;

        // Calcula estoque efetivo
        const effectiveStock = physicalStock + adjustment;

        // Atualiza o objeto produto para refletir esse estoque "virtual" no display
        // Clonamos para não mutar o original se houver cache
        const newProduct = { ...product };
        if (currentChannelId) {
          newProduct.estoquePorCanal = {
            ...product.estoquePorCanal,
            [currentChannelId]: effectiveStock
          };
          // Atualiza também a propriedade de conveniência se estiver sendo usada
          newProduct.estoqueDisponivel = effectiveStock;
        }

        return newProduct;
      })
      .filter(product => {
        // Filtra: Mostra apenas se tiver estoque efetivo > 0
        // Se não tiver canal selecionado, mostra tudo (comportamento padrão de busca global)
        if (!currentChannelId) return true;
        return (product.estoqueDisponivel || 0) > 0;
      });
  }

  displayFn(product: Partial<Product>): string {
    return product && product.nome ? product.nome : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const product = event.option.value as Product;
    this.productSelected.emit(product);
  }

  getStockForChannel(product: Partial<Product>): number | undefined {
    const currentChannelId = this.channelId();
    if (currentChannelId && product.estoquePorCanal) {
      return product.estoquePorCanal[currentChannelId];
    }
    return undefined;
  }
}
