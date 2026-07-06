import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { JwtInterceptor } from './jwt-interceptor';
import { Auth } from '../services/auth';

describe('JwtInterceptor', () => {
  let interceptor: JwtInterceptor;
  let authServiceSpy: any;
  let routerSpy: any;

  beforeEach(() => {
    authServiceSpy = {
      getToken: () => 'fake-token',
      logout: () => {}
    };
    routerSpy = {
      navigate: () => {},
      url: '/dashboard'
    };

    TestBed.configureTestingModule({
      providers: [
        JwtInterceptor,
        { provide: Auth, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    interceptor = TestBed.inject(JwtInterceptor);
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});
