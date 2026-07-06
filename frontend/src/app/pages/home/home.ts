import { NgFor } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

interface Feature {
  icon: string;
  title: string;
  desc: string;
}

interface Stat {
  value: string;
  label: string;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
  imports: [NgFor, RouterLink],
})
export class HomeComponent {
  features: Feature[] = [
    {
      icon: '🔒',
      title: 'Bank-Grade Security',
      desc: 'JWT-secured endpoints with BCrypt encryption and real-time fraud monitoring.',
    },
    {
      icon: '⚡',
      title: 'Instant Transfers',
      desc: 'Send money across accounts in milliseconds with zero downtime.',
    },
    {
      icon: '📊',
      title: 'Budget Insights',
      desc: 'Track spending by category with smart utilization alerts.',
    },
    {
      icon: '🧾',
      title: 'Bill Management',
      desc: 'Schedule bills, get reminders, and never miss a due date.',
    },
    {
      icon: '🏆',
      title: 'Reward Points',
      desc: 'Earn points on every transaction and bill payment.',
    },
    {
      icon: '🛡️',
      title: 'Security Verified',
      desc: 'Bank accounts are approved by admin for maximum security.',
    },
  ];

  stats: Stat[] = [
    { value: '50K+', label: 'Active Users' },
    { value: '₹12Cr+', label: 'Transactions' },
    { value: '99.9%', label: 'Uptime' },
    { value: '4.9★', label: 'User Rating' },
  ];

  constructor(private router: Router) {}

  getStarted(): void {
    this.router.navigate(['/register']);
  }

  signIn(): void {
    this.router.navigate(['/login']);
  }
}
``;
