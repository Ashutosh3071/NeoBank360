// customer-profile.component.ts

import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector:    'app-customer-profile',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-profile.component.html',
  styleUrl:    './customer-profile.component.css',
})
export class CustomerProfileComponent implements OnChanges {

  @Input() activeSubSection = 'profile-details';
  @Input() user: any = null;
  @Output() profileUpdated = new EventEmitter<void>();

  loading  = signal(false);
  error    = signal('');
  success  = signal('');

  // Change Password
  currentPassword = '';
  newPassword     = '';
  confirmPassword = '';
  showCurrent     = signal(false);
  showNew         = signal(false);
  showConfirm     = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    this.error.set('');
    this.success.set('');
    this.resetPasswordForm();
  }

  resetPasswordForm(): void {
    this.currentPassword = '';
    this.newPassword     = '';
    this.confirmPassword = '';
  }

  changePassword(): void {
    this.error.set('');
    this.success.set('');

    if (!this.currentPassword) { this.error.set('Current password is required.'); return; }
    if (!this.newPassword)     { this.error.set('New password is required.'); return; }
    if (this.newPassword.length < 6) { this.error.set('Minimum 6 characters.'); return; }
    if (this.newPassword !== this.confirmPassword) { this.error.set('Passwords do not match.'); return; }
    if (this.currentPassword === this.newPassword) { this.error.set('New password must be different.'); return; }

    this.loading.set(true);

    // Call API — add changePassword method to AuthService
    this.authService.changePassword({
      currentPassword: this.currentPassword,
      newPassword:     this.newPassword,
      confirmPassword: this.confirmPassword,
    }).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success) {
          this.success.set('✅ Password changed successfully!');
          this.resetPasswordForm();
        } else {
          this.error.set(res.message || 'Failed to change password.');
        }
      },
      error: (err: any) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to change password.');
      }
    });
  }

  getPasswordStrength(pwd: string): { label: string; cls: string; width: number } {
    if (!pwd) return { label: '', cls: '', width: 0 };
    let score = 0;
    if (pwd.length >= 6)           score++;
    if (pwd.length >= 10)          score++;
    if (/[A-Z]/.test(pwd))         score++;
    if (/[0-9]/.test(pwd))         score++;
    if (/[^a-zA-Z0-9]/.test(pwd)) score++;
    if (score <= 1) return { label: 'Weak',   cls: 'str-weak',   width: 25  };
    if (score <= 2) return { label: 'Fair',   cls: 'str-fair',   width: 50  };
    if (score <= 3) return { label: 'Good',   cls: 'str-good',   width: 75  };
    return             { label: 'Strong', cls: 'str-strong', width: 100 };
  }

  constructor(private authService: AuthService) {}
  
  
}