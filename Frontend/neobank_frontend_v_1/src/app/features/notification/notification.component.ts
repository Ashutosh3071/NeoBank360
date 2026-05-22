import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.css']
})
export class NotificationComponent {

  notifications$;

  constructor(private notificationService: NotificationService) {
    this.notifications$ = this.notificationService.notifications$;
  }

  remove(id: number) {
    this.notificationService.remove(id);
  }
}