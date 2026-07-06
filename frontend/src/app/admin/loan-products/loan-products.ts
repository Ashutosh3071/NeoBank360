import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../services/loan';
import { LoanProduct } from '../../core/models/loan.model';

@Component({
  selector: 'app-admin-loan-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './loan-products.html',
  styleUrl: './loan-products.css',
})
export class AdminLoanProductsComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly fb = inject(FormBuilder);

  // State Signals
  readonly products = signal<LoanProduct[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly successMessage = signal<string>('');
  readonly errorMessage = signal<string>('');

  productForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.loadProducts();
  }

  private initForm(): void {
    this.productForm = this.fb.group({
      productName: ['', [Validators.required, Validators.minLength(3)]],
      minAmount: [null, [Validators.required, Validators.min(1000)]],
      maxAmount: [null, [Validators.required, Validators.min(1000)]],
      annualInterestRate: [null, [Validators.required, Validators.min(0.01), Validators.max(100)]],
      allowedTenures: ['', [Validators.required, Validators.pattern(/^\d+(,\s*\d+)*$/)]], // comma separated digits
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

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.errorMessage.set('Please fix the validation errors in the form.');
      return;
    }

    const minAmt = this.productForm.value.minAmount;
    const maxAmt = this.productForm.value.maxAmount;
    if (minAmt >= maxAmt) {
      this.errorMessage.set('Max Amount must be strictly greater than Min Amount.');
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.isSaving.set(true);

    const payload: Partial<LoanProduct> = {
      productName: this.productForm.value.productName,
      minAmount: minAmt,
      maxAmount: maxAmt,
      annualInterestRate: this.productForm.value.annualInterestRate,
      allowedTenures: this.productForm.value.allowedTenures.replace(/\s+/g, '') // clean spaces
    };

    this.loanService.createProduct(payload).subscribe({
      next: (res) => {
        this.successMessage.set(`🎉 Loan product "${res.productName}" created successfully!`);
        this.isSaving.set(false);
        this.productForm.reset({
          productName: '',
          minAmount: null,
          maxAmount: null,
          annualInterestRate: null,
          allowedTenures: ''
        });
        this.loadProducts();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to create loan product.');
        this.isSaving.set(false);
      }
    });
  }
}
