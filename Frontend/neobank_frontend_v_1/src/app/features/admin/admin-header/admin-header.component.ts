// admin-header.component.ts

import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../../core/services/theme.service';
@Component({
  selector:    'app-admin-header',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './admin-header.component.html',
  styleUrl:    './admin-header.component.css',
})
export class AdminHeaderComponent {

  themeService = inject(ThemeService);

  @Input() username   = '';
  @Input() fullName   = '';
  @Input() role       = '';
  @Input() pageTitle  = 'Dashboard';
  @Input() isDarkMode = false;

  @Output() toggleSidebarEvt = new EventEmitter<void>();
  @Output() cycleThemeEvt    = new EventEmitter<void>();
  @Output() logoutEvt        = new EventEmitter<void>();

  getUserInitials(): string {
    if (this.fullName) return this.fullName.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
    return this.username.substring(0, 2).toUpperCase();
  }

  toggleTheme() {
  this.themeService.toggle();
  // location.reload();
}
}