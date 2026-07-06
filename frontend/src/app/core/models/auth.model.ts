export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  aadhaarNumber: string;
  panNumber: string;
}

export interface RegisterResponse {
  message: string;
  userId?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId?: number;
  email?: string;
  role?: string;
  message?: string;
}
