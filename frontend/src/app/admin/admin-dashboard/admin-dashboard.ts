import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  signal,
  viewChild,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';

import { AdminService } from '../../services/admin';
import { Auth } from '../../services/auth';
import {
  AdminAccount,
  AdminUser,
  PendingApproval,
  SystemHealth,
  UserActivity,
} from '../../core/models/admin.model';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, CurrencyPipe],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);
  private readonly auth = inject(Auth);

  readonly accountChartRef = viewChild<ElementRef<HTMLCanvasElement>>('accountChart');
  private chartInstance: Chart | null = null;
  private autoRefreshInterval: any = null;

  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly activeTab = signal<
    'approvals' | 'accounts' | 'users' | 'transactions' | 'health' | 'analytics' | 'logs'
  >('approvals');

  // Analytics
  readonly transactionChartRef = viewChild<ElementRef<HTMLCanvasElement>>('transactionChart');
  readonly loanChartRef = viewChild<ElementRef<HTMLCanvasElement>>('loanChart');
  private transactionChartInstance: Chart | null = null;
  private loanChartInstance: Chart | null = null;
  readonly transactionTimeframe = signal<'7d' | '30d' | 'YTD'>('7d');
  readonly loanTimeframe = signal<'7d' | '30d' | 'YTD'>('7d');

  // Logs
  readonly logsErrorChartRef = viewChild<ElementRef<HTMLCanvasElement>>('logsErrorChart');
  readonly logsLatencyChartRef = viewChild<ElementRef<HTMLCanvasElement>>('logsLatencyChart');
  private logsErrorChartInstance: Chart | null = null;
  private logsLatencyChartInstance: Chart | null = null;
  readonly logs = signal<any[]>([]);
  readonly logsFrom = signal<string>('');
  readonly logsTo = signal<string>('');
  readonly logsStatus = signal<number | null>(null);
  readonly logsPage = signal(0);
  readonly logsPageSize = signal(10);
  readonly totalLogsCount = signal(0);
  readonly totalLogsPages = signal(0);
  // KYC Review modal state
  readonly showKycModal = signal(false);
  readonly kycReviewAccount = signal<any | null>(null);
  readonly isLoadingKyc = signal(false);

  // ✅ Admin Credit modal state
  readonly showAdminCreditModal = signal(false);
  readonly adminCreditAccount = signal<any | null>(null);
  readonly adminCreditAmount = signal<number>(0);
  readonly adminCreditDescription = signal<string>('');
  readonly isAdminCrediting = signal(false);

  // Stats & Health
  readonly stats = signal<any | null>(null);
  readonly health = signal<SystemHealth | null>(null);

  // Paginated Users List
  readonly users = signal<AdminUser[]>([]);
  readonly usersPage = signal(0);
  readonly usersPageSize = signal(5);
  readonly totalUsersCount = signal(0);
  readonly totalUsersPages = signal(0);

  // Combined Approvals
  readonly combinedApprovals = signal<PendingApproval[]>([]);
  readonly approvalModuleFilter = signal<string>('');

  // Legacy Accounts list
  readonly accounts = signal<AdminAccount[]>([]);
  readonly recentTransactions = signal<any[]>([]);

  // User status toggle confirmation modal state
  readonly showConfirmModal = signal(false);
  readonly userToToggle = signal<AdminUser | null>(null);

  // User activity modal state
  readonly showActivityModal = signal(false);
  readonly selectedUser = signal<AdminUser | null>(null);
  readonly selectedUserActivity = signal<UserActivity | null>(null);
  readonly isLoadingActivity = signal(false);

  ngOnInit(): void {
    this.loadAll();
    // 60 seconds auto-refresh polling
    this.autoRefreshInterval = setInterval(() => {
      this.loadAll();
    }, 60000);
  }

  ngOnDestroy(): void {
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
    }
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    // 1. Dashboard statistics
    this.adminService.getDashboardStats().subscribe({
      next: (res) => this.stats.set(res),
      error: () => this.errorMessage.set('Failed to load dashboard metrics.'),
    });

    // 2. Paginated Users
    this.loadUsers();

    // 3. Pending Approvals
    this.loadPendingApprovals();

    // 4. Accounts list (Legacy tab support)
    this.adminService.getAccounts().subscribe({
      next: (res) => {
        this.accounts.set(res);
        this.isLoading.set(false);
        setTimeout(() => this.renderAccountChart(), 100);
      },
      error: () => {
        this.errorMessage.set('Failed to load accounts.');
        this.isLoading.set(false);
      },
    });

    // 5. Recent transactions
    this.adminService.getRecentTransactions(10).subscribe({
      next: (res) => this.recentTransactions.set(res),
      error: () => this.errorMessage.set('Failed to load transactions.'),
    });

    // 6. System health monitoring
    this.adminService.getSystemHealth().subscribe({
      next: (res) => this.health.set(res),
      error: () => console.error('Failed to load system health.'),
    });
  }

  setActiveTab(
    tab: 'approvals' | 'accounts' | 'users' | 'transactions' | 'health' | 'analytics' | 'logs',
  ): void {
    this.activeTab.set(tab);
    if (tab === 'accounts') {
      setTimeout(() => this.renderAccountChart(), 100);
    } else if (tab === 'analytics') {
      setTimeout(() => {
        this.loadTransactionAnalytics();
        this.loadLoanAnalytics();
      }, 100);
    } else if (tab === 'logs') {
      this.loadSystemLogs();
    }
  }

  // Load paginated users
  loadUsers(): void {
    this.adminService.getUsersPaginated(this.usersPage(), this.usersPageSize()).subscribe({
      next: (res) => {
        if (res && res.content) {
          this.users.set(res.content);
          this.totalUsersCount.set(res.totalElements);
          this.totalUsersPages.set(res.totalPages);
        } else {
          // fallback to list if not paginated on backend
          this.users.set(Array.isArray(res) ? res : []);
          this.totalUsersPages.set(1);
        }
      },
      error: () => this.errorMessage.set('Failed to load users list.'),
    });
  }

  nextUsersPage(): void {
    if (this.usersPage() < this.totalUsersPages() - 1) {
      this.usersPage.update((p) => p + 1);
      this.loadUsers();
    }
  }

  prevUsersPage(): void {
    if (this.usersPage() > 0) {
      this.usersPage.update((p) => p - 1);
      this.loadUsers();
    }
  }

  // Load approvals with filter
  loadPendingApprovals(): void {
    const filter = this.approvalModuleFilter();
    this.adminService.getPendingApprovals(filter || undefined).subscribe({
      next: (res) => this.combinedApprovals.set(res),
      error: () => this.errorMessage.set('Failed to load pending approvals.'),
    });
  }

  setApprovalFilter(module: string): void {
    this.approvalModuleFilter.set(module);
    this.loadPendingApprovals();
  }

  // =======================
  // Action approval / reject handlers
  // =======================

  approveAccount(accountId: number): void {
    this.adminService.approveAccount(accountId).subscribe({
      next: () => {
        this.successMessage.set('Account approved successfully.');
        this.loadAll();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: () => this.errorMessage.set('Failed to approve account.'),
    });
  }

  rejectAccount(accountId: number): void {
    this.adminService.rejectAccount(accountId).subscribe({
      next: () => {
        this.successMessage.set('Account rejected.');
        this.loadAll();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: () => this.errorMessage.set('Failed to reject account.'),
    });
  }

  reviewApproval(approval: PendingApproval): void {
    if (approval.type === 'LOAN_APPLICATION') {
      this.router.navigate(['/admin/loan-decisions']);
    } else {
      // Fetch full account details with KYC before showing modal
      this.isLoadingKyc.set(true);
      this.showKycModal.set(true);
      this.kycReviewAccount.set(null);

      this.adminService.getAccountDetail(approval.id).subscribe({
        next: (res) => {
          this.kycReviewAccount.set(res);
          this.isLoadingKyc.set(false);
        },
        error: () => {
          this.errorMessage.set('Failed to load account details.');
          this.isLoadingKyc.set(false);
          this.showKycModal.set(false);
        },
      });
    }
  }

  closeKycModal(): void {
    this.showKycModal.set(false);
    this.kycReviewAccount.set(null);
  }

  confirmApproveFromKyc(): void {
    const acc = this.kycReviewAccount();
    if (!acc) return;
    this.approveAccount(acc['id']);
    this.closeKycModal();
  }

  confirmRejectFromKyc(): void {
    const acc = this.kycReviewAccount();
    if (!acc) return;
    this.rejectAccount(acc['id']);
    this.closeKycModal();
  }

  // =======================
  // Admin Credit Modal
  // =======================

  openAdminCreditModal(account: any): void {
    this.adminCreditAccount.set(account);
    this.adminCreditAmount.set(0);
    this.adminCreditDescription.set('');
    this.showAdminCreditModal.set(true);
  }

  closeAdminCreditModal(): void {
    this.showAdminCreditModal.set(false);
    this.adminCreditAccount.set(null);
  }

  confirmAdminCredit(): void {
    const account = this.adminCreditAccount();
    if (!account) return;

    const amount = this.adminCreditAmount();
    if (!amount || amount <= 0) {
      this.errorMessage.set('Please enter a valid amount.');
      return;
    }

    this.isAdminCrediting.set(true);
    this.errorMessage.set('');

    this.adminService
      .adminCreditAccount(account['id'], amount, this.adminCreditDescription() || 'Admin credit')
      .subscribe({
        next: () => {
          this.successMessage.set(
            `₹${amount} credited to account ${account['accountNumber']} successfully.`,
          );
          this.isAdminCrediting.set(false);
          this.closeAdminCreditModal();
          this.loadAll();
          setTimeout(() => this.successMessage.set(''), 4000);
        },
        error: (err: any) => {
          this.errorMessage.set(err?.error?.message || 'Failed to credit account.');
          this.isAdminCrediting.set(false);
        },
      });
  }

  rejectApproval(approval: PendingApproval): void {
    if (approval.type === 'LOAN_APPLICATION') {
      this.router.navigate(['/admin/loan-decisions']);
    } else {
      this.rejectAccount(approval.id);
    }
  }

  // =======================
  // User Management Toggles
  // =======================

  openToggleStatusConfirm(user: AdminUser): void {
    this.userToToggle.set(user);
    this.showConfirmModal.set(true);
  }

  closeConfirmModal(): void {
    this.showConfirmModal.set(false);
    this.userToToggle.set(null);
  }

  confirmToggleStatus(): void {
    const user = this.userToToggle();
    if (!user) return;

    this.adminService.updateUserStatus(user.id, !user.isActive).subscribe({
      next: () => {
        this.successMessage.set(`User status updated for ${user.fullName}.`);
        this.closeConfirmModal();
        this.loadAll();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || 'Failed to update user status.');
        this.closeConfirmModal();
        setTimeout(() => this.errorMessage.set(''), 4000);
      },
    });
  }

  // =======================
  // User Activity Summary
  // =======================

  openUserActivity(user: AdminUser): void {
    this.selectedUser.set(user);
    this.selectedUserActivity.set(null);
    this.showActivityModal.set(true);
    this.isLoadingActivity.set(true);

    this.adminService.getUserActivity(user.id).subscribe({
      next: (res) => {
        this.selectedUserActivity.set(res);
        this.isLoadingActivity.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to fetch user activities.');
        this.isLoadingActivity.set(false);
      },
    });
  }

  closeActivityModal(): void {
    this.showActivityModal.set(false);
    this.selectedUser.set(null);
    this.selectedUserActivity.set(null);
  }

  // =======================
  // Render visual chart
  // =======================

  ngAfterViewInit(): void {
    setTimeout(() => this.renderAccountChart(), 500);
  }

  private renderAccountChart(): void {
    const canvas = this.accountChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.chartInstance) this.chartInstance.destroy();

    const accs = this.accounts();
    const approved = accs.filter((a) => a.accountStatus === 'APPROVED').length;
    const pending = accs.filter((a) => a.accountStatus === 'PENDING_APPROVAL').length;
    const rejected = accs.filter((a) => a.accountStatus === 'REJECTED').length;

    this.chartInstance = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Approved', 'Pending', 'Rejected'],
        datasets: [
          {
            data: [approved, pending, rejected],
            backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
          },
        ],
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#8892a8',
              font: { family: 'Outfit, Inter, sans-serif', size: 12, weight: 'bold' },
            },
          },
        },
      },
    });
  }

  formatUptime(seconds: number | undefined): string {
    if (seconds === undefined) return '0s';
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs}h ${mins}m ${secs}s`;
  }

  getCurrentUserEmail(): string | null {
    return this.auth.getUserEmail();
  }

  // =======================
  // SPRINT 5 NEW ANALYTICS & LOGS METHODS
  // =======================

  loadTransactionAnalytics(): void {
    this.adminService.getTransactionAnalytics(this.transactionTimeframe()).subscribe({
      next: (data) => {
        this.renderTransactionChart(data);
      },
      error: () => this.errorMessage.set('Failed to load transaction analytics.'),
    });
  }

  setTransactionTimeframe(tf: '7d' | '30d' | 'YTD'): void {
    this.transactionTimeframe.set(tf);
    this.loadTransactionAnalytics();
  }

  loadLoanAnalytics(): void {
    this.adminService.getLoanAnalytics(this.loanTimeframe()).subscribe({
      next: (data) => {
        this.renderLoanChart(data);
      },
      error: () => this.errorMessage.set('Failed to load loan analytics.'),
    });
  }

  setLoanTimeframe(tf: '7d' | '30d' | 'YTD'): void {
    this.loanTimeframe.set(tf);
    this.loadLoanAnalytics();
  }

  loadSystemLogs(): void {
    this.isLoading.set(true);
    this.adminService
      .getSystemLogs(
        this.logsFrom() || undefined,
        this.logsTo() || undefined,
        this.logsStatus() ?? undefined,
        this.logsPage(),
        this.logsPageSize(),
      )
      .subscribe({
        next: (res) => {
          this.logs.set(res.content || []);
          this.totalLogsCount.set(res.totalElements || 0);
          this.totalLogsPages.set(res.totalPages || 0);
          this.isLoading.set(false);
          setTimeout(() => this.renderLogsCharts(res.content || []), 100);
        },
        error: () => {
          this.errorMessage.set('Failed to load system logs.');
          this.isLoading.set(false);
        },
      });
  }

  filterLogs(): void {
    this.logsPage.set(0);
    this.loadSystemLogs();
  }

  clearLogsFilter(): void {
    this.logsFrom.set('');
    this.logsTo.set('');
    this.logsStatus.set(null);
    this.logsPage.set(0);
    this.loadSystemLogs();
  }

  nextLogsPage(): void {
    if (this.logsPage() < this.totalLogsPages() - 1) {
      this.logsPage.update((p) => p + 1);
      this.loadSystemLogs();
    }
  }

  prevLogsPage(): void {
    if (this.logsPage() > 0) {
      this.logsPage.update((p) => p - 1);
      this.loadSystemLogs();
    }
  }

  private renderTransactionChart(data: any): void {
    const canvas = this.transactionChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.transactionChartInstance) this.transactionChartInstance.destroy();

    const labels = data.dailyVolumes.map((v: any) => v.date);
    const volumes = data.dailyVolumes.map((v: any) => v.volume);

    this.transactionChartInstance = new Chart(canvas, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Transaction Volume (₹)',
            data: volumes,
            borderColor: '#00d4ff',
            backgroundColor: 'rgba(0, 212, 255, 0.1)',
            fill: true,
            tension: 0.3,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
        },
        scales: {
          x: { ticks: { color: '#8892a8' }, grid: { display: false } },
          y: { ticks: { color: '#8892a8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
        },
      },
    });
  }

  private renderLoanChart(data: any): void {
    const canvas = this.loanChartRef()?.nativeElement;
    if (!canvas) return;
    if (this.loanChartInstance) this.loanChartInstance.destroy();

    const statuses = ['PENDING', 'APPROVED', 'REJECTED'];
    const productsSet = new Set<string>();
    statuses.forEach((status) => {
      const prodMap = data.loanDistribution[status] || {};
      Object.keys(prodMap).forEach((k) => productsSet.add(k));
    });
    const products = Array.from(productsSet);

    const datasets = statuses.map((status, index) => {
      const colors = ['#f59e0b', '#10b981', '#ef4444'];
      return {
        label: status,
        data: products.map((prod) => (data.loanDistribution[status] || {})[prod] || 0),
        backgroundColor: colors[index],
      };
    });

    this.loanChartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: products.length > 0 ? products : ['No Loans'],
        datasets: datasets,
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#8892a8' } },
        },
        scales: {
          x: { stacked: true, ticks: { color: '#8892a8' }, grid: { display: false } },
          y: {
            stacked: true,
            ticks: { color: '#8892a8' },
            grid: { color: 'rgba(255,255,255,0.05)' },
          },
        },
      },
    });
  }

  private renderLogsCharts(logsList: any[]): void {
    const errorCanvas = this.logsErrorChartRef()?.nativeElement;
    const latencyCanvas = this.logsLatencyChartRef()?.nativeElement;

    if (errorCanvas) {
      if (this.logsErrorChartInstance) this.logsErrorChartInstance.destroy();
      const total = logsList.length;
      const errors = logsList.filter((l) => l.responseStatus >= 400).length;
      const success = total - errors;

      this.logsErrorChartInstance = new Chart(errorCanvas, {
        type: 'doughnut',
        data: {
          labels: ['Success', 'Errors'],
          datasets: [
            {
              data: [success, errors],
              backgroundColor: ['#10b981', '#ef4444'],
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { color: '#8892a8' } },
          },
        },
      });
    }

    if (latencyCanvas) {
      if (this.logsLatencyChartInstance) this.logsLatencyChartInstance.destroy();
      const recentLogs = [...logsList].reverse();
      const labels = recentLogs.map((_, i) => `#${i + 1}`);
      const latencies = recentLogs.map((l) => l.executionTimeMs);

      this.logsLatencyChartInstance = new Chart(latencyCanvas, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Latency (ms)',
              data: latencies,
              borderColor: '#a78bfa',
              backgroundColor: 'rgba(167, 139, 250, 0.1)',
              fill: true,
              tension: 0.4,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
          },
          scales: {
            x: { display: false },
            y: { ticks: { color: '#8892a8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
          },
        },
      });
    }
  }
}
