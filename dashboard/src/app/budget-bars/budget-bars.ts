import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';
import { BudgetStatus } from '../models/models';

@Component({
  selector: 'app-budget-bars',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './budget-bars.html',
  styleUrl: './budget-bars.css'
})
export class BudgetBars implements OnInit {

  budgets = signal<BudgetStatus[]>([]);
  loading = signal<boolean>(true);
  error = signal<boolean>(false);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getBudgetStatus().subscribe({
      next: (data) => {
        this.budgets.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  barColor(percent: number): string {
    if (percent >= 100) return '#dc2626'; // rojo
    if (percent >= 80) return '#f59e0b';  // amarillo
    return '#16a34a';                     // verde
  }

  barWidth(percent: number): number {
    return Math.min(percent, 100); // la barra no pasa del 100% visualmente
  }
}
