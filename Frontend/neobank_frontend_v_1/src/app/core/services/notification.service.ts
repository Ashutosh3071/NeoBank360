import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Notification {
  id: number;
  message: string;
  type: 'success' | 'warning' | 'danger' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private notifications: Notification[] = [];
  private subject = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.subject.asObservable();

  private id = 0;

  show(message: string, type: Notification['type'] = 'info') {
    const notification: Notification = {
      id: ++this.id,
      message,
      type
    };

    // ✅ NEW MESSAGE ADDED ON TOP
    this.notifications.unshift(notification);

    this.subject.next([...this.notifications]);

    setTimeout(() => this.remove(notification.id), 4000);
  }

  remove(id: number) {
    this.notifications = this.notifications.filter(n => n.id !== id);
    this.subject.next([...this.notifications]);
  }
}