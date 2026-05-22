// src/app/core/guards/auth.guard.ts

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

const getUser = () => {
  try {
    const json = localStorage.getItem('user');
    return json ? JSON.parse(json) : null;
  } catch { return null; }
};

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = getUser();
  if (!user?.userId) { router.navigate(['/login']); return false; }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = getUser();
  if (!user?.userId) { router.navigate(['/login']); return false; }
  if (!['ADMIN','SUPER_ADMIN'].includes(user.role)) {
    router.navigate(['/dashboard']); return false;
  }
  return true;
};

export const managerGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = getUser();
  if (!user?.userId) { router.navigate(['/login']); return false; }
  if (!['ADMIN','SUPER_ADMIN','MANAGER'].includes(user.role)) {
    router.navigate(['/dashboard']); return false;
  }
  return true;
};

export const customerGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = getUser();
  if (!user?.userId) { router.navigate(['/login']); return false; }
  if (user.role !== 'CUSTOMER') {
    router.navigate(['/admin']); return false;
  }
  return true;
};

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const user = getUser();
  if (!user?.userId) return true;
  if (['ADMIN','SUPER_ADMIN'].includes(user.role)) {
    router.navigate(['/admin']);
  } else if (user.role === 'MANAGER') {
    router.navigate(['/manager']);
  } else {
    router.navigate(['/dashboard']);
  }
  return false;
};