import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Auth } from '../services/auth';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private authService: Auth, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    // Skip auth endpoints entirely — no token, no error handling
    const isAuthRequest = req.url.includes('/auth/login') || req.url.includes('/auth/register');
    if (isAuthRequest) {
      return next.handle(req);
    }

    // No token available — let request go but handle 401
    if (!token) {
      return next.handle(req).pipe(catchError((err) => this.handleAuthError(err)));
    }

    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });

    return next.handle(clonedRequest).pipe(catchError((err) => this.handleAuthError(err)));
  }

  private handleAuthError(err: HttpErrorResponse): Observable<never> {
    if (err.status === 401 && !this.router.url.startsWith('/login')) {
      const token = this.authService.getToken();
      if (token) {
        // Any 401 with a token means the token is no longer valid — clear and redirect
        this.authService.logout();
        this.router.navigate(['/login'], {
          queryParams: { sessionExpired: 'true' },
        });
      }
    }
    return throwError(() => err);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true; // Malformed token — treat as expired
    }
  }
}
