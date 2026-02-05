import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

/**
 * Representa um Item de Venda conforme retornado pela API.
 * Os dados do produto vêm "achatados" no DTO, não como um objeto aninhado.
 */
export interface SaleItem {
  id: number;
  produtoId: number;
  produtoSku: string;
  nomeProduto: string;
  quantidade: number;
  precoUnitario: number;
  precoTotal: number;
}

/**
 * Representa uma Venda conforme retornado pela API.
 */
export interface Sale {
  id: number;
  dataCriacao: string; // ISO Date string
  dataAtualizacao: string; // ISO Date string
  valorTotal: number;
  nomeCanalVenda: string; // O backend retorna apenas o nome, não o objeto completo
  itens: SaleItem[];
  _links?: Hateoas['_links'];
}

/**
 * Payload para criação de uma nova Venda.
 */
export interface SaleRequest {
  canalVendaId: number;
  itens: {
    produtoId: number;
    quantidade: number;
  }[];
}

/**
 * Resposta paginada da API para a listagem de vendas.
 */
export interface ApiResponseSales extends Hateoas {
  _embedded: {
    vendas: Sale[]; // Corrigido para corresponder ao @Relation(collectionRelation = "vendas") do backend
  };
  page: PageInfo;
}
