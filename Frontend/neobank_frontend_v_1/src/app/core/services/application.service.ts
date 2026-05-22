// src/app/core/services/application.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';
// Add to src/app/core/services/application.service.ts
// Add to application.service.ts

export interface AccountResponse {
  id:                number;
  accountNumber:     string;
  accountType:       string;
  balance:           string;
  availableBalance:  string;
  minimumBalance:    string;
  currency:          string;
  branchName:        string;
  ifscCode:          string;
  interestRate:      string;
  status:            string;
  openedOn:          string;
  nomineeName:       string;
  nomineeRelation:   string;
  lastTransactionAt: string;
  netBankingEnabled: boolean;
  upiEnabled:        boolean;
  debitCardEnabled:  boolean;
}

export interface TransactionResponse {
  id:                number;
  referenceNumber:   string;
  transactionType:   string;
  transactionMode:   string;
  amount:            string;
  fromAccountNumber: string;
  toAccountNumber:   string;
  beneficiaryName:   string;
  upiId:             string;
  description:       string;
  status:            string;
  balanceAfter:      string;
  createdAt:         string;
}


export interface ApplicationListItem {
  applicationId:  string;
  fullName:       string;
  emailId:        string;
  phoneNumber:    string;
  accountType:    string;
  gender:         string;
  occupation:     string;
  currentCity:    string;
  currentState:   string;
  status:         string;
  submittedOn:    string;
  aadhaarNumber:  string;
  panNumber:      string;
}

export interface ApplicationDetail {
  // Basic
  applicationId:   string;
  status:          string;
  submittedOn:     string;
  approvedBy:      string | null;
  approvedAt:      string | null;
  rejectionReason: string | null;
  // Personal
  accountType:     string;
  fullName:        string;
  fatherName:      string;
  motherName:      string;
  dateOfBirth:     string;
  gender:          string;
  maritalStatus:   string;
  nationality:     string;
  occupation:      string;
  annualIncome:    string;
  phoneNumber:     string;
  emailId:         string;
  aadhaarNumber:   string;
  panNumber:       string;
  // Address
  currentAddressLine:  string;
  currentCity:         string;
  currentState:        string;
  currentPincode:      string;
  permanentAddressLine: string;
  permanentCity:        string;
  permanentState:       string;
  permanentPincode:     string;
  // Nominee
  nomineeName:          string;
  nomineeRelation:      string;
  nomineeAge:           string;
  nomineeMobileNumber:  string;
  nomineeAddress:       string;
  // Documents (base64)
  aadhaarCardFileType:          string;
  aadhaarCardFileBase64:        string;
  panCardFileType:              string;
  panCardFileBase64:            string;
  profilePhotoType:             string;
  profilePhotoBase64:           string;
  signatureImageType:           string;
  signatureImageBase64:         string;
  addressProofDocumentType:     string;
  addressProofDocumentBase64:   string;
  passportFileType:             string | null;
  passportFileBase64:           string | null;
  voterIdFileType:              string | null;
  voterIdFileBase64:            string | null;
}

export interface PageResponse<T> {
  success: boolean;
  message: string;
  data?: {
    content:          T[];
    totalElements:    number;
    totalPages:       number;
    size:             number;
    number:           number;
    first:            boolean;
    last:             boolean;
  };
}

export interface ApproveRequest {
  applicationId:   string;
  action:          'APPROVE' | 'REJECT';
  rejectionReason?: string;
  branchName?:     string;
  branchCode?:     string;
  ifscCode?:       string;
}

export interface ApproveResponse {
  success:       boolean;
  message:       string;
  data?: {
    applicationId: string;
    status:        string;
    userId:        string;
    accountNumber: string;
    fullName:      string;
    emailId:       string;
    message:       string;
  };
}

export interface ApplicationResponse {
  success: boolean;
  message: string;
  data?: {
    applicationId: string;
    fullName: string;
    accountType: string;
    phoneNumber: string;
    emailId: string;
    submittedOn: string;
    status: string;
  };
}

export interface SendOtpResponse {
  success: boolean;
  message: string;
}

export interface ApplicationStatusResponse {
  success: boolean;
  message: string;
  data?: {
    applicationId: string;
    fullName: string;
    accountType: string;
    phoneNumber: string;
    emailId: string;
    aadhaarNumber: string;
    panNumber: string;
    status: string;
    submittedOn: string;
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
    nomineeAge: string;
    nomineeMobileNumber: string;
    nomineeAddress: string;
    occupation: string;
    annualIncome: string;
    dateOfBirth: string;
    gender: string;
    nationality: string;
    maritalStatus: string;
    fatherName: string;
    motherName: string;
  };
}

@Injectable({ providedIn: 'root' })
export class ApplicationService {

  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ── Submit: Send OTP ──
  sendOtp(email: string): Observable<SendOtpResponse> {
    return this.http.post<SendOtpResponse>(
      `${this.base}/application/send-otp`,
      { email }
    );
  }

  // ── Submit: Full application ──
  submitApplication(formData: FormData): Observable<ApplicationResponse> {
    return this.http.post<ApplicationResponse>(
      `${this.base}/application/submit`,
      formData
    );
  }

  // ── Status: Send OTP to verify identity ──
  sendStatusOtp(query: string): Observable<SendOtpResponse> {
    return this.http.post<SendOtpResponse>(
      `${this.base}/application/status/send-otp`,
      { query }   // email or applicationId
    );
  }

  // ── Status: Verify OTP and get full details ──
  verifyStatusOtp(query: string, otp: string): Observable<ApplicationStatusResponse> {
    return this.http.post<ApplicationStatusResponse>(
      `${this.base}/application/status/verify`,
      { query, otp }
    );
  }

  // ── Add these methods to ApplicationService class ──

  getAllApplications(
  status?: string,
  page  = 0,
  size  = 10
): Observable<PageResponse<ApplicationListItem>> {
  let params: any = { page, size };
  if (status) params['status'] = status;
  return this.http.get<PageResponse<ApplicationListItem>>(
    `${this.base}/application/all`, { params }
  );
}

getApplicationDetail(applicationId: string): Observable<ApiResponse<ApplicationDetail>> {
  return this.http.get<ApiResponse<ApplicationDetail>>(
    `${this.base}/application/${applicationId}`
  );
}

approveApplication(req: ApproveRequest): Observable<ApproveResponse> {
  const adminUser = JSON.parse(localStorage.getItem('user') || '{}');
  return this.http.post<ApproveResponse>(
    `${this.base}/application/approve`,
    req
    // ,
    // { headers: { 'X-Admin-Username': adminUser.username || 'admin' } }
  );
}


  // ── Customer Accounts ──
getMyAccounts(): Observable<ApiResponse<AccountResponse[]>> {
  return this.http.get<ApiResponse<AccountResponse[]>>(
    `${this.base}/account/my`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

// ── Customer Transactions ──
getMyTransactions(accountNumber: string, page = 0, size = 10): Observable<any> {
  return this.http.get(
    `${this.base}/transaction/my/${accountNumber}`,
    {
      params: { page, size },
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }
  );
}

// transfer(payload: {
//   fromAccountNumber: string;
//   toAccountNumber:   string;
//   amount:            string;
//   description?:      string;
//   mode?:             string;
//   upiId?:            string;
//   beneficiaryName?:  string;
//   beneficiaryIfsc?:  string;
// }): Observable<any> {
//   return this.http.post(
//     `${this.base}/transaction/transfer`, payload,
//     { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
//   );
// }

// Replace old transfer() with 3 separate methods:

upiTransfer(payload: {
  fromAccountNumber: string;
  upiId:             string;
  amount:            string;
  description?:      string;
}): Observable<any> {
  return this.http.post(
    `${this.base}/transaction/upi`,
    payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

neftTransfer(payload: {
  fromAccountNumber:       string;
  beneficiaryAccountNumber: string;
  beneficiaryName:          string;
  beneficiaryIfsc:          string;
  beneficiaryBankName?:     string;
  amount:                   string;
  description?:             string;
  mode?:                    string;
}): Observable<any> {
  return this.http.post(
    `${this.base}/transaction/neft`,
    payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

selfTransfer(payload: {
  fromAccountNumber: string;
  toAccountNumber:   string;
  amount:            string;
  description?:      string;
}): Observable<any> {
  return this.http.post(
    `${this.base}/transaction/self`,
    payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}


// ── Admin ──
adminDeposit(payload: { accountNumber: string; amount: string; description: string; }): Observable<any> {
  return this.http.post(`${this.base}/transaction/admin/deposit`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

adminWithdraw(payload: { accountNumber: string; amount: string; description: string; }): Observable<any> {
  return this.http.post(`${this.base}/transaction/admin/withdraw`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

getAllAccounts(status?: string, page = 0, size = 10): Observable<any> {
  let params: any = { page, size };
  if (status) params['status'] = status;
  return this.http.get(`${this.base}/account/admin/all`,
    { params, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

updateAccountStatus(payload: { accountNumber: string; status: string; reason: string }): Observable<any> {
  return this.http.put(`${this.base}/account/admin/status`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

getAllAdminTransactions(page = 0, size = 10): Observable<any> {
  return this.http.get(`${this.base}/transaction/admin/all`,
    {
      params: { page, size },
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }
  );
}

// Add to ApplicationService in application.service.ts

submitApplicationAuthenticated(formData: FormData): Observable<any> {
  return this.http.post(
    `${this.base}/application/submit-auth`,
    formData,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

// Add to ApplicationService in application.service.ts

getAllUsers(status?: string, page = 0, size = 10): Observable<any> {
  let params: any = { page, size };
  if (status) params['status'] = status;
  return this.http.get(`${this.base}/user/admin/all`,
    { params, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

updateUserStatus(payload: { userId: number; status: string; reason: string }): Observable<any> {
  return this.http.put(`${this.base}/user/admin/status`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

// ── Account Requests ──
submitAccountRequest(payload: { accountType: string; reason: string }): Observable<any> {
  return this.http.post(`${this.base}/account-request/submit`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getMyAccountRequests(page = 0, size = 10): Observable<any> {
  return this.http.get(`${this.base}/account-request/my`,
    { params: { page, size }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getAllAccountRequests(status?: string, page = 0, size = 10): Observable<any> {
  let params: any = { page, size };
  if (status) params['status'] = status;
  return this.http.get(`${this.base}/account-request/admin/all`,
    { params, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getAccountRequestDetail(requestId: string): Observable<any> {
  return this.http.get(`${this.base}/account-request/admin/${requestId}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

approveAccountRequest(payload: any): Observable<any> {
  return this.http.post(`${this.base}/account-request/admin/approve`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

rejectAccountRequest(payload: any): Observable<any> {
  return this.http.post(`${this.base}/account-request/admin/reject`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

// ── UPI ──
// createUpiId(payload: { accountNumber: string; vpaPrefix: string; mobile: string }): Observable<any> {
//   return this.http.post(`${this.base}/upi/create`, payload,
//     { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
// }

// Update in application.service.ts

createUpiId(payload: {
  accountNumber: string;
  vpaPrefix:     string;
  // NO mobile field
}): Observable<any> {
  return this.http.post(`${this.base}/upi/create`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
  );
}

getAllMyUpiIds(): Observable<any> {
  return this.http.get(`${this.base}/upi/my`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getUpiIdsByAccount(accountNumber: string): Observable<any> {
  return this.http.get(`${this.base}/upi/account/${accountNumber}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

sendUpiPinOtp(vpa: string): Observable<any> {
  return this.http.post(`${this.base}/upi/pin/send-otp`, null,
    { params: { vpa }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

setUpiPin(payload: { vpa: string; otp: string; newPin: string; confirmPin: string }): Observable<any> {
  return this.http.post(`${this.base}/upi/pin/set`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

changeUpiPin(payload: { vpa: string; currentPin: string; newPin: string; confirmPin: string }): Observable<any> {
  return this.http.post(`${this.base}/upi/pin/change`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

lookupVpa(vpa: string): Observable<any> {
  return this.http.get(`${this.base}/upi/lookup`, { params: { vpa } });
}

upiPay(payload: {
  senderVpa: string; receiverVpa: string;
  amount: string; upiPin: string; description?: string;
}): Observable<any> {
  return this.http.post(`${this.base}/upi/pay`, payload,
    { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

getUpiTransactions(vpa: string, page = 0, size = 15): Observable<any> {
  return this.http.get(`${this.base}/upi/transactions/${vpa}`,
    { params: { page, size }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

setPrimaryUpi(vpa: string): Observable<any> {
  return this.http.post(`${this.base}/upi/set-primary`, null,
    { params: { vpa }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

blockUpiId(vpa: string): Observable<any> {
  return this.http.post(`${this.base}/upi/block`, null,
    { params: { vpa }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

deleteUpiId(vpa: string): Observable<any> {
  return this.http.delete(`${this.base}/upi/delete`,
    { params: { vpa }, headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

updateUpiLimits(vpa: string, dailyLimit: number, perTxnLimit: number): Observable<any> {
  return this.http.put(`${this.base}/upi/limits`, null,
    { params: { vpa, dailyLimit, perTxnLimit },
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
}

}