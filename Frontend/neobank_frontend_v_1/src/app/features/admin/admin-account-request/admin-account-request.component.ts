// admin-account-request.component.ts

import {
  Component, OnInit, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

type ModalMode = 'none' | 'detail' | 'approve' | 'reject';

@Component({
  selector:    'app-admin-account-request',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-account-request.component.html',
  styleUrl:    './admin-account-request.component.css',
})
export class AdminAccountRequestComponent implements OnInit {

  requests      = signal<any[]>([]);
  loading       = signal(false);
  error         = signal('');
  success       = signal('');
  searchQuery   = '';
  page          = signal(0);
  totalPages    = signal(0);
  totalElements = signal(0);
  pageSize      = 10;
  statusFilter  = 'all';

  modalMode     = signal<ModalMode>('none');
  selected      = signal<any>(null);
  detailLoading = signal(false);
  actionLoading = signal(false);

  // Approve form
  branchName  = 'Main Branch';
  branchCode  = '001';
  ifscCode    = 'NEOB0000001';

  // Reject form
  rejectionReason = '';

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void { this.loadRequests(); }

  loadRequests(): void {
    this.loading.set(true);
    this.error.set('');
    const status = this.statusFilter === 'all' ? undefined : this.statusFilter.toUpperCase();
    this.applicationService.getAllAccountRequests(status, this.page(), this.pageSize).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.requests.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  onFilterChange(): void { this.page.set(0); this.loadRequests(); }

  get filteredRequests(): any[] {
    if (!this.searchQuery.trim()) return this.requests();
    const q = this.searchQuery.toLowerCase();
    return this.requests().filter(r =>
      r.requestId?.toLowerCase().includes(q) ||
      r.fullName?.toLowerCase().includes(q) ||
      r.email?.toLowerCase().includes(q)
    );
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page.set(p); this.loadRequests();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  viewDetail(req: any): void {
    this.detailLoading.set(true);
    this.modalMode.set('detail');
    this.selected.set(null);

    this.applicationService.getAccountRequestDetail(req.requestId).subscribe({
      next: (res: any) => {
        this.detailLoading.set(false);
        if (res.success) this.selected.set(res.data);
        else { this.error.set(res.message); this.modalMode.set('none'); }
      },
      error: (err: any) => {
        this.detailLoading.set(false);
        this.error.set(err.error?.message || 'Failed to load detail.');
        this.modalMode.set('none');
      },
    });
  }

  openApprove(): void { this.modalMode.set('approve'); this.error.set(''); }

  confirmApprove(): void {
    this.actionLoading.set(true);
    this.error.set('');

    this.applicationService.approveAccountRequest({
      requestId:  this.selected().requestId,
      action:     'APPROVE',
      branchName: this.branchName,
      branchCode: this.branchCode,
      ifscCode:   this.ifscCode,
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set('✅ Account created: ' + res.data?.createdAccountNumber);
          this.closeModal();
          this.loadRequests();
          setTimeout(() => this.success.set(''), 5000);
        } else {
          this.error.set(res.message);
          this.modalMode.set('detail');
        }
      },
      error: (err: any) => {
        this.actionLoading.set(false);
        this.error.set(err.error?.message || 'Approval failed.');
        this.modalMode.set('detail');
      },
    });
  }

  openReject(): void { this.rejectionReason = ''; this.modalMode.set('reject'); this.error.set(''); }

  confirmReject(): void {
    if (!this.rejectionReason.trim()) {
      this.error.set('Rejection reason required.'); return;
    }
    this.actionLoading.set(true);

    this.applicationService.rejectAccountRequest({
      requestId:       this.selected().requestId,
      action:          'REJECT',
      rejectionReason: this.rejectionReason.trim(),
    }).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (res.success) {
          this.success.set('Request rejected.');
          this.closeModal();
          this.loadRequests();
          setTimeout(() => this.success.set(''), 4000);
        } else {
          this.error.set(res.message);
        }
      },
      error: (err: any) => {
        this.actionLoading.set(false);
        this.error.set(err.error?.message || 'Rejection failed.');
      },
    });
  }

  closeModal():   void { this.modalMode.set('none'); this.selected.set(null); this.error.set(''); }
  backToDetail(): void { this.modalMode.set('detail'); this.error.set(''); }

  getStatusClass(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'st-pend', APPROVED: 'st-ok', REJECTED: 'st-err',
    };
    return m[s] ?? 'st-pend';
  }
}