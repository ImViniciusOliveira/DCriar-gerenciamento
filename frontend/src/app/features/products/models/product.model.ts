import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';
import { MaterialType } from '../../stock/models/material-type.model';

/**
 * Representa as dimensões físicas de um produto, aplicável a produtos de corte.
 */
export interface Dimensions {
  larguraCm: number;
  comprimentoCm: number;
}

/**
 * Representa o mapa de especificações técnicas de um produto, como um objeto chave-valor.
 */
export type Specifications = Record<string, string>;

/**
 * Representa a entidade Produto no sistema.
 * Mapeia a estrutura de dados retornada pela API, unificando as propriedades
 * dos tipos 'CORTE' e 'CONSUMO_DIRETO' e incluindo links HATEOAS.
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

  /** Propriedades exclusivas para produtos do tipo CORTE. */
  cor?: string;
  dimensoes?: Dimensions;

  /** Propriedades exclusivas para produtos do tipo CONSUMO_DIRETO. */
  codigoFabricante?: string;
  especificacoes?: Specifications;

  /**
   * Dados de estoque por canal, injetados dinamicamente pelo ProductService.
   * Não vem diretamente do endpoint principal de produtos.
   */
  estoquePorCanal?: Record<string, number>;
}

/**
 * Estrutura aninhada `_embedded` específica para a lista de Produtos.
 */
export interface EmbeddedProducts {
  produtos: Product[];
}

/**
 * Estrutura de resposta padrão da API para listagens paginadas de Produtos.
 * Contém os dados em `_embedded`, links de navegação e metadados de paginação.
 */
export interface ApiResponseProducts extends Hateoas {
  _embedded: EmbeddedProducts;
  page?: PageInfo;
}
