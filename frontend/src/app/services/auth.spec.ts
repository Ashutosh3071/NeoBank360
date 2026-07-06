import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { Auth } from './auth';

describe('Auth', () => {
  let service: Auth;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        Auth,
        { provide: HttpClient, useValue: {} }
      ]
    });
    service = TestBed.inject(Auth);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
