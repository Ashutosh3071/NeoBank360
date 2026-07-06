import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { AdminService } from './admin';

describe('AdminService', () => {
  let service: AdminService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminService,
        { provide: HttpClient, useValue: {} }
      ]
    });
    service = TestBed.inject(AdminService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
