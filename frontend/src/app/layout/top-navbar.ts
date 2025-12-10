import { Component, computed, inject } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { ApiRoot } from '../core/services/api-root';

import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

/**
 * Define a estrutura de um link de navegação.
 */
type NavLink = { path: string; label: string; icon?: string };

/**
 * Componente da barra de navegação superior da aplicação.
 * Ele exibe links de navegação que são gerados dinamicamente
 * com base nos endpoints disponíveis na API (HATEOAS).
 */
@Component({
  selector: 'app-top-navbar',
  imports: [
    MatToolbarModule,
    MatButtonModule,
    RouterModule,
    MatIconModule
],
  templateUrl: './top-navbar.html',
  styleUrl: './top-navbar.scss',
  standalone: true,
})
export class TopNavbar {
  // Injeta o serviço ApiRoot para acessar os endpoints da API.
  private readonly apiRoot = inject(ApiRoot);

  // Define a ordem desejada dos links de navegação na barra.
  private readonly navOrder: string[] = [
    'dashboard',
    'produtos',
    'lotes-materia-prima',
    'ordens-de-producao',
    'vendas'
  ];

  // Mapeia as chaves dos endpoints para objetos NavLink com labels e ícones.
  private readonly navLinksMap: Record<string, NavLink> = {
    'dashboard': { path: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    'produtos': { path: 'produtos', label: 'Produtos', icon: 'inventory_2' },
    'lotes-materia-prima': { path: 'lotes-materia-prima', label: 'Lotes', icon: 'view_in_ar' },
    'ordens-de-producao': { path: 'ordens-de-producao', label: 'Ordens de Produção', icon: 'content_cut' },
    'vendas': { path: 'vendas', label: 'Vendas', icon: 'point_of_sale' },
  };

  /**
   * Sinal computado que retorna os links de navegação disponíveis.
   * Ele reage automaticamente a mudanças nos endpoints da API.
   *
   * O filtro `key in endpoints._links` garante que apenas links para
   * endpoints que a API realmente expõe sejam exibidos.
   */
  readonly availableNavLinks = computed(() => {
    const endpoints = this.apiRoot.endpoints();
    // Verifica se endpoints e _links existem antes de tentar acessá-los.
    if (!endpoints || !endpoints._links) {
      return [];
    }

    return this.navOrder
      // O '!' afirma que _links não é nulo aqui.
      .filter(key => key in endpoints._links!)
      .map(key => this.navLinksMap[key]);
  });
}
