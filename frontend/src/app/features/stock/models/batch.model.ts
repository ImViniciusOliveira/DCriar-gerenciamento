import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

/**
 * Representa um Lote de Matéria-Prima.
 * Contém informações sobre o estoque, custos e atributos específicos do lote.
 */
export interface Batch {
  id: string | number;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  unidadeDeEstoque?: string;
  saldoEstoque?: number;
  custoTotalLote?: number;
  atributos?: { [key: string]: any };
  motivo?: string;
  _links?: Hateoas['_links'];
}

/**
 * Payload para criação ou atualização de um Lote de Matéria-Prima.
 */
export interface BatchRequest {
  tipoMateriaPrimaId: number;
  unidadeDeEstoque: string;
  quantidadeInicial: number;
  custoTotalLote: number;
  atributos?: { [key: string]: any };
  motivo: string;
}

/**
 * Resposta paginada da API para a listagem de lotes.
 */
export interface EmbeddedBatches {
  'lotes-materia-prima': Batch[];
}

/**
 * Representa a resposta completa da API para uma pesquisa paginada de lotes.
 */
export interface ApiResponseBatches extends Hateoas {
  _embedded: EmbeddedBatches;
  page: PageInfo;
}
