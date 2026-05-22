// customer-bills.component.ts

import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RewordBudgetBillServise } from '../../../core/services/reword-budget-billservice';

@Component({
  selector:    'app-customer-bills',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-bills.component.html',
  styleUrl:    './customer-bills.component.css',
})
export class CustomerBillsComponent implements OnInit {

  bills        = signal<any[]>([]);
  loading      = signal(false);
  error        = signal('');
  success      = signal('');
  showCreate   = signal(false);
  createLoading = signal(false);
  createError  = signal('');

  // Create form
  billerName  = '';
  amount      = '';
  dueDate     = '';
  description = '';

  // Filter
  statusFilter = 'ALL';

  constructor(private applicationService: RewordBudgetBillServise) {}

  ngOnInit(): void {
    // Default due date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.dueDate = tomorrow.toISOString().split('T')[0];
    this.loadBills();
  }

  loadBills(): void {
    this.loading.set(true);
    this.applicationService.getMyBills().subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success) this.bills.set(res.data || []);
      },
      error: () => this.loading.set(false),
    });
  }

  get filteredBills(): any[] {
    if (this.statusFilter === 'ALL') return this.bills();
    return this.bills().filter(b => b.status === this.statusFilter);
  }

  get pendingCount():  number { return this.bills().filter(b => b.status === 'PENDING').length; }
  get reminderCount(): number { return this.bills().filter(b => b.remindMe).length; }
  get overdueCount():  number { return this.bills().filter(b => b.status === 'OVERDUE').length; }

  createBill(): void {
    this.createError.set('');
    if (!this.billerName.trim()) { this.createError.set('Biller name required.'); return; }
    if (!this.amount || parseFloat(this.amount) <= 0) { this.createError.set('Enter valid amount.'); return; }
    if (!this.dueDate) { this.createError.set('Due date required.'); return; }

    const today = new Date().toISOString().split('T')[0];
    if (this.dueDate <= today) { this.createError.set('Due date must be a future date.'); return; }

    this.createLoading.set(true);

    this.applicationService.createBill({
      billerName:  this.billerName.trim(),
      amount:      this.amount,
      dueDate:     this.dueDate,
      description: this.description,
    }).subscribe({
      next: (res: any) => {
        this.createLoading.set(false);
        if (res.success) {
          this.success.set('✅ Bill created: ' + this.billerName);
          this.showCreate.set(false);
          this.billerName  = '';
          this.amount      = '';
          this.description = '';
          this.loadBills();
          setTimeout(() => this.success.set(''), 3000);
        } else {
          this.createError.set(res.message || 'Failed.');
        }
      },
      error: (err: any) => {
        this.createLoading.set(false);
        const code = err.error?.errorCode || '';
        if (code === 'BILL_EXISTS') {
          this.createError.set('A bill for this biller already exists this month.');
        } else if (code === 'PAST_DUE_DATE') {
          this.createError.set('Due date must be in the future.');
        } else {
          this.createError.set(err.error?.message || 'Failed.');
        }
      },
    });
  }

  markAsPaid(bill: any): void {
    if (!confirm(`Mark "${bill.billerName}" as PAID?`)) return;
    this.applicationService.updateBillStatus(bill.id, 'PAID').subscribe({
      next: (res: any) => {
        if (res.success) {
          this.success.set(`✅ ${bill.billerName} marked as PAID. +10 reward points!`);
          this.loadBills();
          setTimeout(() => this.success.set(''), 4000);
        }
      },
      error: (err: any) => this.error.set(err.error?.message || 'Failed.'),
    });
  }

  markAsOverdue(bill: any): void {
    this.applicationService.updateBillStatus(bill.id, 'OVERDUE').subscribe({
      next: (res: any) => { if (res.success) this.loadBills(); },
      error: () => {},
    });
  }

  deleteBill(bill: any): void {
    if (!confirm(`Delete bill "${bill.billerName}"?`)) return;
    this.applicationService.deleteBill(bill.id).subscribe({
      next: (res: any) => {
        if (res.success) { this.success.set('Bill deleted.'); this.loadBills(); }
      },
      error: (err: any) => this.error.set(err.error?.message || 'Failed.'),
    });
  }

  getStatusClass(status: string): string {
    const m: Record<string, string> = {
      PENDING: 'st-pend', PAID: 'st-ok', OVERDUE: 'st-err',
    };
    return m[status] ?? 'st-pend';
  }

  formatAmount(v: string): string {
    return '₹' + parseFloat(v || '0').toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }
}