export interface TransferRequest {
  sourceAccountId: number;
  destinationAccountNumber: string;
  amount: number;
  description?: string;
}

export interface TransferResponse {
  referenceId: string;
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: number;
  sourceBalanceAfter: number;
  destinationBalanceAfter: number;
  transactionDate: string;
  pointsEarned?: number;
}
``