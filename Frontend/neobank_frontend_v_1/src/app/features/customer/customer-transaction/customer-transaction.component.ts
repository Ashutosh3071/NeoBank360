// customer-transaction.component.ts

import {
  Component, Input, OnChanges, SimpleChanges, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

type TransferStep = 'form' | 'confirm' | 'success';

interface UpiErrors {
  fromAccount?: string;
  upiId?:       string;
  amount?:      string;
}
interface NeftErrors {
  fromAccount?:              string;
  beneficiaryAccountNumber?: string;
  beneficiaryName?:          string;
  beneficiaryIfsc?:          string;
  amount?:                   string;
}
interface SelfErrors {
  fromAccount?: string;
  toAccount?:   string;
  amount?:      string;
}

@Component({
  selector:    'app-customer-transaction',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-transaction.component.html',
  styleUrl:    './customer-transaction.component.css',
})
export class CustomerTransactionComponent implements OnChanges {

  @Input() activeSubSection = 'transfer-upi';
  @Input() accounts: any[]  = [];

  // ── Transfer UI ──
  step    = signal<TransferStep>('form');
  loading = signal(false);
  error   = signal('');
  success = signal('');
  lastTxn = signal<any>(null);

  // ── UPI Form ──
  upiForm = {
    fromAccount: '',
    upiId:       '',
    amount:      '',
    description: '',
  };
  upiErrors: UpiErrors = {};
  upiValidating = signal(false);
  upiIdValid    = signal<boolean | null>(null);
  upiIdMsg      = signal('');
  private upiDebounce: any;

  // ── NEFT Form ──
  neftForm = {
    fromAccount:              '',
    beneficiaryAccountNumber: '',
    beneficiaryName:          '',
    beneficiaryIfsc:          '',
    beneficiaryBankName:      '',
    amount:                   '',
    description:              '',
    mode:                     'NEFT',
  };
  neftErrors: NeftErrors = {};

  // ── Self Transfer Form ──
  selfForm = {
    fromAccount: '',
    toAccount:   '',
    amount:      '',
    description: '',
  };
  selfErrors: SelfErrors = {};

  // ── Transaction History ──
  transactions    = signal<any[]>([]);
  txnLoading      = signal(false);
  txnError        = signal('');
  page            = signal(0);
  totalPages      = signal(0);
  totalElements   = signal(0);
  readonly pageSize = 15;

  // ── History Filters ──
  selectedAccount  = '';
  txnTypeFilter    = '';
  txnStatusFilter  = '';

  constructor(private applicationService: ApplicationService) {}

  // ─────────────────────────────────────────────
  //  LIFECYCLE — only ONE ngOnChanges
  // ─────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    // Pre-fill account selectors on first load
    if (this.accounts.length > 0) {
      if (!this.upiForm.fromAccount)  this.upiForm.fromAccount  = this.accounts[0].accountNumber;
      if (!this.neftForm.fromAccount) this.neftForm.fromAccount = this.accounts[0].accountNumber;
      if (!this.selfForm.fromAccount) this.selfForm.fromAccount = this.accounts[0].accountNumber;
      if (!this.selectedAccount)      this.selectedAccount      = this.accounts[0].accountNumber;
    }

    // Load transactions when switching to history tab
    if (this.activeSubSection === 'transactions') {
      this.page.set(0);
      this.loadTransactions();
    }

    // Reset transfer form when switching between transfer tabs
    if (this.activeSubSection !== 'transactions') {
      this.resetToForm();
    }
  }

  // ─────────────────────────────────────────────
  //  COMPUTED GETTERS
  // ─────────────────────────────────────────────

  get sectionTitle(): string {
    const m: Record<string, string> = {
      'transfer-upi':  'UPI Payment',
      'transfer-neft': 'NEFT / RTGS Transfer',
      'transfer-self': 'Self Transfer',
      'transactions':  'Transaction History',
    };
    return m[this.activeSubSection] ?? 'Transfer';
  }

  get selectedFromAccount(): any {
    const accNo =
      this.activeSubSection === 'transfer-upi'  ? this.upiForm.fromAccount
    : this.activeSubSection === 'transfer-neft' ? this.neftForm.fromAccount
    : this.selfForm.fromAccount;
    return this.accounts.find(a => a.accountNumber === accNo);
  }

  get toAccounts(): any[] {
    return this.accounts.filter(
      a => a.accountNumber !== this.selfForm.fromAccount
    );
  }

  // ─────────────────────────────────────────────
  //  TRANSACTION HISTORY — filters + pagination
  // ─────────────────────────────────────────────

  loadTransactions(): void {
    const accountNumber = this.selectedAccount
      || this.accounts[0]?.accountNumber;

    if (!accountNumber) return;

    this.txnLoading.set(true);
    this.txnError.set('');

    this.applicationService
      .getMyTransactions(accountNumber, this.page(), this.pageSize)
      .subscribe({
        next: (res: any) => {
          this.txnLoading.set(false);
          if (res.success && res.data) {
            this.transactions.set(res.data.content   || []);
            this.totalPages.set(res.data.totalPages   || 0);
            this.totalElements.set(res.data.totalElements || 0);
          } else {
            this.txnError.set(res.message || 'Failed to load transactions.');
          }
        },
        error: (err: any) => {
          this.txnLoading.set(false);
          this.txnError.set(
            err.error?.message || 'Failed to load transactions.'
          );
        },
      });
  }

  onAccountChange(): void {
    this.page.set(0);
    this.transactions.set([]);
    this.loadTransactions();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p);
    this.loadTransactions();
  }

  // Smart pagination — show first, last, and ±1 around current
  get pageNumbers(): number[] {
    const total   = this.totalPages();
    const current = this.page();
    const set = new Set<number>();

    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - current) <= 1) {
        set.add(i);
      }
    }
    return [...set].sort((a, b) => a - b);
  }

  // Is this transaction a credit for the selected account?
  isCreditTxn(txn: any): boolean {
    return txn.toAccountNumber === this.selectedAccount;
  }

  // Client-side filter on top of server-side pagination
  get filteredTransactions(): any[] {
    let list = this.transactions();
    if (this.txnTypeFilter) {
      list = list.filter(t => t.transactionType === this.txnTypeFilter);
    }
    if (this.txnStatusFilter) {
      list = list.filter(t => t.status === this.txnStatusFilter);
    }
    return list;
  }

  // ─────────────────────────────────────────────
  //  UPI ID FORMAT CHECK (debounced)
  // ─────────────────────────────────────────────

  onUpiIdChange(): void {
    clearTimeout(this.upiDebounce);
    const id = this.upiForm.upiId.trim();
    this.upiIdValid.set(null);
    this.upiIdMsg.set('');

    if (!id) return;

    const valid = /^[a-zA-Z0-9._\-]+@[a-zA-Z0-9]+$/.test(id);
    if (!valid) {
      this.upiIdValid.set(false);
      this.upiIdMsg.set('Invalid format. Use: name@neobank');
      return;
    }

    this.upiValidating.set(true);
    this.upiDebounce = setTimeout(() => {
      this.upiValidating.set(false);
      this.upiIdValid.set(true);
      this.upiIdMsg.set('Format valid');
    }, 600);
  }

  // ─────────────────────────────────────────────
  //  VALIDATION
  // ─────────────────────────────────────────────

  private validateUpi(): boolean {
    this.upiErrors = {};
    let ok = true;

    if (!this.upiForm.fromAccount) {
      this.upiErrors.fromAccount = 'Select source account'; ok = false;
    }
    if (!this.upiForm.upiId.trim()) {
      this.upiErrors.upiId = 'UPI ID is required'; ok = false;
    } else if (!/^[a-zA-Z0-9._\-]+@[a-zA-Z0-9]+$/.test(this.upiForm.upiId.trim())) {
      this.upiErrors.upiId = 'Invalid UPI ID format'; ok = false;
    }
    if (!this.upiForm.amount || parseFloat(this.upiForm.amount) <= 0) {
      this.upiErrors.amount = 'Enter valid amount'; ok = false;
    } else if (parseFloat(this.upiForm.amount) > 100000) {
      this.upiErrors.amount = 'Max ₹1,00,000 per UPI transaction'; ok = false;
    }
    return ok;
  }

  private validateNeft(): boolean {
    this.neftErrors = {};
    let ok = true;

    if (!this.neftForm.fromAccount) {
      this.neftErrors.fromAccount = 'Select source account'; ok = false;
    }
    if (!this.neftForm.beneficiaryAccountNumber.trim()) {
      this.neftErrors.beneficiaryAccountNumber = 'Account number required'; ok = false;
    }
    if (!this.neftForm.beneficiaryName.trim()) {
      this.neftErrors.beneficiaryName = 'Beneficiary name required'; ok = false;
    }
    if (!this.neftForm.beneficiaryIfsc.trim()) {
      this.neftErrors.beneficiaryIfsc = 'IFSC code required'; ok = false;
    } else if (!/^[A-Z]{4}0[A-Z0-9]{6}$/.test(
        this.neftForm.beneficiaryIfsc.trim().toUpperCase())) {
      this.neftErrors.beneficiaryIfsc = 'Invalid IFSC. Example: SBIN0001234'; ok = false;
    }
    if (!this.neftForm.amount || parseFloat(this.neftForm.amount) <= 0) {
      this.neftErrors.amount = 'Enter valid amount'; ok = false;
    }
    return ok;
  }

  private validateSelf(): boolean {
    this.selfErrors = {};
    let ok = true;

    if (!this.selfForm.fromAccount) {
      this.selfErrors.fromAccount = 'Select source account'; ok = false;
    }
    if (!this.selfForm.toAccount) {
      this.selfErrors.toAccount = 'Select destination account'; ok = false;
    }
    if (this.selfForm.fromAccount === this.selfForm.toAccount
        && this.selfForm.fromAccount) {
      this.selfErrors.toAccount = 'Source and destination cannot be same'; ok = false;
    }
    if (!this.selfForm.amount || parseFloat(this.selfForm.amount) <= 0) {
      this.selfErrors.amount = 'Enter valid amount'; ok = false;
    } else {
      const fromAcc = this.accounts.find(
        a => a.accountNumber === this.selfForm.fromAccount
      );
      if (fromAcc && parseFloat(this.selfForm.amount)
                   > parseFloat(fromAcc.availableBalance)) {
        this.selfErrors.amount =
          `Insufficient balance. Available: ${this.formatAmount(fromAcc.availableBalance)}`;
        ok = false;
      }
    }
    return ok;
  }

  // ─────────────────────────────────────────────
  //  TRANSFER FLOW
  // ─────────────────────────────────────────────

  goToConfirm(): void {
    this.error.set('');
    let valid = false;

    if (this.activeSubSection === 'transfer-upi')  valid = this.validateUpi();
    if (this.activeSubSection === 'transfer-neft') valid = this.validateNeft();
    if (this.activeSubSection === 'transfer-self') valid = this.validateSelf();

    if (valid) this.step.set('confirm');
  }

  submit(): void {
    this.loading.set(true);
    this.error.set('');

    if (this.activeSubSection === 'transfer-upi') {
      this.applicationService.upiTransfer({
        fromAccountNumber: this.upiForm.fromAccount,
        upiId:             this.upiForm.upiId.trim(),
        amount:            this.upiForm.amount,
        description:       this.upiForm.description || 'UPI Payment',
      }).subscribe({
        next:  (res: any) => this.handleSuccess(res),
        error: (err: any) => this.handleError(err),
      });

    } else if (this.activeSubSection === 'transfer-neft') {
      this.applicationService.neftTransfer({
        fromAccountNumber:        this.neftForm.fromAccount,
        beneficiaryAccountNumber: this.neftForm.beneficiaryAccountNumber.trim(),
        beneficiaryName:          this.neftForm.beneficiaryName.trim(),
        beneficiaryIfsc:          this.neftForm.beneficiaryIfsc.trim().toUpperCase(),
        beneficiaryBankName:      this.neftForm.beneficiaryBankName.trim(),
        amount:                   this.neftForm.amount,
        description:              this.neftForm.description || 'NEFT Transfer',
        mode:                     this.neftForm.mode,
      }).subscribe({
        next:  (res: any) => this.handleSuccess(res),
        error: (err: any) => this.handleError(err),
      });

    } else if (this.activeSubSection === 'transfer-self') {
      this.applicationService.selfTransfer({
        fromAccountNumber: this.selfForm.fromAccount,
        toAccountNumber:   this.selfForm.toAccount,
        amount:            this.selfForm.amount,
        description:       this.selfForm.description || 'Self Transfer',
      }).subscribe({
        next:  (res: any) => this.handleSuccess(res),
        error: (err: any) => this.handleError(err),
      });
    }
  }

  private handleSuccess(res: any): void {
    this.loading.set(false);
    if (res.success) {
      this.lastTxn.set(res.data);
      this.step.set('success');
    } else {
      this.error.set(res.message || 'Transfer failed.');
      this.step.set('form');
    }
  }

  private handleError(err: any): void {
    this.loading.set(false);
    this.error.set(err.error?.message || 'Transfer failed. Please try again.');
    this.step.set('form');
  }

  startNew(): void {
    this.resetToForm();
    if (this.activeSubSection === 'transfer-upi') {
      this.upiForm = {
        fromAccount: this.accounts[0]?.accountNumber || '',
        upiId: '', amount: '', description: '',
      };
    } else if (this.activeSubSection === 'transfer-neft') {
      this.neftForm = {
        fromAccount: this.accounts[0]?.accountNumber || '',
        beneficiaryAccountNumber: '', beneficiaryName: '',
        beneficiaryIfsc: '', beneficiaryBankName: '',
        amount: '', description: '', mode: 'NEFT',
      };
    } else {
      this.selfForm = {
        fromAccount: this.accounts[0]?.accountNumber || '',
        toAccount: '', amount: '', description: '',
      };
    }
  }

  // ─────────────────────────────────────────────
  //  HELPERS
  // ─────────────────────────────────────────────

  resetToForm(): void {
    this.step.set('form');
    this.error.set('');
    this.success.set('');
    this.lastTxn.set(null);
    this.upiErrors  = {};
    this.neftErrors = {};
    this.selfErrors = {};
    this.upiIdValid.set(null);
    this.upiIdMsg.set('');
  }

  formatAmount(v: string | number): string {
    return '₹' + parseFloat(String(v || 0))
      .toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  txnIcon(type: string): string {
    const m: Record<string, string> = {
      DEPOSIT:    '⬇️',
      WITHDRAWAL: '⬆️',
      TRANSFER:   '🔄',
      UPI:        '📱',
      NEFT:       '🏦',
      RTGS:       '🏦',
      IMPS:       '⚡',
    };
    return m[type] ?? '💸';
  }
}