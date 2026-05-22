import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserDto } from '../../../core/model/all';
import { HttpClientModule } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';
@Component({
  selector: 'app-customer-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './customer-transfer.component.html',
  styleUrls: ['./customer-transfer.component.css']
})
export class CustomerTransferComponent  {
 
}
