import {
  Component,
  OnDestroy,
  ChangeDetectorRef
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { interval, Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-dev-mail-popup',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button (click)="startFetchingMails()">Start Fetch Mails</button>

    <div class="popup" *ngIf="mails.length">

      <div class="card" *ngFor="let m of mails">
        <div class="header">
          <span class="title">{{ m.subject }}</span>
          <button class="close" (click)="markAsSeen(m.id)">✖</button>
        </div>

        <p class="to"><b>To:</b> {{ m.recipient }}</p>
        <p class="body">{{ m.body }}</p>

        <div *ngIf="m.otp" class="otp-box">
          <span class="otp">{{ m.otp }}</span>
          <button class="copy" (click)="copyOtp(m.otp); markAsSeen(m.id)">
            Copy
          </button>
        </div>

        <button class="seen-btn" (click)="markAsSeen(m.id)">
          Mark Seen
        </button>
      </div>

    </div>

    <div class="toast" *ngIf="showToast">
      OTP Copied ✔
    </div>
  `
})
export class DevMailPopupComponent implements OnDestroy {

  api = 'http://localhost:8765/api/dev/mail';
  mails: any[] = [];
  pollSub!: Subscription;
  showToast = false;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  // 🚀 MAIN METHOD (You asked for method name)
  startFetchingMails() {
    this.stopFetching(); // safety

    this.pollSub = interval(3000)
      .pipe(take(5)) // ✅ only 5 times
      .subscribe(() => {
        this.fetchUnseenMails();
      });
  }

  // 📡 API CALL METHOD
  fetchUnseenMails() {
    this.http.get<any>(`${this.api}/unseen`).subscribe(res => {
      const newData = (res?.data || []).map((m: any) => ({
        ...m,
        otp: m.otp && /^\d{6}$/.test(m.otp) ? m.otp : null
      }));

      if (JSON.stringify(newData) !== JSON.stringify(this.mails)) {
        this.mails = newData;
        this.cdr.detectChanges();
      }
    });
  }

  // 🛑 STOP METHOD
  stopFetching() {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
    }
  }

  // 📋 COPY OTP
  copyOtp(otp: string) {
    navigator.clipboard.writeText(otp);
    this.showToast = true;

    setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 2000);
  }

  // ✅ MARK AS SEEN
  markAsSeen(id: number) {
    this.http.patch(`${this.api}/${id}/seen`, {}).subscribe(() => {
      this.fetchUnseenMails();
    });
  }

  ngOnDestroy() {
    this.stopFetching();
  }
}