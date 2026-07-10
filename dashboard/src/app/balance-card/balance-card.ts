import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';
import { Balance } from '../models/models';

@Component({
  selector: 'app-balance-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balance-card.html',
  styleUrl: './balance-card.css'
})
export class BalanceCard implements OnInit {

  balance = signal<Balance | null>(null);
  loading = signal<boolean>(true);
  error = signal<boolean>(false);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getBalance().subscribe({
      next: (data) => {
        this.balance.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }
}
