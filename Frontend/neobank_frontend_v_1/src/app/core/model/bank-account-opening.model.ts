export type AccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export interface BankAccountOpeningDocumentFiles {
  aadhaarCardFile?: File | null;
  panCardFile?: File | null;
  passportFile?: File | null;
  voterIdFile?: File | null;
  profilePhoto?: File | null;
  signatureImage?: File | null;
  addressProofDocument?: File | null;
}

export interface BankAccountOpeningFlatForm {
  accountType: AccountType;
  fullName: string;
  fatherName: string;
  motherName: string;
  dateOfBirth: string;
  gender: string;
  maritalStatus: string;
  nationality: string;
  occupation: string;
  annualIncome: string;
  phoneNumber: string;
  emailId: string;
  currentAddressLine: string;
  currentCity: string;
  currentState: string;
  currentPincode: string;
  permanentAddressLine: string;
  permanentCity: string;
  permanentState: string;
  permanentPincode: string;
  nomineeName: string;
  nomineeRelation: string;
  nomineeAge: number;
  nomineeMobileNumber: string;
  nomineeAddress: string;
  aadhaarCardFile?: File | null;
  panCardFile?: File | null;
  passportFile?: File | null;
  voterIdFile?: File | null;
  profilePhoto?: File | null;
  signatureImage?: File | null;
  addressProofDocument?: File | null;
  captcha?: string;
  otp?: string;
  applicationId?: string;
}

export type BankAccountOpeningModel = BankAccountOpeningFlatForm;
