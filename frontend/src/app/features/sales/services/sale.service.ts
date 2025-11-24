import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Sale } from '../models/sale.model';

// Dados mocados realistas
const MOCK_SALES: Sale[] = [
  {
    "id": 1,
    "nomeCanalVenda": "Site Próprio",
    "valorTotal": 99.9,
    "itens": [
      { "id": 1, "produtoId": 1, "produtoSku": "CV-PREM-9X5", "nomeProduto": "Cartão de Visita Premium", "quantidade": 1, "precoUnitario": 99.9, "precoTotal": 99.9, "_links": { "produto": { "href": "http://localhost:8080/api/v1/produtos/1" } } }
    ],
    "dataCriacao": "2025-11-20T22:49:48.696937",
    "dataAtualizacao": "2025-11-20T22:49:48.696937",
    "_links": { "self": { "href": "http://localhost:8080/api/v1/vendas/1" }, "vendas": { "href": "http://localhost:8080/api/v1/vendas" } }
  },
  {
    "id": 2,
    "nomeCanalVenda": "Equipe de Vendas",
    "valorTotal": 170,
    "itens": [
      { "id": 2, "produtoId": 2, "produtoSku": "BNR-COM-120X80", "nomeProduto": "Banner Comercial 1,20x0,80m", "quantidade": 2, "precoUnitario": 85, "precoTotal": 170, "_links": { "produto": { "href": "http://localhost:8080/api/v1/produtos/2" } } }
    ],
    "dataCriacao": "2025-11-21T10:49:48.696937",
    "dataAtualizacao": "2025-11-21T10:49:48.696937",
    "_links": { "self": { "href": "http://localhost:8080/api/v1/vendas/2" }, "vendas": { "href": "http://localhost:8080/api/v1/vendas" } }
  },
  {
    "id": 3,
    "nomeCanalVenda": "Shopee",
    "valorTotal": 75,
    "itens": [
      { "id": 3, "produtoId": 3, "produtoSku": "ADSV-RD-5", "nomeProduto": "Adesivo Redondo 5cm", "quantidade": 1, "precoUnitario": 45, "precoTotal": 45, "_links": { "produto": { "href": "http://localhost:8080/api/v1/produtos/3" } } },
      { "id": 4, "produtoId": 7, "produtoSku": "TAG-KFT-4X9", "nomeProduto": "Tag para Roupas Kraft", "quantidade": 1, "precoUnitario": 30, "precoTotal": 30, "_links": { "produto": { "href": "http://localhost:8080/api/v1/produtos/7" } } }
    ],
    "dataCriacao": "2025-11-21T22:49:48.696937",
    "dataAtualizacao": "2025-11-21T22:49:48.696937",
    "_links": { "self": { "href": "http://localhost:8080/api/v1/vendas/3" }, "vendas": { "href": "http://localhost:8080/api/v1/vendas" } }
  }
];

@Injectable({
  providedIn: 'root'
})
export class SaleService {

  getSales(page: number, size: number, sort: string): Observable<any> {
    const [sortKey, sortDirection] = sort.split(',');
    const sortedData = [...MOCK_SALES].sort((a, b) => {
      const valA = a[sortKey as keyof Sale];
      const valB = b[sortKey as keyof Sale];

      if (valA == null) return 1;
      if (valB == null) return -1;

      if (valA < valB) return sortDirection === 'asc' ? -1 : 1;
      if (valA > valB) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    const startIndex = page * size;
    const pageData = sortedData.slice(startIndex, startIndex + size);

    const response = {
      _embedded: {
        vendas: pageData // Chave alterada para "vendas"
      },
      page: {
        size: size,
        totalElements: MOCK_SALES.length,
        totalPages: Math.ceil(MOCK_SALES.length / size),
        number: page
      }
    };

    return of(response).pipe(delay(500));
  }

  deleteSale(url: string): Observable<void> {
    const id = parseInt(url.split('/').pop() || '0', 10);
    const index = MOCK_SALES.findIndex(s => s.id === id);
    if (index > -1) {
      MOCK_SALES.splice(index, 1);
    }
    return of(undefined).pipe(delay(300));
  }
}
