import { Hateoas } from '../../../core/models/hateoas.model';

// Renomeado de MaterialType para maior clareza, baseado na resposta da API.
export interface MateriaPrima {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links']; // Adicionado para compatibilidade com MaterialType
}

export interface Dimensoes {
  larguraCm: number;
  comprimentoCm: number;
}

export type Especificacoes = { [key: string]: string };

export interface Product {
  id: number;
  tipoProduto: 'CORTE' | 'CONSUMO_DIRETO';
  nome: string;
  sku: string;
  descricao: string;
  unidadesPorProduto: number;
  ativo: boolean;
  estoqueFisicoTotal: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
  fotoPrincipalUrl: string;
  materiaPrima: MateriaPrima;
  _links?: Hateoas['_links'];

  // Atributos específicos de ProdutoDeCorte (opcionais)
  cor?: string;
  dimensoes?: Dimensoes;

  // Atributos específicos de ProdutoDeConsumoDireto (opcionais)
  codigoFabricante?: string;
  especificacoes?: Especificacoes;
}

export interface EmbeddedProducts {
  produtos: Product[];
}

export interface ApiResponseProducts extends Hateoas {
  _embedded: EmbeddedProducts;
  page?: PageInfo;
}

export interface PageInfo {
  size: number;
  totalElements: number;
  totalPages: number;
  number: number;
}
