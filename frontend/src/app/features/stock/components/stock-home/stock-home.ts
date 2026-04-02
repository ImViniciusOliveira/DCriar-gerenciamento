import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-stock-home',
  standalone: true,
  imports: [MatCardModule, MatIconModule],
  templateUrl: './stock-home.html',
  styleUrl: './stock-home.scss',
})
export class StockHome {}
