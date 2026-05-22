// customer-open-account.component.ts

import { Component, Input, Output, EventEmitter, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector:    'app-customer-open-account',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-open-account.component.html',
  styleUrl:    './customer-open-account.component.css',
})
export class CustomerOpenAccountComponent implements OnInit {

  @Input() user: any = null;
  @Output() requestSubmitted = new EventEmitter<void>();

  loading       = signal(false);
  error         = signal('');
  success       = signal('');
  myRequests    = signal<any[]>([]);
  requestsLoading = signal(false);

  // Form
  accountType = '';
  reason      = '';

  readonly accountTypes = [
    { value: 'SAVINGS', label: 'Savings Account', desc: 'Earn interest, everyday banking' },
    { value: 'CURRENT', label: 'Current Account', desc: 'Unlimited transactions, business use' },
    { value: 'SALARY',  label: 'Salary Account',  desc: 'Auto-credit, zero balance' },
  ];

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void { this.loadMyRequests(); }

  loadMyRequests(): void {
    this.requestsLoading.set(true);
    this.applicationService.getMyAccountRequests().subscribe({
      next: (res: any) => {
        this.requestsLoading.set(false);
        if (res.success && res.data) {
          this.myRequests.set(res.data.content);
        }
      },
      error: () => this.requestsLoading.set(false),
    });
  }

  submit(): void {
    if (!this.accountType) { this.error.set('Please select an account type.'); return; }
    this.loading.set(true);
    this.error.set('');

    this.applicationService.submitAccountRequest({
      accountType: this.accountType,
      reason:      this.reason,
    }).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success) {
          this.success.set('✅ Request submitted! Request ID: ' + res.data?.requestId);
          this.accountType = '';
          this.reason      = '';
          this.loadMyRequests();
          this.requestSubmitted.emit();
        } else {
          this.error.set(res.message || 'Request failed.');
        }
      },
      error: (err: any) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to submit request.');
      },
    });
  }

  getStatusClass(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'st-pend', APPROVED: 'st-ok', REJECTED: 'st-err',
    };
    return m[s] ?? 'st-pend';
  }

  getStatusIcon(s: string): string {
    const m: Record<string, string> = {
      PENDING: '⏳', APPROVED: '✅', REJECTED: '❌',
    };
    return m[s] ?? '⏳';
  }
}