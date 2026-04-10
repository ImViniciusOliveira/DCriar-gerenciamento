import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";
import { FieldLockMetadata } from "../../../shared/utils/field-locks";

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
  identificadorPublico?: string;
  identificadorOrigemPublico?: string | null;
  nivelArvore?: number;
  cadeiaPublica?: string | null;
  cadeiaIdentificadoresPublicos?: string[];
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
  contextoItensImpactados?: 'COM_ITENS_IMPACTADOS' | 'PERDA_NAO_RECALCULA_DERIVADOS' | 'MATERIA_PRIMA_NAO_GERA_RETALHO' | 'SEM_RETALHOS_VINCULADOS' | 'SEM_RETALHOS_COM_SALDO';
  itensImpactados: BatchAdjustmentImpactItem[];
}

export interface BatchAdjustmentApplyRequest extends BatchAdjustmentCalculateRequest {
  idsItensImpactadosAtualizados?: number[];
}

export interface BatchMovement {
  id: number;
  data: string;
  tipo: string;
  quantidade: number;
  motivo?: string;
  _links?: Hateoas['_links'];
}

export interface EmbeddedBatchMovements {
  movimentacoes: BatchMovement[];
}

export interface ApiResponseBatchMovements extends Hateoas {
  _embedded: EmbeddedBatchMovements;
  totalMovimentacoes?: number;
}

/**
 * Representa a entidade Lote de Matéria-Prima no sistema.
 * Mapeia a estrutura de dados retornada pela API, incluindo links HATEOAS para navegação.
 */
export interface Batch extends FieldLockMetadata {
  id: string | number;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  unidadeDeEstoque?: string;
  unidadeCadastroEstoque?: string;
  unidadeDescricao?: string;
  unidadeSimbolo?: string;
  saldoEstoque?: number;
  saldoInternoAtual?: number;
  custoTotalLote?: number;
  valorAtualLote?: number;
  custoUnitarioAtual?: number;
  atributos?: { [key: string]: any };
  loteDeOrigemId?: number;
  identificadorPublico?: string;
  identificadorOrigemPublico?: string | null;
  tipoEstrutural?: 'LOTE_PRINCIPAL' | 'RETALHO' | string;
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
