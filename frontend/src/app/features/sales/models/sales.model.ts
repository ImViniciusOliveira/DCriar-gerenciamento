import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";
import { Product } from '../../products/models/product.model';

/**
 * Representa um Item de Venda.
 */
export interface SaleItem {
  produto: Product;
  quantidade: number;
  precoUnitario: number;
  precoTotal: number;
}

/**
 * Representa uma Venda.
 * Contém informações sobre a transação, itens vendidos e canal de venda.
 */
export interface Sale {
  id: number;
  dataVenda: string; // ISO Date string
  valorTotal: number;
  canalVenda: {
    id: number;
    nome: string;
  };
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
    vendaModelList: Sale[];
  };
  page: PageInfo;
}
