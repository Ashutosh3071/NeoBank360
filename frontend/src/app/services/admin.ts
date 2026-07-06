import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdminAccount,
  AdminDashboardStats,
  AdminRecentTransaction,
  AdminUser,
} from '../core/models/admin.model';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/admin';

  // =======================
  // Dashboard data
  // =======================

  getDashboardStats(): Observable<AdminDashboardStats> {
    return this.http.get<AdminDashboardStats>(`${this.API_URL}/dashboard`);
  }

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.API_URL}/users`);
  }

  getAccounts(): Observable<AdminAccount[]> {
    return this.http.get<AdminAccount[]>(`${this.API_URL}/accounts`);
  }

  getRecentTransactions(size = 10): Observable<AdminRecentTransaction[]> {
    return this.http.get<AdminRecentTransaction[]>(
      `${this.API_URL}/transactions/recent?size=${size}`,
    );
  }

  // =======================
  // ✅ Account approval APIs
  // =======================

  /**
   * Get accounts pending approval
   */
  getPendingAccounts(): Observable<AdminAccount[]> {
    return this.http.get<AdminAccount[]>(`${this.API_URL}/accounts/pending`);
  }

  /**
   * Approve account
   */
  approveAccount(accountId: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/accounts/${accountId}/approve`, {});
  }

  /**
   * Reject account
   */
  rejectAccount(accountId: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/accounts/${accountId}/reject`, {});
  }
  /**
   * ✅ Get single account detail with KYC info for admin review
   */
  getAccountDetail(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/accounts/${accountId}`);
  }

  /**
   * ✅ Admin credits money directly into a user's account
   */
  adminCreditAccount(accountId: number, amount: number, description: string): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/accounts/${accountId}/credit`, {
      amount,
      description,
    });
  }

  /**
   * ✅ NEW – Get combined pending approvals with optional module filter
   */
  getPendingApprovals(
    module?: string,
  ): Observable<import('../core/models/admin.model').PendingApproval[]> {
    const url = module
      ? `${this.API_URL}/pending-approvals?module=${module}`
      : `${this.API_URL}/pending-approvals`;
    return this.http.get<import('../core/models/admin.model').PendingApproval[]>(url);
  }

  /**
   * ✅ NEW – Get system connectivity and session counts
   */
  getSystemHealth(): Observable<import('../core/models/admin.model').SystemHealth> {
    return this.http.get<import('../core/models/admin.model').SystemHealth>(
      `${this.API_URL}/system-health`,
    );
  }

  /**
   * ✅ NEW – Update customer status (Activate/Deactivate)
   */
  updateUserStatus(userId: number, isActive: boolean): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/users/${userId}/status`, { isActive });
  }

  /**
   * ✅ NEW – Get customer activity summary (last 20 txs and last 5 logins)
   */
  getUserActivity(userId: number): Observable<import('../core/models/admin.model').UserActivity> {
    return this.http.get<import('../core/models/admin.model').UserActivity>(
      `${this.API_URL}/users/${userId}/activity`,
    );
  }

  /**
   * ✅ NEW – Get paginated users
   */
  getUsersPaginated(page: number, size: number): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/users?page=${page}&size=${size}`);
  }

  getTransactionAnalytics(timeframe = '7d'): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/analytics/transactions?timeframe=${timeframe}`);
  }

  getLoanAnalytics(timeframe = '7d'): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/analytics/loans?timeframe=${timeframe}`);
  }

  getSystemLogs(from?: string, to?: string, status?: number, page = 0, size = 10): Observable<any> {
    let url = `${this.API_URL}/system-logs?page=${page}&size=${size}`;
    if (from) url += `&from=${from}`;
    if (to) url += `&to=${to}`;
    if (status !== undefined && status !== null) url += `&status=${status}`;
    return this.http.get<any>(url);
  }
}
