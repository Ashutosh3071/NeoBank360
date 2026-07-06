import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { TransactionService } from './transaction';
import { AccountService } from './account';

describe('TransactionService', () => {
  let service: TransactionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransactionService,
        { provide: HttpClient, useValue: {} },
        { provide: AccountService, useValue: {} }
      ]
    });
    service = TestBed.inject(TransactionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
