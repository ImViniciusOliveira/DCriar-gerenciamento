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
    path: 'produtos',
    title: 'D-Criar | Produtos',
    loadComponent: () =>
      import('./features/products/components/product-list/product-list').then(
        (m) => m.ProductList
      ),
  },
  {
    path: 'vendas',
    title: 'D-Criar | Vendas',
    loadComponent: () =>
      import('./features/sales/components/sale-list/sale-list').then(
        (m) => m.SaleListComponent
      ),
  },
  {
    path: 'ordens-de-producao',
    title: 'D-Criar | Ordens de Produção',
    loadComponent: () =>
      import(
        './features/production/components/production-order-form/production-order-form'
      ).then((m) => m.ProductionOrderForm),
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
    path: 'tipos-materia-prima',
    title: 'D-Criar | Tipos de Matéria-Prima',
    loadComponent: () =>
      import(
        './features/stock/components/material-type-list/material-type-list'
      ).then((m) => m.MaterialTypeList),
  },
  // Rota curinga: redireciona qualquer URL não encontrada para o dashboard.
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
