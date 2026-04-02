import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

export type BatchAdjustmentOperation = 'AJUSTE' | 'PERDA_DESCARTE';

export type BatchAdjustmentDirection = 'ADICIONAR' | 'RETIRAR';

export interface BatchAdjustmentCalculateRequest {
  tipoOperacao: BatchAdjustmentOperation;
  direcao?: BatchAdjustmentDirection | null;
  quantidade: number;
  motivo: string;
}

export interface BatchAdjustmentImpactItem {
  id: number;
  tipoItem: string;
  descricao: string;
  saldoAtual: number;
  saldoDescricao: string;
  dimensaoDescricao?: string | null;
  valorAtual: number;
  valorProjetado: number;
  selecionadoPorPadrao: boolean;
}

export interface BatchAdjustmentCalculateResponse {
  loteId: number;
  tipoOperacao: BatchAdjustmentOperation;
  tipoOperacaoDescricao: string;
  direcao?: BatchAdjustmentDirection | null;
  direcaoDescricao?: string | null;
  unidadeApresentacao: string;
  unidadeSimbolo: string;
  saldoAtual: number;
  saldoProjetado: number;
  valorAtualLote: number;
  valorProjetadoLote: number;
  custoUnitarioAtual: number;
  custoUnitarioProjetado: number;
  tipoMovimentacaoGerada: string;
  quantidadeMovimentacaoGerada: number;
  itensImpactados: BatchAdjustmentImpactItem[];
}

export interface BatchAdjustmentApplyRequest extends BatchAdjustmentCalculateRequest {
  idsItensImpactadosAtualizados?: number[];
}

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
  valorAtualLote?: number;
  custoUnitarioAtual?: number;
  atributos?: { [key: string]: any };
  loteDeOrigemId?: number;
  motivo?: string;
  dataCriacao?: string;
  dataAtualizacao?: string;
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
