import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';
import { MaterialType } from '../../stock/models/material-type.model';

/**
 * Dimensões físicas aplicáveis a produtos de corte.
 */
export interface Dimensions {
  larguraCm: number;
  comprimentoCm: number;
}

/**
 * Mapa dinâmico de especificações técnicas.
 */
export type Specifications = Record<string, string>;

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
  materiaPrima: MaterialType;
  _links?: Hateoas['_links'];

  /** Propriedades exclusivas para produtos do tipo CORTE */
  cor?: string;
  dimensoes?: Dimensions;

  /** Propriedades exclusivas para produtos do tipo CONSUMO_DIRETO */
  codigoFabricante?: string;
  especificacoes?: Specifications;

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
