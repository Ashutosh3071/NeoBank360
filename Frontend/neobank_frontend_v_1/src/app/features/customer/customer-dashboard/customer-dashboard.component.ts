import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountDto , TransactionDto } from '../../../core/model/all'; 

@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customer-dashboard.component.html',
  styleUrls: ['./customer-dashboard.component.css']
})
export class CustomerDashboardComponent {
  @Input() totalBalance?: number;
  @Input() accounts: AccountDto[] = [];
  @Input() transactions: TransactionDto[] = [];
}
