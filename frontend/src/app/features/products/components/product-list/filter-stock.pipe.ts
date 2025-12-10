import { Pipe, PipeTransform } from '@angular/core';
import { KeyValue } from '@angular/common';

@Pipe({
  name: 'filterStock',
  standalone: true,
})
export class FilterStockPipe implements PipeTransform {
  /**
   * Filtra uma lista de KeyValue pairs (geralmente de um objeto de estoque)
   * para retornar apenas os itens cujo valor (quantidade) é maior que um limite.
   * @param items A lista de KeyValue, geralmente vinda do pipe `keyvalue`.
   * @param threshold O valor mínimo (não inclusivo) para o estoque ser exibido.
   * @returns Uma nova lista contendo apenas os itens que passaram no filtro.
   */
  transform(
    items: ReadonlyArray<KeyValue<unknown, any>> | null,
    threshold: number
  ): any[] {
    if (!items) {
      return [];
    }
    // A lógica de filtro permanece a mesma.
    // O TypeScript agora aceita a entrada genérica do keyvalue.
    return items.filter(item => (item.value as number) > threshold);
  }
}
