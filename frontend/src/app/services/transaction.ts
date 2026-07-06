import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CreateTransactionRequest,
  PageResponse,
  Transaction,
} from '../core/models/transaction.model';
import { AccountService } from './account';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  getTransactions(
    accountId: number,
    page = 0,
    size = 10
  ): Observable<PageResponse<Transaction>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<Transaction>>(
      `http://localhost:8080/api/accounts/${accountId}/transactions`,
      { params }
    );
  }

  createTransaction(
    accountId: number,
    payload: CreateTransactionRequest
  ): Observable<Transaction> {
    return this.http
      .post<Transaction>(
        `http://localhost:8080/api/accounts/${accountId}/transactions`,
        payload
      )
      .pipe(
        tap(() => {
          // Refresh dashboard balances after successful transaction
          this.accountService.refreshAccounts();
        })
      );
  }
}