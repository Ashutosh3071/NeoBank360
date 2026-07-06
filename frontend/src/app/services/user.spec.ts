import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { UserService } from './user';

describe('UserService', () => {
  let service: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UserService,
        { provide: HttpClient, useValue: {} }
      ]
    });
    service = TestBed.inject(UserService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
