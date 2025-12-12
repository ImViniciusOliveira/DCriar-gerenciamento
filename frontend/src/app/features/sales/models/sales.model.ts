import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";
import { Product } from '../../products/models/product.model';

/**
 * Representa uma Venda.
 * Contém informações sobre a transação, itens vendidos e canal de venda.
 */
export interface ItemVenda {
  produto: Product;
  quantidade: number;
  precoUnitario: number;
  precoTotal: number;
}

export interface Venda {
  id: number;
  dataVenda: string; // ISO Date string
  valorTotal: number;
  canalVenda: {
    id: number;
    nome: string;
  };
  itens: ItemVenda[];
  _links?: Hateoas['_links'];
}

/**
 * Payload para criação de uma nova Venda.
 */
export interface VendaRequest {
  canalVendaId: number;
  itens: {
    produtoId: number;
    quantidade: number;
  }[];
}

/**
 * Resposta paginada da API para a listagem de vendas.
 */
export interface ApiResponseVendas extends Hateoas {
  _embedded: {
    vendaModelList: Venda[];
  };
  page: PageInfo;
}
