// -------------------------  register-multi.component.ts  -------------------------

import { Component, signal, OnInit } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
// import { AuthService } from '../../../core/services/auth.service';
// import { KycService } from '../../../core/services/kyc.service';
import { ApplicationService, ApplicationResponse } from '../../../core/services/application.service';

// ─────────────────────────────────────────────
//  MODELS
// ─────────────────────────────────────────────

export type AccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type MaritalStatus = 'SINGLE' | 'MARRIED' | 'DIVORCED' | 'WIDOWED';
export type NomineeRelation =
  | 'SPOUSE' | 'FATHER' | 'MOTHER' | 'SON' | 'DAUGHTER' | 'SIBLING' | 'OTHER';

export interface Step1CustomerDetails {
  accountType: AccountType | '';
  fullName: string;
  fatherName: string;
  motherName: string;
  dateOfBirth: string;
  gender: Gender | '';
  maritalStatus: MaritalStatus | '';
  nationality: string;
  occupation: string;
  annualIncome: string;
  phoneNumber: string;
  emailId: string;
  aadhaarNumber: string;
  panNumber: string;
}

export interface Step2AddressDetails {
  currentAddressLine: string;
  currentCity: string;
  currentState: string;
  currentPincode: string;
  permanentAddressLine: string;
  permanentCity: string;
  permanentState: string;
  permanentPincode: string;
  sameAsCurrent: boolean;
}

export interface Step3NomineeDetails {
  nomineeName: string;
  nomineeRelation: NomineeRelation | '';
  nomineeAge: string;
  nomineeMobileNumber: string;
  nomineeAddress: string;
}

export interface Step4Documents {
  aadhaarCardFile: File | null;
  panCardFile: File | null;
  passportFile: File | null;
  voterIdFile: File | null;
  profilePhoto: File | null;
  signatureImage: File | null;
  addressProofDocument: File | null;
}

export interface FormErrors {
// Step 1
  accountType?: string;
  fullName?: string;
  fatherName?: string;
  motherName?: string;
  dateOfBirth?: string;
  gender?: string;
  occupation?: string;
  phoneNumber?: string;
  emailId?: string;
  aadhaarNumber?: string;
  panNumber?: string;
  // Step 2
  currentAddressLine?: string;
  currentCity?: string;
  currentState?: string;
  currentPincode?: string;
  permanentAddressLine?: string;
  permanentCity?: string;
  permanentState?: string;
  permanentPincode?: string;
  // Step 3
  nomineeName?: string;
  nomineeRelation?: string;
  nomineeAge?: string;
  nomineeMobileNumber?: string;
  nomineeAddress?: string;
  // Step 4
  aadhaarCardFile?: string;
  panCardFile?: string;
  profilePhoto?: string;
  signatureImage?: string;
  addressProofDocument?: string;
  // Step 5
  captcha?: string;
  confirmCheck?: string;
  otp?: string;
  password?: string;
  confirmPassword?: string;
}

// ─────────────────────────────────────────────
//  COMPONENT
// ─────────────────────────────────────────────

@Component({
  selector: 'app-register-multi',
  templateUrl: './register-multi.component.html',
  styleUrls: ['./register-multi.component.css'],
  standalone: true,
  imports: [RouterLink ,CommonModule, FormsModule],
})
export class RegisterMultiComponent implements OnInit {

  // ── Navigation state ──
  step = signal(1);
  totalSteps = 5;

  // ── UI state ──
  loading = signal(false);
  error = signal('');
  successMessage = signal('');
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  showOtpModal = signal(false);
  showSuccessScreen = signal(false);

  // ── Auth data ──
  userId = signal<number | null>(null);
  applicationId = signal('');

  // ── STEP 1 ──
customerDetails: Step1CustomerDetails = {
    accountType: '',
    fullName: '',
    fatherName: '',
    motherName: '',
    dateOfBirth: '',
    gender: '',
    maritalStatus: '',
    nationality: 'India',
    occupation: '',
    annualIncome: '',
    phoneNumber: '',
    emailId: '',
    aadhaarNumber: '',
    panNumber: '',
  };
  password = '';
  confirmPassword = '';

  // ── STEP 2 ──
  addressDetails: Step2AddressDetails = {
    currentAddressLine: '',
    currentCity: '',
    currentState: '',
    currentPincode: '',
    permanentAddressLine: '',
    permanentCity: '',
    permanentState: '',
    permanentPincode: '',
    sameAsCurrent: false,
  };

  // ── STEP 3 ──
  nomineeDetails: Step3NomineeDetails = {
    nomineeName: '',
    nomineeRelation: '',
    nomineeAge: '',
    nomineeMobileNumber: '',
    nomineeAddress: '',
  };

  // ── STEP 4 ──
  documents: Step4Documents = {
    aadhaarCardFile: null,
    panCardFile: null,
    passportFile: null,
    voterIdFile: null,
    profilePhoto: null,
    signatureImage: null,
    addressProofDocument: null,
  };

  // File name signals for display
  aadhaarCardFileName = signal('');
  panCardFileName = signal('');
  passportFileName = signal('');
  voterIdFileName = signal('');
  profilePhotoName = signal('');
  signatureImageName = signal('');
  addressProofDocumentName = signal('');

  // ── STEP 5 ──
  captchaCode = signal('');
  captchaUserInput = '';
  confirmCheck = false;
  otp = '';
  generatedOtp = ''; // demo only – replace with real API

  errors: FormErrors = {};

  // ── Dropdown options ──
  readonly accountTypes = [
    { value: 'SAVINGS', label: 'Savings Account' },
    { value: 'CURRENT', label: 'Current Account' },
    { value: 'SALARY',  label: 'Salary Account'  },
  ];
  readonly genders = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
    { value: 'OTHER', label: 'Other' },
  ];
  readonly maritalStatuses = [
    { value: 'SINGLE',   label: 'Single'   },
    { value: 'MARRIED',  label: 'Married'  },
    { value: 'DIVORCED', label: 'Divorced' },
    { value: 'WIDOWED',  label: 'Widowed'  },
  ];
  readonly nomineeRelations = [
    { value: 'SPOUSE',   label: 'Spouse'   },
    { value: 'FATHER',   label: 'Father'   },
    { value: 'MOTHER',   label: 'Mother'   },
    { value: 'SON',      label: 'Son'      },
    { value: 'DAUGHTER', label: 'Daughter' },
    { value: 'SIBLING',  label: 'Sibling'  },
    { value: 'OTHER',    label: 'Other'    },
  ];

  readonly stepDefs = [
    { n: 1, label: 'Details' },
    { n: 2, label: 'Address' },
    { n: 3, label: 'Nominee' },
    { n: 4, label: 'Docs' },
    { n: 5, label: 'Review' },
  ];

  readonly today = new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' });
  readonly maxDob = new Date(new Date().setFullYear(new Date().getFullYear() - 18)).toISOString().split('T')[0];

  readonly indianStates = [
    'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh','Goa','Gujarat',
    'Haryana','Himachal Pradesh','Jharkhand','Karnataka','Kerala','Madhya Pradesh',
    'Maharashtra','Manipur','Meghalaya','Mizoram','Nagaland','Odisha','Punjab',
    'Rajasthan','Sikkim','Tamil Nadu','Telangana','Tripura','Uttar Pradesh',
    'Uttarakhand','West Bengal','Delhi','Jammu & Kashmir','Ladakh',
  ];

constructor(
    private applicationService: ApplicationService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const s = Number(params['start']);
      if (!isNaN(s) && s >= 1 && s <= 5) this.step.set(s);
    });
    this.refreshCaptcha();
  }


  // ── Success popup data ──
  applicationResponse = signal<ApplicationResponse['data'] | null>(null);
  showSuccessPopup = signal(false);
  pdfAutoDownloaded = false;

  // ─────────────────────────────────────────────
  //  HELPERS
  // ─────────────────────────────────────────────

  get progressPercent(): number { return (this.step() / this.totalSteps) * 100; }

  getStepTitle(): string {
    const titles = ['Customer Details','Address Details','Nominee Details','Upload Documents','Review & Submit'];
    return titles[this.step() - 1] ?? '';
  }

  getStepSubtitle(): string {
    const subs = [
      'Personal & contact information',
      'Current & permanent address',
      'Beneficiary information',
      'Identity & address proof documents',
      'Verify all details before submission',
    ];
    return subs[this.step() - 1] ?? '';
  }

  isStepDone(n: number): boolean { return this.step() > n; }
  isStepActive(n: number): boolean { return this.step() === n; }

  togglePassword(): void { this.showPassword.update(v => !v); }
  toggleConfirmPassword(): void { this.showConfirmPassword.update(v => !v); }

  // ─────────────────────────────────────────────
  //  CAPTCHA
  // ─────────────────────────────────────────────

  refreshCaptcha(): void {
    const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
    this.captchaCode.set(code);
    this.captchaUserInput = '';
  }

  // ─────────────────────────────────────────────
  //  SAME ADDRESS COPY
  // ─────────────────────────────────────────────

  onSameAsCurrentChange(): void {
    if (this.addressDetails.sameAsCurrent) {
      this.addressDetails.permanentAddressLine = this.addressDetails.currentAddressLine;
      this.addressDetails.permanentCity = this.addressDetails.currentCity;
      this.addressDetails.permanentState = this.addressDetails.currentState;
      this.addressDetails.permanentPincode = this.addressDetails.currentPincode;
    } else {
      this.addressDetails.permanentAddressLine = '';
      this.addressDetails.permanentCity = '';
      this.addressDetails.permanentState = '';
      this.addressDetails.permanentPincode = '';
    }
  }

  // ─────────────────────────────────────────────
  //  FILE HANDLING
  // ─────────────────────────────────────────────

  onFileSelect(event: Event, field: keyof Step4Documents, imageOnly = false): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;
    const file = input.files[0];

    const allowedTypes = imageOnly
      ? ['image/jpeg', 'image/png']
      : ['application/pdf', 'image/jpeg', 'image/png'];
    const maxMB = imageOnly ? 2 : 5;

    if (!allowedTypes.includes(file.type)) {
      this.setFileError(field, `Only ${imageOnly ? 'JPG/PNG' : 'PDF/JPG/PNG'} files allowed`);
      return;
    }
    if (file.size > maxMB * 1024 * 1024) {
      this.setFileError(field, `File must be under ${maxMB}MB`);
      return;
    }

    this.documents[field] = file;
    this.setFileName(field, file.name);
    this.clearFieldError(field);
  }

  onRemoveFile(field: keyof Step4Documents): void {
    this.documents[field] = null;
    this.setFileName(field, '');
  }

  private setFileName(field: keyof Step4Documents, name: string): void {
    const map: Record<keyof Step4Documents, () => void> = {
      aadhaarCardFile:      () => this.aadhaarCardFileName.set(name),
      panCardFile:          () => this.panCardFileName.set(name),
      passportFile:         () => this.passportFileName.set(name),
      voterIdFile:          () => this.voterIdFileName.set(name),
      profilePhoto:         () => this.profilePhotoName.set(name),
      signatureImage:       () => this.signatureImageName.set(name),
      addressProofDocument: () => this.addressProofDocumentName.set(name),
    };
    map[field]?.();
  }

  getFileName(field: keyof Step4Documents): string {
    const map: Record<keyof Step4Documents, () => string> = {
      aadhaarCardFile:      () => this.aadhaarCardFileName(),
      panCardFile:          () => this.panCardFileName(),
      passportFile:         () => this.passportFileName(),
      voterIdFile:          () => this.voterIdFileName(),
      profilePhoto:         () => this.profilePhotoName(),
      signatureImage:       () => this.signatureImageName(),
      addressProofDocument: () => this.addressProofDocumentName(),
    };
    return map[field]?.() ?? '';
  }

  private setFileError(field: keyof Step4Documents, msg: string): void {
    (this.errors as any)[field] = msg;
  }

  private clearFieldError(field: string): void {
    delete (this.errors as any)[field];
  }

  // ─────────────────────────────────────────────
  //  VALIDATION
  // ─────────────────────────────────────────────

  private validateStep1(): boolean {
    this.errors = {};
    let ok = true;
    const d = this.customerDetails;

    if (!d.accountType) { this.errors.accountType = 'Account type is required'; ok = false; }
    if (!d.fullName?.trim()) { this.errors.fullName = 'Full name is required'; ok = false; }
    if (!d.fatherName?.trim()) { this.errors.fatherName = "Father's name is required"; ok = false; }
    if (!d.motherName?.trim()) { this.errors.motherName = "Mother's name is required"; ok = false; }
    if (!d.dateOfBirth) { this.errors.dateOfBirth = 'Date of birth is required'; ok = false; }
    if (!d.gender) { this.errors.gender = 'Gender is required'; ok = false; }
    if (!d.occupation?.trim()) { this.errors.occupation = 'Occupation is required'; ok = false; }

    if (!d.phoneNumber) {
      this.errors.phoneNumber = 'Mobile number is required'; ok = false;
    } else if (!/^[6-9]\d{9}$/.test(d.phoneNumber)) {
      this.errors.phoneNumber = 'Enter valid 10-digit Indian mobile number'; ok = false;
    }

    if (!d.emailId) {
      this.errors.emailId = 'Email is required'; ok = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.emailId)) {
      this.errors.emailId = 'Enter a valid email address'; ok = false;
    }

    // if (!this.password) {
    //   this.errors['password' as keyof FormErrors] = 'Password is required'; ok = false;
    // } else if (this.password.length < 6) {
    //   this.errors['password' as keyof FormErrors] = 'Minimum 6 characters'; ok = false;
    // }
    // if (!this.confirmPassword) {
    //   this.errors['confirmPassword' as keyof FormErrors] = 'Please confirm password'; ok = false;
    // } else if (this.password !== this.confirmPassword) {
    //   this.errors['confirmPassword' as keyof FormErrors] = 'Passwords do not match'; ok = false;
    // }

    if (!d.aadhaarNumber) {
      this.errors.aadhaarNumber = 'Aadhaar number is required'; ok = false;
    } else if (!/^\d{12}$/.test(d.aadhaarNumber)) {
      this.errors.aadhaarNumber = 'Enter valid 12-digit Aadhaar number'; ok = false;
    }

    if (!d.panNumber) {
      this.errors.panNumber = 'PAN number is required'; ok = false;
    } else if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(d.panNumber.toUpperCase())) {
      this.errors.panNumber = 'Enter valid PAN (e.g. ABCDE1234F)'; ok = false;
    }

    return ok;
  }

  private validateStep2(): boolean {
    this.errors = {};
    let ok = true;
    const a = this.addressDetails;

    if (!a.currentAddressLine?.trim()) { this.errors.currentAddressLine = 'Address is required'; ok = false; }
    if (!a.currentCity?.trim()) { this.errors.currentCity = 'City is required'; ok = false; }
    if (!a.currentState?.trim()) { this.errors.currentState = 'State is required'; ok = false; }
    if (!a.currentPincode) { this.errors.currentPincode = 'Pincode is required'; ok = false; }
    else if (!/^\d{6}$/.test(a.currentPincode)) { this.errors.currentPincode = '6-digit pincode required'; ok = false; }

    if (!a.permanentAddressLine?.trim()) { this.errors.permanentAddressLine = 'Address is required'; ok = false; }
    if (!a.permanentCity?.trim()) { this.errors.permanentCity = 'City is required'; ok = false; }
    if (!a.permanentState?.trim()) { this.errors.permanentState = 'State is required'; ok = false; }
    if (!a.permanentPincode) { this.errors.permanentPincode = 'Pincode is required'; ok = false; }
    else if (!/^\d{6}$/.test(a.permanentPincode)) { this.errors.permanentPincode = '6-digit pincode required'; ok = false; }

    return ok;
  }

  private validateStep3(): boolean {
    this.errors = {};
    let ok = true;
    const n = this.nomineeDetails;

    if (!n.nomineeName?.trim()) { this.errors.nomineeName = 'Nominee name is required'; ok = false; }
    if (!n.nomineeRelation) { this.errors.nomineeRelation = 'Relation is required'; ok = false; }
    if (!n.nomineeAge) { this.errors.nomineeAge = 'Age is required'; ok = false; }
    else if (Number(n.nomineeAge) < 1 || Number(n.nomineeAge) > 120) { this.errors.nomineeAge = 'Enter valid age'; ok = false; }
    if (!n.nomineeMobileNumber) {
      this.errors.nomineeMobileNumber = 'Mobile number is required'; ok = false;
    } else if (!/^\d{10}$/.test(n.nomineeMobileNumber)) {
      this.errors.nomineeMobileNumber = 'Enter valid 10-digit number'; ok = false;
    }
    if (!n.nomineeAddress?.trim()) { this.errors.nomineeAddress = 'Address is required'; ok = false; }

    return ok;
  }

  private validateStep4(): boolean {
    this.errors = {};
    let ok = true;
    const requiredDocs: Array<[keyof Step4Documents, string]> = [
      ['aadhaarCardFile',      'Aadhaar Card is required'],
      ['panCardFile',          'PAN Card is required'],
      ['profilePhoto',         'Profile photo is required'],
      ['signatureImage',       'Signature image is required'],
      ['addressProofDocument', 'Address proof is required'],
    ];
    requiredDocs.forEach(([field, msg]) => {
      if (!this.documents[field]) { (this.errors as any)[field] = msg; ok = false; }
    });
    return ok;
  }

  private validateStep5(): boolean {
    this.errors = {};
    let ok = true;
    if (!this.captchaUserInput || this.captchaUserInput.toUpperCase() !== this.captchaCode()) {
      this.errors.captcha = 'Captcha does not match. Please try again.';
      this.refreshCaptcha();
      ok = false;
    }
    if (!this.confirmCheck) {
      this.errors.confirmCheck = 'Please confirm that all details are correct.';
      ok = false;
    }
    return ok;
  }

  // ─────────────────────────────────────────────
  //  NAVIGATION
  // ─────────────────────────────────────────────

  goToStep(n: number): void {
  // Only allow clicking on already-completed steps
  if (!this.isStepDone(n)) return;
  this.errors = {};
  this.error.set('');
  this.step.set(n);
  window.scrollTo(0, 0);
}

  // goNext(): void {
  //   this.error.set('');
  //   const validators: Record<number, () => boolean> = {
  //     1: () => this.validateStep1(),
  //     2: () => this.validateStep2(),
  //     3: () => this.validateStep3(),
  //     4: () => this.validateStep4(),
  //   };
  //   if (validators[this.step()] && !validators[this.step()]()) return;

  //   if (this.step() === 1) {
  //     this.registerUser();
  //   } else {
  //     this.step.update(s => Math.min(5, s + 1));
  //     window.scrollTo(0, 0);
  //   }
  // }

  goNext(): void {
  this.error.set('');
  const validators: Record<number, () => boolean> = {
    1: () => this.validateStep1(),
    2: () => this.validateStep2(),
    3: () => this.validateStep3(),
    4: () => this.validateStep4(),
  };

  if (validators[this.step()] && !validators[this.step()]()) return;

  // No backend call here — just advance locally
  this.step.update(s => Math.min(5, s + 1));
  window.scrollTo(0, 0);
}

  goBack(): void {
    this.errors = {};
    this.error.set('');
    this.step.update(s => Math.max(1, s - 1));
    window.scrollTo(0, 0);
  }

  // ─────────────────────────────────────────────
  //  API CALLS
  // ─────────────────────────────────────────────

  // private registerUser(): void {
  //   this.loading.set(true);
  //   this.authService.register({
  //     email: this.customerDetails.emailId,
  //     phone: this.customerDetails.phoneNumber,
  //     password: this.password,
  //   }).subscribe({
  //     next: (res) => {
  //       this.loading.set(false);
  //       if (res.success) {
  //         this.userId.set(res.data?.id ?? null);
  //         this.step.set(2);
  //         window.scrollTo(0, 0);
  //       } else {
  //         this.error.set(res.message);
  //       }
  //     },
  //     error: (err) => {
  //       this.loading.set(false);
  //       this.error.set(err.error?.message || 'Registration failed. Please try again.');
  //     },
  //   });
  // }

// ── Step 5: Send OTP then show modal ──

  // ── Step 5: Send OTP ──
submitApplication(): void {
  if (!this.validateStep5()) return;
  this.loading.set(true);
  this.error.set('');

  this.applicationService.sendOtp(this.customerDetails.emailId).subscribe({
    next: (res) => {
      this.loading.set(false);
      if (res.success) {
        this.otp = '';
        this.errors.otp = undefined;
        this.showOtpModal.set(true);
      } else {
        this.error.set(res.message || 'Failed to send OTP. Please try again.');
      }
    },
    error: (err) => {
      this.loading.set(false);
      // ── No demo fallback — show real error ──
      this.error.set(this.extractErrorMessage(err));
    },
  });
}

onResendOtp(): void {
  this.loading.set(true);
  this.errors.otp = undefined;

  this.applicationService.sendOtp(this.customerDetails.emailId).subscribe({
    next: (res) => {
      this.loading.set(false);
      if (res.success) {
        this.successMessage.set(`New OTP sent to ${this.customerDetails.emailId}`);
        setTimeout(() => this.successMessage.set(''), 3000);
      } else {
        this.errors.otp = res.message || 'Failed to resend OTP.';
      }
    },
    error: (err) => {
      this.loading.set(false);
      // ── No demo fallback — show real error ──
      this.errors.otp = this.extractErrorMessage(err);
    },
  });
}

onVerifyOtp(): void {
  if (!this.otp || this.otp.length !== 6) {
    this.errors.otp = 'Enter 6-digit OTP';
    return;
  }
  this.errors.otp = undefined;
  this.showOtpModal.set(false);
  this.finalSubmit();
}

closeOtpModal(): void {
  this.showOtpModal.set(false);
  this.otp = '';
  this.errors.otp = undefined;
}

private finalSubmit(): void {
  this.loading.set(true);
  this.error.set('');

  const formData = new FormData();

  // ── Customer details ──
  Object.entries(this.customerDetails).forEach(([k, v]) =>
    formData.append(k, v ?? '')
  );

  // ── OTP ──
  formData.append('otp', this.otp);

  // ── Address ──
  Object.entries(this.addressDetails).forEach(([k, v]) =>
    formData.append(k, String(v))
  );

  // ── Nominee ──
  Object.entries(this.nomineeDetails).forEach(([k, v]) =>
    formData.append(k, v ?? '')
  );

  // ── Files ──
  (Object.entries(this.documents) as [string, File | null][]).forEach(([k, f]) => {
    if (f) formData.append(k, f, f.name);
  });

  this.applicationService.submitApplication(formData).subscribe({
    next: (res) => {
      this.loading.set(false);
      if (res.success && res.data) {
        this.applicationId.set(res.data.applicationId);
        this.applicationResponse.set(res.data);
        this.showSuccessPopup.set(true);
        // Auto-download PDF after 3 seconds
        setTimeout(() => {
          if (!this.pdfAutoDownloaded) {
            this.pdfAutoDownloaded = true;
            this.downloadPDF();
          }
        }, 3000);
      } else {
        this.error.set(res.message || 'Submission failed. Please try again.');
      }
    },
    error: (err) => {
      this.loading.set(false);
      this.handleSubmitError(err);
    },
  });
}

  private generateAppId(): string {
    return 'NB' + Date.now().toString(36).toUpperCase() +
           Math.random().toString(36).substr(2, 4).toUpperCase();
  }

  goToLogin(): void { this.router.navigate(['/login']); }
    goToHome(): void { this.router.navigate(['/home']); }

  // ─────────────────────────────────────────────
  //  PDF DOWNLOAD (client-side via jsPDF CDN)
  // ─────────────────────────────────────────────

  onDownloadAndClose(): void {
    this.pdfAutoDownloaded = true;
    this.downloadPDF();
    this.showSuccessPopup.set(false);
    this.showSuccessScreen.set(true);
    window.scrollTo(0, 0);
  }

  onCloseSuccessPopup(): void {
    // If not downloaded yet — auto download on close
    if (!this.pdfAutoDownloaded) {
      this.pdfAutoDownloaded = true;
      this.downloadPDF();
    }
    this.showSuccessPopup.set(false);
    this.showSuccessScreen.set(true);
    window.scrollTo(0, 0);
  }
  
  downloadPDF(): void {
    const script = document.createElement('script');
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js';
    script.onload = () => this.generatePDF();
    document.head.appendChild(script);
  }

  private generatePDF(): void {
    const { jsPDF } = (window as any).jspdf;
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const W = 210;
    let y = 36;

    const line = (text: string, size = 10, bold = false, color = [0, 0, 0]) => {
      doc.setFontSize(size); doc.setFont('helvetica', bold ? 'bold' : 'normal');
      doc.setTextColor(...color);
      const lines: string[] = doc.splitTextToSize(text, W - 40);
      lines.forEach((l: string) => { if (y > 270) { doc.addPage(); y = 20; } doc.text(l, 20, y); y += size * 0.45 + 2; });
    };
    const section = (title: string) => {
      y += 5; doc.setDrawColor(99, 102, 241); doc.setLineWidth(0.5); doc.line(20, y, W - 20, y); y += 5;
      line(title, 11, true, [99, 102, 241]); y += 2;
    };
    const row = (label: string, val: string) => {
      doc.setFontSize(9); doc.setFont('helvetica', 'bold'); doc.setTextColor(100, 116, 139);
      doc.text(label + ':', 20, y);
      doc.setFont('helvetica', 'normal'); doc.setTextColor(15, 23, 42);
      const lines: string[] = doc.splitTextToSize(val || '—', W - 80);
      lines.forEach((l: string, i: number) => doc.text(l, 80, y + i * 4.5));
      y += Math.max(5, lines.length * 4.5) + 2;
    };

    // Header
    doc.setFillColor(99, 102, 241); doc.rect(0, 0, W, 28, 'F');
    doc.setFontSize(16); doc.setFont('helvetica', 'bold'); doc.setTextColor(255, 255, 255);
    doc.text('NeoBank — Account Opening Application', W / 2, 12, { align: 'center' });
    doc.setFontSize(9); doc.setFont('helvetica', 'normal');
    doc.text(`Application ID: ${this.applicationId()}   |   Date: ${new Date().toLocaleDateString('en-IN')}`, W / 2, 21, { align: 'center' });

    section('PERSONAL INFORMATION');
    const d = this.customerDetails;
    row('Account Type', d.accountType); row('Full Name', d.fullName);
    row("Father's Name", d.fatherName); row("Mother's Name", d.motherName);
    row('Date of Birth', d.dateOfBirth); row('Gender', d.gender);
    row('Marital Status', d.maritalStatus); row('Nationality', d.nationality);
    row('Occupation', d.occupation);
    row('Annual Income', d.annualIncome ? '₹' + Number(d.annualIncome).toLocaleString() : '—');
    row('Mobile', d.phoneNumber); row('Email', d.emailId);

    section('CURRENT ADDRESS');
    const a = this.addressDetails;
    row('Address', a.currentAddressLine);
    row('City / State', `${a.currentCity}, ${a.currentState}`);
    row('Pincode', a.currentPincode);

    section('PERMANENT ADDRESS');
    row('Address', a.permanentAddressLine);
    row('City / State', `${a.permanentCity}, ${a.permanentState}`);
    row('Pincode', a.permanentPincode);

    section('NOMINEE DETAILS');
    const n = this.nomineeDetails;
    row('Nominee Name', n.nomineeName); row('Relation', n.nomineeRelation);
    row('Age', n.nomineeAge); row('Mobile', n.nomineeMobileNumber);
    row('Address', n.nomineeAddress);

    section('DOCUMENTS SUBMITTED');
    const docs: Array<[keyof Step4Documents, string]> = [
      ['aadhaarCardFile', 'Aadhaar Card'],
      ['panCardFile', 'PAN Card'],
      ['passportFile', 'Passport'],
      ['voterIdFile', 'Voter ID'],
      ['profilePhoto', 'Profile Photo'],
      ['signatureImage', 'Signature'],
      ['addressProofDocument', 'Address Proof'],
    ];
    docs.forEach(([field, label]) => {
      const f = this.documents[field];
      row(label, f ? `✓ Uploaded (${f.name})` : 'Not uploaded');
    });

    section('APPLICATION STATUS');
    row('Status', 'SUCCESS — Submitted for Verification');
    row('Application ID', this.applicationId());
    row('Submitted On', new Date().toLocaleString('en-IN'));

    y += 6;
    doc.setFontSize(8); doc.setTextColor(150, 150, 150);
    doc.text('This is a computer-generated document. For support: support@neobank.in', W / 2, y, { align: 'center' });

    doc.save(`NeoBank_Application_${this.applicationId()}.pdf`);
  }

  // ─────────────────────────────────────────────
//  ERROR HANDLERS
// ─────────────────────────────────────────────

private extractErrorMessage(err: any): string {
  // Spring Boot ApiResponseDTO error format
  if (err.error?.message) return err.error.message;

  // HTTP status based fallbacks
  switch (err.status) {
    case 0:    return 'Cannot connect to server. Please check your internet connection.';
    case 400:  return err.error?.message || 'Invalid request. Please check your details.';
    case 404:  return 'Service not found. Please try again later.';
    case 409:  return err.error?.message || 'A conflict occurred. Please try again.';
    case 429:  return 'Too many attempts. Please wait and try again.';
    case 500:  return 'Server error. Please try again later.';
    case 503:  return 'Service temporarily unavailable. Please try again later.';
    default:   return 'Something went wrong. Please try again.';
  }
}

private handleSubmitError(err: any): void {
  const errorCode  = err.error?.errorCode  || '';
  const message    = err.error?.message    || '';

  switch (errorCode) {

    // ── OTP errors → reopen modal ──
    case 'OTP_INVALID':
    case 'OTP_EXPIRED':
    case 'OTP_NOT_FOUND':
      this.errors.otp = message;
      this.otp = '';
      this.showOtpModal.set(true);
      break;

    // ── Max OTP attempts → reopen modal, disable resend hint ──
    case 'OTP_MAX_ATTEMPTS':
      this.errors.otp = message;
      this.otp = '';
      this.showOtpModal.set(true);
      break;

    // ── Duplicate application ──
    case 'ACTIVE_APPLICATION_EXISTS':
      this.error.set(message);
      this.step.set(1);
      window.scrollTo(0, 0);
      break;

    // ── Email send failed ──
    case 'EMAIL_SEND_FAILED':
      this.error.set('Failed to send OTP email. Please check your email address and try again.');
      break;

    // ── Generic / unknown ──
    default:
      // Check message text as fallback (in case errorCode missing)
      if (message.toLowerCase().includes('otp')) {
        this.errors.otp = message;
        this.otp = '';
        this.showOtpModal.set(true);
      } else {
        this.error.set(this.extractErrorMessage(err));
      }
      break;
  }
}
}
