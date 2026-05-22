// admin-profile.component.ts

import { Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector:    'app-admin-profile',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './admin-profile.component.html',
  styleUrl:    './admin-profile.component.css'
})
export class AdminProfileComponent implements OnChanges {

  @Input() activeSubSection = 'admin-profile-details';
  @Input() user: any = null;

  loading  = signal(false);
  error    = signal('');
  success  = signal('');

  currentPassword  = '';
  newPassword      = '';
  confirmPassword  = '';
  showCurrent      = signal(false);
  showNew          = signal(false);
  showConfirm      = signal(false);

  ngOnChanges(c: SimpleChanges): void {
    this.error.set('');
    this.success.set('');
    this.currentPassword = '';
    this.newPassword     = '';
    this.confirmPassword = '';
  }

  changePassword(): void {
    this.error.set('');
    this.success.set('');

    if (!this.currentPassword) { this.error.set('Current password required.'); return; }
    if (!this.newPassword || this.newPassword.length < 6) { this.error.set('Min 6 characters.'); return; }
    if (this.newPassword !== this.confirmPassword) { this.error.set('Passwords do not match.'); return; }
    if (this.currentPassword === this.newPassword) { this.error.set('New password must differ.'); return; }

    this.loading.set(true);

    this.authService.changePassword({
      currentPassword: this.currentPassword,
      newPassword:     this.newPassword,
      confirmPassword: this.confirmPassword,
    }).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success) {
          this.success.set('✅ Password changed successfully!');
          this.currentPassword = '';
          this.newPassword     = '';
          this.confirmPassword = '';
        } else {
          this.error.set(res.message || 'Failed.');
        }
      },
      error: (err: any) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to change password.');
      },
    });
  }

  getPasswordStrength(p: string): { label: string; cls: string; width: number } {
    if (!p) return { label: '', cls: '', width: 0 };
    let s = 0;
    if (p.length >= 6) s++;
    if (p.length >= 10) s++;
    if (/[A-Z]/.test(p)) s++;
    if (/[0-9]/.test(p)) s++;
    if (/[^a-zA-Z0-9]/.test(p)) s++;
    if (s <= 1) return { label: 'Weak',   cls: 'str-weak',   width: 25 };
    if (s <= 2) return { label: 'Fair',   cls: 'str-fair',   width: 50 };
    if (s <= 3) return { label: 'Good',   cls: 'str-good',   width: 75 };
    return           { label: 'Strong', cls: 'str-strong', width: 100 };
  }

  constructor(private authService: AuthService) {}
}