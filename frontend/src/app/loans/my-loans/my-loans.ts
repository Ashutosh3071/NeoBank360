import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../services/loan';
import { LoanAccount } from '../../core/models/loan.model';

@Component({
  selector: 'app-my-loans',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-loans.html',
  styleUrl: './my-loans.css',
})
export class MyLoansComponent implements OnInit {
  private readonly loanService = inject(LoanService);

  readonly accounts = signal<LoanAccount[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string>('');

  ngOnInit(): void {
    this.loadMyAccounts();
  }

  loadMyAccounts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.loanService.getMyAccounts().subscribe({
      next: (res) => {
        this.accounts.set(res);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to fetch active loan accounts.');
        this.isLoading.set(false);
      }
    });
  }

  // Helper to format account number in monospace
  formatAccountNumber(accountId: number): string {
    return 'LN-' + accountId.toString().padStart(6, '0');
  }
}
