import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AppNotificationItem, AppNotificationService } from '../../services/app-notification';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationCenter {
  private readonly notificationService = inject(AppNotificationService);

  readonly notifications = this.notificationService.notifications;
  readonly now = this.notificationService.now;

  dismiss(notificationId: number): void {
    this.notificationService.dismiss(notificationId);
  }

  pause(notificationId: number): void {
    this.notificationService.pause(notificationId);
  }

  resume(notificationId: number): void {
    this.notificationService.resume(notificationId);
  }

  trackByNotificationId(_: number, notification: AppNotificationItem): number {
    return notification.id;
  }

  getNotificationTitle(type: AppNotificationItem['type']): string {
    if (type === 'success') {
      return 'Concluído';
    }
    if (type === 'info') {
      return 'Informação';
    }
    return 'Erro';
  }

  getNotificationIcon(type: AppNotificationItem['type']): string {
    if (type === 'success') {
      return 'check_circle';
    }
    if (type === 'info') {
      return 'info';
    }
    return 'error';
  }

  getAnimationDelay(notification: AppNotificationItem): string {
    const elapsedMs = Math.max(notification.durationMs - notification.remainingMs, 0);
    return `-${elapsedMs}ms`;
  }

  getLifeScale(notification: AppNotificationItem): number {
    return this.getRemainingRatio(notification);
  }

  getOpacity(notification: AppNotificationItem): number {
    if (notification.paused) {
      return 1;
    }

    const remainingRatio = this.getRemainingRatio(notification);
    if (remainingRatio > 0.25) {
      return 1;
    }

    return Math.max(remainingRatio / 0.25, 0);
  }

  getTransform(notification: AppNotificationItem): string {
    if (notification.paused) {
      return 'translateY(0)';
    }

    const remainingRatio = this.getRemainingRatio(notification);
    if (remainingRatio > 0.25) {
      return 'translateY(0)';
    }

    const fadeProgress = 1 - Math.max(remainingRatio / 0.25, 0);
    return `translateY(${(fadeProgress * 10).toFixed(2)}px)`;
  }

  private getRemainingRatio(notification: AppNotificationItem): number {
    const remainingMs = this.getRemainingMs(notification);
    if (notification.durationMs <= 0) {
      return 0;
    }
    return Math.min(Math.max(remainingMs / notification.durationMs, 0), 1);
  }

  private getRemainingMs(notification: AppNotificationItem): number {
    if (notification.paused || notification.startedAt === null) {
      return notification.remainingMs;
    }

    const elapsedMs = Math.max(this.now() - notification.startedAt, 0);
    return Math.max(notification.remainingMs - elapsedMs, 0);
  }
}
