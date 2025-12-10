import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

/**
 * Representa a matéria-prima base de um produto.
 */
export interface MateriaPrima {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links'];
}

/**
 * Dimensões físicas aplicáveis a produtos de corte.
 */
export interface Dimensoes {
  larguraCm: number;
  comprimentoCm: number;
}

/**
 * Mapa dinâmico de especificações técnicas.
 */
export type Especificacoes = Record<string, string>;

/**
 * Modelo principal de Produto.
 * Unifica propriedades de produtos de Corte e Consumo Direto.
 */
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

  /** Propriedades exclusivas para produtos do tipo CORTE */
  cor?: string;
  dimensoes?: Dimensoes;

  /** Propriedades exclusivas para produtos do tipo CONSUMO_DIRETO */
  codigoFabricante?: string;
  especificacoes?: Especificacoes;

  /**
   * Dados de estoque por canal, injetados dinamicamente pelo ProductService.
   * Não vem diretamente do endpoint principal de produtos.
   */
  estoquePorCanal?: Record<string, number>;
}

/**
 * Estrutura da lista de produtos embutida na resposta da API.
 */
export interface EmbeddedProducts {
  produtos: Product[];
}

/**
 * Resposta paginada da API de produtos.
 */
export interface ApiResponseProducts extends Hateoas {
  _embedded: EmbeddedProducts;
  page?: PageInfo;
}
