import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { Auth } from '../../services/auth';
import { LoginRequest } from '../../core/models/auth.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isSubmitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly email = computed(() => this.loginForm.get('email'));
  readonly password = computed(() => this.loginForm.get('password'));

  ngOnInit(): void {
    if (this.route.snapshot.queryParams['sessionExpired']) {
      this.errorMessage.set('Your session has expired. Please log in again.');
    }
  }

  onSubmit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const payload: LoginRequest = {
      email: this.loginForm.getRawValue().email,
      password: this.loginForm.getRawValue().password,
    };

    this.auth.login(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Login successful. Redirecting...');
        const target = this.auth.isAdmin() ? '/admin/dashboard' : '/dashboard';
        this.router.navigate([target]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);

        if (error.status === 401) {
          this.errorMessage.set('Invalid email or password.');
          return;
        }

        if (error.error?.message) {
          this.errorMessage.set(error.error.message);
          return;
        }

        this.errorMessage.set('Login failed. Please try again.');
      },
    });
  }
}
