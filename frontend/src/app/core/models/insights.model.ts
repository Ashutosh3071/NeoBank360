export interface TrendEntry {
  year: number;
  month: number;
  monthLabel: string;
  totalIncome: number;
  totalExpense: number;
}

export interface FinancialInsights {
  totalIncome: number;
  totalExpense: number;
  savings: number;
  trendSummary: TrendEntry[];
}
