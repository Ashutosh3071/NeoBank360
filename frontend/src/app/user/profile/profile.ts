import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Auth } from '../../services/auth';
import { UpdateProfileRequest, UserProfile } from '../../core/models/profile.model';
import { UserService } from '../../services/user';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly auth = inject(Auth);

  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly profile = signal<UserProfile | null>(null);
  readonly isAdmin = signal(false);

  readonly profileForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
  });

  constructor() {
    this.isAdmin.set(this.auth.isAdmin());
    this.loadProfile();
  }

  get fullName() {
    return this.profileForm.get('fullName');
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.userService.getMyProfile().subscribe({
      next: (response) => {
        if (response && response.createdAt) {
          try {
            response.createdAt = new Date(response.createdAt) as any;
          } catch (e) {
            console.error("Error parsing date: ", e);
          }
        }
        this.profile.set(response);
        this.profileForm.patchValue({
          fullName: response.fullName ?? '',
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load profile.');
        this.isLoading.set(false);
      },
    });
  }

  onSubmit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    const payload: UpdateProfileRequest = {
      fullName: this.profileForm.getRawValue().fullName,
    };

    this.userService.updateMyProfile(payload).subscribe({
      next: (response) => {
        if (response && response.createdAt) {
          try {
            response.createdAt = new Date(response.createdAt) as any;
          } catch (e) {
            console.error("Error parsing date: ", e);
          }
        }
        this.profile.set(response);
        this.successMessage.set('Profile updated successfully.');
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to update profile.');
        this.isSaving.set(false);
      },
    });
  }
}