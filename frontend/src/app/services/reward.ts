import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RewardResponse } from '../core/models/reward.model';

@Injectable({ providedIn: 'root' })
export class RewardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/rewards';

  getBalance(): Observable<RewardResponse> {
    return this.http.get<RewardResponse>(this.baseUrl);
  }

  redeemPoints(points: number, rewardId: string, accountId?: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/redeem`, { points, rewardId, accountId });
  }
}
