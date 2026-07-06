import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { TransferRequest, TransferResponse } from '../core/models/transfer.model';
import { AccountService } from './account';

@Injectable({
  providedIn: 'root',
})
export class TransferService {
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  private readonly API_URL = 'http://localhost:8080/api/transfers';

  transferMoney(payload: TransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(this.API_URL, payload).pipe(
      tap(() => {
        this.accountService.refreshAccounts();
      })
    );
  }
}
