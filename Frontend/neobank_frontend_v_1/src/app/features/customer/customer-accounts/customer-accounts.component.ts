import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-customer-accounts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customer-accounts.component.html',
  styleUrls: ['./customer-accounts.component.css']
})
export class CustomerAccountsComponent {
}

