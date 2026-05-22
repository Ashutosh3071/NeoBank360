// admin-sidebar.component.ts

import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface NavItem {
  id:       string;
  label:    string;
  icon:     string;
  badge?:   number;
  children?: NavItem[];
}

@Component({
  selector:    'app-admin-sidebar',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './admin-sidebar.component.html',
  styleUrl:    './admin-sidebar.component.css',
})
export class AdminSidebarComponent {

  @Input() sidebarOpen      = true;
  @Input() sidebarCollapsed = false;
  @Input() currentSection   = 'dashboard';
  @Input() username         = '';
  @Input() fullName         = '';
  @Input() role             = '';
  @Input() pendingCount     = 0;

  @Output() sectionSelected = new EventEmitter<string>();
  @Output() closeSidebarEvt = new EventEmitter<void>();
  @Output() logoutEvt       = new EventEmitter<void>();
  @Output() openSidebarCollapsed=new EventEmitter<void>();

  expandedItems = signal<Set<string>>(new Set());

  get navItems(): NavItem[] {
    return [
      { id: 'dashboard', label: 'Dashboard', icon: '⊞' },
      {
        id: 'applications', label: 'Applications', icon: '📋',
        badge: this.pendingCount,
        children: [
          { id: 'applications-all',      label: 'All Applications', icon: '📄' },
          { id: 'applications-pending',  label: 'Pending Review',   icon: '⏳', badge: this.pendingCount },
          { id: 'applications-approved', label: 'Approved',         icon: '✅' },
          { id: 'applications-rejected', label: 'Rejected',         icon: '❌' },
          { id: 'account-requests', label: 'Open Account Requests', icon: '🏦' },
        ]
      },
      {
        id: 'users', label: 'Users', icon: '👥',
        children: [
          { id: 'users-all',    label: 'All Users', icon: '👤' },
          { id: 'users-active', label: 'Active',    icon: '🟢' },
          { id: 'users-locked', label: 'Locked',    icon: '🔒' },
        ]
      },
      {
        id: 'accounts', label: 'Accounts', icon: '🏦',
        children: [
          { id: 'accounts-all',    label: 'All Accounts', icon: '💳' },
          { id: 'accounts-active', label: 'Active',       icon: '🟢' },
          { id: 'accounts-frozen', label: 'Frozen',       icon: '❄️' },
        ]
      },
      { id: 'transactions', label: 'Transactions', icon: '💸' },
      { id: 'analytics',    label: 'Analytics',    icon: '📊' },
      {
        id: 'management', label: 'Management', icon: '⚙️',
        children: [
          { id: 'management-admins',   label: 'Admins',   icon: '👑' },
          { id: 'management-settings', label: 'Settings', icon: '🛠️' },
        ]
      },
    ];
  }

  toggleExpand(id: string): void {
    this.expandedItems.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  isExpanded(id: string): boolean { return this.expandedItems().has(id); }

  navigate(item: NavItem): void {
    if (item.children?.length) { this.toggleExpand(item.id); return; }
    this.sectionSelected.emit(item.id);
  }

  isActive(id: string): boolean {
    return this.currentSection === id ||
           this.currentSection.startsWith(id + '-');
  }

  getUserInitials(): string {
    if (this.fullName) return this.fullName.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
    return this.username.substring(0, 2).toUpperCase();
  }

  close():  void { this.closeSidebarEvt.emit(); }
  logout(): void { this.logoutEvt.emit(); }
}