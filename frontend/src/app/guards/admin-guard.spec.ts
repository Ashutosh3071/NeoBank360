import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AdminGuard } from './admin-guard';
import { Auth } from '../services/auth';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let authServiceSpy: any;
  let routerSpy: any;

  beforeEach(() => {
    authServiceSpy = {
      isLoggedIn: () => true,
      isAdmin: () => true
    };
    routerSpy = {
      navigate: () => {}
    };

    TestBed.configureTestingModule({
      providers: [
        AdminGuard,
        { provide: Auth, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    guard = TestBed.inject(AdminGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});
