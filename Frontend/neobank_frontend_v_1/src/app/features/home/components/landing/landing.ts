// src/app/features/landing/landing.ts

import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  ApplicationService,
  ApplicationStatusResponse,
} from '../../../../core/services/application.service';

type StatusStep = 'input' | 'otp' | 'result';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {

  // ── Sidebar ──
  sidebarOpen = signal(false);

  // ── Status popup state ──
  showStatusPopup = signal(false);
  statusStep      = signal<StatusStep>('input');
  statusLoading   = signal(false);
  statusError     = signal('');
  statusSuccess   = signal('');

  // ── Form fields ──
  searchQuery = '';
  statusOtp   = '';

  // ── Result ──
  statusResult = signal<ApplicationStatusResponse['data'] | null>(null);

  constructor(
    private themeService: ThemeService,
    private applicationService: ApplicationService,
  ) {}

  // ── Theme ──
  isDarkMode() { return this.themeService.isDark(); }
  cycleTheme() { this.themeService.cycle(); }

  // ── Sidebar ──
  toggleSidebar() { this.sidebarOpen.update(v => !v); }
  closeSidebar()  { this.sidebarOpen.set(false); }

  // ── Open status popup ──
  openStatusPopup() {
    this.closeSidebar();
    this.resetStatusPopup();
    this.showStatusPopup.set(true);
  }

  closeStatusPopup() {
    this.showStatusPopup.set(false);
    this.resetStatusPopup();
  }

  private resetStatusPopup() {
    this.statusStep.set('input');
    this.statusError.set('');
    this.statusSuccess.set('');
    this.searchQuery = '';
    this.statusOtp   = '';
    this.statusResult.set(null);
  }

  // ────────────────────────────────────────────
  //  STEP 1 — Send OTP
  // ────────────────────────────────────────────

  sendStatusOtp() {
    if (!this.searchQuery.trim()) {
      this.statusError.set('Please enter your Application ID or registered Email.');
      return;
    }
    this.statusLoading.set(true);
    this.statusError.set('');

    this.applicationService.sendStatusOtp(this.searchQuery.trim()).subscribe({
      next: (res) => {
        this.statusLoading.set(false);
        if (res.success) {
          this.statusSuccess.set(`OTP sent to your registered email.`);
          this.statusStep.set('otp');
        } else {
          this.statusError.set(res.message || 'Failed to send OTP.');
        }
      },
      error: (err) => {
        this.statusLoading.set(false);
        this.statusError.set(this.extractError(err));
      },
    });
  }

  // ────────────────────────────────────────────
  //  STEP 2 — Verify OTP + get details
  // ────────────────────────────────────────────

  verifyStatusOtp() {
    if (!this.statusOtp || this.statusOtp.length !== 6) {
      this.statusError.set('Enter valid 6-digit OTP.');
      return;
    }
    this.statusLoading.set(true);
    this.statusError.set('');

    this.applicationService
      .verifyStatusOtp(this.searchQuery.trim(), this.statusOtp)
      .subscribe({
        next: (res) => {
          this.statusLoading.set(false);
          if (res.success && res.data) {
            this.statusResult.set(res.data);
            this.statusStep.set('result');
          } else {
            this.statusError.set(res.message || 'Verification failed.');
          }
        },
        error: (err) => {
          this.statusLoading.set(false);
          this.statusError.set(this.extractError(err));
        },
      });
  }

  resendStatusOtp() {
    this.statusOtp   = '';
    this.statusError.set('');
    this.statusSuccess.set('');
    this.sendStatusOtp();
  }

  goBackToInput() {
    this.statusStep.set('input');
    this.statusError.set('');
    this.statusSuccess.set('');
    this.statusOtp = '';
  }

  // ── Helpers ──
  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING:   'status-pending',
      SUBMITTED: 'status-submitted',
      APPROVED:  'status-approved',
      REJECTED:  'status-rejected',
    };
    return map[status] ?? 'status-pending';
  }

  getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      PENDING:   '🕐',
      SUBMITTED: '📋',
      APPROVED:  '✅',
      REJECTED:  '❌',
    };
    return map[status] ?? '🕐';
  }

  getStatusMessage(status: string): string {
    const map: Record<string, string> = {
      PENDING:   'Submitted but OTP not verified yet.',
      SUBMITTED: 'Under review by our team. Expected: 2–3 business days.',
      APPROVED:  'Congratulations! Your account has been activated.',
      REJECTED:  'Application was not approved. You may apply again.',
    };
    return map[status] ?? '';
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    switch (err.status) {
      case 0:   return 'Cannot connect to server. Check your connection.';
      case 400: return err.error?.message || 'Invalid request.';
      case 404: return 'No application found for the given details.';
      case 429: return 'Too many attempts. Please wait and try again.';
      case 500: return 'Server error. Please try again later.';
      default:  return 'Something went wrong. Please try again.';
    }
  }
}