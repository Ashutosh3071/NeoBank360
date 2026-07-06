import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

export const GuestGuard: CanActivateFn = () => {
  const authService = inject(Auth);
  const router = inject(Router);

  // If user is logged in, redirect away from guest pages
  if (authService.isLoggedIn()) {

    if (authService.isAdmin && authService.isAdmin()) {
      return router.createUrlTree(['/admin/dashboard']);
    }

    return router.createUrlTree(['/dashboard']);
  }

  // ✅ Guest user allowed
  return true;
};
