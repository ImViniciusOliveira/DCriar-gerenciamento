// Fonte temporária de dados do dashboard.
// Quando a API real estiver pronta, remova este arquivo e substitua os computeds
// do DashboardPage pelas chamadas de serviço correspondentes.

export type DashboardSalesPeriod = '1d' | '7d' | '30d' | '90d' | '180d';

export interface DashboardPeriodOption {
  key: DashboardSalesPeriod;
  label: string;
}

export interface DashboardSalesSeriesPoint {
  label: string;
  helperLabel?: string;
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

export interface DashboardSalesSnapshot {
  revenueTotal: number;
  orderCount: number;
  leadingChannelName: string;
  leadingChannelHelper: string;
  comparisonDeltaPercent: number;
  trendLabel: string;
  series: DashboardSalesSeriesPoint[];
  topProducts: DashboardTopProduct[];
}

export interface DashboardAlertItem {
  id: number;
  name: string;
  currentAmount: number;
  minimumAmount: number;
  unitLabel: string;
  actionLabel: string;
  route: string;
}

export interface DashboardQuickAction {
  label: string;
  icon: string;
  route: string;
}

export const DASHBOARD_PERIOD_OPTIONS: DashboardPeriodOption[] = [
  { key: '1d', label: 'Hoje' },
  { key: '7d', label: '7 dias' },
  { key: '30d', label: '30 dias' },
  { key: '90d', label: '3 meses' },
  { key: '180d', label: '6 meses' }
];

const SALES_BY_PERIOD: Record<DashboardSalesPeriod, DashboardSalesSnapshot> = {
  '1d': {
    revenueTotal: 5860,
    orderCount: 14,
    leadingChannelName: 'Shopee',
    leadingChannelHelper: '6 pedidos hoje',
    comparisonDeltaPercent: 12.6,
    trendLabel: 'Receita por faixa do dia',
    series: [
      { label: 'Manhã', helperLabel: '00h às 08h', revenue: 1940, orderCount: 5 },
      { label: 'Tarde', helperLabel: '08h às 16h', revenue: 3100, orderCount: 6 },
      { label: 'Noite', helperLabel: '16h às 24h', revenue: 820, orderCount: 3 }
    ],
    topProducts: [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', revenue: 1420, unitsSold: 5 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', revenue: 1180, unitsSold: 11 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', revenue: 960, unitsSold: 3 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', revenue: 740, unitsSold: 7 },
      { productId: 17, name: 'Kit de Tags Premium', sku: 'KTP-001', revenue: 560, unitsSold: 3 }
    ]
  },
  '7d': {
    revenueTotal: 18420.9,
    orderCount: 46,
    leadingChannelName: 'Shopee',
    leadingChannelHelper: '17 pedidos no período',
    comparisonDeltaPercent: 12.8,
    trendLabel: 'Receita diária',
    series: [
      { label: '04 Abr', revenue: 1980, orderCount: 5 },
      { label: '05 Abr', revenue: 2140, orderCount: 6 },
      { label: '06 Abr', revenue: 2310, orderCount: 7 },
      { label: '07 Abr', revenue: 2560, orderCount: 6 },
      { label: '08 Abr', revenue: 2740, orderCount: 8 },
      { label: '09 Abr', revenue: 3010, orderCount: 7 },
      { label: '10 Abr', revenue: 3680.9, orderCount: 7 }
    ],
    topProducts: [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', revenue: 4860, unitsSold: 18 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', revenue: 3520, unitsSold: 32 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', revenue: 2980, unitsSold: 9 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', revenue: 2470, unitsSold: 21 },
      { productId: 17, name: 'Kit de Tags Premium', sku: 'KTP-001', revenue: 1990.9, unitsSold: 11 }
    ]
  },
  '30d': {
    revenueTotal: 76940.55,
    orderCount: 188,
    leadingChannelName: 'Shopee',
    leadingChannelHelper: '68 pedidos no período',
    comparisonDeltaPercent: 18.4,
    trendLabel: 'Receita por faixas do mês',
    series: [
      { label: 'Dias 1-7', revenue: 15640, orderCount: 39 },
      { label: '8-14', revenue: 18220, orderCount: 46 },
      { label: '15-21', revenue: 20140, orderCount: 49 },
      { label: '22-30/31', revenue: 22940.55, orderCount: 54 }
    ],
    topProducts: [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', revenue: 16840, unitsSold: 65 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', revenue: 13210, unitsSold: 118 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', revenue: 10890, unitsSold: 34 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', revenue: 9560, unitsSold: 77 },
      { productId: 5, name: 'Flyer Couchê 150g', sku: 'FLC-150', revenue: 7840.55, unitsSold: 52 }
    ]
  },
  '90d': {
    revenueTotal: 214580.3,
    orderCount: 534,
    leadingChannelName: 'Shopee',
    leadingChannelHelper: '196 pedidos no período',
    comparisonDeltaPercent: 9.7,
    trendLabel: 'Receita mensal',
    series: [
      { label: 'Jan', revenue: 64810.4, orderCount: 163 },
      { label: 'Fev', revenue: 70195.2, orderCount: 175 },
      { label: 'Mar', revenue: 79574.7, orderCount: 196 }
    ],
    topProducts: [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', revenue: 46820, unitsSold: 182 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', revenue: 35140, unitsSold: 312 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', revenue: 28190, unitsSold: 93 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', revenue: 24260, unitsSold: 188 },
      { productId: 17, name: 'Kit de Tags Premium', sku: 'KTP-001', revenue: 18370.3, unitsSold: 102 }
    ]
  },
  '180d': {
    revenueTotal: 431280.9,
    orderCount: 1064,
    leadingChannelName: 'Shopee',
    leadingChannelHelper: '382 pedidos no período',
    comparisonDeltaPercent: 16.3,
    trendLabel: 'Receita mensal',
    series: [
      { label: 'Nov', revenue: 58240.4, orderCount: 141 },
      { label: 'Dez', revenue: 61780.1, orderCount: 152 },
      { label: 'Jan', revenue: 64810.4, orderCount: 163 },
      { label: 'Fev', revenue: 70195.2, orderCount: 175 },
      { label: 'Mar', revenue: 79574.7, orderCount: 196 },
      { label: 'Abr', revenue: 96680.1, orderCount: 237 }
    ],
    topProducts: [
      { productId: 11, name: 'Cartão de Visita Premium', sku: 'CVP-300G', revenue: 92840, unitsSold: 362 },
      { productId: 22, name: 'Adesivo Redondo 5cm', sku: 'ARD-5CM', revenue: 71240, unitsSold: 624 },
      { productId: 14, name: 'Banner 120x80cm', sku: 'BNR-120X80', revenue: 56840, unitsSold: 186 },
      { productId: 8, name: 'Etiqueta Vinil Fosco', sku: 'EVF-A6', revenue: 49820, unitsSold: 377 },
      { productId: 17, name: 'Kit de Tags Premium', sku: 'KTP-001', revenue: 37460.9, unitsSold: 211 }
    ]
  }
};

export const DASHBOARD_MOCK = {
  sales: {
    byPeriod: SALES_BY_PERIOD
  },
  lowStockProducts: [
    { id: 11, name: 'Cartão de Visita Premium', currentAmount: 8, minimumAmount: 20, unitLabel: 'un', actionLabel: 'Adicionar saldo em produto', route: '/ordens-de-producao' },
    { id: 14, name: 'Banner 120x80cm', currentAmount: 3, minimumAmount: 12, unitLabel: 'un', actionLabel: 'Adicionar saldo em produto', route: '/ordens-de-producao' },
    { id: 5, name: 'Flyer Couchê 150g', currentAmount: 15, minimumAmount: 25, unitLabel: 'un', actionLabel: 'Adicionar saldo em produto', route: '/ordens-de-producao' },
    { id: 27, name: 'Etiqueta Kraft 4x4cm', currentAmount: 6, minimumAmount: 18, unitLabel: 'un', actionLabel: 'Adicionar saldo em produto', route: '/ordens-de-producao' }
  ] satisfies DashboardAlertItem[],
  criticalMaterials: [
    { id: 1, name: 'Lona Fosca 440g', currentAmount: 12, minimumAmount: 50, unitLabel: 'm²', actionLabel: 'Adicionar lote de matéria-prima', route: '/lotes-materia-prima' },
    { id: 2, name: 'Adesivo BOPP Transparente', currentAmount: 28, minimumAmount: 70, unitLabel: 'm²', actionLabel: 'Adicionar lote de matéria-prima', route: '/lotes-materia-prima' },
    { id: 3, name: 'PVC Expandido 2mm', currentAmount: 4, minimumAmount: 12, unitLabel: 'chapas', actionLabel: 'Adicionar lote de matéria-prima', route: '/lotes-materia-prima' },
    { id: 4, name: 'Papel Couchê 250g', currentAmount: 180, minimumAmount: 300, unitLabel: 'folhas', actionLabel: 'Adicionar lote de matéria-prima', route: '/lotes-materia-prima' }
  ] satisfies DashboardAlertItem[],
  quickActions: [
    {
      label: 'Nova venda',
      icon: 'point_of_sale',
      route: '/vendas'
    },
    {
      label: 'Matérias-primas',
      icon: 'category',
      route: '/tipos-materia-prima'
    },
    {
      label: 'Nova produção',
      icon: 'precision_manufacturing',
      route: '/ordens-de-producao'
    },
    {
      label: 'Ajustes',
      icon: 'tune',
      route: '/estoques'
    }
  ] satisfies DashboardQuickAction[]
} as const;
