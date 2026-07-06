export interface BudgetRequest {
  category: string;
  budgetMonth: string;
  limitAmount: number;
}

export interface BudgetResponse {
  id: number;
  category: string;
  budgetMonth: string;
  limitAmount: number;
  spent: number;
  remaining: number;
  utilizationPercentage: number;
}
