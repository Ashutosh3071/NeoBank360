import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BudgetRequest, BudgetResponse } from '../core/models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/budgets';

  create(request: BudgetRequest): Observable<BudgetResponse> {
    return this.http.post<BudgetResponse>(this.baseUrl, request);
  }

  getSummary(userId: number, month: string): Observable<BudgetResponse[]> {
    return this.http.get<BudgetResponse[]>(`${this.baseUrl}/${userId}/${month}`);
  }

  getAll(): Observable<BudgetResponse[]> {
    return this.http.get<BudgetResponse[]>(this.baseUrl);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
