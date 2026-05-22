// admin-account.component.ts

import {
  Component, Input, OnInit, OnChanges,
  SimpleChanges, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector:    'app-admin-account',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-accounts.component.html',
  styleUrl:    './admin-accounts.component.css',
})
export class AdminAccountComponent implements OnInit, OnChanges {

  @Input() activeSubSection = 'accounts-all';

  accounts      = signal<any[]>([]);
  loading       = signal(false);
  error         = signal('');
  success       = signal('');
  searchQuery   = '';
  page          = signal(0);
  totalPages    = signal(0);
  totalElements = signal(0);
  pageSize      = 10;

  // Deposit / Withdraw Modal
  selectedAccount = signal<any>(null);
  modalMode       = signal<'none' | 'deposit' | 'withdraw' | 'status'>('none');
  actionLoading   = signal(false);
  txnAmount       = '';
  txnDescription  = '';
  newStatus       = '';
  statusReason    = '';

  constructor(private applicationService: ApplicationService) {}

  ngOnInit():    void { this.loadAccounts(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['activeSubSection'] && !c['activeSubSection'].firstChange) {
      this.page.set(0); this.searchQuery = '';
      this.loadAccounts();
    }
  }

  get statusFilter(): string | undefined {
    const m: Record<string, string> = {
      'accounts-active': 'ACTIVE',
      'accounts-frozen': 'FROZEN',
    };
    return m[this.activeSubSection];
  }

  get sectionTitle(): string {
    const m: Record<string, string> = {
      'accounts-all':    'All Accounts',
      'accounts-active': 'Active Accounts',
      'accounts-frozen': 'Frozen Accounts',
    };
    return m[this.activeSubSection] ?? 'Accounts';
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.error.set('');
    this.applicationService.getAllAccounts(
      this.statusFilter, this.page(), this.pageSize
    ).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.accounts.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  get filteredAccounts(): any[] {
    if (!this.searchQuery.trim()) return this.accounts();
    const q = this.searchQuery.toLowerCase();
    return this.accounts().filter(a =>
      a.accountNumber?.includes(q) ||
      a.ifscCode?.toLowerCase().includes(q)
    );
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p); this.loadAccounts();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  openDeposit(acc: any):  void { this.selectedAccount.set(acc); this.txnAmount = ''; this.txnDescription = ''; this.modalMode.set('deposit'); }
  openWithdraw(acc: any): void { this.selectedAccount.set(acc); this.txnAmount = ''; this.txnDescription = ''; this.modalMode.set('withdraw'); }
  openStatus(acc: any):   void { this.selectedAccount.set(acc); this.newStatus = acc.status; this.statusReason = ''; this.modalMode.set('status'); }
  closeModal():           void { this.modalMode.set('none'); this.selectedAccount.set(null); this.error.set(''); }

  submitDeposit(): void {
    if (!this.txnAmount || parseFloat(this.txnAmount) <= 0) {
      this.error.set('Enter valid amount.'); return;
    }
    this.actionLoading.set(true);
    this.applicationService.adminDeposit({
      accountNumber: this.selectedAccount().accountNumber,
      amount:        this.txnAmount,
      description:   this.txnDescription || 'Admin Deposit',
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set(`✅ Deposited ₹${this.txnAmount} to ${this.selectedAccount().accountNumber}`);
          this.closeModal(); this.loadAccounts();
          setTimeout(() => this.success.set(''), 4000);
        } else { this.error.set(res.message); }
      },
      error: (err: any) => { this.actionLoading.set(false); this.error.set(err.error?.message || 'Failed.'); },
    });
  }

  submitWithdraw(): void {
    if (!this.txnAmount || parseFloat(this.txnAmount) <= 0) {
      this.error.set('Enter valid amount.'); return;
    }
    this.actionLoading.set(true);
    this.applicationService.adminWithdraw({
      accountNumber: this.selectedAccount().accountNumber,
      amount:        this.txnAmount,
      description:   this.txnDescription || 'Admin Withdrawal',
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set(`✅ Withdrew ₹${this.txnAmount} from ${this.selectedAccount().accountNumber}`);
          this.closeModal(); this.loadAccounts();
          setTimeout(() => this.success.set(''), 4000);
        } else { this.error.set(res.message); }
      },
      error: (err: any) => { this.actionLoading.set(false); this.error.set(err.error?.message || 'Failed.'); },
    });
  }

  submitStatusUpdate(): void {
    this.actionLoading.set(true);
    this.applicationService.updateAccountStatus({
      accountNumber: this.selectedAccount().accountNumber,
      status:        this.newStatus,
      reason:        this.statusReason,
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set(`Account ${this.selectedAccount().accountNumber} → ${this.newStatus}`);
          this.closeModal(); this.loadAccounts();
          setTimeout(() => this.success.set(''), 4000);
        } else { this.error.set(res.message); }
      },
      error: (err: any) => { this.actionLoading.set(false); this.error.set(err.error?.message || 'Failed.'); },
    });
  }

  getStatusClass(s: string): string {
    const m: Record<string, string> = {
      ACTIVE: 'st-active', FROZEN: 'st-frozen',
      INACTIVE: 'st-inactive', CLOSED: 'st-closed', DORMANT: 'st-dormant',
    };
    return m[s] ?? 'st-inactive';
  }

  formatAmount(v: string): string {
    return '₹' + parseFloat(v || '0').toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }
}