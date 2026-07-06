import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BudgetResponse } from '../core/models/budget.model';
import { BudgetService } from '../services/budget';

@Component({
  selector: 'app-budget-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe],
  templateUrl: './budget-dashboard.html',
  styleUrl: './budget-dashboard.css',
})
export class BudgetDashboardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly budgetService = inject(BudgetService);

  readonly budgets = signal<BudgetResponse[]>([]);
  readonly isLoading = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly isCreating = signal(false);

  readonly categories = ['GROCERIES', 'UTILITIES', 'RENT', 'ENTERTAINMENT', 'TRANSFER', 'OTHER'];

  readonly createForm = this.fb.group({
    category: ['', Validators.required],
    budgetMonth: ['', Validators.required],
    limitAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  private userId: number = 0;

  constructor() {
    this.extractUserId();
    this.loadBudgets();
  }

  private extractUserId(): void {
    const token = sessionStorage.getItem('token');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.userId = payload.userId;
    }
  }

  loadBudgets(): void {
    const month = this.getCurrentMonth();
    this.isLoading.set(true);
    this.budgetService.getSummary(this.userId, month).subscribe({
      next: (res) => {
        this.budgets.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load budgets.');
      },
    });
  }

  onCreateBudget(): void {
    if (this.createForm.invalid) return;
    this.isCreating.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const val = this.createForm.value;
    this.budgetService
      .create({
        category: val.category!,
        budgetMonth: val.budgetMonth!,
        limitAmount: val.limitAmount!,
      })
      .subscribe({
        next: () => {
          this.successMessage.set('Budget created successfully!');
          this.createForm.reset();
          this.isCreating.set(false);
          this.loadBudgets();
        },
        error: (err) => {
          this.isCreating.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create budget.');
        },
      });
  }

  deleteBudget(id: number): void {
    this.budgetService.delete(id).subscribe({
      next: () => {
        this.successMessage.set('Budget deleted.');
        this.loadBudgets();
      },
      error: () => this.errorMessage.set('Failed to delete budget.'),
    });
  }

  getUtilizationColor(percent: number): string {
    if (percent >= 100) return 'red';
    if (percent >= 75) return 'amber';
    return 'green';
  }

  private getCurrentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }
}
