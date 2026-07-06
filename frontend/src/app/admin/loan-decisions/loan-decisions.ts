import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../services/loan';
import { LoanApplication, LoanStatus } from '../../core/models/loan.model';

@Component({
  selector: 'app-admin-loan-decisions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './loan-decisions.html',
  styleUrl: './loan-decisions.css',
})
export class AdminLoanDecisionsComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly fb = inject(FormBuilder);

  // State Signals
  readonly applications = signal<LoanApplication[]>([]);
  readonly activeFilter = signal<string>('PENDING'); // show PENDING by default
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly successMessage = signal<string>('');
  readonly errorMessage = signal<string>('');

  // Selected Application for Decision Modal
  readonly selectedAppForReview = signal<LoanApplication | null>(null);
  decisionForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.loadApplications();
  }

  private initForm(): void {
    this.decisionForm = this.fb.group({
      decision: ['APPROVED', Validators.required],
      adminRemarks: ['', [Validators.maxLength(255)]],
    });
  }

  loadApplications(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    const filterStatus = this.activeFilter() as LoanStatus;
    
    // We call the admin applications endpoint
    this.loanService.getAdminApplications(filterStatus || undefined).subscribe({
      next: (res) => {
        this.applications.set(res || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to fetch loan applications.');
        this.isLoading.set(false);
      }
    });
  }

  setFilter(status: string): void {
    this.activeFilter.set(status);
    this.loadApplications();
  }

  openReviewModal(app: LoanApplication): void {
    this.selectedAppForReview.set(app);
    this.decisionForm.reset({
      decision: 'APPROVED',
      adminRemarks: ''
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeReviewModal(): void {
    this.selectedAppForReview.set(null);
  }

  submitDecision(): void {
    const app = this.selectedAppForReview();
    if (!app) return;

    if (this.decisionForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const payload = {
      decision: this.decisionForm.value.decision as LoanStatus,
      adminRemarks: this.decisionForm.value.adminRemarks || ''
    };

    this.loanService.decideApplication(app.id, payload).subscribe({
      next: (res) => {
        this.successMessage.set(`🎉 Decision for application #${res.id} submitted successfully! Status is now ${res.status}.`);
        this.isSubmitting.set(false);
        this.closeReviewModal();
        this.loadApplications();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to update decision.');
        this.isSubmitting.set(false);
      }
    });
  }
}
