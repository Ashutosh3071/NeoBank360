import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { AccountService } from './account';

describe('AccountService', () => {
  let service: AccountService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountService,
        { provide: HttpClient, useValue: {} }
      ]
    });
    service = TestBed.inject(AccountService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
