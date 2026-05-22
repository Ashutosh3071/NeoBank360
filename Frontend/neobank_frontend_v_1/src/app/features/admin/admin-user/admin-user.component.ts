// admin-user.component.ts

import {
  Component, Input, OnInit, OnChanges,
  SimpleChanges, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector:    'app-admin-user',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-user.component.html',
  styleUrl:    './admin-user.component.css',
})
export class AdminUserComponent implements OnInit, OnChanges {

  @Input() activeSubSection = 'users-all';

  users        = signal<any[]>([]);
  loading      = signal(false);
  error        = signal('');
  success      = signal('');
  searchQuery  = '';
  page         = signal(0);
  totalPages   = signal(0);
  totalElements = signal(0);
  pageSize     = 10;

  // Modal
  selectedUser   = signal<any>(null);
  showModal      = signal(false);
  actionLoading  = signal(false);
  newStatus      = '';
  statusReason   = '';

  constructor(private applicationService: ApplicationService) {}

  ngOnInit():    void { this.loadUsers(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['activeSubSection'] && !c['activeSubSection'].firstChange) {
      this.page.set(0);
      this.searchQuery = '';
      this.loadUsers();
    }
  }

  get statusFilter(): string | undefined {
    const m: Record<string, string> = {
      'users-active': 'ACTIVE',
      'users-locked': 'LOCKED',
    };
    return m[this.activeSubSection];
  }

  get sectionTitle(): string {
    const m: Record<string, string> = {
      'users-all':    'All Users',
      'users-active': 'Active Users',
      'users-locked': 'Locked Users',
    };
    return m[this.activeSubSection] ?? 'Users';
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set('');
    this.applicationService.getAllUsers(
      this.statusFilter, this.page(), this.pageSize
    ).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.users.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  get filteredUsers(): any[] {
    if (!this.searchQuery.trim()) return this.users();
    const q = this.searchQuery.toLowerCase();
    return this.users().filter(u =>
      u.username?.toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      u.fullName?.toLowerCase().includes(q) ||
      u.phone?.includes(q)
    );
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p); this.loadUsers();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  openModal(user: any): void {
    this.selectedUser.set(user);
    this.newStatus   = user.status;
    this.statusReason = '';
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.selectedUser.set(null);
    this.error.set('');
  }

  updateUserStatus(): void {
    if (!this.selectedUser()) return;
    this.actionLoading.set(true);
    this.error.set('');

    this.applicationService.updateUserStatus({
      userId:   this.selectedUser().id,
      status:   this.newStatus,
      reason:   this.statusReason,
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set(`User ${this.selectedUser().username} status updated to ${this.newStatus}`);
          this.closeModal();
          this.loadUsers();
          setTimeout(() => this.success.set(''), 4000);
        } else {
          this.error.set(res.message || 'Update failed.');
        }
      },
      error: (err: any) => {
        this.actionLoading.set(false);
        this.error.set(err.error?.message || 'Update failed.');
      },
    });
  }

  unlockUser(user: any): void {
    this.applicationService.updateUserStatus({
      userId: user.id, status: 'ACTIVE', reason: 'Unlocked by admin',
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.success.set(`${user.username} unlocked.`);
          this.loadUsers();
          setTimeout(() => this.success.set(''), 3000);
        }
      },
      error: () => {},
    });
  }

  getRoleBadgeClass(role: string): string {
    const m: Record<string, string> = {
      CUSTOMER: 'role-customer', ADMIN: 'role-admin',
      SUPER_ADMIN: 'role-super', MANAGER: 'role-manager',
    };
    return m[role] ?? 'role-customer';
  }

  getStatusClass(status: string): string {
    const m: Record<string, string> = {
      ACTIVE: 'st-active', INACTIVE: 'st-inactive',
      LOCKED: 'st-locked', SUSPENDED: 'st-suspended',
    };
    return m[status] ?? 'st-inactive';
  }
}