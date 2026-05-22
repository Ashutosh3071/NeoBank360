// import { Injectable, signal } from '@angular/core';
// import { HttpClient } from '@angular/common/http';
// // import { Observable } from 'rxjs';
// import { KycRequest , ApiResponse ,KycDto} from '../model/all';
// import { HttpClient as _HttpClient } from '@angular/common/http';
// import { of, Observable } from 'rxjs';

// @Injectable({ providedIn: 'root' })
// export class KycService {
//   private apiUrl = 'http://localhost:8765/api/kyc';

//   getKycStatus(userId: number): Observable<ApiResponse<KycDto>> {
//     return this.http.get<ApiResponse<KycDto>>(`${this.apiUrl}/status/${userId}`);
//   }

//   constructor(private http: HttpClient) {}

//   submitKyc(request: KycRequest): Observable<ApiResponse<KycDto>> {
//     const formData = new FormData();
//     formData.append('userId', request.userId.toString());
//     formData.append('fullName', request.fullName);
//     formData.append('fatherName', request.fatherName);
//     formData.append('motherName', request.motherName);
//     formData.append('maritalStatus', request.maritalStatus || 'SINGLE');
//     formData.append('occupation', request.occupation);
//     formData.append('annualIncome', request.annualIncome || '0');
//     formData.append('aadhaarNumber', request.aadhaarNumber);
//     formData.append('panNumber', request.panNumber);
//     formData.append('addressProofType', request.addressProofType || 'VOTER_ID');
//     formData.append('nomineeName', request.nomineeName || '');
//     formData.append('nomineeRelation', request.nomineeRelation || '');
//     formData.append('nomineePhone', request.nomineePhone || '');
//     formData.append('accountType', request.accountType || 'SAVINGS');

//     // Only attach document files if explicitly uploaded in this submission
//     if (request.documentUploaded) {
//       if (request.aadhaarFile) formData.append('aadhaarFile', request.aadhaarFile);
//       if (request.panFile) formData.append('panFile', request.panFile);
//       if (request.addressProofFile) formData.append('addressProofFile', request.addressProofFile);
//       if (request.photoFile) formData.append('photoFile', request.photoFile);
//     }

//     return this.http.post<ApiResponse<KycDto>>(`${this.apiUrl}/submit`, formData);
//   }



// // submitKyc(request: KycRequest): Observable<ApiResponse<KycDto>> {

// //   const response: ApiResponse<KycDto> = {
// //     timestamp: new Date().toISOString(),
// //     status: 200,
// //     success: true,
// //     message: 'KYC submitted successfully',
// //     data: {
// //       id: 1,
// //       userId: request.userId,
// //       fullName: request.fullName,
// //       fatherName: request.fatherName,
// //       motherName: request.motherName,
// //       maritalStatus: request.maritalStatus || 'SINGLE',
// //       occupation: request.occupation,
// //       annualIncome: Number(request.annualIncome) || 0,
// //       aadhaarNumber: request.aadhaarNumber,
// //       panNumber: request.panNumber,
// //       addressProofType: request.addressProofType || 'VOTER_ID',
// //       nomineeName: request.nomineeName || '',
// //       nomineeRelation: request.nomineeRelation || '',
// //       nomineeDob: new Date().toISOString(),
// //       nomineePhone: request.nomineePhone || '',
// //       verificationStatus: 'PENDING',
// //       remarks: 'KYC under review',
// //       createdAt: new Date().toISOString(),
// //       documentUploaded: request.documentUploaded || false
// //     }
// //   };

// //   return of(response);
// // }

//   getKycByUserId(userId: number): Observable<ApiResponse<KycDto>> {
//     return this.http.get<ApiResponse<KycDto>>(`${this.apiUrl}/user/${userId}`);
//   }

//   getAllPendingKyc(): Observable<ApiResponse<KycDto[]>> {
//     return this.http.get<ApiResponse<KycDto[]>>(`${this.apiUrl}/pending`);
//   }

//   getAllKyc(): Observable<ApiResponse<KycDto[]>> {
//     return this.http.get<ApiResponse<KycDto[]>>(`${this.apiUrl}/all`);
//   }

//   approveKyc(kycId: number, adminId: number, remarks?: string): Observable<ApiResponse<KycDto>> {
//     return this.http.post<ApiResponse<KycDto>>(`${this.apiUrl}/approve/${kycId}?adminId=${adminId}&remarks=${remarks || ''}`, {});
//   }

//   rejectKyc(kycId: number, adminId: number, remarks?: string): Observable<ApiResponse<KycDto>> {
//     return this.http.post<ApiResponse<KycDto>>(`${this.apiUrl}/reject/${kycId}?adminId=${adminId}&remarks=${remarks || ''}`, {});
//   }
// }
