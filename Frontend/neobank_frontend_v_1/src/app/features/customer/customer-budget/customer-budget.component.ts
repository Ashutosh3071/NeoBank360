// customer-budget.component.ts

import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RewordBudgetBillServise } from '../../../core/services/reword-budget-billservice';

@Component({
  selector:    'app-customer-budget',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-budget.component.html',
  styleUrl:    './customer-budget.component.css',
})
export class CustomerBudgetComponent implements OnInit {

  @Input() user: any = null;

  // ── UI ──
  loading  = signal(false);
  error    = signal('');
  success  = signal('');
  activeTab = signal<'dashboard' | 'create' | 'list'>('dashboard');

  // ── Summary ──
  summary      = signal<any>(null);
  summaryLoad  = signal(false);
  selectedMonth = '';

  // ── Create Form ──
  createCategory   = '';
  createMonth      = '';
  createLimit      = '';
  createError      = signal('');
  createLoading    = signal(false);

  // ── My Budgets List ──
  budgets      = signal<any[]>([]);
  budgetsLoad  = signal(false);

  readonly categories = [
    { value: 'GROCERIES',     label: '🛒 Groceries',     color: '#22c55e' },
    { value: 'UTILITIES',     label: '⚡ Utilities',      color: '#f59e0b' },
    { value: 'RENT',          label: '🏠 Rent',           color: '#6366f1' },
    { value: 'ENTERTAINMENT', label: '🎬 Entertainment',  color: '#ec4899' },
    { value: 'TRANSFER',      label: '🔄 Transfer',       color: '#3b82f6' },
    { value: 'OTHER',         label: '📦 Other',          color: '#94a3b8' },
  ];

  constructor(private servise: RewordBudgetBillServise) {}

  ngOnInit(): void {
    // Default to current month
    const now = new Date();
    this.selectedMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    this.createMonth   = this.selectedMonth;
    this.loadSummary();
    this.loadBudgets();
  }

  // ─────────────────────────────────────────────
  //  LOAD SUMMARY
  // ─────────────────────────────────────────────

  loadSummary(): void {
    if (!this.user?.userId) return;
    this.summaryLoad.set(true);

    this.servise
      .getBudgetSummary(this.user.userId, this.selectedMonth)
      .subscribe({
        next: (res: any) => {
          this.summaryLoad.set(false);
          if (res.success) this.summary.set(res.data);
        },
        error: () => this.summaryLoad.set(false),
      });
  }

  onMonthChange(): void { this.loadSummary(); }

  // ─────────────────────────────────────────────
  //  LOAD MY BUDGETS
  // ─────────────────────────────────────────────

  loadBudgets(): void {
    this.budgetsLoad.set(true);
    this.servise.getMyBudgets().subscribe({
      next: (res: any) => {
        this.budgetsLoad.set(false);
        if (res.success) this.budgets.set(res.data || []);
      },
      error: () => this.budgetsLoad.set(false),
    });
  }

  // ─────────────────────────────────────────────
  //  CREATE BUDGET
  // ─────────────────────────────────────────────

  createBudget(): void {
    this.createError.set('');
    if (!this.createCategory) { this.createError.set('Select a category.'); return; }
    if (!this.createMonth)    { this.createError.set('Select a month.'); return; }
    if (!this.createLimit || parseFloat(this.createLimit) <= 0) {
      this.createError.set('Enter a valid limit amount.'); return;
    }

    this.createLoading.set(true);

    this.servise.createBudget({
      category:    this.createCategory,
      budgetMonth: this.createMonth,
      limitAmount: this.createLimit,
    }).subscribe({
      next: (res: any) => {
        this.createLoading.set(false);
        if (res.success) {
          this.success.set('✅ Budget created for ' + this.createCategory);
          this.createCategory = '';
          this.createLimit    = '';
          this.loadBudgets();
          this.loadSummary();
          this.activeTab.set('dashboard');
          setTimeout(() => this.success.set(''), 3000);
        } else {
          this.createError.set(res.message || 'Failed.');
        }
      },
      error: (err: any) => {
        this.createLoading.set(false);
        const code = err.error?.errorCode || '';
        if (code === 'BUDGET_EXISTS') {
          this.createError.set('A budget for this category and month already exists.');
        } else {
          this.createError.set(err.error?.message || 'Failed to create budget.');
        }
      },
    });
  }

  // ─────────────────────────────────────────────
  //  DELETE BUDGET
  // ─────────────────────────────────────────────

  deleteBudget(id: number, category: string): void {
    if (!confirm(`Delete budget for ${category}?`)) return;
    this.servise.deleteBudget(id).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.success.set('Budget deleted.');
          this.loadBudgets();
          this.loadSummary();
          setTimeout(() => this.success.set(''), 3000);
        }
      },
      error: () => {},
    });
  }

  // ─────────────────────────────────────────────
  //  HELPERS
  // ─────────────────────────────────────────────

  getCategoryColor(cat: string): string {
    return this.categories.find(c => c.value === cat)?.color || '#94a3b8';
  }

  getCategoryLabel(cat: string): string {
    return this.categories.find(c => c.value === cat)?.label || cat;
  }

  getStatusClass(status: string): string {
    const m: Record<string, string> = {
      SAFE: 'bar-safe', WARNING: 'bar-warn', EXCEEDED: 'bar-exceeded',
    };
    return m[status] ?? 'bar-safe';
  }

  getBarWidth(utilization: number): number {
    return Math.min(utilization, 100);
  }

  formatAmount(v: string | number): string {
    return '₹' + parseFloat(String(v || 0))
      .toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  get monthOptions(): { value: string; label: string }[] {
    const options = [];
    const now = new Date();
    for (let i = 0; i < 12; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const label = d.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
      options.push({ value, label });
    }
    return options;
  }

  getRemainingAmount(cat: any): string {
  const spent = parseFloat(cat.spentAmount) || 0;
  const limit = parseFloat(cat.limitAmount) || 0;
  return this.formatAmount((spent - limit).toString());
}
}