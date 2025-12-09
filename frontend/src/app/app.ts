import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { TopNavbar } from './layout/top-navbar';
import { Footer } from './layout/footer';

/**
 * O componente raiz (root) da aplicação.
 * Ele serve como o "casco" principal, definindo a estrutura de layout
 * com um cabeçalho, uma área de conteúdo principal (onde as rotas são renderizadas)
 * e um rodapé.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopNavbar, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  // Este componente não possui lógica de negócio, sua única responsabilidade
  // é fornecer a estrutura de layout para a aplicação.
}
