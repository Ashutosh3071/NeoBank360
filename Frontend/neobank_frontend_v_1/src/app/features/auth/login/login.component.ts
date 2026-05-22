// src/app/features/auth/login/login.component.ts

import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

type ActiveView  = 'login' | 'forgotUsername' | 'forgotPassword';
type ForgotUStep = 'input' | 'otp' | 'done';
type ForgotPStep = 'input' | 'otp' | 'reset' | 'done';

interface LoginErrors {
  usernameOrEmail?: string;
  password?:        string;
}

interface ForgotUErrors {
  email?:   string;
  captcha?: string;
  otp?:     string;
}

interface ForgotPErrors {
  email?:           string;
  captcha?:         string;
  otp?:             string;
  newPassword?:     string;
  confirmPassword?: string;
}

@Component({
  selector:    'app-login',
  standalone:  true,
  imports:     [RouterLink, FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl:    './login.component.css',
})
export class LoginComponent implements OnInit, OnDestroy {

  // ── Active view ──
  activeView = signal<ActiveView>('login');

  // ─────────────────────────────────────────────
  //  LOGIN
  // ─────────────────────────────────────────────

  usernameOrEmail = '';
  password        = '';
  showPassword    = signal(false);
  rememberMe      = false;

  loginLoading = signal(false);
  loginError   = signal('');
  loginErrors: LoginErrors = {};

  // ─────────────────────────────────────────────
  //  FORGOT USERNAME
  // ─────────────────────────────────────────────

  forgotUStep = signal<ForgotUStep>('input');

  fuEmail         = '';
  fuCaptchaAnswer = '';
  fuOtp           = '';

  fuLoading = signal(false);
  fuError   = signal('');
  fuSuccess = signal('');
  fuErrors: ForgotUErrors = {};

  fuCaptchaToken = signal('');
  fuCaptchaText  = signal('');

  fuResendCooldown = signal(0);
  private fuTimer: any;

  // ─────────────────────────────────────────────
  //  FORGOT PASSWORD
  // ─────────────────────────────────────────────

  forgotPStep = signal<ForgotPStep>('input');

  fpEmail           = '';
  fpCaptchaAnswer   = '';
  fpOtp             = '';
  fpNewPassword     = '';
  fpConfirmPassword = '';

  fpLoading      = signal(false);
  fpError        = signal('');
  fpSuccess      = signal('');
  fpErrors: ForgotPErrors = {};
  showFpPassword = signal(false);
  showFpConfirm  = signal(false);

  fpCaptchaToken = signal('');
  fpCaptchaText  = signal('');

  fpResendCooldown = signal(0);
  private fpTimer: any;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {}

  ngOnDestroy(): void {
    clearInterval(this.fuTimer);
    clearInterval(this.fpTimer);
  }

  // ─────────────────────────────────────────────
  //  VIEW SWITCH
  // ─────────────────────────────────────────────

  showForgotUsername(): void {
    this.activeView.set('forgotUsername');
    this.resetForgotU();
    this.loadCaptcha('forgotU');
  }

  showForgotPassword(): void {
    this.activeView.set('forgotPassword');
    this.resetForgotP();
    this.loadCaptcha('forgotP');
  }

  backToLogin(): void {
    this.activeView.set('login');
    this.loginError.set('');
  }

  // ─────────────────────────────────────────────
  //  CAPTCHA
  // ─────────────────────────────────────────────

  loadCaptcha(target: 'forgotU' | 'forgotP'): void {
    this.authService.getCaptcha().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          if (target === 'forgotU') {
            this.fuCaptchaToken.set(res.data.token);
            this.fuCaptchaText.set(res.data.captchaText);
            this.fuCaptchaAnswer = '';
          } else {
            this.fpCaptchaToken.set(res.data.token);
            this.fpCaptchaText.set(res.data.captchaText);
            this.fpCaptchaAnswer = '';
          }
        }
      },
      error: () => {},
    });
  }

  refreshCaptcha(target: 'forgotU' | 'forgotP'): void {
    if (target === 'forgotU') this.fuErrors.captcha = undefined;
    if (target === 'forgotP') this.fpErrors.captcha = undefined;
    this.loadCaptcha(target);
  }

  // ─────────────────────────────────────────────
  //  LOGIN
  // ─────────────────────────────────────────────

  login(): void {
    this.loginErrors = {};
    this.loginError.set('');

    if (!this.usernameOrEmail.trim()) {
      this.loginErrors.usernameOrEmail = 'Username or email is required.';
      return;
    }
    if (!this.password) {
      this.loginErrors.password = 'Password is required.';
      return;
    }

    this.loginLoading.set(true);

    this.authService.login({
      usernameOrEmail: this.usernameOrEmail.trim(),
      password:        this.password,
    }).subscribe({
      next: (res) => {
        this.loginLoading.set(false);
        if (res.success && res.data) {
          // Store token
          localStorage.setItem('token', res.data.token ?? '');
          localStorage.setItem('user', JSON.stringify(res.data));

          // Navigate by role
          const role = res.data.role;
          if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
            this.router.navigate(['/admin']);
          } else if (role === 'MANAGER') {
            this.router.navigate(['/manager']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        } else {
          this.loginError.set(res.message || 'Login failed.');
        }
      },
      error: (err) => {
        this.loginLoading.set(false);
        this.loginError.set(this.extractError(err));
      },
    });
  }

// // localStorage keys:
// localStorage.setItem('token', res.data.token);   // JWT string
// localStorage.setItem('user', JSON.stringify({     // User object
//   userId:        number,
//   username:      string,
//   email:         string,
//   fullName:      string | null,
//   accountNumber: string | null,
//   role:          'CUSTOMER' | 'ADMIN' | 'SUPER_ADMIN' | 'MANAGER',
//   token:         string
// }));


  // ─────────────────────────────────────────────
  //  FORGOT USERNAME
  // ─────────────────────────────────────────────

  sendForgotUOtp(): void {
    this.fuErrors = {};
    if (!this.fuEmail.trim()) {
      this.fuErrors.email = 'Email is required.'; return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.fuEmail)) {
      this.fuErrors.email = 'Enter a valid email.'; return;
    }
    if (!this.fuCaptchaAnswer.trim()) {
      this.fuErrors.captcha = 'Please enter the captcha.'; return;
    }

    this.fuLoading.set(true);
    this.fuError.set('');

    this.authService.forgotUsername({
      email:         this.fuEmail.trim().toLowerCase(),
      captchaToken:  this.fuCaptchaToken(),
      captchaAnswer: this.fuCaptchaAnswer.trim(),
    }).subscribe({
      next: (res) => {
        this.fuLoading.set(false);
        if (res.success) {
          this.forgotUStep.set('otp');
          this.fuSuccess.set(`OTP sent to ${this.fuEmail}`);
          this.startTimer('fu');
        } else {
          this.fuError.set(res.message);
          this.refreshCaptcha('forgotU');
        }
      },
      error: (err) => {
        this.fuLoading.set(false);
        this.fuError.set(this.extractError(err));
        this.refreshCaptcha('forgotU');
      },
    });
  }

  verifyForgotUOtp(): void {
    if (!this.fuOtp || this.fuOtp.length !== 6) {
      this.fuErrors.otp = 'Enter valid 6-digit OTP.'; return;
    }
    this.fuErrors.otp = undefined;
    this.fuLoading.set(true);
    this.fuError.set('');

    this.authService.verifyForgotUsernameOtp({
      email: this.fuEmail.trim().toLowerCase(),
      otp:   this.fuOtp,
    }).subscribe({
      next: (res) => {
        this.fuLoading.set(false);
        if (res.success) {
          this.forgotUStep.set('done');
          clearInterval(this.fuTimer);
        } else {
          this.fuError.set(res.message);
        }
      },
      error: (err) => {
        this.fuLoading.set(false);
        const code = err.error?.errorCode || '';
        if (code.includes('OTP')) {
          this.fuErrors.otp = err.error?.message || 'Invalid OTP.';
        } else {
          this.fuError.set(this.extractError(err));
        }
      },
    });
  }

  resendFuOtp(): void {
    if (this.fuResendCooldown() > 0) return;
    this.fuLoading.set(true);
    this.fuErrors.otp = undefined;

    this.authService.resendOtp(this.fuEmail, 'USERNAME_RECOVERY').subscribe({
      next: () => {
        this.fuLoading.set(false);
        this.fuSuccess.set('New OTP sent.');
        this.fuOtp = '';
        this.startTimer('fu');
        setTimeout(() => this.fuSuccess.set(''), 4000);
      },
      error: (err) => {
        this.fuLoading.set(false);
        this.fuErrors.otp = this.extractError(err);
      },
    });
  }

  private resetForgotU(): void {
    this.forgotUStep.set('input');
    this.fuEmail = '';
    this.fuCaptchaAnswer = '';
    this.fuOtp = '';
    this.fuErrors = {};
    this.fuError.set('');
    this.fuSuccess.set('');
    this.fuResendCooldown.set(0);
    clearInterval(this.fuTimer);
  }

  // ─────────────────────────────────────────────
  //  FORGOT PASSWORD
  // ─────────────────────────────────────────────

  sendForgotPOtp(): void {
    this.fpErrors = {};
    if (!this.fpEmail.trim()) {
      this.fpErrors.email = 'Email is required.'; return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.fpEmail)) {
      this.fpErrors.email = 'Enter a valid email.'; return;
    }
    if (!this.fpCaptchaAnswer.trim()) {
      this.fpErrors.captcha = 'Please enter the captcha.'; return;
    }

    this.fpLoading.set(true);
    this.fpError.set('');

    this.authService.forgotPassword({
      email:         this.fpEmail.trim().toLowerCase(),
      captchaToken:  this.fpCaptchaToken(),
      captchaAnswer: this.fpCaptchaAnswer.trim(),
    }).subscribe({
      next: (res) => {
        this.fpLoading.set(false);
        if (res.success) {
          this.forgotPStep.set('otp');
          this.fpSuccess.set(`OTP sent to ${this.fpEmail}`);
          this.startTimer('fp');
        } else {
          this.fpError.set(res.message);
          this.refreshCaptcha('forgotP');
        }
      },
      error: (err) => {
        this.fpLoading.set(false);
        this.fpError.set(this.extractError(err));
        this.refreshCaptcha('forgotP');
      },
    });
  }

  verifyForgotPOtp(): void {
    if (!this.fpOtp || this.fpOtp.length !== 6) {
      this.fpErrors.otp = 'Enter valid 6-digit OTP.'; return;
    }
    this.fpErrors.otp = undefined;
    this.forgotPStep.set('reset');
  }

  resendFpOtp(): void {
    if (this.fpResendCooldown() > 0) return;
    this.fpLoading.set(true);
    this.fpErrors.otp = undefined;

    this.authService.resendOtp(this.fpEmail, 'PASSWORD_RESET').subscribe({
      next: () => {
        this.fpLoading.set(false);
        this.fpSuccess.set('New OTP sent.');
        this.fpOtp = '';
        this.startTimer('fp');
        setTimeout(() => this.fpSuccess.set(''), 4000);
      },
      error: (err) => {
        this.fpLoading.set(false);
        this.fpErrors.otp = this.extractError(err);
      },
    });
  }

  submitResetPassword(): void {
    this.fpErrors = {};
    let ok = true;

    if (!this.fpNewPassword) {
      this.fpErrors.newPassword = 'Password is required.'; ok = false;
    } else if (this.fpNewPassword.length < 6) {
      this.fpErrors.newPassword = 'Minimum 6 characters.'; ok = false;
    }
    if (!this.fpConfirmPassword) {
      this.fpErrors.confirmPassword = 'Please confirm password.'; ok = false;
    } else if (this.fpNewPassword !== this.fpConfirmPassword) {
      this.fpErrors.confirmPassword = 'Passwords do not match.'; ok = false;
    }
    if (!ok) return;

    this.fpLoading.set(true);
    this.fpError.set('');

    this.authService.resetPassword({
      email:           this.fpEmail.trim().toLowerCase(),
      otp:             this.fpOtp,
      newPassword:     this.fpNewPassword,
      confirmPassword: this.fpConfirmPassword,
    }).subscribe({
      next: (res) => {
        this.fpLoading.set(false);
        if (res.success) {
          this.forgotPStep.set('done');
          clearInterval(this.fpTimer);
        } else {
          this.fpError.set(res.message);
        }
      },
      error: (err) => {
        this.fpLoading.set(false);
        const code = err.error?.errorCode || '';
        if (code.includes('OTP')) {
          this.fpErrors.otp = err.error?.message || 'OTP invalid. Go back.';
          this.forgotPStep.set('otp');
        } else {
          this.fpError.set(this.extractError(err));
        }
      },
    });
  }

  private resetForgotP(): void {
    this.forgotPStep.set('input');
    this.fpEmail = '';
    this.fpCaptchaAnswer = '';
    this.fpOtp = '';
    this.fpNewPassword = '';
    this.fpConfirmPassword = '';
    this.fpErrors = {};
    this.fpError.set('');
    this.fpSuccess.set('');
    this.fpResendCooldown.set(0);
    clearInterval(this.fpTimer);
  }

  // ─────────────────────────────────────────────
  //  PASSWORD STRENGTH
  // ─────────────────────────────────────────────

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

  // ─────────────────────────────────────────────
  //  TIMER
  // ─────────────────────────────────────────────

  private startTimer(target: 'fu' | 'fp'): void {
    const set = (v: number) => target === 'fu'
      ? this.fuResendCooldown.set(v)
      : this.fpResendCooldown.set(v);
    const get = () => target === 'fu'
      ? this.fuResendCooldown()
      : this.fpResendCooldown();
    const timerKey = target === 'fu' ? 'fuTimer' : 'fpTimer';

    set(60);
    clearInterval((this as any)[timerKey]);
    (this as any)[timerKey] = setInterval(() => {
      const cur = get();
      if (cur <= 1) { clearInterval((this as any)[timerKey]); set(0); }
      else set(cur - 1);
    }, 1000);
  }

  // ─────────────────────────────────────────────
  //  HELPERS
  // ─────────────────────────────────────────────

  togglePassword():   void { this.showPassword.update(v => !v); }
  toggleFpPassword(): void { this.showFpPassword.update(v => !v); }
  toggleFpConfirm():  void { this.showFpConfirm.update(v => !v); }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    switch (err.status) {
      case 0:   return 'Cannot connect to server. Check your connection.';
      case 401: return err.error?.message || 'Invalid credentials.';
      case 403: return err.error?.message || 'Account is locked or inactive.';
      case 404: return 'No account found with this email.';
      case 429: return 'Too many attempts. Please wait.';
      case 500: return 'Server error. Please try again later.';
      default:  return 'Something went wrong. Please try again.';
    }
  }
}