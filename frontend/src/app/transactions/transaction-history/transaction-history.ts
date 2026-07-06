import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import {
  CreateTransactionRequest,
  Transaction,
  TransactionType,
} from '../../core/models/transaction.model';
import { TransactionService } from '../../services/transaction';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './transaction-history.html',
  styleUrl: './transaction-history.css',
})
export class TransactionHistoryComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly transactionService = inject(TransactionService);

  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');

  readonly accountId = signal<number>(0);
  readonly transactions = signal<Transaction[]>([]);

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);

  readonly filterType = signal<'ALL' | TransactionType>('ALL');
  readonly sortDirection = signal<'desc' | 'asc'>('desc');

  readonly transactionForm = this.fb.nonNullable.group({
    transactionType: ['CREDIT' as TransactionType, [Validators.required]],
    amount: [0.01, [Validators.required, Validators.min(0.01)]],
    description: [''],
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.accountId.set(id);
    this.loadTransactions();
  }

  get transactionType() {
    return this.transactionForm.get('transactionType');
  }

  get amount() {
    return this.transactionForm.get('amount');
  }

  get description() {
    return this.transactionForm.get('description');
  }

  readonly filteredAndSortedTransactions = computed(() => {
    let data = [...this.transactions()];

    if (this.filterType() !== 'ALL') {
      data = data.filter((tx) => tx.transactionType === this.filterType());
    }

    data.sort((a, b) => {
      const aTime = new Date(a.transactionDate).getTime();
      const bTime = new Date(b.transactionDate).getTime();

      return this.sortDirection() === 'asc' ? aTime - bTime : bTime - aTime;
    });

    return data;
  });

  loadTransactions(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.transactionService.getTransactions(this.accountId(), this.page(), this.size()).subscribe({
      next: (response) => {
        this.transactions.set(response.content ?? []);
        this.totalPages.set(response.totalPages ?? 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load transactions.');
        this.isLoading.set(false);
      },
    });
  }

  onSubmit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.transactionForm.invalid) {
      this.transactionForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const payload: CreateTransactionRequest = {
      transactionType: this.transactionForm.getRawValue().transactionType,
      amount: this.transactionForm.getRawValue().amount,
      description: this.transactionForm.getRawValue().description || '',
    };

    this.transactionService.createTransaction(this.accountId(), payload).subscribe({
      next: () => {
        this.successMessage.set('Transaction completed successfully.');
        this.isSubmitting.set(false);

        this.transactionForm.reset({
          transactionType: 'CREDIT',
          amount: 0.01,
          description: '',
        });

        this.loadTransactions();
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Failed to process transaction.');
        this.isSubmitting.set(false);
      },
    });
  }

  setFilter(type: 'ALL' | TransactionType): void {
    this.filterType.set(type);
  }

  setSort(direction: 'asc' | 'desc'): void {
    this.sortDirection.set(direction);
  }

  goToPreviousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadTransactions();
    }
  }

  goToNextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.loadTransactions();
    }
  }
}
