export interface BillRequest {
  billerName: string;
  amount: number;
  dueDate: string;
  category?: string;
}

export interface BillResponse {
  id: number;
  billerName: string;
  amount: number;
  dueDate: string;
  status: string;
  remindMe: boolean;
  createdAt: string;
  category?: string;
  pointsEarned?: number;
}

export interface BillStatusUpdate {
  status: string;
  accountId?: number;
}
