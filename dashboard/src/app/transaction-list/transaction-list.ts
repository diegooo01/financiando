import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';
import { Transaction } from '../models/models';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.css'
})
export class TransactionList implements OnInit {

  transactions = signal<Transaction[]>([]);
  loading = signal<boolean>(true);
  error = signal<boolean>(false);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getTransactions().subscribe({
      next: (data) => {
        // Ordenar por fecha de creación, más reciente primero, y tomar las últimas 10
        const sorted = [...data].sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.transactions.set(sorted.slice(0, 10));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }
}
