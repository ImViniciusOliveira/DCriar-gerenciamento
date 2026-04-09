import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

export interface AdjustmentLotSummary {
  loteId: number;
  identificadorPublico: string;
  identificadorOrigemPublico?: string | null;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  tipoEstrutural: 'LOTE_PRINCIPAL' | 'RETALHO' | string;
  saldoEstoque: number;
  unidadeSimbolo?: string;
  valorAtualLote: number;
  custoUnitarioAtual: number;
}

export interface AdjustmentProductSummary {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
  estoqueFisicoTotal: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
}

export interface AdjustmentChannelSummary {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
  canalVendaId: number;
  nomeCanalVenda: string;
  quantidadeNoCanal: number;
  estoqueFisicoTotal: number;
  estoqueDistribuidoTotal: number;
  estoqueDisponivelParaAlocar: number;
}

interface EmbeddedAdjustmentLots {
  ajusteLoteResumoDTOList: AdjustmentLotSummary[];
}

interface EmbeddedAdjustmentProducts {
  ajusteEstoqueProdutoResumoDTOList: AdjustmentProductSummary[];
}

interface EmbeddedAdjustmentChannels {
  ajusteEstoqueCanalResumoDTOList: AdjustmentChannelSummary[];
}

export interface ApiResponseAdjustmentLots extends Hateoas {
  _embedded: EmbeddedAdjustmentLots;
  page: PageInfo;
}

export interface ApiResponseAdjustmentProducts extends Hateoas {
  _embedded: EmbeddedAdjustmentProducts;
  page: PageInfo;
}

export interface ApiResponseAdjustmentChannels extends Hateoas {
  _embedded: EmbeddedAdjustmentChannels;
  page: PageInfo;
}
