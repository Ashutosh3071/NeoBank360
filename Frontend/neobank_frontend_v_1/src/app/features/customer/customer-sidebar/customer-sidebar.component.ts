// customer-sidebar.component.ts

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
  selector:    'app-customer-sidebar',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './customer-sidebar.component.html',
  styleUrl:    './customer-sidebar.component.css',
})
export class CustomerSidebarComponent {

  @Input() sidebarOpen      = false;
  @Input() sidebarCollapsed = false;
  @Input() currentSection   = 'overview';
  @Input() username         = '';
  @Input() fullName         = '';
  @Input() accountNumber    = '';
  @Input() isDarkMode       = false;

  @Output() sectionSelected  = new EventEmitter<string>();
  @Output() closeSidebarEvt  = new EventEmitter<void>();
  @Output() logoutEvt        = new EventEmitter<void>();
  @Output() openSidebarCollapsed=new EventEmitter<void>();

  expandedItems = signal<Set<string>>(new Set());
  @Input() hasAccount = false;

  get navItems(): NavItem[] {
  if (!this.hasAccount) {
    // No account — show only apply options
    return [
      { id: 'apply-account',    label: 'Open Account',        icon: '🏦' },
      { id: 'application-status', label: 'Application Status', icon: '🔍' },
    ];
  }
  // Full nav
  return [
    { id: 'overview', label: 'Overview', icon: '⊞' },
    {
      id: 'accounts', label: 'My Accounts', icon: '🏦',
      children: [
        { id: 'accounts-summary',    label: 'Summary',    icon: '📊' },
        { id: 'accounts-savings',    label: 'Savings',    icon: '💰' },
        { id: 'accounts-statements', label: 'Statements', icon: '📄' },
      ]
    },
    {
      id: 'transfer', label: 'Transfer', icon: '💸',
      children: [
        { id: 'upi',          label: 'UPI Payments',  icon: '📱' },
        { id: 'transfer-upi',  label: 'UPI Payment',  icon: '📱' },
        { id: 'transfer-neft', label: 'NEFT / RTGS',  icon: '🔄' },
        { id: 'transfer-self', label: 'Self Transfer', icon: '↔️' },
      ]
    },
    { id: 'transactions', label: 'Transactions', icon: '📋' },
    { id: 'cards',        label: 'Cards',         icon: '💳' },
    { id: 'loans',        label: 'Loans',          icon: '🏠' },
    {
      id: 'services', label: 'Services', icon: '🛎️',
      children: [
        { id: 'services-cheque',  label: 'Cheque Book', icon: '📒' },
        { id: 'services-nominee', label: 'Nominee',     icon: '👨‍👩‍👦' },
        { id: 'services-kyc',     label: 'Update KYC',  icon: '📝' },
      ]
    },
    { id: 'budget',  label: 'Budget Planner', icon: '💰' },
    { id: 'bills',   label: 'Bill Manager',   icon: '📄' },
    { id: 'rewards', label: 'Rewards',        icon: '🏆' },
    { id: 'open-account', label: 'Open Another Account', icon: '🏦' },
    {
      id: 'profile', label: 'Profile', icon: '👤',
      children: [
        { id: 'profile-details',  label: 'My Details',     icon: '🪪' },
        { id: 'profile-password', label: 'Change Password', icon: '🔒' },
      ]
    },
  ];
}


  // navItems: NavItem[] = [
  //   { id: 'overview',  label: 'Overview',     icon: '⊞' },
  //   {
  //     id: 'accounts', label: 'My Accounts', icon: '🏦',
  //     children: [
  //       { id: 'accounts-summary',    label: 'Summary',    icon: '📊' },
  //       { id: 'accounts-savings',    label: 'Savings',    icon: '💰' },
  //       { id: 'accounts-statements', label: 'Statements', icon: '📄' },
  //     ]
  //   },
  //   {
  //     id: 'transfer', label: 'Transfer', icon: '💸',
  //     children: [
  //       { id: 'transfer-upi',  label: 'UPI Payment',  icon: '📱' },
  //       { id: 'transfer-neft', label: 'NEFT / RTGS',  icon: '🔄' },
  //       { id: 'transfer-self', label: 'Self Transfer', icon: '↔️' },
  //     ]
  //   },
  //   { id: 'transactions', label: 'Transactions', icon: '📋', badge: 0 },
  //   { id: 'cards',        label: 'Cards',         icon: '💳' },
  //   { id: 'loans',        label: 'Loans',          icon: '🏠' },
  //   {
  //     id: 'services', label: 'Services', icon: '🛎️',
  //     children: [
  //       { id: 'services-cheque',  label: 'Cheque Book',  icon: '📒' },
  //       { id: 'services-nominee', label: 'Nominee',      icon: '👨‍👩‍👦' },
  //       { id: 'services-kyc',     label: 'Update KYC',   icon: '📝' },
  //     ]
  //   },
  //   {
  //     id: 'profile', label: 'Profile', icon: '👤',
  //     children: [
  //       { id: 'profile-details',  label: 'My Details',     icon: '🪪' },
  //       { id: 'profile-password', label: 'Change Password', icon: '🔒' },
  //     ]
  //   },
  // ];

  // ── Expand/collapse sub-menus ──
  toggleExpand(id: string): void {
    this.expandedItems.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }
  

  isExpanded(id: string): boolean {
    return this.expandedItems().has(id);
  }

  // ── Navigate ──
  navigate(item: NavItem): void {
    if (item.children?.length) {
      this.toggleExpand(item.id);
      return;
    }
    this.sectionSelected.emit(item.id);
  }

  isActive(id: string): boolean {
    return this.currentSection === id ||
           this.currentSection.startsWith(id + '-');
  }

  getUserInitials(): string {
    if (this.fullName) {
      return this.fullName.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
    }
    return this.username.substring(0, 2).toUpperCase();
  }

  getMaskedAccount(): string {
    if (!this.accountNumber) return 'No Account';
    return '•••• ' + this.accountNumber.slice(-4);
  }

  close():  void { this.closeSidebarEvt.emit(); }
  logout(): void { this.logoutEvt.emit(); }
}

