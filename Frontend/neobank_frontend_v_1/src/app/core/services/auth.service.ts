// src/app/core/services/auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  status: number;
  errorCode?: string;
  data?: T;
}

export interface CaptchaResponse {
  token: string;
  captchaText: string;
}

export interface UsernameCheckResponse {
  username: string;
  available: boolean;
  message: string;
}

export interface RegisterResponse {
  userId: number;
  email: string;
  username: string;
  message: string;
}

export interface LoginResponse {
  userId: number;
  username: string;
  email: string;
  fullName: string;
  accountNumber: string;
  role: string;
  token?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  // ── Captcha ──
  getCaptcha(): Observable<ApiResponse<CaptchaResponse>> {
    return this.http.get<ApiResponse<CaptchaResponse>>(`${this.base}/captcha`);
  }

  // ── Username check ──
  checkUsername(username: string): Observable<ApiResponse<UsernameCheckResponse>> {
    return this.http.get<ApiResponse<UsernameCheckResponse>>(
      `${this.base}/check-username`, { params: { username } }
    );
  }

  // ── Send registration OTP ──
  sendRegistrationOtp(payload: {
    email: string;
    captchaToken: string;
    captchaAnswer: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/send-registration-otp`, payload
    );
  }

  // ── Register ──
  register(payload: {
    email: string;
    username: string;
    password: string;
    confirmPassword: string;
    otp: string;
  }): Observable<ApiResponse<RegisterResponse>> {
    return this.http.post<ApiResponse<RegisterResponse>>(
      `${this.base}/register`, payload
    );
  }

  // ── Login ──
  login(payload: {
    usernameOrEmail: string;
    password: string;
  }): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(
      `${this.base}/login`, payload
    );
  }

  // ── Resend OTP ──
  resendOtp(email: string, purpose: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/resend-otp`, { email, purpose }
    );
  }

  // ── Forgot Username ──
  forgotUsername(payload: {
    email: string;
    captchaToken: string;
    captchaAnswer: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/forgot-username`, payload
    );
  }

  verifyForgotUsernameOtp(payload: {
    email: string;
    otp: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/forgot-username/verify`, payload
    );
  }

  // ── Forgot Password ──
  forgotPassword(payload: {
    email: string;
    captchaToken: string;
    captchaAnswer: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/forgot-password`, payload
    );
  }

  resetPassword(payload: {
    email: string;
    otp: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.base}/reset-password`, payload
    );
  }

  changePassword(payload: {
  currentPassword: string;
  newPassword:     string;
  confirmPassword: string;
}): Observable<ApiResponse<void>> {
  return this.http.post<ApiResponse<void>>(
    `${this.base}/change-password`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

}

// import { Injectable, signal } from '@angular/core';
// import { HttpClient } from '@angular/common/http';
// // import { Observable } from 'rxjs';
// import { of, Observable } from 'rxjs';

// import {
//   ApiResponse,
//   UserDto,
//   LoginRequest,
//   LoginResponse,
//   RegisterRequest,
//   VerifyEmailRequest,
//   VerifyLoginRequest
// } from '../model/all';

// @Injectable({ providedIn: 'root' })
// export class AuthService {
//   private apiUrl = 'http://localhost:8765/api/auth';
//   currentUser = signal<UserDto | null>(null);

//   constructor(private http: HttpClient) {}

//   register(request: RegisterRequest): Observable<ApiResponse<UserDto>> {
//     return this.http.post<ApiResponse<UserDto>>(`${this.apiUrl}/register`, request);
//   //     return of({
//   //   timestamp: new Date().toISOString(),
//   //   status: 200,
//   //   success: true,
//   //   message: 'User registered successfully',
//   //   data: {
//   //     id: 1,
//   //     email: request.email,
//   //     phone: request.phone,
//   //     emailVerified: false,
//   //     status: 'PENDING',
//   //     roleName: 'CUSTOMER',
//   //     kycStatus: 'PENDING',
//   //     accountNumber: 'NB000123456',
//   //     token: 'sample-token'
//   //   }
//   // });

//   }

//   verifyEmail(request: VerifyEmailRequest): Observable<ApiResponse<void>> {
//     return this.http.post<ApiResponse<void>>(`${this.apiUrl}/verify-email`, request);
//   }

//   login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
//     return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, request);
//   }

//   verifyLogin(request: VerifyLoginRequest): Observable<ApiResponse<UserDto>> {
//     return this.http.post<ApiResponse<UserDto>>(`${this.apiUrl}/verify-login`, request);
//   }

//   getCurrentUser(): Observable<ApiResponse<UserDto>> {
//     return this.http.get<ApiResponse<UserDto>>(`${this.apiUrl}/current-user`);
//   }

//   isLoggedIn(): boolean {
//     return this.currentUser() !== null;
//   }

//   logout(): void {
//     this.currentUser.set(null);
//     localStorage.removeItem('user');
//     localStorage.removeItem('token');
//   }
// }