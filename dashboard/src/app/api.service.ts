import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Balance, Transaction, Category, CategorySpending, BudgetStatus } from './models/models';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getBalance(): Observable<Balance> {
    return this.http.get<Balance>(`${this.baseUrl}/balance`);
  }

  getTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/transactions`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/categories`);
  }

  getSpendingByCategory(): Observable<CategorySpending[]> {
    return this.http.get<CategorySpending[]>(`${this.baseUrl}/reports/spending-by-category`);
  }

  getBudgetStatus(): Observable<BudgetStatus[]> {
    return this.http.get<BudgetStatus[]>(`${this.baseUrl}/reports/budget-status`);
  }
}
