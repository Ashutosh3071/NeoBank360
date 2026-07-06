import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { Auth } from '../../services/auth';
import { RegisterRequest } from '../../core/models/auth.model';

function passwordMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordMismatch: true };
  };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly fieldErrors = signal<string[]>([]);

  readonly registerForm = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).+$/),
        ],
      ],
      aadhaarNumber: [
        '',
        [Validators.required, Validators.pattern(/^[0-9]{12}$/)],
      ],
      panNumber: [
        '',
        [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    {
      validators: passwordMatchValidator(),
    }
  );

  get fullName() {
    return this.registerForm.get('fullName');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get aadhaarNumber() {
    return this.registerForm.get('aadhaarNumber');
  }

  get panNumber() {
    return this.registerForm.get('panNumber');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  onPanInput(): void {
    const current = this.registerForm.get('panNumber')?.value ?? '';
    this.registerForm.patchValue(
      { panNumber: current.toUpperCase() },
      { emitEvent: false }
    );
  }

  onSubmit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');
    this.fieldErrors.set([]);

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const raw = this.registerForm.getRawValue();

    const payload: RegisterRequest = {
      fullName: raw.fullName.trim(),
      email: raw.email.trim(),
      password: raw.password,
      aadhaarNumber: raw.aadhaarNumber.trim(),
      panNumber: raw.panNumber.trim().toUpperCase(),
    };

    this.auth.register(payload).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.successMessage.set(response?.message || 'Registration successful. You can now login.');

        this.registerForm.reset({
          fullName: '',
          email: '',
          password: '',
          aadhaarNumber: '',
          panNumber: '',
          confirmPassword: '',
        });

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1200);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);

        if (error.error?.message) {
          this.errorMessage.set(error.error.message);
          return;
        }

        if (error.error?.errors && Array.isArray(error.error.errors)) {
          this.fieldErrors.set(error.error.errors);
          return;
        }

        this.errorMessage.set('Registration failed. Please try again.');
      },
    });
  }
}
``