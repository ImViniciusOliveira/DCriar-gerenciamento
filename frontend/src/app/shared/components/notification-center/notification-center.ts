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
}
