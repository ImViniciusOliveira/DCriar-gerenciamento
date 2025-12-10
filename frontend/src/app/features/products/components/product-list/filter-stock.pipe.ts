import { Pipe, PipeTransform } from '@angular/core';
import { KeyValue } from '@angular/common';

@Pipe({
  name: 'filterStock',
  standalone: true,
})
export class FilterStockPipe implements PipeTransform {
  /**
   * Filtra os canais de estoque para exibir apenas aqueles com quantidade acima do limite.
   *
   * @param items Lista de pares chave/valor gerada pelo pipe `keyvalue`.
   * @param threshold Valor mínimo para exibição (ex: 0 para ocultar itens sem estoque).
   * @returns Lista filtrada e tipada corretamente para o template.
   */
  transform(
    items: ReadonlyArray<KeyValue<unknown, any>> | null,
    threshold: number
  ): KeyValue<string, number>[] {
    if (!items) {
      return [];
    }

    // Filtra os itens convertendo o valor para número e força a tipagem de retorno
    // para que o template reconheça como pares de string/number.
    return items.filter(item => (item.value as number) > threshold) as KeyValue<string, number>[];
  }
}
