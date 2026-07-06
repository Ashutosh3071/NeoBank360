import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LoanService } from '../../services/loan';
import { LoanRepayment } from '../../core/models/loan.model';
import { AccountService } from '../../services/account';
import { Account } from '../../core/models/account.model';

@Component({
  selector: 'app-repayment-schedule',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './repayment-schedule.html',
  styleUrl: './repayment-schedule.css',
})
export class RepaymentScheduleComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly accountService = inject(AccountService);
  private readonly route = inject(ActivatedRoute);

  // Route Params
  readonly loanAccountId = signal<number | null>(null);

  // Schedule Data
  readonly repayments = signal<LoanRepayment[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);

  // Filters & State
  readonly activeStatusFilter = signal<string>(''); // empty string means ALL
  readonly isLoading = signal<boolean>(false);
  readonly isPaying = signal<number | null>(null); // holds repayment ID being paid
  readonly successMessage = signal<string>('');
  readonly errorMessage = signal<string>('');

  // Modal & Account State
  readonly showAccountModal = signal<boolean>(false);
  readonly userAccounts = signal<Account[]>([]);
  readonly selectedAccountId = signal<number | null>(null);
  readonly pendingRepaymentId = signal<number | null>(null);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idStr = params.get('id');
      if (idStr) {
        this.loanAccountId.set(+idStr);
        this.loadRepayments();
      }
    });
  }

  loadRepayments(page: number = 0): void {
    const accId = this.loanAccountId();
    if (!accId) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    const filter = this.activeStatusFilter();

    this.loanService.getRepaymentsForAccount(accId, filter || undefined, page, this.pageSize()).subscribe({
      next: (res) => {
        this.repayments.set(res.content || []);
        this.totalElements.set(res.totalElements || 0);
        this.totalPages.set(res.totalPages || 0);
        this.currentPage.set(res.number || 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to fetch repayment schedule.');
        this.isLoading.set(false);
      }
    });
  }

  setFilter(status: string): void {
    this.activeStatusFilter.set(status);
    this.loadRepayments(0);
  }

  payInstallment(repaymentId: number): void {
    const accId = this.loanAccountId();
    if (!accId) return;

    this.errorMessage.set('');
    this.successMessage.set('');
    this.isLoading.set(true);

    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.isLoading.set(false);
        const activeAccounts = accounts.filter(acc => acc.isActive && acc.accountStatus === 'APPROVED');
        if (activeAccounts.length === 0) {
          this.errorMessage.set('You do not have any active, approved financial accounts to pay from. Please create one first.');
          return;
        }
        this.userAccounts.set(activeAccounts);
        this.selectedAccountId.set(activeAccounts[0].id);
        this.pendingRepaymentId.set(repaymentId);
        this.showAccountModal.set(true);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to fetch your financial accounts for debit.');
      }
    });
  }

  confirmInstallmentPayment(): void {
    const repaymentId = this.pendingRepaymentId();
    const accountId = this.selectedAccountId();
    const loanAccountId = this.loanAccountId();

    if (!repaymentId || !accountId || !loanAccountId) {
      this.closeAccountModal();
      return;
    }

    this.showAccountModal.set(false);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.isPaying.set(repaymentId);

    this.loanService.payInstallment(loanAccountId, repaymentId, accountId).subscribe({
      next: (res) => {
        this.successMessage.set(`🎉 Installment #${res.instalmentNumber} of ${res.emiAmount} paid successfully!`);
        this.isPaying.set(null);
        this.pendingRepaymentId.set(null);
        this.loadRepayments(this.currentPage());
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Repayment processing failed.');
        this.isPaying.set(null);
        this.pendingRepaymentId.set(null);
      }
    });
  }

  closeAccountModal(): void {
    this.showAccountModal.set(false);
    this.pendingRepaymentId.set(null);
  }

  changePage(pageOffset: number): void {
    const targetPage = this.currentPage() + pageOffset;
    if (targetPage >= 0 && targetPage < this.totalPages()) {
      this.loadRepayments(targetPage);
    }
  }

  formatAccountNumber(accountId: number | null): string {
    if (!accountId) return '';
    return 'LN-' + accountId.toString().padStart(6, '0');
  }
}
