export type TransactionType = 'CREDIT' | 'DEBIT';

export interface Transaction {
  id: number;
  accountId: number;
  accountNumber: string;
  transactionType: TransactionType;
  amount: number;
  balanceAfter: number;
  description: string;
  transactionDate: string;
}

export interface CreateTransactionRequest {
  transactionType: TransactionType;
  amount: number;
  description?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
