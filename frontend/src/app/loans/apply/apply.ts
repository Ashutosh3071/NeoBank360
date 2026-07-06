import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LoanService } from '../../services/loan';
import { LoanProduct, LoanApplication } from '../../core/models/loan.model';
import { AccountService } from '../../services/account';
import { Account } from '../../core/models/account.model';

@Component({
  selector: 'app-apply-loan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './apply.html',
  styleUrl: './apply.css',
})
export class ApplyLoanComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly accountService = inject(AccountService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // Signals
  readonly userAccounts = signal<Account[]>([]);
  readonly products = signal<LoanProduct[]>([]);
  readonly selectedProduct = signal<LoanProduct | null>(null);
  readonly currentStep = signal<number>(1);
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly successMessage = signal<string>('');
  readonly errorMessage = signal<string>('');
  readonly requestedAmount = signal<number>(0);
  readonly requestedTenureMonths = signal<number>(0);

  // Form Group
  loanForm!: FormGroup;

  // Parsed Tenures of selected product
  readonly allowedTenuresList = computed(() => {
    const product = this.selectedProduct();
    if (!product || !product.allowedTenures) return [];
    return product.allowedTenures.split(',').map(t => parseInt(t.trim(), 10)).filter(t => !isNaN(t));
  });

  // Estimated EMI
  readonly estimatedEmi = computed(() => {
    const product = this.selectedProduct();
    if (!product) return 0;
    const amount = this.requestedAmount();
    const tenure = this.requestedTenureMonths();
    if (amount <= 0 || tenure <= 0) return 0;

    const annualRate = product.annualInterestRate;
    const monthlyRate = annualRate / 12 / 100;
    if (monthlyRate === 0) return amount / tenure;
    
    const emi = (amount * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                (Math.pow(1 + monthlyRate, tenure) - 1);
    return Math.round((emi + Number.EPSILON) * 100) / 100;
  });

  // Selected Disbursement Account
  readonly selectedAccount = computed(() => {
    const id = this.loanForm?.get('disbursementAccountId')?.value;
    if (!id) return null;
    return this.userAccounts().find(a => a.id === +id) || null;
  });

  ngOnInit(): void {
    this.initForm();
    this.loadProducts();
    this.loadAccounts();
  }

  private initForm(): void {
    this.loanForm = this.fb.group({
      loanProductId: ['', Validators.required],
      requestedAmount: [null, [Validators.required, Validators.min(1)]],
      requestedTenureMonths: ['', Validators.required],
      disbursementAccountId: ['', Validators.required],
    });

    // Sync form values to signals for computed properties reactivity
    this.loanForm.valueChanges.subscribe(val => {
      this.requestedAmount.set(val.requestedAmount ? +val.requestedAmount : 0);
      this.requestedTenureMonths.set(val.requestedTenureMonths ? +val.requestedTenureMonths : 0);
    });

    // Listen to product selection change
    this.loanForm.get('loanProductId')?.valueChanges.subscribe(productId => {
      const prod = this.products().find(p => p.id === +productId) || null;
      this.selectedProduct.set(prod);
      
      if (prod) {
        // Reset amount and tenure to default valid values or clear
        const amountCtrl = this.loanForm.get('requestedAmount');
        amountCtrl?.setValidators([
          Validators.required,
          Validators.min(prod.minAmount),
          Validators.max(prod.maxAmount)
        ]);
        amountCtrl?.updateValueAndValidity();

        const tenureCtrl = this.loanForm.get('requestedTenureMonths');
        tenureCtrl?.setValue('');
      }
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.loanService.getProducts().subscribe({
      next: (res) => {
        this.products.set(res);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to fetch loan products.');
        this.isLoading.set(false);
      }
    });
  }

  loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        // Only show active, approved accounts
        this.userAccounts.set(res.filter(a => a.isActive && a.accountStatus === 'APPROVED'));
      },
      error: () => {
        // Non-fatal: user may not have accounts yet
        this.userAccounts.set([]);
      }
    });
  }

  selectProductCard(product: LoanProduct): void {
    this.loanForm.get('loanProductId')?.setValue(product.id);
    this.goToStep(2);
  }

  goToStep(step: number): void {
    this.errorMessage.set('');
    
    if (step === 2) {
      if (!this.selectedProduct()) {
        this.errorMessage.set('Please select a loan product first.');
        return;
      }
    }
    
    if (step === 3) {
      if (this.loanForm.invalid) {
        this.errorMessage.set('Please complete all form fields with valid values.');
        this.loanForm.markAllAsTouched();
        return;
      }
    }

    this.currentStep.set(step);
  }

  onSubmit(): void {
    if (this.loanForm.invalid) {
      this.errorMessage.set('Please correct the validation errors first.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.isSubmitting.set(true);

    const payload = {
      loanProductId: +this.loanForm.value.loanProductId,
      requestedAmount: this.loanForm.value.requestedAmount,
      requestedTenureMonths: +this.loanForm.value.requestedTenureMonths,
      disbursementAccountId: +this.loanForm.value.disbursementAccountId
    };

    this.loanService.applyLoan(payload).subscribe({
      next: (res) => {
        this.successMessage.set(`🎉 Loan application of ${res.requestedAmount} submitted successfully! Status: PENDING.`);
        this.isSubmitting.set(false);
        this.loanForm.reset();
        this.selectedProduct.set(null);
        setTimeout(() => {
          this.router.navigate(['/loans/my-loans']);
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to submit loan application.');
        this.isSubmitting.set(false);
      }
    });
  }
}
