import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { TransferService } from './transfer';
import { AccountService } from './account';

describe('TransferService', () => {
  let service: TransferService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransferService,
        { provide: HttpClient, useValue: {} },
        { provide: AccountService, useValue: {} }
      ]
    });
    service = TestBed.inject(TransferService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
