import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { TopNavbar } from './layout/top-navbar';
import { Footer } from './layout/footer';
import { NotificationCenter } from './shared/components/notification-center/notification-center';

/**
 * O componente raiz (root) da aplicação.
 * Ele serve como o "casco" principal, definindo a estrutura de layout
 * com um cabeçalho, uma área de conteúdo principal (onde as rotas são renderizadas)
 * e um rodapé.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopNavbar, Footer, NotificationCenter],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App { }
