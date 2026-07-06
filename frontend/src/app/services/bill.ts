import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillRequest, BillResponse, BillStatusUpdate } from '../core/models/bill.model';

@Injectable({ providedIn: 'root' })
export class BillService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/bills';

  create(request: BillRequest): Observable<BillResponse> {
    return this.http.post<BillResponse>(this.baseUrl, request);
  }

  getAll(): Observable<BillResponse[]> {
    return this.http.get<BillResponse[]>(this.baseUrl);
  }

  getById(id: number): Observable<BillResponse> {
    return this.http.get<BillResponse>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, update: BillStatusUpdate): Observable<BillResponse> {
    return this.http.patch<BillResponse>(`${this.baseUrl}/${id}/status`, update);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
