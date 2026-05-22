// customer-account.component.ts

import { Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector:    'app-customer-account',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './customer-account.component.html',
  styleUrl:    './customer-account.component.css',
})
export class CustomerAccountComponent implements OnChanges {

  @Input() activeSubSection = 'accounts-summary';
  @Input() accounts: any[]  = [];

  transactions   = signal<any[]>([]);
  txnLoading     = signal(false);
  selectedAccount = signal<any>(null);
  page           = signal(0);
  totalPages     = signal(0);

  constructor(private applicationService: ApplicationService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.accounts.length > 0 && !this.selectedAccount()) {
      this.selectedAccount.set(this.accounts[0]);
    }
    if (this.activeSubSection === 'accounts-statements') {
      this.loadTransactions();
    }
  }

  selectAccount(acc: any): void {
    this.selectedAccount.set(acc);
    if (this.activeSubSection === 'accounts-statements') this.loadTransactions();
  }

  loadTransactions(): void {
    if (!this.selectedAccount()) return;
    this.txnLoading.set(true);
    this.applicationService
      .getMyTransactions(this.selectedAccount().accountNumber, this.page(), 15)
      .subscribe({
        next: (res: any) => {
          this.txnLoading.set(false);
          if (res.success && res.data) {
            this.transactions.set(res.data.content);
            this.totalPages.set(res.data.totalPages);
          }
        },
        error: () => this.txnLoading.set(false),
      });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.loadTransactions();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  formatAmount(val: string): string {
    return '₹' + parseFloat(val || '0').toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  txnIcon(type: string): string {
    const m: Record<string, string> = {
      DEPOSIT: '⬇️', WITHDRAWAL: '⬆️', TRANSFER: '🔄',
      UPI: '📱', NEFT: '🏦', RTGS: '🏦',
    };
    return m[type] ?? '💸';
  }

  txnColor(type: string, accountNumber: string, txn: any): string {
    return txn.toAccountNumber === accountNumber ? 'credit' : 'debit';
  }

  get sectionTitle(): string {
    const m: Record<string, string> = {
      'accounts-summary':    'Account Summary',
      'accounts-savings':    'Savings Account',
      'accounts-statements': 'Account Statements',
    };
    return m[this.activeSubSection] ?? 'Accounts';
  }
}