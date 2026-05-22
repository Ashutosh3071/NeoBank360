// admin-layout.component.ts

import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar.component';
import { AdminHeaderComponent } from '../admin-header/admin-header.component';
import { AdminApplicationComponent } from '../admin-application/admin-application.component';
// import { AdminUsersComponent } from '../admin-users/admin-users.component';  ← add when ready

import { AdminUserComponent }        from '../admin-user/admin-user.component';
import { AdminAccountComponent } from '../admin-accounts/admin-accounts.component';
import { AdminTransactionComponent } from '../admin-transactions/admin-transactions.component';
import { AdminProfileComponent }     from '../admin-profile/admin-profile.component';

import { AdminAccountRequestComponent } from '../admin-account-request/admin-account-request.component';



interface StoredUser {
  userId:   number;
  username: string;
  email:    string;
  fullName: string | null;
  role:     string;
  token:    string;
}

@Component({
  selector:    'app-admin-layout',
  standalone:  true,
  imports: [
    CommonModule,
    AdminSidebarComponent,
    AdminHeaderComponent,
    AdminAccountRequestComponent,
    AdminApplicationComponent,
    // AdminUsersComponent,   ← add when ready
    AdminUserComponent,
AdminAccountComponent,
AdminTransactionComponent,
AdminProfileComponent,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl:    './admin-layout.component.css',
})
export class AdminLayoutComponent implements OnInit {

  adminUser        = signal<StoredUser | null>(null);
  sidebarOpen      = signal(true);
  sidebarCollapsed = signal(false);
  isDarkMode       = signal(false);
  currentSection   = signal('dashboard');
  error            = signal('');
  success          = signal('');
  pendingCount     = signal(0);
  isMobile         = signal(window.innerWidth < 1024);

  constructor(
    private themeService: ThemeService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.isDarkMode.set(this.themeService.isDark());
    if (window.innerWidth >= 1024) this.sidebarOpen.set(true);
    window.addEventListener('resize', () => {
      this.isMobile.set(window.innerWidth < 1024);
      if (window.innerWidth >= 1024) this.sidebarOpen.set(true);
    });
  }

  private loadUser(): void {
    try {
      const json = localStorage.getItem('user');
      if (json) this.adminUser.set(JSON.parse(json));
    } catch {}
  }

  toggleSidebar(): void {
    if (this.isMobile()) this.sidebarOpen.update(v => !v);
    else this.sidebarCollapsed.update(v => !v);
  }
  toggleSidebarOpenCollapsed(): void{
    // alert(this.sidebarCollapsed());
    if(this.sidebarCollapsed()){
       this.sidebarCollapsed.update(v => !v);
    }
  }

  closeSidebar(): void { this.sidebarOpen.set(false); }

  onSectionSelected(sectionId: string): void {
    this.currentSection.set(sectionId);
    this.error.set('');
    this.success.set('');
  }

  cycleTheme(): void {
    this.themeService.cycle();
    this.isDarkMode.set(this.themeService.isDark());
  }

  getPageTitle(): string {
    const map: Record<string, string> = {
      'dashboard':              'Dashboard',
      'applications-all':      'All Applications',
      'applications-pending':  'Pending Review',
      'applications-approved': 'Approved Applications',
      'applications-rejected': 'Rejected Applications',
      'users-all':             'All Users',
      'users-active':          'Active Users',
      'users-locked':          'Locked Users',
      'accounts-all':          'All Accounts',
      'accounts-active':       'Active Accounts',
      'accounts-frozen':       'Frozen Accounts',
      'transactions':          'Transactions',
      'analytics':             'Analytics',
      'management-admins':     'Manage Admins',
      'management-settings':   'Settings',
      'account-requests': 'Open Account Requests',
      
    };
    return map[this.currentSection()] ?? 'Dashboard';
  }

  // Check if current section belongs to a component group
  isApplicationSection(): boolean {
    return this.currentSection().startsWith('applications');
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }


// Add helper methods:
isUsersSection():       boolean { return this.currentSection().startsWith('users'); }
isAccountsSection():    boolean { return this.currentSection().startsWith('accounts'); }
isTransactionSection(): boolean { return this.currentSection() === 'transactions'; }
isProfileSection():     boolean { return this.currentSection().startsWith('admin-profile'); }
isAccountRequestSection():     boolean { return this.currentSection().startsWith('account-requests'); }
}













// // Add imports:
// import { AdminUserComponent }        from '../admin-user/admin-user.component';
// import { AdminAccountComponent }     from '../admin-account/admin-account.component';
// import { AdminTransactionComponent } from '../admin-transaction/admin-transaction.component';
// import { AdminProfileComponent }     from '../admin-profile/admin-profile.component';

// // Add to imports array:
// AdminUserComponent,
// AdminAccountComponent,
// AdminTransactionComponent,
// AdminProfileComponent,

// // Add helper methods:
// isUsersSection():       boolean { return this.currentSection().startsWith('users'); }
// isAccountsSection():    boolean { return this.currentSection().startsWith('accounts'); }
// isTransactionSection(): boolean { return this.currentSection() === 'transactions'; }
// isProfileSection():     boolean { return this.currentSection().startsWith('admin-profile'); }