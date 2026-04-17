import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    title: 'D-Criar | Dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard-page/dashboard-page').then(
        (m) => m.DashboardPage
      ),
  },
  {
    path: 'teste-dashboard',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'produtos',
    title: 'D-Criar | Produtos',
    loadComponent: () =>
      import('./features/products/components/product-list/product-list').then(
        (m) => m.ProductList
      ),
  },
  {
    path: 'tipos-materia-prima',
    title: 'D-Criar | Tipos de Matéria-Prima',
    loadComponent: () =>
      import(
        './features/stock/components/material-type-list/material-type-list'
      ).then((m) => m.MaterialTypeList),
  },
  {
    path: 'lotes-materia-prima',
    title: 'D-Criar | Lotes de Matéria-Prima',
    loadComponent: () =>
      import('./features/stock/components/batch-list/batch-list').then(
        (m) => m.BatchList
      ),
  },
  {
    path: 'ordens-de-producao',
    title: 'D-Criar | Ordens de Produção',
    loadComponent: () =>
      import(
        './features/production/components/production-list/production-list'
      ).then((m) => m.ProductionList),
  },
  {
    path: 'vendas',
    title: 'D-Criar | Vendas',
    loadComponent: () =>
      import('./features/sales/components/sales-list/sales-list').then(
        (m) => m.SalesList
      ),
  },
  {
    path: 'estoques',
    title: 'D-Criar | Estoque',
    loadComponent: () =>
      import('./features/stock/components/stock-home/stock-home').then(
        (m) => m.StockHome
      ),
  },
  // Rota curinga: redireciona qualquer URL não encontrada para o dashboard.
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
