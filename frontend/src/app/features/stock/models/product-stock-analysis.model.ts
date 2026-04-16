import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

export type ProductStockAnalysisStatus = 'SEM_PARAMETRIZACAO' | 'CRITICO' | 'ACEITAVEL';

export interface ProductStockAnalysisSummary {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
  tipoProduto: 'CORTE' | 'CONSUMO' | string;
  estoqueFisicoTotal: number;
  saldoConsiderado: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
  estoqueCritico?: number | null;
  percentualRisco?: number | null;
  statusAnalise: ProductStockAnalysisStatus;
  _links?: Hateoas['_links'];
}

export interface EmbeddedProductStockAnalyses {
  'analises-estoque-produtos': ProductStockAnalysisSummary[];
}

export interface ApiResponseProductStockAnalyses extends Hateoas {
  _embedded: EmbeddedProductStockAnalyses;
  page: PageInfo;
}
