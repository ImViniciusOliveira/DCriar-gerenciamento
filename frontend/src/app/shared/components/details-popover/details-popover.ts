import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Representa um par de chave/valor para exibição no popover.
 */
export interface DetailItem {
  key: string;
  value: string | number;
}

/**
 * Um componente reutilizável que exibe uma lista de detalhes (chave/valor)
 * em um popover que aparece ao passar o mouse sobre um texto gatilho.
 */
@Component({
  selector: 'app-details-popover',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './details-popover.html',
  styleUrls: ['./details-popover.scss']
})
export class DetailsPopover {
  /**
   * A lista de itens (chave/valor) a serem exibidos no popover.
   * Utiliza a função `input()` do Angular 17+ para uma declaração de entrada mais moderna.
   */
  items = input<DetailItem[]>([]);

  /**
   * O texto que servirá como gatilho para exibir o popover.
   */
  triggerText = input<string>('Exibir detalhes');

  /**
   * Define se o popover deve mostrar a chave junto do valor.
   * O padrão é manter a descrição visível para preservar o comportamento atual do sistema.
   */
  showKeys = input<boolean>(true);
}
