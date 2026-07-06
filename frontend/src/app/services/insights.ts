import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FinancialInsights } from '../core/models/insights.model';

@Injectable({
  providedIn: 'root',
})
export class InsightsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/insights';

  getInsights(userId: number): Observable<FinancialInsights> {
    return this.http.get<FinancialInsights>(`${this.API_URL}/${userId}`);
  }
}
