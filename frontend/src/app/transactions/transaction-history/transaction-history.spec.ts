import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { TransactionHistoryComponent } from './transaction-history';
import { TransactionService } from '../../services/transaction';

describe('TransactionHistoryComponent', () => {
  let component: TransactionHistoryComponent;
  let fixture: ComponentFixture<TransactionHistoryComponent>;

  beforeEach(async () => {
    const transactionServiceSpy = {
      getTransactions: () => of({ content: [], totalPages: 0 })
    };

    await TestBed.configureTestingModule({
      imports: [TransactionHistoryComponent],
      providers: [
        provideRouter([]),
        { provide: TransactionService, useValue: transactionServiceSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransactionHistoryComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
