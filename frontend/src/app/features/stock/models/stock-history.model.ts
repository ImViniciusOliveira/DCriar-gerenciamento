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
}

export interface EmbeddedStockHistory {
  historicoEstoqueConsolidadoResponseDTOList: StockHistoryItem[];
}

export interface ApiResponseStockHistory extends Hateoas {
  _embedded?: EmbeddedStockHistory;
  page?: PageInfo;
}
