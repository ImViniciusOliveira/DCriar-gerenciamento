import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type RetalhoValue = '1' | '5' | '10' | '50' | '100' | 'R';

@Component({
  selector: 'app-retalho-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="retalho-icon-box">
      <span class="retalho-badge retalho-badge--a">{{ value }}</span>
    </span>
  `,
  styleUrls: ['./retalho-badge.component.scss']
})
export class RethalboBadgeComponent {
  @Input() value: RetalhoValue = '1';
}

