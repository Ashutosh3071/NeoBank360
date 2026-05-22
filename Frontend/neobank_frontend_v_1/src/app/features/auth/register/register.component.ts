// src/app/features/auth/register-new/register-new.component.ts

import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

type RegStep        = 'form'     | 'otp'      | 'success';
type ForgotUStep    = 'input'    | 'otp'      | 'done';
type ForgotPStep    = 'input'    | 'otp'      | 'reset'   | 'done';
type ActiveView     = 'register' | 'forgotUsername' | 'forgotPassword';

interface RegErrors {
  email?:           string;
  username?:        string;
  password?:        string;
  confirmPassword?: string;
  captcha?:         string;
  otp?:             string;
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
  selector:    'app-register-new',
  standalone:  true,
  imports:     [RouterLink, FormsModule, CommonModule],
  templateUrl: './register.component.html',
  styleUrl:    './register.component.css',
})
export class RegisterComponent implements OnInit, OnDestroy {

  // ── Active View ──
  activeView = signal<ActiveView>('register');

  // ─────────────────────────────────────────────
  //  REGISTER
  // ─────────────────────────────────────────────

  regStep = signal<RegStep>('form');

  // form fields
  email           = '';
  username        = '';
  password        = '';
  confirmPassword = '';
  captchaAnswer   = '';
  otp             = '';

  // ui
  loading          = signal(false);
  showPassword     = signal(false);
  showConfirm      = signal(false);
  error            = signal('');
  successMsg       = signal('');
  regErrors: RegErrors = {};

  // captcha
  captchaToken = signal('');
  captchaText  = signal('');

  // username check
  usernameStatus  = signal<'idle'|'checking'|'available'|'taken'|'invalid'>('idle');
  usernameMessage = signal('');
  private usernameInput$ = new Subject<string>();

  // resend timer
  resendCooldown = signal(0);
  private regTimer: any;

  // ─────────────────────────────────────────────
  //  FORGOT USERNAME
  // ─────────────────────────────────────────────

  forgotUStep = signal<ForgotUStep>('input');

  fuEmail         = '';
  fuCaptchaAnswer = '';
  fuOtp           = '';

  fuLoading    = signal(false);
  fuError      = signal('');
  fuSuccess    = signal('');
  fuErrors: ForgotUErrors = {};

  fuCaptchaToken = signal('');
  fuCaptchaText  = signal('');

  fuResendCooldown = signal(0);
  private fuTimer: any;

  // ─────────────────────────────────────────────
  //  FORGOT PASSWORD
  // ─────────────────────────────────────────────

  forgotPStep = signal<ForgotPStep>('input');

  fpEmail          = '';
  fpCaptchaAnswer  = '';
  fpOtp            = '';
  fpNewPassword    = '';
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

  // ─────────────────────────────────────────────
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadCaptcha('register');
    this.setupUsernameCheck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    clearInterval(this.regTimer);
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

  backToRegister(): void {
    this.activeView.set('register');
    this.error.set('');
  }

  // ─────────────────────────────────────────────
  //  CAPTCHA
  // ─────────────────────────────────────────────

  loadCaptcha(target: 'register' | 'forgotU' | 'forgotP'): void {
    this.authService.getCaptcha().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          if (target === 'register') {
            this.captchaToken.set(res.data.token);
            this.captchaText.set(res.data.captchaText);
            this.captchaAnswer = '';
          } else if (target === 'forgotU') {
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

  refreshCaptcha(target: 'register' | 'forgotU' | 'forgotP'): void {
    if (target === 'register') this.regErrors.captcha = undefined;
    if (target === 'forgotU')  this.fuErrors.captcha  = undefined;
    if (target === 'forgotP')  this.fpErrors.captcha  = undefined;
    this.loadCaptcha(target);
  }

  // ─────────────────────────────────────────────
  //  USERNAME CHECK
  // ─────────────────────────────────────────────

  setupUsernameCheck(): void {
    this.usernameInput$.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(u => {
      if (!u || u.length < 4) {
        this.usernameStatus.set('idle');
        this.usernameMessage.set('');
        return;
      }
      this.usernameStatus.set('checking');
      this.authService.checkUsername(u).subscribe({
        next: (res) => {
          if (res.data) {
            this.usernameStatus.set(res.data.available ? 'available' : 'taken');
            this.usernameMessage.set(res.data.message);
          }
        },
        error: () => this.usernameStatus.set('idle'),
      });
    });
  }

  onUsernameInput(): void {
    if (!this.username.match(/^[a-zA-Z0-9._]*$/)) {
      this.usernameStatus.set('invalid');
      this.usernameMessage.set('Only letters, numbers, dots and underscores allowed.');
      return;
    }
    this.usernameStatus.set('checking');
    this.usernameInput$.next(this.username);
  }

  // ─────────────────────────────────────────────
  //  REGISTER — VALIDATION
  // ─────────────────────────────────────────────

  private validateRegForm(): boolean {
    this.regErrors = {};
    let ok = true;

    if (!this.email.trim()) {
      this.regErrors.email = 'Email is required.'; ok = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      this.regErrors.email = 'Enter a valid email address.'; ok = false;
    }

    // if (!this.username.trim()) {
    //   this.regErrors.username = 'Username is required.'; ok = false;
    // } else if (this.username.length < 4) {
    //   this.regErrors.username = 'Username must be at least 4 characters.'; ok = false;
    // } else if (this.usernameStatus() === 'taken') {
    //   this.regErrors.username = 'Username is already taken.'; ok = false;
    // } else if (this.usernameStatus() === 'invalid') {
    //   this.regErrors.username = 'Invalid username format.'; ok = false;
    // }

    if (!this.password) {
      this.regErrors.password = 'Password is required.'; ok = false;
    } else if (this.password.length < 6) {
      this.regErrors.password = 'Minimum 6 characters.'; ok = false;
    }

    if (!this.confirmPassword) {
      this.regErrors.confirmPassword = 'Please confirm your password.'; ok = false;
    } else if (this.password !== this.confirmPassword) {
      this.regErrors.confirmPassword = 'Passwords do not match.'; ok = false;
    }

    if (!this.captchaAnswer.trim()) {
      this.regErrors.captcha = 'Please enter the captcha.'; ok = false;
    }

    return ok;
  }

  // ─────────────────────────────────────────────
  //  REGISTER — SEND OTP
  // ─────────────────────────────────────────────

  sendOtp(): void {
    if (!this.validateRegForm()) return;
    this.loading.set(true);
    this.error.set('');

    this.authService.sendRegistrationOtp({
      email:         this.email.trim().toLowerCase(),
      captchaToken:  this.captchaToken(),
      captchaAnswer: this.captchaAnswer.trim(),
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.regStep.set('otp');
          this.successMsg.set(`OTP sent to ${this.email}`);
          this.startTimer('reg');
        } else {
          this.error.set(res.message);
          this.refreshCaptcha('register');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.extractError(err));
        this.refreshCaptcha('register');
      },
    });
  }

  // ─────────────────────────────────────────────
  //  REGISTER — VERIFY OTP + CREATE
  // ─────────────────────────────────────────────

  verifyAndRegister(): void {
    if (!this.otp || this.otp.length !== 6) {
      this.regErrors.otp = 'Enter valid 6-digit OTP.'; return;
    }
    this.regErrors.otp = undefined;
    this.loading.set(true);
    this.error.set('');

    this.authService.register({
      email:           this.email.trim().toLowerCase(),
      username:        this.username.trim(),
      password:        this.password,
      confirmPassword: this.confirmPassword,
      otp:             this.otp,
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.regStep.set('success');
          clearInterval(this.regTimer);
        } else {
          this.error.set(res.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const code = err.error?.errorCode || '';
        if (code.includes('OTP')) {
          this.regErrors.otp = err.error?.message || 'Invalid OTP.';
        } else {
          this.error.set(this.extractError(err));
        }
      },
    });
  }

  resendRegOtp(): void {
    if (this.resendCooldown() > 0) return;
    this.loading.set(true);
    this.regErrors.otp = undefined;
    this.error.set('');

    this.authService.resendOtp(this.email, 'REGISTRATION').subscribe({
      next: () => {
        this.loading.set(false);
        this.successMsg.set('New OTP sent to your email.');
        this.otp = '';
        this.startTimer('reg');
        setTimeout(() => this.successMsg.set(''), 4000);
      },
      error: (err) => {
        this.loading.set(false);
        this.regErrors.otp = this.extractError(err);
      },
    });
  }

  goBackToForm(): void {
    this.regStep.set('form');
    this.otp = '';
    this.regErrors.otp = undefined;
    this.error.set('');
    this.refreshCaptcha('register');
    clearInterval(this.regTimer);
    this.resendCooldown.set(0);
  }

  goToLogin(): void { this.router.navigate(['/login']); }

  // ─────────────────────────────────────────────
  //  FORGOT USERNAME — STEP 1: input
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

  // ─────────────────────────────────────────────
  //  FORGOT USERNAME — STEP 2: verify OTP
  // ─────────────────────────────────────────────

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
  //  FORGOT PASSWORD — STEP 1: input
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

  // ─────────────────────────────────────────────
  //  FORGOT PASSWORD — STEP 2: verify OTP
  // ─────────────────────────────────────────────

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

  // ─────────────────────────────────────────────
  //  FORGOT PASSWORD — STEP 3: reset
  // ─────────────────────────────────────────────

  submitResetPassword(): void {
    this.fpErrors = {};
    let ok = true;

    if (!this.fpNewPassword) {
      this.fpErrors.newPassword = 'Password is required.'; ok = false;
    } else if (this.fpNewPassword.length < 6) {
      this.fpErrors.newPassword = 'Minimum 6 characters.'; ok = false;
    }

    if (!this.fpConfirmPassword) {
      this.fpErrors.confirmPassword = 'Please confirm your password.'; ok = false;
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
          this.fpErrors.otp = err.error?.message || 'Invalid OTP. Go back and try again.';
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
  //  TIMER HELPER
  // ─────────────────────────────────────────────

  private startTimer(target: 'reg' | 'fu' | 'fp'): void {
    const setVal = (v: number) => {
      if (target === 'reg') this.resendCooldown.set(v);
      if (target === 'fu')  this.fuResendCooldown.set(v);
      if (target === 'fp')  this.fpResendCooldown.set(v);
    };
    const timer = target === 'reg' ? 'regTimer' :
                  target === 'fu'  ? 'fuTimer'  : 'fpTimer';

    setVal(60);
    clearInterval((this as any)[timer]);
    (this as any)[timer] = setInterval(() => {
      const cur = target === 'reg' ? this.resendCooldown() :
                  target === 'fu'  ? this.fuResendCooldown() : this.fpResendCooldown();
      if (cur <= 1) { clearInterval((this as any)[timer]); setVal(0); }
      else setVal(cur - 1);
    }, 1000);
  }

  // ─────────────────────────────────────────────
  //  MISC TOGGLES
  // ─────────────────────────────────────────────

  togglePassword():        void { this.showPassword.update(v => !v); }
  toggleConfirmPassword(): void { this.showConfirm.update(v => !v); }
  toggleFpPassword():      void { this.showFpPassword.update(v => !v); }
  toggleFpConfirm():       void { this.showFpConfirm.update(v => !v); }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    switch (err.status) {
      case 0:   return 'Cannot connect to server.';
      case 404: return 'No account found with this email.';
      case 409: return err.error?.message || 'Already exists.';
      case 400: return err.error?.message || 'Invalid input.';
      case 500: return 'Server error. Please try again later.';
      default:  return 'Something went wrong. Please try again.';
    }
  }
}