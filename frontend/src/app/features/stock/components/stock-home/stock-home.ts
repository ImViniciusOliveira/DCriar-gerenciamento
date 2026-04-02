import { Component, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

type StockSectionKey = 'consultas' | 'ajustes' | 'historico';

interface StockSection {
  key: StockSectionKey;
  title: string;
  buttonLabel: string;
  subtitle: string;
}

@Component({
  selector: 'app-stock-home',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './stock-home.html',
  styleUrl: './stock-home.scss',
})
export class StockHome {
  protected readonly sections: StockSection[] = [
    {
      key: 'ajustes',
      title: 'Ajustes',
      buttonLabel: 'Ajustes',
      subtitle: 'Correcao do estoque fisico total e redistribuicao de saldo por canal'
    },
    {
      key: 'consultas',
      title: 'Consultas',
      buttonLabel: 'Consultas',
      subtitle: 'Consultas de estoque fisico, distribuicao por canal e visoes consolidadas'
    },
    {
      key: 'historico',
      title: 'Historico',
      buttonLabel: 'Historico',
      subtitle: 'Acompanhamento de mudancas, conferencias e auditoria do estoque'
    }
  ];

  protected readonly activeSection = signal<StockSection>(this.sections[0]);

  protected setActiveSection(section: StockSection): void {
    this.activeSection.set(section);
  }
}
