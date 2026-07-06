export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: string;
  isActive: boolean;
  createdAt: string;
}

export interface UpdateProfileRequest {
  fullName: string;
}
