import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DetailItem {
  key: string;
  value: string | number;
}

@Component({
  selector: 'app-details-popover',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './details-popover.html',
  styleUrls: ['./details-popover.scss']
})
export class DetailsPopover {
  @Input() items: DetailItem[] = [];
  @Input() triggerText: string = 'Exibir detalhes';
}
