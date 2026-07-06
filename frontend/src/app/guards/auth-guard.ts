import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { Auth } from '../services/auth';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private authService: Auth, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    if (!this.authService.hasValidToken()) {
      this.router.navigate(['/login']);
      return false;
    }

    // Redirect admin away from customer pages to admin dashboard
    const path = route.routeConfig?.path || '';
    if (this.authService.isAdmin() && !path.startsWith('admin') && path !== 'profile') {
      this.router.navigate(['/admin/dashboard']);
      return false;
    }

    return true;
  }
}
