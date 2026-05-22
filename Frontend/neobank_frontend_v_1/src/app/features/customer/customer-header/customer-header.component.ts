// customer-header.component.ts

import { Component, Input, Output, EventEmitter,inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector:    'app-customer-header',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './customer-header.component.html',
  styleUrl:    './customer-header.component.css',
})
export class CustomerHeaderComponent {

  
  themeService = inject(ThemeService);

  @Input() username      = '';
  @Input() fullName      = '';
  @Input() pageTitle     = 'Overview';
  @Input() isDarkMode    = false;

  @Output() toggleSidebarEvt = new EventEmitter<void>();
  @Output() cycleThemeEvt    = new EventEmitter<void>();
  @Output() logoutEvt        = new EventEmitter<void>();
  @Output() profileEvt       = new EventEmitter<void>();

  getUserInitials(): string {
    if (this.fullName) {
      return this.fullName.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
    }
    return this.username.substring(0, 2).toUpperCase();
  }

  toggleTheme() {
  this.themeService.toggle();
}
  
}