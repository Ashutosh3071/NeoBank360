import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
} from '../core/models/auth.model';
import {
  UpdateProfileRequest,
  UserProfile,
} from '../core/models/profile.model';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly AUTH_API_URL = 'http://localhost:8080/api/auth/';
  private readonly USER_API_URL = 'http://localhost:8080/api/users/';
  private readonly TOKEN_KEY = 'token';

  private readonly http = inject(HttpClient);

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(
      `${this.AUTH_API_URL}register`,
      payload
    );
  }

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.AUTH_API_URL}login`, payload)
      .pipe(
        tap((response) => {
          if (response?.token) {
            sessionStorage.setItem(this.TOKEN_KEY, response.token);
          }
        })
      );
  }

  logout(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  getDecodedToken(): Record<string, any> | null {
    const token = this.getToken();

    if (!token) {
      return null;
    }

    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  getUserRole(): string | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken?.['role'] ?? null;
  }

  getUserEmail(): string | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken?.['sub'] ?? null;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isLoggedIn(): boolean {
    return this.hasValidToken();
  }

  hasValidToken(): boolean {
    const token = this.getToken();

    if (!token) {
      return false;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp;

      if (!exp) {
        return false;
      }

      const nowInSeconds = Math.floor(Date.now() / 1000);
      return exp > nowInSeconds;
    } catch {
      return false;
    }
  }

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.USER_API_URL}me`);
  }

  updateMyProfile(payload: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.USER_API_URL}me`, payload);
  }
}