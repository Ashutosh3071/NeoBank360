import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Account, CreateAccountRequest } from '../core/models/account.model';

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/accounts';

  private readonly accountsSubject = new BehaviorSubject<Account[]>([]);
  readonly accounts$ = this.accountsSubject.asObservable();

  /**
   * ✅ Get logged‑in user's accounts
   * Includes PENDING / APPROVED / REJECTED accounts
   */
  getMyAccounts(): Observable<Account[]> {
    return this.http
      .get<Account[]>(`${this.API_URL}/my`)
      .pipe(tap((accounts) => this.accountsSubject.next(accounts)));
  }

  /**
   * ✅ Create new account
   * Backend sets:
   * - accountStatus = PENDING_APPROVAL
   * - isActive = false
   */
  createAccount(payload: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(this.API_URL, payload).pipe(tap(() => this.refreshAccounts()));
  }

  /**
   * ✅ Reload account list
   */
  refreshAccounts(): void {
    this.getMyAccounts().subscribe({
      error: (err) => console.error('Failed to refresh accounts', err),
    });
  }

  /**
   * ✅ Used by components that need current snapshot
   */
  getCurrentAccounts(): Account[] {
    return this.accountsSubject.getValue();
  }

  /**
   * ✅ Optional manual setter
   */
  setAccounts(accounts: Account[]): void {
    this.accountsSubject.next(accounts);
  }
}
