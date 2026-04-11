import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

export type MaterialStockAnalysisPolicy = 'TODOS' | 'SEM_RETALHOS' | 'APENAS_RETALHOS';

export type MaterialStockAnalysisStatus = 'SEM_PARAMETRIZACAO' | 'CRITICO' | 'ATENCAO' | 'ACEITAVEL';

export interface MaterialStockAnalysisSummary {
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  unidadeDeConsumo: string;
  unidadeDescricao?: string;
  unidadeSimbolo?: string;
  tipoProdutoCompativel: 'CORTE' | 'CONSUMO' | string;
  saldoLotesPrincipais: number;
  saldoRetalhos: number;
  saldoTotal: number;
  saldoConsiderado: number;
  quantidadeLotesPrincipais: number;
  quantidadeRetalhos: number;
  estoqueCritico?: number | null;
  estoqueAceitavel?: number | null;
  percentualRisco?: number | null;
  statusAnalise: MaterialStockAnalysisStatus;
  _links?: Hateoas['_links'];
}

export interface EmbeddedMaterialStockAnalyses {
  'analises-estoque-materias-primas': MaterialStockAnalysisSummary[];
}

export interface ApiResponseMaterialStockAnalyses extends Hateoas {
  _embedded: EmbeddedMaterialStockAnalyses;
  page: PageInfo;
}
