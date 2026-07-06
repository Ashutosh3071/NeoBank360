import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/analytics';

  getSpendingAnalytics(userId: number, months = 6): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/spending/${userId}?months=${months}`);
  }

  getWealthAnalytics(userId: number): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/wealth/${userId}`);
  }
}
