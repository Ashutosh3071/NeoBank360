import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {  } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { KycDto } from '../../../core/model/all';
// import { KycService } from '../../../core/services/kyc.service';


@Component({
  selector: 'app-admin-kyc-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-kyc-requests.component.html',
  styleUrls: ['./admin-kyc-requests.component.css']
})
export class AdminKycRequestsComponent  {



}
