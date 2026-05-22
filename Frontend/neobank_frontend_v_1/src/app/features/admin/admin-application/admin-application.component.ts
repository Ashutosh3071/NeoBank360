// admin-application.component.ts

import {
  Component, Input, Output, EventEmitter,
  OnInit, OnChanges, SimpleChanges, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ApplicationService,
  ApplicationListItem,
  ApplicationDetail,
  ApproveRequest,
} from '../../../core/services/application.service';


type ModalMode = 'none' | 'detail' | 'approve' | 'reject';

@Component({
  selector:    'app-admin-application',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-application.component.html',
  styleUrl:    './admin-application.component.css',
})
export class AdminApplicationComponent implements OnInit, OnChanges {
readonly Math = Math;
  // ── Input: which sub-section is active ──
  @Input() activeSubSection = 'applications-all';

  // ── Output: tell layout how many pending ──
  @Output() pendingCountChange = new EventEmitter<number>();

  // ── List state ──
  applications  = signal<ApplicationListItem[]>([]);
  loading       = signal(false);
  error         = signal('');
  success       = signal('');

  // ── Pagination ──
  currentPage   = signal(0);
  totalPages    = signal(0);
  totalElements = signal(0);
  pageSize      = 10;

  // ── Search / Filter ──
  searchQuery   = '';

  // ── Detail Modal ──
  modalMode     = signal<ModalMode>('none');
  selectedApp   = signal<ApplicationDetail | null>(null);
  detailLoading = signal(false);

  // ── Approve / Reject ──
  approveLoading  = signal(false);
  rejectionReason = '';
  branchName      = 'Main Branch';
  branchCode      = '001';
  ifscCode        = 'NEOB0000001';

  // ── Active document tab in detail view ──
  activeDocTab = signal<string>('aadhaar');

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void { this.loadApplications(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['activeSubSection'] && !changes['activeSubSection'].firstChange) {
      this.currentPage.set(0);
      this.searchQuery = '';
      this.loadApplications();
    }
  }

  // ─────────────────────────────────────────────
  //  STATUS FILTER from sub-section
  // ─────────────────────────────────────────────

  get statusFilter(): string | undefined {
    const map: Record<string, string> = {
      'applications-pending':  'SUBMITTED',
      'applications-approved': 'APPROVED',
      'applications-rejected': 'REJECTED',
    };
    return map[this.activeSubSection];
  }

  get sectionTitle(): string {
    const map: Record<string, string> = {
      'applications-all':      'All Applications',
      'applications-pending':  'Pending Review',
      'applications-approved': 'Approved',
      'applications-rejected': 'Rejected',
    };
    return map[this.activeSubSection] ?? 'Applications';
  }

  get sectionSubtitle(): string {
    const map: Record<string, string> = {
      'applications-all':      'View and manage all bank account applications',
      'applications-pending':  'Applications awaiting review and approval',
      'applications-approved': 'Successfully approved applications',
      'applications-rejected': 'Applications that were rejected',
    };
    return map[this.activeSubSection] ?? '';
  }

  // ─────────────────────────────────────────────
  //  LOAD LIST
  // ─────────────────────────────────────────────

  loadApplications(): void {
    this.loading.set(true);
    this.error.set('');

    this.applicationService.getAllApplications(
      this.statusFilter,
      this.currentPage(),
      this.pageSize
    ).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.applications.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);

          // Update pending badge
          if (this.activeSubSection === 'applications-pending') {
            this.pendingCountChange.emit(res.data.totalElements);
          }
        } else {
          this.error.set(res.message || 'Failed to load applications.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to load. Please try again.');
      },
    });
  }

  // ─────────────────────────────────────────────
  //  PAGINATION
  // ─────────────────────────────────────────────

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadApplications();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  // ─────────────────────────────────────────────
  //  SEARCH (client-side)
  // ─────────────────────────────────────────────

  get filteredApplications(): ApplicationListItem[] {
    if (!this.searchQuery.trim()) return this.applications();
    const q = this.searchQuery.toLowerCase();
    return this.applications().filter(a =>
      a.fullName.toLowerCase().includes(q) ||
      a.emailId.toLowerCase().includes(q) ||
      a.applicationId.toLowerCase().includes(q) ||
      a.phoneNumber.includes(q)
    );
  }

  // ─────────────────────────────────────────────
  //  VIEW DETAIL
  // ─────────────────────────────────────────────

  viewApplication(applicationId: string): void {
    this.detailLoading.set(true);
    this.modalMode.set('detail');
    this.selectedApp.set(null);
    this.activeDocTab.set('aadhaar');

    this.applicationService.getApplicationDetail(applicationId).subscribe({
      next: (res) => {
        this.detailLoading.set(false);
        if (res.success && res.data) {
          this.selectedApp.set(res.data);
        } else {
          this.error.set(res.message || 'Failed to load details.');
          this.modalMode.set('none');
        }
      },
      error: (err) => {
        this.detailLoading.set(false);
        this.error.set(err.error?.message || 'Failed to load details.');
        this.modalMode.set('none');
      },
    });
  }

  // ─────────────────────────────────────────────
  //  APPROVE
  // ─────────────────────────────────────────────

  openApproveModal(): void {
    this.modalMode.set('approve');
  }

  confirmApprove(): void {
    if (!this.selectedApp()) return;
    this.approveLoading.set(true);
    this.error.set('');

    const req: ApproveRequest = {
      applicationId: this.selectedApp()!.applicationId,
      action:        'APPROVE',
      branchName:    this.branchName,
      branchCode:    this.branchCode,
      ifscCode:      this.ifscCode,
    };

    this.applicationService.approveApplication(req).subscribe({
      next: (res) => {
        this.approveLoading.set(false);
        if (res.success) {
          this.success.set(`✅ Application approved! Account: ${res.data?.accountNumber}`);
          this.closeModal();
          this.loadApplications();
          setTimeout(() => this.success.set(''), 5000);
        } else {
          this.error.set(res.message || 'Approval failed.');
          this.modalMode.set('detail');
        }
      },
      error: (err) => {
        this.approveLoading.set(false);
        this.error.set(err.error?.message || 'Approval failed.');
        this.modalMode.set('detail');
      },
    });
  }

  // ─────────────────────────────────────────────
  //  REJECT
  // ─────────────────────────────────────────────

  openRejectModal(): void {
    this.rejectionReason = '';
    this.modalMode.set('reject');
  }

  confirmReject(): void {
    if (!this.rejectionReason.trim()) {
      this.error.set('Please provide a rejection reason.');
      return;
    }
    if (!this.selectedApp()) return;
    this.approveLoading.set(true);
    this.error.set('');

    const req: ApproveRequest = {
      applicationId:   this.selectedApp()!.applicationId,
      action:          'REJECT',
      rejectionReason: this.rejectionReason.trim(),
    };

    this.applicationService.approveApplication(req).subscribe({
      next: (res) => {
        this.approveLoading.set(false);
        if (res.success) {
          this.success.set('❌ Application rejected successfully.');
          this.closeModal();
          this.loadApplications();
          setTimeout(() => this.success.set(''), 4000);
        } else {
          this.error.set(res.message || 'Rejection failed.');
          this.modalMode.set('reject');
        }
      },
      error: (err) => {
        this.approveLoading.set(false);
        this.error.set(err.error?.message || 'Rejection failed.');
      },
    });
  }

  // ─────────────────────────────────────────────
  //  MODAL CLOSE
  // ─────────────────────────────────────────────

  closeModal():     void { this.modalMode.set('none'); this.selectedApp.set(null); this.error.set(''); }
  backToDetail():   void { this.modalMode.set('detail'); this.error.set(''); }

  // ─────────────────────────────────────────────
  //  DOCUMENT HELPERS
  // ─────────────────────────────────────────────

  getDocumentUrl(base64: string | null, type: string | null): string {
    if (!base64 || !type) return '';
    return `data:${type};base64,${base64}`;
  }

  isPdf(type: string | null): boolean {
    return type === 'application/pdf';
  }

  docTabs = [
    { id: 'aadhaar',  label: 'Aadhaar'       },
    { id: 'pan',      label: 'PAN Card'       },
    { id: 'photo',    label: 'Photo'          },
    { id: 'sign',     label: 'Signature'      },
    { id: 'address',  label: 'Address Proof'  },
    { id: 'passport', label: 'Passport'       },
    { id: 'voter',    label: 'Voter ID'       },
  ];

  getDocForTab(tab: string): { base64: string | null; type: string | null; label: string } {
    const app = this.selectedApp();
    if (!app) return { base64: null, type: null, label: '' };
    const map: Record<string, { base64: string | null; type: string | null; label: string }> = {
      aadhaar:  { base64: app.aadhaarCardFileBase64,      type: app.aadhaarCardFileType,      label: 'Aadhaar Card'     },
      pan:      { base64: app.panCardFileBase64,          type: app.panCardFileType,          label: 'PAN Card'         },
      photo:    { base64: app.profilePhotoBase64,         type: app.profilePhotoType,         label: 'Profile Photo'    },
      sign:     { base64: app.signatureImageBase64,       type: app.signatureImageType,       label: 'Signature'        },
      address:  { base64: app.addressProofDocumentBase64, type: app.addressProofDocumentType, label: 'Address Proof'    },
      passport: { base64: app.passportFileBase64 ?? null, type: app.passportFileType ?? null, label: 'Passport'         },
      voter:    { base64: app.voterIdFileBase64 ?? null,  type: app.voterIdFileType ?? null,  label: 'Voter ID'         },
    };
    return map[tab] ?? { base64: null, type: null, label: '' };
  }

  // ─────────────────────────────────────────────
  //  STATUS HELPERS
  // ─────────────────────────────────────────────

  statusClass(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'badge-blue',
      APPROVED:  'badge-green',
      REJECTED:  'badge-red',
      PENDING:   'badge-yellow',
    };
    return map[status] ?? 'badge-gray';
  }

  statusIcon(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: '📋', APPROVED: '✅', REJECTED: '❌', PENDING: '⏳',
    };
    return map[status] ?? '📄';
  }

  canApprove(): boolean {
    return this.selectedApp()?.status === 'SUBMITTED';
  }

  canReject(): boolean {
    return this.selectedApp()?.status === 'SUBMITTED';
  }
}