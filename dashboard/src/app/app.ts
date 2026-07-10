import { Component, signal } from '@angular/core';
import { BalanceCard } from './balance-card/balance-card';
import { SpendingChart } from './spending-chart/spending-chart';
import { TransactionList } from './transaction-list/transaction-list';
import { BudgetBars } from './budget-bars/budget-bars';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [BalanceCard, SpendingChart, TransactionList, BudgetBars],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  isDark = signal<boolean>(false);

  greeting = this.buildGreeting();

  toggleTheme(): void {
    this.isDark.update(v => !v);
    const root = document.documentElement;
    if (this.isDark()) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }

  private buildGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Buenos días';
    if (hour < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }
}
