import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UpdateProfileRequest, UserProfile } from '../core/models/profile.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/auth/';

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}me`);
  }

  updateMyProfile(payload: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.API_URL}me`, payload);
  }
}
