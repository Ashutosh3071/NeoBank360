import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DashboardComponent } from './dashboard';
import { AccountService } from '../services/account';
import { TransferService } from '../services/transfer';
import { Auth } from '../services/auth';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    const accountServiceSpy = {
      getMyAccounts: () => of([]),
      accounts$: of([])
    };
    const transferServiceSpy = {};
    const authSpy = {
      isAdmin: () => false
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: TransferService, useValue: transferServiceSpy },
        { provide: Auth, useValue: authSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
