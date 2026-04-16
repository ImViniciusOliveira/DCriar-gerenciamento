import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';
import { MaterialType } from '../../stock/models/material-type.model';
import { FieldLockMetadata } from '../../../shared/utils/field-locks';

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
 * dos tipos 'CORTE' e 'CONSUMO' e incluindo links HATEOAS.
 */
export interface Product extends FieldLockMetadata {
  id: number;
  tipoProduto: 'CORTE' | 'CONSUMO';
  nome: string;
  sku: string;
  descricao: string;
  unidadesPorProduto: number;
  unidadeCadastroConsumo?: string;
  ativo: boolean;
  estoqueFisicoTotal: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
  estoqueCritico?: number;
  precoComercial?: number;
  dataCriacao?: string;
  dataAtualizacao?: string;
  fotoPrincipalUrl: string;
  materiaPrima: MaterialType;
  _links?: Hateoas['_links'];

  /** Propriedades exclusivas para produtos do tipo CORTE. */
  cor?: string;
  dimensoes?: Dimensions;

  /** Propriedades exclusivas para produtos do tipo CONSUMO. */
  codigoFabricante?: string;
  especificacoes?: Specifications;

  /**
   * Dados de estoque por canal, injetados dinamicamente pelo ProductService.
   * Não vem diretamente do endpoint principal de produtos.
   */
  estoquePorCanal?: Record<string, number>;

  /**
   * Estoque disponível específico para um contexto (ex: canal selecionado).
   * Preenchido quando o produto vem de endpoints de resumo de estoque.
   */
  estoqueDisponivel?: number;

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
