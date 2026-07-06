import { Transaction } from './transaction.model';

/**
 * Dashboard counters
 */
export interface AdminDashboardStats {
  totalUsers: number;
  totalAdmins: number;
  totalCustomers: number;
  totalAccounts: number;
  totalTransactions: number;
}

/**
 * User details (unchanged)
 */
export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  role: string;
  isActive: boolean;
  createdAt: string;
}

/**
 * ✅ Account details with approval support
 */
export type AccountStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';

export interface AdminAccount {
  id: number;
  accountNumber: string;
  accountType: string;
  balance: number;
  createdAt: string;

  // ✅ NEW – required for account approval feature
  accountStatus: AccountStatus;
  isActive: boolean;

  // ✅ Used to identify the owner in admin dashboard
  userId: number;
  userEmail: string;
}

/**
 * Recent transactions (unchanged)
 */
export type AdminRecentTransaction = Transaction;

/**
 * ✅ NEW: Sprint 4 Pending approvals standard model
 */
export interface PendingApproval {
  id: number;
  type: 'LOAN_APPLICATION' | 'ACCOUNT_APPROVAL';
  applicantName: string;
  productName: string;
  requestedAmount: number;
  appliedAt: string;
}

/**
 * ✅ NEW: Sprint 4 System health model
 */
export interface SystemHealth {
  dbStatus: 'UP' | 'DOWN';
  activeSessions: number;
  serverUptimeSeconds: number;
}

/**
 * ✅ NEW: Sprint 4 User activity model
 */
export interface UserActivity {
  recentTransactions: any[];
  loginEvents: string[];
}