import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  LoanProduct,
  LoanApplication,
  LoanApplicationRequest,
  LoanDecision,
  LoanAccount,
  LoanRepayment,
  LoanStatus
} from '../core/models/loan.model';

@Injectable({
  providedIn: 'root',
})
export class LoanService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/loans';

  // --- Products ---
  getProducts(): Observable<LoanProduct[]> {
    return this.http.get<LoanProduct[]>(`${this.API_URL}/products`);
  }

  createProduct(product: Partial<LoanProduct>): Observable<LoanProduct> {
    return this.http.post<LoanProduct>(`${this.API_URL}/products`, product);
  }

  getProductById(id: number): Observable<LoanProduct> {
    return this.http.get<LoanProduct>(`${this.API_URL}/products/${id}`);
  }

  // --- Applications ---
  applyLoan(payload: LoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(`${this.API_URL}/apply`, payload);
  }

  getMyApplications(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.API_URL}/my-applications`);
  }

  getAdminApplications(status?: LoanStatus): Observable<LoanApplication[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<LoanApplication[]>(`${this.API_URL}/admin/applications`, { params });
  }

  decideApplication(loanApplicationId: number, payload: LoanDecision): Observable<LoanApplication> {
    return this.http.put<LoanApplication>(`${this.API_URL}/${loanApplicationId}/decision`, payload);
  }

  // --- Accounts ---
  getMyAccounts(): Observable<LoanAccount[]> {
    return this.http.get<LoanAccount[]>(`${this.API_URL}/my-accounts`);
  }

  // --- Repayments ---
  getRepaymentsForAccount(
    loanAccountId: number,
    status?: string,
    page: number = 0,
    size: number = 10
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<any>(`${this.API_URL}/${loanAccountId}/repayments`, { params });
  }

  payInstallment(loanAccountId: number, repaymentId: number, accountId: number): Observable<LoanRepayment> {
    return this.http.patch<LoanRepayment>(
      `${this.API_URL}/${loanAccountId}/repayments/${repaymentId}/pay?accountId=${accountId}`,
      {}
    );
  }
}
