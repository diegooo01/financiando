export interface Balance {
  start: string;
  end: string;
  income: number;
  expenses: number;
  net: number;
}

export interface Transaction {
  id: number;
  amount: number;
  description: string;
  categoryId: number;
  categoryName: string;
  occurredAt: string;
  source: string;
  type: string;
  createdAt: string;
}

export interface Category {
  id: number;
  name: string;
  emoji: string;
  type: string;
  keywords: string[];
}

export interface CategorySpending {
  categoryName: string;
  emoji: string;
  total: number;
}

export interface BudgetStatus {
  categoryName: string;
  emoji: string;
  limit: number;
  spent: number;
  remaining: number;
  percentUsed: number;
}
