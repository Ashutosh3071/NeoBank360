import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login';
import { RegisterComponent } from './auth/register/register';
import { ProfileComponent } from './user/profile/profile';
import { DashboardComponent } from './dashboard/dashboard';
import { TransactionHistoryComponent } from './transactions/transaction-history/transaction-history';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard';
import { BudgetDashboardComponent } from './budget/budget-dashboard';
import { BillsComponent } from './bills/bills';
import { RewardsComponent } from './rewards/rewards';
import { AuthGuard } from './guards/auth-guard';
import { AdminGuard } from './guards/admin-guard';
import { HomeComponent } from './pages/home/home';
import { GuestGuard } from './guards/guest-guard';
import { ContactComponent } from './pages/contact/contact';

// Sprint 3 Loan Components
import { ApplyLoanComponent } from './loans/apply/apply';
import { MyLoansComponent } from './loans/my-loans/my-loans';
import { RepaymentScheduleComponent } from './loans/repayment-schedule/repayment-schedule';
import { AdminLoanProductsComponent } from './admin/loan-products/loan-products';
import { AdminLoanDecisionsComponent } from './admin/loan-decisions/loan-decisions';

// Sprint 4 Insights Component
import { InsightsDashboardComponent } from './insights/insights';

export const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [GuestGuard] },
  { path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [GuestGuard] },
  { path: 'contact', component: ContactComponent },

  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'insights', component: InsightsDashboardComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'budget', component: BudgetDashboardComponent, canActivate: [AuthGuard] },
  { path: 'bills', component: BillsComponent, canActivate: [AuthGuard] },
  { path: 'rewards', component: RewardsComponent, canActivate: [AuthGuard] },
  {
    path: 'accounts/:id/transactions',
    component: TransactionHistoryComponent,
    canActivate: [AuthGuard],
  },
  
  // CUSTOMER LOANS
  { path: 'loans/apply', component: ApplyLoanComponent, canActivate: [AuthGuard] },
  { path: 'loans/my-loans', component: MyLoansComponent, canActivate: [AuthGuard] },
  { path: 'loans/:id/repayments', component: RepaymentScheduleComponent, canActivate: [AuthGuard] },

  // ADMIN PANEL & LOANS
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [AuthGuard, AdminGuard],
  },
  {
    path: 'admin/loan-products',
    component: AdminLoanProductsComponent,
    canActivate: [AuthGuard, AdminGuard],
  },
  {
    path: 'admin/loan-decisions',
    component: AdminLoanDecisionsComponent,
    canActivate: [AuthGuard, AdminGuard],
  },

  { path: '**', redirectTo: '' },
];
