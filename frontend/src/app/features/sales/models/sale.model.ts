import { Hateoas } from "../../../core/models/hateoas.model";

export interface SaleItem {
  id: number;
  produtoId: number;
  produtoSku: string;
  nomeProduto: string;
  quantidade: number;
  precoUnitario: number;
  precoTotal: number;
  _links?: Hateoas['_links'];
}

export interface Sale {
  id: number;
  nomeCanalVenda: string;
  valorTotal: number;
  itens: SaleItem[];
  dataCriacao: string;
  dataAtualizacao: string;
  _links?: Hateoas['_links'];
}
