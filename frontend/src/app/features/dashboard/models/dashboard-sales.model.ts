import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

export type DashboardSalesPeriod = '1d' | '7d' | '30d' | '90d' | '180d';

export interface DashboardPeriodOption {
  key: DashboardSalesPeriod;
  label: string;
}

export interface DashboardDateRange {
  dataInicio: string;
  dataFim: string;
}

export interface DashboardSalesSeriesPoint {
  label: string;
  helperLabel?: string | null;
  revenue: number;
  orderCount: number;
}

export interface DashboardTopProduct {
  productId: number;
  name: string;
  sku: string;
  revenue: number;
  unitsSold: number;
}

export interface DashboardSalesSummary {
  revenueTotal: number;
  orderCount: number;
  revenuePeriodAnterior: number;
  leadingChannelName: string;
  leadingChannelHelper: string;
  comparisonDeltaPercent: number | null;
  trendLabel: string;
  series: DashboardSalesSeriesPoint[];
  topProducts: DashboardTopProduct[];
}

export interface SalesAnalysisTotalsResponse extends Hateoas {
  receita: number | null;
  totalPedidos: number | null;
  receitaPeriodoAnterior?: number | null;
  deltaPercent?: number | null;
}

export interface SalesAnalysisChannelItem extends Hateoas {
  canalVendaId: number;
  nomeCanal: string;
  receita: number | null;
  totalPedidos: number | null;
}

export interface SalesAnalysisProductItem extends Hateoas {
  produtoId: number;
  nomeProduto: string;
  skuProduto: string;
  receita: number | null;
  unidadesVendidas: number | null;
}

export interface SalesAnalysisSeriesItem {
  label: string;
  helperLabel?: string | null;
  receita: number | null;
  totalPedidos: number | null;
}

export interface SalesAnalysisByChannelResponse extends Hateoas {
  _embedded?: {
    'vendas-analise-por-canal'?: SalesAnalysisChannelItem[];
  };
  page?: PageInfo;
}

export interface SalesAnalysisByProductResponse extends Hateoas {
  _embedded?: {
    'vendas-analise-por-produto'?: SalesAnalysisProductItem[];
  };
  page?: PageInfo;
}

export interface SalesAnalysisSeriesResponse extends Hateoas {
  trendLabel: string;
  serie: SalesAnalysisSeriesItem[];
}

export const DASHBOARD_PERIOD_OPTIONS: DashboardPeriodOption[] = [
  { key: '1d', label: 'Hoje' },
  { key: '7d', label: '7 dias' },
  { key: '30d', label: '30 dias' },
  { key: '90d', label: '3 meses' },
  { key: '180d', label: '6 meses' }
];

export const EMPTY_DASHBOARD_SALES_SUMMARY: DashboardSalesSummary = {
  revenueTotal: 0,
  orderCount: 0,
  revenuePeriodAnterior: 0,
  leadingChannelName: 'Sem vendas',
  leadingChannelHelper: 'Nenhuma venda no período selecionado.',
  comparisonDeltaPercent: null,
  trendLabel: 'Receita do período',
  series: [],
  topProducts: []
};
