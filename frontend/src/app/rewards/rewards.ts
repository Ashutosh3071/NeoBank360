import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { RewardResponse } from '../core/models/reward.model';
import { RewardService } from '../services/reward';
import { AccountService } from '../services/account';
import { Account } from '../core/models/account.model';

interface RewardTier {
  name: string;
  icon: string;
  minPoints: number;
  color: string;
  perks: string[];
}

interface RedeemOption {
  id: string;
  name: string;
  description: string;
  cost: number;
  icon: string;
}

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rewards.html',
  styleUrl: './rewards.css',
})
export class RewardsComponent {
  private readonly rewardService = inject(RewardService);
  private readonly accountService = inject(AccountService);

  readonly reward = signal<RewardResponse | null>(null);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  // Account selector modal states for cashback
  readonly showAccountModal = signal(false);
  readonly userAccounts = signal<Account[]>([]);
  readonly selectedAccountId = signal<number | null>(null);
  readonly pendingRedeemOption = signal<RedeemOption | null>(null);

  readonly tiers: RewardTier[] = [
    {
      name: 'Bronze',
      icon: '🥉',
      minPoints: 0,
      color: '#cd7f32',
      perks: ['Basic rewards', 'Bill pay points'],
    },
    {
      name: 'Silver',
      icon: '🥈',
      minPoints: 100,
      color: '#94a3b8',
      perks: ['2x bill pay points', 'Priority support'],
    },
    {
      name: 'Gold',
      icon: '🥇',
      minPoints: 500,
      color: '#d4af37',
      perks: ['3x points', 'Cashback offers', 'Exclusive deals'],
    },
    {
      name: 'Platinum',
      icon: '💎',
      minPoints: 1000,
      color: '#6366f1',
      perks: ['5x points', 'Free transfers', 'VIP support', 'Lounge access'],
    },
  ];

  readonly redeemOptions: RedeemOption[] = [
    {
      id: 'cashback-50',
      name: '₹50 Cashback',
      description: 'Get ₹50 credited to your account',
      cost: 50,
      icon: '💵',
    },
    {
      id: 'cashback-100',
      name: '₹100 Cashback',
      description: 'Get ₹100 credited to your account',
      cost: 100,
      icon: '💰',
    },
    {
      id: 'free-transfer',
      name: 'Free Transfer',
      description: '1 free inter-bank transfer',
      cost: 25,
      icon: '🔄',
    },
    {
      id: 'donation',
      name: 'Donate to Charity',
      description: 'Donate equivalent to NGO partner',
      cost: 30,
      icon: '❤️',
    },
  ];

  readonly currentTier = computed(() => {
    const pts = this.reward()?.pointsBalance ?? 0;
    let tier = this.tiers[0];
    for (const t of this.tiers) {
      if (pts >= t.minPoints) tier = t;
    }
    return tier;
  });

  readonly nextTier = computed(() => {
    const pts = this.reward()?.pointsBalance ?? 0;
    return this.tiers.find((t) => t.minPoints > pts) ?? null;
  });

  readonly progressToNext = computed(() => {
    const pts = this.reward()?.pointsBalance ?? 0;
    const next = this.nextTier();
    const current = this.currentTier();
    if (!next) return 100;
    const range = next.minPoints - current.minPoints;
    const progress = pts - current.minPoints;
    return Math.min(100, Math.round((progress / range) * 100));
  });

  constructor() {
    this.loadRewards();
  }

  loadRewards(): void {
    this.isLoading.set(true);
    this.rewardService.getBalance().subscribe({
      next: (res) => {
        this.reward.set(res);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load rewards.');
      },
    });
  }

  canRedeem(cost: number): boolean {
    return (this.reward()?.pointsBalance ?? 0) >= cost;
  }

  redeem(option: RedeemOption): void {
    if (!this.canRedeem(option.cost)) return;
    this.errorMessage.set('');
    this.successMessage.set('');

    if (option.id.startsWith('cashback-')) {
      this.isLoading.set(true);
      this.accountService.getMyAccounts().subscribe({
        next: (accounts) => {
          this.isLoading.set(false);
          const approvedAccounts = accounts.filter(
            (acc) => acc.accountStatus === 'APPROVED' && acc.isActive
          );
          if (approvedAccounts.length === 0) {
            this.errorMessage.set('You need at least one approved and active account to receive cashback.');
            return;
          }
          this.userAccounts.set(approvedAccounts);
          this.selectedAccountId.set(approvedAccounts[0].id);
          this.pendingRedeemOption.set(option);
          this.showAccountModal.set(true);
        },
        error: (err: any) => {
          this.isLoading.set(false);
          this.errorMessage.set('Failed to load accounts. Please try again.');
        },
      });
    } else {
      this.executeRedeem(option);
    }
  }

  private executeRedeem(option: RedeemOption, accountId?: number): void {
    this.isLoading.set(true);
    this.rewardService.redeemPoints(option.cost, option.id, accountId).subscribe({
      next: () => {
        this.successMessage.set(`🎉 Redeemed "${option.name}" successfully!`);
        this.loadRewards();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Redemption failed.');
      },
    });
  }

  confirmCashbackRedeem(): void {
    const option = this.pendingRedeemOption();
    const accId = this.selectedAccountId();
    if (!option || !accId) return;

    this.showAccountModal.set(false);
    this.executeRedeem(option, accId);
  }

  closeAccountModal(): void {
    this.showAccountModal.set(false);
    this.pendingRedeemOption.set(null);
  }
}
