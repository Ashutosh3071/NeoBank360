import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BillResponse } from '../core/models/bill.model';
import { BillService } from '../services/bill';
import { AccountService } from '../services/account';
import { Account } from '../core/models/account.model';

type BillFilter = 'ALL' | 'PENDING' | 'PAID' | 'OVERDUE';
type BillSort = 'dueDate' | 'amount' | 'billerName';

@Component({
  selector: 'app-bills',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './bills.html',
  styleUrl: './bills.css',
})
export class BillsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly billService = inject(BillService);
  private readonly accountService = inject(AccountService);

  readonly bills = signal<BillResponse[]>([]);
  readonly userAccounts = signal<Account[]>([]);
  readonly selectedAccounts = signal<Record<number, number>>({});
  readonly isLoading = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly isCreating = signal(false);
  readonly showCreateForm = signal(false);

  readonly activeFilter = signal<BillFilter>('ALL');
  readonly activeSort = signal<BillSort>('dueDate');
  readonly sortAsc = signal(true);
  readonly searchQuery = signal('');

  readonly stats = computed(() => {
    const all = this.bills();
    const pending = all.filter((b) => b.status === 'PENDING');
    const paid = all.filter((b) => b.status === 'PAID');
    const overdue = all.filter((b) => b.status === 'OVERDUE');
    return {
      total: all.length,
      pending: pending.length,
      paid: paid.length,
      overdue: overdue.length,
      totalPending: pending.reduce((sum, b) => sum + b.amount, 0),
      totalPaid: paid.reduce((sum, b) => sum + b.amount, 0),
    };
  });

  readonly filteredBills = computed(() => {
    let result = this.bills();
    const filter = this.activeFilter();
    const query = this.searchQuery().toLowerCase();

    if (filter !== 'ALL') {
      result = result.filter((b) => b.status === filter);
    }
    if (query) {
      result = result.filter((b) => b.billerName.toLowerCase().includes(query));
    }

    const sort = this.activeSort();
    const asc = this.sortAsc();
    return [...result].sort((a, b) => {
      let cmp = 0;
      if (sort === 'dueDate') cmp = a.dueDate.localeCompare(b.dueDate);
      else if (sort === 'amount') cmp = a.amount - b.amount;
      else cmp = a.billerName.localeCompare(b.billerName);
      return asc ? cmp : -cmp;
    });
  });

  readonly createForm = this.fb.group({
    billerName: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    category: ['OTHER', Validators.required],
    dueDate: ['', Validators.required],
  });

  constructor() {
    this.loadBills();
    this.loadAccounts();
  }

  loadBills(): void {
    this.isLoading.set(true);
    this.billService.getAll().subscribe({
      next: (res) => {
        this.bills.set(res);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load bills.');
      },
    });
  }

  loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        const active = res.filter((acc) => acc.accountStatus === 'APPROVED' && acc.isActive);
        this.userAccounts.set(active);
      },
      error: (err: any) => console.error('Failed to load accounts for bill payment', err),
    });
  }

  selectAccountForBill(billId: number, accountId: number): void {
    this.selectedAccounts.update((prev) => ({
      ...prev,
      [billId]: accountId,
    }));
  }

  onCreateBill(): void {
    if (this.createForm.invalid) return;
    this.isCreating.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const val = this.createForm.value;
    this.billService
      .create({
        billerName: val.billerName!,
        amount: val.amount!,
        dueDate: val.dueDate!,
        category: val.category!,
      })
      .subscribe({
        next: () => {
          this.successMessage.set('Bill created successfully!');
          this.createForm.reset();
          this.isCreating.set(false);
          this.showCreateForm.set(false);
          this.loadBills();
          this.autoClear();
        },
        error: (err: any) => {
          this.isCreating.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create bill.');
        },
      });
  }

  markAsPaid(id: number): void {
    const accountId = this.selectedAccounts()[id];
    if (!accountId) {
      this.errorMessage.set('Please select an account to pay the bill from.');
      return;
    }

    const bill = this.bills().find((b) => b.id === id);
    const wasPending = bill?.status === 'PENDING';

    this.errorMessage.set('');
    this.billService.updateStatus(id, { status: 'PAID', accountId }).subscribe({
      next: (res: any) => {
        const pts = res.pointsEarned;
        if (pts !== undefined && pts !== null) {
          if (pts > 0) {
            this.successMessage.set(`Bill marked as paid! +${pts} reward points earned 🎉`);
          } else {
            this.successMessage.set('Bill marked as paid successfully! (No reward points earned for overdue bill)');
          }
        } else {
          if (wasPending) {
            this.successMessage.set('Bill marked as paid! +100 reward points earned 🎉');
          } else {
            this.successMessage.set('Bill marked as paid successfully!');
          }
        }
        this.loadBills();
        this.loadAccounts();
        this.autoClear();
      },
      error: (err: any) => this.errorMessage.set(err.error?.message || 'Failed to update.'),
    });
  }

  markAsOverdue(id: number): void {
    this.billService.updateStatus(id, { status: 'OVERDUE' }).subscribe({
      next: () => {
        this.successMessage.set('Bill marked as overdue.');
        this.loadBills();
        this.autoClear();
      },
      error: (err: any) => this.errorMessage.set(err.error?.message || 'Failed to update.'),
    });
  }

  deleteBill(id: number): void {
    if (!confirm('Delete this bill?')) return;
    this.billService.delete(id).subscribe({
      next: () => {
        this.successMessage.set('Bill deleted.');
        this.loadBills();
        this.autoClear();
      },
      error: (err: any) => this.errorMessage.set(err.error?.message || 'Failed to delete.'),
    });
  }

  setFilter(f: BillFilter): void {
    this.activeFilter.set(f);
  }

  setSort(s: BillSort): void {
    if (this.activeSort() === s) this.sortAsc.set(!this.sortAsc());
    else {
      this.activeSort.set(s);
      this.sortAsc.set(true);
    }
  }

  onSearch(e: Event): void {
    this.searchQuery.set((e.target as HTMLInputElement).value);
  }

  private autoClear(): void {
    setTimeout(() => this.successMessage.set(''), 4000);
  }
}
