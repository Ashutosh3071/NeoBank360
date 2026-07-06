import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthGuard } from './auth-guard';
import { Auth } from '../services/auth';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authServiceSpy: any;
  let routerSpy: any;

  beforeEach(() => {
    authServiceSpy = {
      hasValidToken: () => true,
      isAdmin: () => false
    };
    routerSpy = {
      navigate: () => {}
    };

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Auth, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    guard = TestBed.inject(AuthGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});
