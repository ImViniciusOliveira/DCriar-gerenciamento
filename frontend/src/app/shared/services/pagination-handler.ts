import { Injectable, signal, computed } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';

@Injectable({
  providedIn: 'root'
})
export class PaginationHandler {
  // Estado da Paginação
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly totalElements = signal(0);

  // Estado da Ordenação
  readonly sortActive = signal('id'); // Valor padrão genérico
  readonly sortDirection = signal<Sort['direction']>('asc');

  // Propriedade Computada para a API
  readonly sortString = computed(() => {
    const active = this.sortActive();
    const direction = this.sortDirection();
    return direction ? `${active},${direction}` : active;
  });

  handlePageEvent(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
  }

  handleSortChange(sort: Sort): void {
    this.sortActive.set(sort.direction ? sort.active : 'id');
    this.sortDirection.set(sort.direction || 'asc');
    this.pageIndex.set(0); // Volta para a primeira página ao reordenar
  }

  updateTotalElements(total: number): void {
    this.totalElements.set(total);
  }

  reset(): void {
    this.pageIndex.set(0);
    this.totalElements.set(0);
  }
}
