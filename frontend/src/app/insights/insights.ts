import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { AfterViewInit, Component, ElementRef, inject, signal, viewChild, OnInit, OnDestroy } from '@angular/core';
import { Chart, registerables } from 'chart.js';

import { Auth } from '../services/auth';
import { InsightsService } from '../services/insights';
import { AnalyticsService } from '../services/analytics';
import { BudgetService } from '../services/budget';
import { FinancialInsights } from '../core/models/insights.model';
import { BudgetResponse } from '../core/models/budget.model';

Chart.register(...registerables);

@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe],
  templateUrl: './insights.html',
  styleUrl: './insights.css',
})
export class InsightsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly insightsService = inject(InsightsService);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly budgetService = inject(BudgetService);
  private readonly auth = inject(Auth);

  // Sub-tabs navigation
  readonly activeSubTab = signal<'overview' | 'spending' | 'wealth'>('overview');

  // Chart Canvas Elements
  readonly trendChartRef = viewChild<ElementRef<HTMLCanvasElement>>('trendChart');
  readonly rewardsChartRef = viewChild<ElementRef<HTMLCanvasElement>>('rewardsChart');
  readonly spendingDoughnutChartRef = viewChild<ElementRef<HTMLCanvasElement>>('spendingDoughnutChart');
  readonly budgetBarChartRef = viewChild<ElementRef<HTMLCanvasElement>>('budgetBarChart');
  readonly netWorthChartRef = viewChild<ElementRef<HTMLCanvasElement>>('netWorthChart');

  // Chart instances for garbage collection
  private trendChartInstance: Chart | null = null;
  private rewardsChartInstance: Chart | null = null;
  private spendingDoughnutChartInstance: Chart | null = null;
  private budgetBarChartInstance: Chart | null = null;
  private netWorthChartInstance: Chart | null = null;

  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  // Loaded Data Signals
  readonly insights = signal<FinancialInsights | null>(null);
  readonly spendingData = signal<any[]>([]);
  readonly wealthData = signal<any | null>(null);
  readonly budgets = signal<BudgetResponse[]>([]);

  ngOnInit(): void {
    this.loadAll();
  }

  ngOnDestroy(): void {
    this.destroyAllCharts();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.renderAllActiveCharts(), 600);
  }

  setActiveSubTab(tab: 'overview' | 'spending' | 'wealth'): void {
    this.activeSubTab.set(tab);
    setTimeout(() => this.renderAllActiveCharts(), 100);
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const decoded = this.auth.getDecodedToken();
    const userId = decoded?.['userId'];

    if (!userId) {
      this.errorMessage.set('Failed to resolve authenticated user.');
      this.isLoading.set(false);
      return;
    }

    // Determine current month in YYYY-MM format
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const currentMonthStr = `${year}-${month}`;

    // 1. Load Overview Metrics
    this.insightsService.getInsights(userId).subscribe({
      next: (res) => {
        this.insights.set(res);
        if (this.activeSubTab() === 'overview') {
          setTimeout(() => this.renderTrendChart(), 100);
        }
      },
      error: (err) => this.errorMessage.set(err.error?.message || 'Failed to load general insights.')
    });

    // 2. Load Spending Analytics (doughnut categories)
    this.analyticsService.getSpendingAnalytics(userId).subscribe({
      next: (res) => {
        this.spendingData.set(res || []);
        if (this.activeSubTab() === 'spending') {
          setTimeout(() => this.renderSpendingDoughnut(), 100);
        }
      },
      error: (err) => console.error('Failed to load spending categories analytics', err)
    });

    // 3. Load Wealth timelines and Forecasts
    this.analyticsService.getWealthAnalytics(userId).subscribe({
      next: (res) => {
        this.wealthData.set(res);
        if (this.activeSubTab() === 'wealth') {
          setTimeout(() => this.renderNetWorthChart(), 100);
        }
        if (this.activeSubTab() === 'overview') {
          setTimeout(() => this.renderRewardsChart(), 100);
        }
      },
      error: (err) => console.error('Failed to load wealth analytics timeline', err)
    });

    // 4. Load Budget Limits vs Actual
    this.budgetService.getSummary(userId, currentMonthStr).subscribe({
      next: (res) => {
        this.budgets.set(res || []);
        this.isLoading.set(false);
        if (this.activeSubTab() === 'spending') {
          setTimeout(() => this.renderBudgetBar(), 100);
        }
      },
      error: (err) => {
        console.error('Failed to load budget details', err);
        this.isLoading.set(false);
      }
    });
  }

  private renderAllActiveCharts(): void {
    const tab = this.activeSubTab();
    if (tab === 'overview') {
      this.renderTrendChart();
      this.renderRewardsChart();
    } else if (tab === 'spending') {
      this.renderSpendingDoughnut();
      this.renderBudgetBar();
    } else if (tab === 'wealth') {
      this.renderNetWorthChart();
    }
  }

  private destroyAllCharts(): void {
    if (this.trendChartInstance) this.trendChartInstance.destroy();
    if (this.rewardsChartInstance) this.rewardsChartInstance.destroy();
    if (this.spendingDoughnutChartInstance) this.spendingDoughnutChartInstance.destroy();
    if (this.budgetBarChartInstance) this.budgetBarChartInstance.destroy();
    if (this.netWorthChartInstance) this.netWorthChartInstance.destroy();
  }

  // ==========================================
  // CHART RENDERERS
  // ==========================================

  private renderTrendChart(): void {
    const canvas = this.trendChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.trendChartInstance) this.trendChartInstance.destroy();

    const data = this.insights();
    if (!data || !data.trendSummary || data.trendSummary.length === 0) return;

    const labels = data.trendSummary.map(entry => entry.monthLabel);
    const trendIncome = data.trendSummary.map(entry => entry.totalIncome);
    const trendExpense = data.trendSummary.map(entry => entry.totalExpense);

    this.trendChartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Income',
            data: trendIncome,
            backgroundColor: '#34d399',
            borderRadius: 4,
          },
          {
            label: 'Expense',
            data: trendExpense,
            backgroundColor: '#f87171',
            borderRadius: 4,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#8892a8',
              font: { family: 'Outfit, Inter, sans-serif', size: 12, weight: 'bold' }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#8892a8', font: { family: 'Outfit, Inter, sans-serif' } }
          },
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#8892a8', font: { family: 'Outfit, Inter, sans-serif' } }
          }
        }
      }
    });
  }

  private renderRewardsChart(): void {
    const canvas = this.rewardsChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.rewardsChartInstance) this.rewardsChartInstance.destroy();

    const data = this.wealthData();
    if (!data || !data.rewardAccrualHistory || data.rewardAccrualHistory.length === 0) return;

    const labels = data.rewardAccrualHistory.map((h: any) => h.month);
    const points = data.rewardAccrualHistory.map((h: any) => h.points);

    this.rewardsChartInstance = new Chart(canvas, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Reward Points Balance',
          data: points,
          borderColor: '#fbbf24',
          backgroundColor: 'rgba(251, 191, 36, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.3,
          pointBackgroundColor: '#fbbf24',
          pointBorderColor: '#0b0f19',
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#8892a8', font: { family: 'Outfit, Inter, sans-serif' } }
          },
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#8892a8', font: { family: 'Outfit, Inter, sans-serif' } }
          }
        }
      }
    });
  }

  private renderSpendingDoughnut(): void {
    const canvas = this.spendingDoughnutChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.spendingDoughnutChartInstance) this.spendingDoughnutChartInstance.destroy();

    const history = this.spendingData();
    if (history.length === 0) return;

    // Grab current month spending (last index)
    const currentMonthData = history[history.length - 1];
    if (!currentMonthData || !currentMonthData.spending) return;

    const rawMap = currentMonthData.spending;
    const categories: string[] = [];
    const values: number[] = [];

    // Filter out categories with zero spending
    Object.keys(rawMap).forEach(key => {
      const val = Number(rawMap[key]);
      if (val > 0) {
        // Format category name for UX
        const name = key.charAt(0).toUpperCase() + key.slice(1).toLowerCase();
        categories.push(name);
        values.push(val);
      }
    });

    const categoryColors = {
      'Groceries': '#10b981',
      'Utilities': '#3b82f6',
      'Rent': '#8b5cf6',
      'Entertainment': '#ec4899',
      'Transfer': '#f59e0b',
      'Other': '#6b7280'
    };
    const colors = categories.map(cat => (categoryColors as any)[cat] || '#38bdf8');

    this.spendingDoughnutChartInstance = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: categories.length > 0 ? categories : ['No Expenses'],
        datasets: [{
          data: values.length > 0 ? values : [0],
          backgroundColor: colors.length > 0 ? colors : ['rgba(255,255,255,0.05)'],
          borderWidth: 2,
          borderColor: '#0f172a'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#8892a8',
              font: { family: 'Outfit, Inter, sans-serif', size: 12, weight: 'bold' }
            }
          }
        }
      }
    });
  }

  private renderBudgetBar(): void {
    const canvas = this.budgetBarChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.budgetBarChartInstance) this.budgetBarChartInstance.destroy();

    const budgetsList = this.budgets();
    if (budgetsList.length === 0) return;

    const categories = budgetsList.map(b => b.category.charAt(0).toUpperCase() + b.category.slice(1).toLowerCase());
    const spentData = budgetsList.map(b => b.spent);
    const remainingData = budgetsList.map(b => Math.max(0, b.limitAmount - b.spent));

    this.budgetBarChartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: categories,
        datasets: [
          {
            label: 'Spent (₹)',
            data: spentData,
            backgroundColor: '#a78bfa',
            borderRadius: 4
          },
          {
            label: 'Remaining Budget (₹)',
            data: remainingData,
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            borderColor: 'rgba(255, 255, 255, 0.1)',
            borderWidth: 1,
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#8892a8' }
          }
        },
        scales: {
          x: {
            stacked: true,
            grid: { display: false },
            ticks: { color: '#8892a8' }
          },
          y: {
            stacked: true,
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#8892a8' }
          }
        }
      }
    });
  }

  private renderNetWorthChart(): void {
    const canvas = this.netWorthChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.netWorthChartInstance) this.netWorthChartInstance.destroy();

    const data = this.wealthData();
    if (!data || !data.netWorthTimeline || data.netWorthTimeline.length === 0) return;

    const labels = data.netWorthTimeline.map((n: any) => n.month);
    const assets = data.netWorthTimeline.map((n: any) => n.assets);
    const liabilities = data.netWorthTimeline.map((n: any) => n.liabilities);
    const netWorth = data.netWorthTimeline.map((n: any) => n.netWorth);

    this.netWorthChartInstance = new Chart(canvas, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Net Worth',
            data: netWorth,
            borderColor: '#00d4ff',
            backgroundColor: 'rgba(0, 212, 255, 0.1)',
            fill: true,
            tension: 0.3,
            borderWidth: 3,
            pointBackgroundColor: '#00d4ff'
          },
          {
            label: 'Liquid Assets (Balances)',
            data: assets,
            borderColor: '#10b981',
            backgroundColor: 'transparent',
            tension: 0.3,
            borderWidth: 2,
            borderDash: [5, 5],
            pointBackgroundColor: '#10b981'
          },
          {
            label: 'Liabilities (Outstanding Loans)',
            data: liabilities,
            borderColor: '#ef4444',
            backgroundColor: 'transparent',
            tension: 0.3,
            borderWidth: 2,
            borderDash: [5, 5],
            pointBackgroundColor: '#ef4444'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#8892a8' }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#8892a8' }
          },
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#8892a8' }
          }
        }
      }
    });
  }
}
