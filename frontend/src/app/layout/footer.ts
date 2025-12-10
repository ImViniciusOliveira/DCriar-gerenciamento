import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';

/**
 * Componente do rodapé da aplicação.
 * Exibe informações como o ano atual e o nome da empresa.
 * Seu posicionamento é controlado pelo layout flexbox do componente raiz (`app-root`).
 */
@Component({
  selector: 'app-footer',
  imports: [MatToolbarModule],
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
  standalone: true,
})
export class Footer {
  /**
   * Armazena o ano atual, calculado uma vez na inicialização do componente.
   */
  currentYear = new Date().getFullYear();
}
