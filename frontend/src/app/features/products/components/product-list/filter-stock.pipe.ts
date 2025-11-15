import { Pipe, PipeTransform } from '@angular/core';
import { KeyValue } from '@angular/common';

@Pipe({
  name: 'filterStock',
  standalone: true,
})
export class FilterStockPipe implements PipeTransform {
  transform(
    items: KeyValue<string, number>[] | null,
    threshold: number
  ): KeyValue<string, number>[] {
    if (!items) return [];
    return items.filter(item => item.value > threshold);
  }
}
