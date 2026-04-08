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
  precoComercialOriginal: number;
  precoUnitario: number;
  precoTotal: number;
  tipoPrecoAplicado: 'PRECO_PADRAO' | 'PRECO_ALTERADO' | 'DESCONTO_TOTAL';
  motivoAlteracaoPreco?: string | null;
}

/**
 * Representa uma Venda conforme retornado pela API.
 */
export interface Sale {
  id: number;
  dataCriacao: string; // ISO Date string
  dataAtualizacao: string; // ISO Date string
  valorTotal: number;
  canalVendaId: number;
  nomeCanalVenda: string;
  nomeCompleto?: string | null;
  pais?: string | null;
  apelido?: string | null;
  endereco?: string | null;
  numero?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  estado?: string | null;
  cep?: string | null;
  cpf?: string | null;
  observacao?: string | null;
  modoLocalidade?: 'BRASIL' | 'LIVRE' | null;
  itens: SaleItem[];
  _links?: Hateoas['_links'];
}

export interface SaleBrazilStateOption {
  uf: string;
  nome: string;
}

export interface SaleLocationConfig {
  pais: string;
  modoLocalidade: 'BRASIL' | 'LIVRE';
  estadosBrasil: SaleBrazilStateOption[];
}

/**
 * Payload para criação de uma nova Venda.
 */
export interface SaleRequest {
  canalVendaId: number;
  nomeCompleto?: string | null;
  pais?: string | null;
  apelido?: string | null;
  endereco?: string | null;
  numero?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  estado?: string | null;
  cep?: string | null;
  cpf?: string | null;
  observacao?: string | null;
  itens: {
    produtoId: number;
    quantidade: number;
    precoAplicado: number;
    precoTotal: number;
    tipoPrecoAplicado: 'PRECO_PADRAO' | 'PRECO_ALTERADO' | 'DESCONTO_TOTAL';
    motivoAlteracaoPreco?: string | null;
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
