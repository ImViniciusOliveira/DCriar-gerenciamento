import { Hateoas } from '../../../core/models/hateoas.model';

export interface StockConsultationSummary {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
  canalVendaId: number;
  nomeCanalVenda: string;
  quantidadeNoCanal: number;
  estoqueFisicoTotal: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
  statusDivergencia?: 'CONSISTENTE' | 'INCONSISTENTE' | string;
  camposBloqueados?: string[];
  motivosBloqueio?: Record<string, string>;
  _links?: Hateoas['_links'];
}

export interface ApiResponseStockConsultations {
  _embedded?: {
    consultasEstoqueCanal?: StockConsultationSummary[];
  };
  page?: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
}
