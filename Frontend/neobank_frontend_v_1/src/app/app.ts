//------------------------- app.ts -----------------------------//
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationComponent } from './features/notification/notification.component';
import { DevMailPopupComponent } from './dev-mail-popup.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet,NotificationComponent
    // ,DevMailPopupComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('neobank_frontend_v_1');
}
