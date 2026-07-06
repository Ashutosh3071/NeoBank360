import { inject, Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { Auth } from '../services/auth';

@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  canActivate(): boolean {
    if (this.auth.isLoggedIn() && this.auth.isAdmin()) {
      return true;
    }

    this.router.navigate(['/dashboard']);
    return false;
  }
}