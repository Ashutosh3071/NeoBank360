import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class RewordBudgetBillServise {

  private base = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

    // ── Budget APIs ──
createBudget(payload: {
  category: string; budgetMonth: string; limitAmount: string;
}): Observable<any> {
  return this.http.post(`${this.base}/budgets`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getMyBudgets(): Observable<any> {
  return this.http.get(`${this.base}/budgets`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getBudgetSummary(userId: number, month: string): Observable<any> {
  return this.http.get(`${this.base}/budgets/${userId}/${month}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

deleteBudget(id: number): Observable<any> {
  return this.http.delete(`${this.base}/budgets/${id}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

// ── Bill APIs ──
createBill(payload: {
  billerName: string; amount: string; dueDate: string; description?: string;
}): Observable<any> {
  return this.http.post(`${this.base}/bills`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getMyBills(): Observable<any> {
  return this.http.get(`${this.base}/bills`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

updateBillStatus(id: number, status: string): Observable<any> {
  return this.http.patch(`${this.base}/bills/${id}/status`, { status },
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

deleteBill(id: number): Observable<any> {
  return this.http.delete(`${this.base}/bills/${id}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

// ── Reward APIs ──
getMyRewards(): Observable<any> {
  return this.http.get(`${this.base}/rewards/my`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getRewardHistory(page = 0, size = 20): Observable<any> {
  return this.http.get(`${this.base}/rewards/history`,
    { params: { page, size },
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

}