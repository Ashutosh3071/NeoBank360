export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  success: boolean;
  message: string;
  errorCode?: string;
  data: T;
}

export interface RegisterRequest {
  email: string;
  phone: string;
  password: string;
  confirmPassword?: string;
}

export interface VerifyEmailRequest {
  email: string;
  otp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface VerifyLoginRequest {
  userId: number;
  otp: string;
}

export interface UserDto {
  id: number;
  email: string;
  phone: string;
  emailVerified: boolean;
  status: string;
  roleName: string;
  kycStatus: string;
  accountNumber: string;
  token?: string;
}

export interface LoginResponse {
  userId: number;
  email: string;
  roleName: string;
  token?: string;
}

export interface KycRequest {
  userId: number;
  fullName: string;
  fatherName: string;
  motherName: string;
  maritalStatus: string;
  occupation: string;
  annualIncome: string;
  aadhaarNumber: string;
  panNumber: string;
  addressProofType: string;
  nomineeName: string;
  nomineeRelation: string;
  nomineePhone: string;
  accountType: string;
  // Indicates whether the document uploads have been applied in this submission
  documentUploaded?: boolean;
  aadhaarFile?: File | null;
  panFile?: File | null;
  addressProofFile?: File | null;
  photoFile?: File | null;
}

export interface KycDto {
  id: number;
  userId: number;
  fullName: string;
  fatherName: string;
  motherName: string;
  maritalStatus: string;
  occupation: string;
  annualIncome: number;
  aadhaarNumber: string;
  panNumber: string;
  addressProofType: string;
  nomineeName: string;
  nomineeRelation: string;
  nomineeDob: string;
  nomineePhone: string;
  verificationStatus: string;
  remarks: string;
  createdAt: string;
  documentUploaded?: boolean;
}

export interface AccountDto {
  id: number;
  accountNumber: string;
  accountType: string;
  balance: number;
  currency: string;
  status: string;
  ifscCode: string;
  branchName: string;
  createdAt: string;
  userId: number;
  userEmail: string;
}

export interface TransactionDto {
  id: number;
  senderAccountId: number;
  senderAccountNumber: string;
  receiverAccountId: number;
  receiverAccountNumber: string;
  amount: number;
  transactionType: string;
  transactionStatus: string;
  referenceId: string;
  description: string;
  transactionDate: string;
}

export interface TransactionRequest {
  senderAccountId: number;
  receiverAccountId: number;
  amount: number;
  transactionType: string;
  description?: string;
}

// Re-export Bank Account Opening models for broader use
export * from './bank-account-opening.model';
