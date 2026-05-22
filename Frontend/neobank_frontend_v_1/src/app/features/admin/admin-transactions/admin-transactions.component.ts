// admin-transaction.component.ts

import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector:    'app-admin-transaction',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-transactions.component.html',
  styleUrl:    './admin-transactions.component.css',
})
export class AdminTransactionComponent implements OnInit {

  transactions  = signal<any[]>([]);
  loading       = signal(false);
  error         = signal('');
  searchQuery   = '';
  page          = signal(0);
  totalPages    = signal(0);
  totalElements = signal(0);
  pageSize      = 15;

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void { this.loadTransactions(); }

  loadTransactions(): void {
    this.loading.set(true);
    this.applicationService.getAllAdminTransactions(this.page(), this.pageSize).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.transactions.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  get filteredTransactions(): any[] {
    if (!this.searchQuery.trim()) return this.transactions();
    const q = this.searchQuery.toLowerCase();
    return this.transactions().filter(t =>
      t.referenceNumber?.toLowerCase().includes(q) ||
      t.fromAccountNumber?.includes(q) ||
      t.toAccountNumber?.includes(q)
    );
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p); this.loadTransactions();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  txnIcon(type: string): string {
    const m: Record<string, string> = {
      DEPOSIT: '⬇️', WITHDRAWAL: '⬆️', TRANSFER: '🔄',
      UPI: '📱', NEFT: '🏦', RTGS: '🏦',
    };
    return m[type] ?? '💸';
  }

  getStatusClass(s: string): string {
    const m: Record<string, string> = {
      SUCCESS: 'st-ok', PENDING: 'st-pend', FAILED: 'st-fail', REVERSED: 'st-rev',
    };
    return m[s] ?? 'st-pend';
  }

  formatAmount(v: string): string {
    return '₹' + parseFloat(v || '0').toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }
}