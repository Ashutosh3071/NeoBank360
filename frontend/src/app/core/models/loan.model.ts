export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type RepaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE';

export interface LoanProduct {
  id: number;
  productName: string;
  minAmount: number;
  maxAmount: number;
  annualInterestRate: number;
  allowedTenures: string; // Comma-separated list like "12,24,36"
}

export interface LoanApplication {
  id: number;
  userId: number;
  userEmail: string;
  userFullName: string;
  loanProductId: number;
  loanProductName: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  status: LoanStatus;
  adminRemarks?: string;
  appliedAt: string;
  decidedAt?: string;
  disbursementAccountId?: number;
  disbursementAccountNumber?: string;
}

export interface LoanApplicationRequest {
  loanProductId: number;
  requestedAmount: number;
  requestedTenureMonths: number;
  disbursementAccountId: number;
}

export interface LoanDecision {
  decision: LoanStatus;
  adminRemarks?: string;
}

export interface LoanAccount {
  id: number;
  loanApplicationId: number;
  userId: number;
  userEmail: string;
  productName: string;
  principalAmount: number;
  annualInterestRate: number;
  tenureMonths: number;
  emiAmount: number;
  disbursedAt: string;
}

export interface LoanRepayment {
  id: number;
  loanAccountId: number;
  instalmentNumber: number;
  dueDate: string; // ISO date string
  emiAmount: number;
  principalComponent: number;
  interestComponent: number;
  paymentStatus: RepaymentStatus;
  paidAt?: string; // ISO datetime string
}
