import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

export interface StockHistoryItem {
  id: number;
  data: string;
  tipo: string;
  tipoDescricao: string;
  quantidade: number;
  motivo: string;
  produtoId: number;
  produtoNome: string;
  produtoSku: string;
  produtoNomeSnapshot: string;
  produtoSkuSnapshot: string;
  ordemProducaoId: number | null;
  vendaId: number | null;
}

export interface StockMovementTypeOption {
  name: string;
  descricao: string;
}

export interface EmbeddedStockMovementTypes {
  tiposMovimentacaoProduto: StockMovementTypeOption[];
}

export interface EmbeddedStockHistory {
  historicoEstoqueConsolidadoResponseDTOList: StockHistoryItem[];
}

export interface ApiResponseStockHistory extends Hateoas {
  _embedded?: EmbeddedStockHistory;
  page?: PageInfo;
}

export interface ApiResponseStockMovementTypes extends Hateoas {
  _embedded?: EmbeddedStockMovementTypes;
}
