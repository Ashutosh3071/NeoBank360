export type AccountStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
export type AccountType = 'SAVINGS' | 'CURRENT';

export interface Account {
  id: number;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  createdAt: string;

  // ✅ Account approval fields
  accountStatus: AccountStatus;
  isActive: boolean;

  // ✅ Needed only for admin view
  userId?: number;
  userEmail?: string;
}

export interface CreateAccountRequest {
  accountType: AccountType;
}
``