import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

/**
 * Representa a entidade Lote de Matéria-Prima no sistema.
 * Mapeia a estrutura de dados retornada pela API, incluindo links HATEOAS para navegação.
 */
export interface Batch {
  id: string | number;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  unidadeDeEstoque?: string;
  unidadeCadastroEstoque?: string;
  unidadeDescricao?: string;
  unidadeSimbolo?: string;
  saldoEstoque?: number;
  custoTotalLote?: number;
  atributos?: { [key: string]: any };
  motivo?: string;
  _links?: Hateoas['_links'];
}

/**
 * Objeto de transferência de dados (DTO) utilizado para criar ou atualizar um Lote de Matéria-Prima.
 * Contém apenas os dados mutáveis necessários para a operação.
 */
export interface BatchRequest {
  tipoMateriaPrimaId: number;
  unidadeDeEstoque: string;
  unidadeCadastroEstoque?: string;
  quantidadeInicial: number;
  custoTotalLote: number;
  atributos?: { [key: string]: any };
  motivo: string;
}

/**
 * Estrutura aninhada `_embedded` específica para a lista de Lotes de Matéria-Prima.
 */
export interface EmbeddedBatches {
  'lotes-materia-prima': Batch[];
}

/**
 * Estrutura de resposta padrão da API para listagens paginadas de Lotes de Matéria-Prima.
 * Contém os dados em `_embedded`, links de navegação e metadados de paginação.
 */
export interface ApiResponseBatches extends Hateoas {
  _embedded: EmbeddedBatches;
  page: PageInfo;
}
