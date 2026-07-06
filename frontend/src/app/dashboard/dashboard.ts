import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
  AfterViewInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';

import { Account, AccountType, CreateAccountRequest } from '../core/models/account.model';
import { TransferRequest } from '../core/models/transfer.model';
import { AccountService } from '../services/account';
import { Auth } from '../services/auth';
import { TransferService } from '../services/transfer';
import { TransactionService } from '../services/transaction';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);
  private readonly transactionService = inject(TransactionService);
  private readonly auth = inject(Auth);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAdmin = signal(this.auth.isAdmin());

  readonly balanceChartRef = viewChild<ElementRef>('balanceChart');
  private balanceChart: Chart | null = null;

  readonly isLoading = signal(false);
  readonly isCreating = signal(false);
  readonly isTransferring = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');

  // ✅ Debit modal state (user clicks on account card)
  readonly showDebitModal = signal(false);
  readonly selectedAccountForDebit = signal<Account | null>(null);
  readonly isDebiting = signal(false);
  readonly debitAmount = signal<number | null>(null);
  readonly debitDescription = signal<string>('');

  readonly accounts = signal<Account[]>([]);

  readonly createAccountForm = this.fb.nonNullable.group({
    accountType: ['SAVINGS' as AccountType, [Validators.required]],
  });

  readonly transferForm = this.fb.nonNullable.group({
    sourceAccountId: [0, [Validators.required, Validators.min(1)]],
    destinationAccountNumber: ['', [Validators.required]],
    amount: [0.01, [Validators.required, Validators.min(0.01)]],
    description: [''],
  });

  constructor() {
    this.loadAccounts();

    this.accountService.accounts$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((accounts) => this.accounts.set(accounts));
  }

  ngAfterViewInit(): void {
    // Charts will render after accounts load
  }

  renderBalanceChart(): void {
    const canvas = this.balanceChartRef()?.nativeElement;
    if (!canvas) return;

    if (this.balanceChart) {
      this.balanceChart.destroy();
    }

    const approvedAccounts = this.accounts().filter((a) => a.accountStatus === 'APPROVED');
    if (approvedAccounts.length === 0) return;

    this.balanceChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: approvedAccounts.map((a) => `${a.accountType} (${a.accountNumber.slice(-6)})`),
        datasets: [
          {
            data: approvedAccounts.map((a) => a.balance),
            backgroundColor: ['#0ea5e9', '#6366f1', '#10b981', '#f59e0b', '#ef4444'],
            borderWidth: 0,
            hoverOffset: 8,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { padding: 16, usePointStyle: true, font: { size: 12 } },
          },
        },
      },
    });
  }

  // =======================
  // Helpers (Approval logic)
  // =======================

  isAccountApproved(account: Account): boolean {
    return account.accountStatus === 'APPROVED' && account.isActive === true;
  }

  getSelectedSourceAccount(): Account | undefined {
    const id = this.transferForm.getRawValue().sourceAccountId;
    return this.accounts().find((acc) => acc.id === Number(id));
  }

  canTransfer(): boolean {
    const acc = this.getSelectedSourceAccount();
    return !!acc && this.isAccountApproved(acc);
  }

  // =======================
  // Load accounts
  // =======================

  loadAccounts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.accountService
      .getMyAccounts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
          this.isLoading.set(false);

          if (accounts.length === 1) {
            this.transferForm.patchValue({
              sourceAccountId: accounts[0].id,
            });
          }

          setTimeout(() => this.renderBalanceChart(), 100);
        },
        error: (err) => {
          this.errorMessage.set(err?.error?.message || 'Failed to load accounts.');
          this.isLoading.set(false);
        },
      });
  }

  // =======================
  // Create account
  // =======================

  onCreateAccount(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.createAccountForm.invalid) {
      this.createAccountForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);

    const payload: CreateAccountRequest = {
      accountType: this.createAccountForm.getRawValue().accountType,
    };

    this.accountService
      .createAccount(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set('Bank account created successfully. Awaiting admin approval.');
          this.isCreating.set(false);
          this.createAccountForm.reset({ accountType: 'SAVINGS' });
          this.loadAccounts();
        },
        error: (err) => {
          this.errorMessage.set(err?.error?.message || 'Failed to create account.');
          this.isCreating.set(false);
        },
      });
  }

  // =======================
  // Debit Modal (user clicks account)
  // =======================

  openDebitModal(account: Account): void {
    if (!this.isAccountApproved(account)) return;
    this.errorMessage.set('');
    this.successMessage.set('');
    this.selectedAccountForDebit.set(account);
    this.debitAmount.set(null);
    this.debitDescription.set('');
    this.showDebitModal.set(true);
  }

  closeDebitModal(): void {
    this.showDebitModal.set(false);
    this.selectedAccountForDebit.set(null);
    this.errorMessage.set('');
  }

  updateDebitAmount(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    if (val === '') {
      this.debitAmount.set(null);
    } else {
      this.debitAmount.set(Number(val));
    }
  }

  onDebit(): void {
    const account = this.selectedAccountForDebit();
    if (!account) return;

    const amount = this.debitAmount();
    if (amount === null || isNaN(amount) || amount <= 0) {
      this.errorMessage.set('Please enter a valid amount.');
      return;
    }
    if (amount > account.balance) {
      this.errorMessage.set('Insufficient balance.');
      return;
    }

    this.isDebiting.set(true);
    this.errorMessage.set('');

    this.transactionService
      .createTransaction(account.id, {
        transactionType: 'DEBIT',
        amount,
        description: this.debitDescription() || 'Withdrawal',
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set(
            `₹${amount} debited from account ${account.accountNumber} successfully.`,
          );
          this.isDebiting.set(false);
          this.closeDebitModal();
          this.loadAccounts();
          setTimeout(() => this.successMessage.set(''), 4000);
        },
        error: (err: any) => {
          this.errorMessage.set(err?.error?.message || 'Failed to process debit.');
          this.isDebiting.set(false);
        },
      });
  }

  // =======================
  // Transfer money
  // =======================

  onTransferMoney(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }

    const selectedAccount = this.getSelectedSourceAccount();

    if (!selectedAccount) {
      this.errorMessage.set('Please select a source account.');
      return;
    }

    if (!this.isAccountApproved(selectedAccount)) {
      this.errorMessage.set('This account is pending admin approval. Transfers are disabled.');
      return;
    }

    const raw = this.transferForm.getRawValue();

    const payload: TransferRequest = {
      sourceAccountId: Number(raw.sourceAccountId),
      destinationAccountNumber: raw.destinationAccountNumber.trim(),
      amount: Number(raw.amount),
      description: raw.description?.trim() || '',
    };

    if (selectedAccount.accountNumber === payload.destinationAccountNumber) {
      this.errorMessage.set('Source and destination accounts must be different.');
      return;
    }

    this.isTransferring.set(true);

    this.transferService
      .transferMoney(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: any) => {
          const pts = res.pointsEarned;
          if (pts !== undefined && pts !== null && pts > 0) {
            this.successMessage.set(`Money transferred successfully! +${pts} reward points earned 🎉`);
          } else {
            this.successMessage.set('Money transferred successfully.');
          }
          this.isTransferring.set(false);
          this.transferForm.reset({
            sourceAccountId: 0,
            destinationAccountNumber: '',
            amount: 0.01,
            description: '',
          });
          this.loadAccounts();
        },
        error: (err) => {
          this.errorMessage.set(err?.error?.message || 'Failed to transfer money.');
          this.isTransferring.set(false);
        },
      });
  }
}
