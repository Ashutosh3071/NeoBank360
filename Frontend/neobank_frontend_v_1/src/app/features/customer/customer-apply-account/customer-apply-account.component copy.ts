// customer-apply-account.component.ts

import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';

export type AccountType    = 'SAVINGS' | 'CURRENT' | 'SALARY';
export type Gender         = 'MALE' | 'FEMALE' | 'OTHER';
export type MaritalStatus  = 'SINGLE' | 'MARRIED' | 'DIVORCED' | 'WIDOWED';
export type NomineeRelation = 'SPOUSE' | 'FATHER' | 'MOTHER' | 'SON' | 'DAUGHTER' | 'SIBLING' | 'OTHER';

export interface Step1Data {
  accountType:   AccountType | '';
  fullName:      string;
  fatherName:    string;
  motherName:    string;
  dateOfBirth:   string;
  gender:        Gender | '';
  maritalStatus: MaritalStatus | '';
  nationality:   string;
  occupation:    string;
  annualIncome:  string;
  phoneNumber:   string;
  emailId:       string;
  aadhaarNumber: string;
  panNumber:     string;
}

export interface Step2Data {
  currentAddressLine:   string;
  currentCity:          string;
  currentState:         string;
  currentPincode:       string;
  permanentAddressLine: string;
  permanentCity:        string;
  permanentState:       string;
  permanentPincode:     string;
  sameAsCurrent:        boolean;
}

export interface Step3Data {
  nomineeName:          string;
  nomineeRelation:      NomineeRelation | '';
  nomineeAge:           string;
  nomineeMobileNumber:  string;
  nomineeAddress:       string;
}

export interface Step4Docs {
  aadhaarCardFile:      File | null;
  panCardFile:          File | null;
  passportFile:         File | null;
  voterIdFile:          File | null;
  profilePhoto:         File | null;
  signatureImage:       File | null;
  addressProofDocument: File | null;
}

export interface ApplyErrors {
     [key: string]: string | undefined; 
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
  currentAddressLine?: string;
  currentCity?: string;
  currentState?: string;
  currentPincode?: string;
  permanentAddressLine?: string;
  permanentCity?: string;
  permanentState?: string;
  permanentPincode?: string;
  nomineeName?: string;
  nomineeRelation?: string;
  nomineeAge?: string;
  nomineeMobileNumber?: string;
  nomineeAddress?: string;
  aadhaarCardFile?: string;
  panCardFile?: string;
  profilePhoto?: string;
  signatureImage?: string;
  addressProofDocument?: string;
  captcha?: string;
  confirmCheck?: string;
}

@Component({
  selector:    'app-customer-apply-account',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './customer-apply-account.component.html',
  styleUrl:    './customer-apply-account.component.css',
})
export class CustomerApplyAccountComponent implements OnInit {

  @Input()  user: any = null;
  @Output() applicationSubmitted = new EventEmitter<void>();

  // ── Step ──
  step       = signal(1);
  totalSteps = 5;

  // ── UI ──
  loading    = signal(false);
  error      = signal('');
  success    = signal('');
  errors: ApplyErrors = {};

  // ── Success ──
  showSuccess    = signal(false);
  applicationId  = signal('');
  submittedData  = signal<any>(null);
  pdfDownloaded  = false;

  readonly today = new Date().toLocaleDateString('en-IN', {
    day: '2-digit', month: 'long', year: 'numeric'
  });

  readonly maxDob = new Date(
    new Date().setFullYear(new Date().getFullYear() - 18)
  ).toISOString().split('T')[0];

  // ── Step 1 ──
  step1: Step1Data = {
    accountType: '', fullName: '', fatherName: '', motherName: '',
    dateOfBirth: '', gender: '', maritalStatus: '', nationality: 'India',
    occupation: '', annualIncome: '', phoneNumber: '', emailId: '',
    aadhaarNumber: '', panNumber: '',
  };

  // ── Step 2 ──
  step2: Step2Data = {
    currentAddressLine: '', currentCity: '', currentState: '', currentPincode: '',
    permanentAddressLine: '', permanentCity: '', permanentState: '', permanentPincode: '',
    sameAsCurrent: false,
  };

  // ── Step 3 ──
  step3: Step3Data = {
    nomineeName: '', nomineeRelation: '', nomineeAge: '',
    nomineeMobileNumber: '', nomineeAddress: '',
  };

  // ── Step 4 ──
  docs: Step4Docs = {
    aadhaarCardFile: null, panCardFile: null, passportFile: null,
    voterIdFile: null, profilePhoto: null, signatureImage: null,
    addressProofDocument: null,
  };

  aadhaarCardFileName    = signal('');
  panCardFileName        = signal('');
  passportFileName       = signal('');
  voterIdFileName        = signal('');
  profilePhotoName       = signal('');
  signatureImageName     = signal('');
  addressProofDocumentName = signal('');

  // ── Step 5 Captcha ──
  captchaCode      = signal('');
  captchaUserInput = '';
  confirmCheck     = false;

  // ── Dropdowns ──
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
    { value: 'SINGLE', label: 'Single' },
    { value: 'MARRIED', label: 'Married' },
    { value: 'DIVORCED', label: 'Divorced' },
    { value: 'WIDOWED', label: 'Widowed' },
  ];
  readonly nomineeRelations = [
    { value: 'SPOUSE', label: 'Spouse' },
    { value: 'FATHER', label: 'Father' },
    { value: 'MOTHER', label: 'Mother' },
    { value: 'SON',    label: 'Son' },
    { value: 'DAUGHTER', label: 'Daughter' },
    { value: 'SIBLING', label: 'Sibling' },
    { value: 'OTHER', label: 'Other' },
  ];
  readonly stepDefs = [
    { n: 1, label: 'Details' },
    { n: 2, label: 'Address' },
    { n: 3, label: 'Nominee' },
    { n: 4, label: 'Docs' },
    { n: 5, label: 'Review' },
  ];
  readonly indianStates = [
    'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh','Goa','Gujarat',
    'Haryana','Himachal Pradesh','Jharkhand','Karnataka','Kerala','Madhya Pradesh',
    'Maharashtra','Manipur','Meghalaya','Mizoram','Nagaland','Odisha','Punjab',
    'Rajasthan','Sikkim','Tamil Nadu','Telangana','Tripura','Uttar Pradesh',
    'Uttarakhand','West Bengal','Delhi','Jammu & Kashmir','Ladakh',
  ];

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void {
    this.refreshCaptcha();
    // Pre-fill from logged-in user
    if (this.user) {
      if (this.user.email)    this.step1.emailId    = this.user.email;
      if (this.user.fullName) this.step1.fullName   = this.user.fullName;
      if (this.user.phone)    this.step1.phoneNumber = this.user.phone;
    }
  }

  // ─────────────────────────────────────────────
  //  NAVIGATION
  // ─────────────────────────────────────────────

  get progressPercent(): number { return (this.step() / this.totalSteps) * 100; }

  isStepDone(n: number):   boolean { return this.step() > n; }
  isStepActive(n: number): boolean { return this.step() === n; }

  goToStep(n: number): void {
    if (!this.isStepDone(n)) return;
    this.errors = {};
    this.error.set('');
    this.step.set(n);
    window.scrollTo(0, 0);
  }

  goNext(): void {
    this.error.set('');
    const validators: Record<number, () => boolean> = {
      1: () => this.validateStep1(),
      2: () => this.validateStep2(),
      3: () => this.validateStep3(),
      4: () => this.validateStep4(),
    };
    if (validators[this.step()] && !validators[this.step()]()) return;
    this.step.update(s => Math.min(5, s + 1));
    window.scrollTo(0, 0);
  }

  goBack(): void {
    this.errors = {};
    this.error.set('');
    this.step.update(s => Math.max(1, s - 1));
    window.scrollTo(0, 0);
  }

  getStepTitle(): string {
    const t = ['Customer Details','Address Details','Nominee Details','Upload Documents','Review & Submit'];
    return t[this.step() - 1] ?? '';
  }

  getStepSubtitle(): string {
    const s = [
      'Personal & contact information',
      'Current & permanent address',
      'Beneficiary information',
      'Identity & address proof documents',
      'Verify all details before submission',
    ];
    return s[this.step() - 1] ?? '';
  }

  // ─────────────────────────────────────────────
  //  VALIDATIONS
  // ─────────────────────────────────────────────

  private validateStep1(): boolean {
    this.errors = {};
    let ok = true;
    const d = this.step1;

    if (!d.accountType) { this.errors.accountType = 'Required'; ok = false; }
    if (!d.fullName?.trim()) { this.errors.fullName = 'Required'; ok = false; }
    if (!d.fatherName?.trim()) { this.errors.fatherName = 'Required'; ok = false; }
    if (!d.motherName?.trim()) { this.errors.motherName = 'Required'; ok = false; }
    if (!d.dateOfBirth) { this.errors.dateOfBirth = 'Required'; ok = false; }
    if (!d.gender) { this.errors.gender = 'Required'; ok = false; }
    if (!d.occupation?.trim()) { this.errors.occupation = 'Required'; ok = false; }

    if (!d.phoneNumber) {
      this.errors.phoneNumber = 'Required'; ok = false;
    } else if (!/^[6-9]\d{9}$/.test(d.phoneNumber)) {
      this.errors.phoneNumber = 'Enter valid 10-digit mobile number'; ok = false;
    }

    if (!d.emailId) {
      this.errors.emailId = 'Required'; ok = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.emailId)) {
      this.errors.emailId = 'Enter valid email'; ok = false;
    }

    if (!d.aadhaarNumber) {
      this.errors.aadhaarNumber = 'Required'; ok = false;
    } else if (!/^\d{12}$/.test(d.aadhaarNumber)) {
      this.errors.aadhaarNumber = 'Enter valid 12-digit Aadhaar'; ok = false;
    }

    if (!d.panNumber) {
      this.errors.panNumber = 'Required'; ok = false;
    } else if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(d.panNumber.toUpperCase())) {
      this.errors.panNumber = 'Enter valid PAN (e.g. ABCDE1234F)'; ok = false;
    }

    return ok;
  }

  private validateStep2(): boolean {
    this.errors = {};
    let ok = true;
    const a = this.step2;

    if (!a.currentAddressLine?.trim())  { this.errors.currentAddressLine = 'Required'; ok = false; }
    if (!a.currentCity?.trim())         { this.errors.currentCity = 'Required'; ok = false; }
    if (!a.currentState?.trim())        { this.errors.currentState = 'Required'; ok = false; }
    if (!a.currentPincode || !/^\d{6}$/.test(a.currentPincode)) {
      this.errors.currentPincode = '6-digit pincode required'; ok = false;
    }
    if (!a.permanentAddressLine?.trim()) { this.errors.permanentAddressLine = 'Required'; ok = false; }
    if (!a.permanentCity?.trim())        { this.errors.permanentCity = 'Required'; ok = false; }
    if (!a.permanentState?.trim())       { this.errors.permanentState = 'Required'; ok = false; }
    if (!a.permanentPincode || !/^\d{6}$/.test(a.permanentPincode)) {
      this.errors.permanentPincode = '6-digit pincode required'; ok = false;
    }
    return ok;
  }

  private validateStep3(): boolean {
    this.errors = {};
    let ok = true;
    const n = this.step3;

    if (!n.nomineeName?.trim()) { this.errors.nomineeName = 'Required'; ok = false; }
    if (!n.nomineeRelation)     { this.errors.nomineeRelation = 'Required'; ok = false; }
    if (!n.nomineeAge || Number(n.nomineeAge) < 1 || Number(n.nomineeAge) > 120) {
      this.errors.nomineeAge = 'Enter valid age'; ok = false;
    }
    if (!n.nomineeMobileNumber || !/^\d{10}$/.test(n.nomineeMobileNumber)) {
      this.errors.nomineeMobileNumber = 'Enter valid 10-digit number'; ok = false;
    }
    if (!n.nomineeAddress?.trim()) { this.errors.nomineeAddress = 'Required'; ok = false; }

    return ok;
  }

  private validateStep4(): boolean {
    this.errors = {};
    let ok = true;
    const required: Array<[keyof Step4Docs, string]> = [
      ['aadhaarCardFile',      'Aadhaar Card is required'],
      ['panCardFile',          'PAN Card is required'],
      ['profilePhoto',         'Profile photo is required'],
      ['signatureImage',       'Signature image is required'],
      ['addressProofDocument', 'Address proof is required'],
    ];
    required.forEach(([field, msg]) => {
      if (!this.docs[field]) { (this.errors as any)[field] = msg; ok = false; }
    });
    return ok;
  }

  private validateStep5(): boolean {
    this.errors = {};
    let ok = true;
    if (!this.captchaUserInput ||
        this.captchaUserInput.toUpperCase() !== this.captchaCode()) {
      this.errors.captcha = 'Captcha does not match.';
      this.refreshCaptcha();
      ok = false;
    }
    if (!this.confirmCheck) {
      this.errors.confirmCheck = 'Please confirm all details are correct.';
      ok = false;
    }
    return ok;
  }

  // ─────────────────────────────────────────────
  //  SUBMIT — uses JWT auth (no OTP)
  // ─────────────────────────────────────────────

  submitApplication(): void {
    if (!this.validateStep5()) return;
    this.loading.set(true);
    this.error.set('');

    const formData = new FormData();

    // Step 1
    Object.entries(this.step1).forEach(([k, v]) => formData.append(k, v ?? ''));
    // Step 2
    Object.entries(this.step2).forEach(([k, v]) => formData.append(k, String(v)));
    // Step 3
    Object.entries(this.step3).forEach(([k, v]) => formData.append(k, v ?? ''));
    // Files
    (Object.entries(this.docs) as [string, File | null][]).forEach(([k, f]) => {
      if (f) formData.append(k, f, f.name);
    });

    // Use authenticated submit endpoint (no OTP needed — JWT identifies user)
    this.applicationService.submitApplicationAuthenticated(formData).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.applicationId.set(res.data.applicationId);
          this.submittedData.set(res.data);
          this.showSuccess.set(true);
          window.scrollTo(0, 0);
          // Auto PDF after 3s
          setTimeout(() => {
            if (!this.pdfDownloaded) {
              this.pdfDownloaded = true;
              this.downloadPDF();
            }
          }, 3000);
          this.applicationSubmitted.emit();
        } else {
          this.error.set(res.message || 'Submission failed. Please try again.');
        }
      },
      error: (err: any) => {
        this.loading.set(false);
        const code    = err.error?.errorCode || '';
        const message = err.error?.message   || '';

        if (code === 'ACTIVE_APPLICATION_EXISTS') {
          this.error.set(message);
          this.step.set(1);
          window.scrollTo(0, 0);
        } else {
          this.error.set(message || 'Submission failed. Please try again.');
        }
      },
    });
  }

  // ─────────────────────────────────────────────
  //  ADDRESS SAME AS CURRENT
  // ─────────────────────────────────────────────

  onSameAsCurrentChange(): void {
    if (this.step2.sameAsCurrent) {
      this.step2.permanentAddressLine = this.step2.currentAddressLine;
      this.step2.permanentCity        = this.step2.currentCity;
      this.step2.permanentState       = this.step2.currentState;
      this.step2.permanentPincode     = this.step2.currentPincode;
    } else {
      this.step2.permanentAddressLine = '';
      this.step2.permanentCity        = '';
      this.step2.permanentState       = '';
      this.step2.permanentPincode     = '';
    }
  }

  // ─────────────────────────────────────────────
  //  FILE HANDLING
  // ─────────────────────────────────────────────

  onFileSelect(event: Event, field: keyof Step4Docs, imageOnly = false): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;
    const file = input.files[0];

    const allowed = imageOnly
      ? ['image/jpeg', 'image/png']
      : ['application/pdf', 'image/jpeg', 'image/png'];
    const maxMB = imageOnly ? 2 : 5;

    if (!allowed.includes(file.type)) {
      (this.errors as any)[field] = `Only ${imageOnly ? 'JPG/PNG' : 'PDF/JPG/PNG'} allowed`;
      return;
    }
    if (file.size > maxMB * 1024 * 1024) {
      (this.errors as any)[field] = `Max ${maxMB}MB allowed`;
      return;
    }
    this.docs[field] = file;
    this.setFileName(field, file.name);
    delete (this.errors as any)[field];
  }

  onRemoveFile(field: keyof Step4Docs): void {
    this.docs[field] = null;
    this.setFileName(field, '');
  }

  private setFileName(field: keyof Step4Docs, name: string): void {
    const map: Record<keyof Step4Docs, () => void> = {
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
  //  PDF DOWNLOAD
  // ─────────────────────────────────────────────

  downloadPDF(): void {
    this.pdfDownloaded = true;
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

    const section = (title: string) => {
      y += 5;
      doc.setDrawColor(99, 102, 241); doc.setLineWidth(0.5);
      doc.line(20, y, W - 20, y); y += 5;
      doc.setFontSize(11); doc.setFont('helvetica', 'bold');
      doc.setTextColor(99, 102, 241);
      doc.text(title, 20, y); y += 6;
    };

    const row = (label: string, val: string) => {
      if (y > 270) { doc.addPage(); y = 20; }
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
    doc.text(`Application ID: ${this.applicationId()}   |   Date: ${new Date().toLocaleDateString('en-IN')}`,
      W / 2, 21, { align: 'center' });

    section('PERSONAL INFORMATION');
    const d = this.step1;
    row('Account Type', d.accountType);
    row('Full Name', d.fullName);
    row("Father's Name", d.fatherName);
    row("Mother's Name", d.motherName);
    row('Date of Birth', d.dateOfBirth);
    row('Gender', d.gender);
    row('Marital Status', d.maritalStatus);
    row('Nationality', d.nationality);
    row('Occupation', d.occupation);
    row('Annual Income', d.annualIncome ? '₹' + Number(d.annualIncome).toLocaleString() : '—');
    row('Mobile', d.phoneNumber);
    row('Email', d.emailId);
    row('Aadhaar', '••••••••' + d.aadhaarNumber.slice(-4));
    row('PAN', d.panNumber);

    section('CURRENT ADDRESS');
    const a = this.step2;
    row('Address', a.currentAddressLine);
    row('City / State', `${a.currentCity}, ${a.currentState}`);
    row('Pincode', a.currentPincode);

    section('PERMANENT ADDRESS');
    row('Address', a.permanentAddressLine);
    row('City / State', `${a.permanentCity}, ${a.permanentState}`);
    row('Pincode', a.permanentPincode);

    section('NOMINEE DETAILS');
    const n = this.step3;
    row('Nominee Name', n.nomineeName);
    row('Relation', n.nomineeRelation);
    row('Age', n.nomineeAge);
    row('Mobile', n.nomineeMobileNumber);
    row('Address', n.nomineeAddress);

    section('APPLICATION STATUS');
    row('Status', 'SUBMITTED — Awaiting Review');
    row('Application ID', this.applicationId());
    row('Submitted On', new Date().toLocaleString('en-IN'));

    y += 6;
    doc.setFontSize(8); doc.setTextColor(150, 150, 150);
    doc.text('Computer-generated document. Support: support@neobank.in', W / 2, y, { align: 'center' });

    doc.save(`NeoBank_Application_${this.applicationId()}.pdf`);
  }

  startNew(): void {
    this.showSuccess.set(false);
    this.step.set(1);
    this.error.set('');
    this.applicationSubmitted.emit();
  }
}