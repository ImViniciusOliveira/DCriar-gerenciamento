import { Injectable, computed, signal } from '@angular/core';

export type AppNotificationType = 'success' | 'error';

export interface AppNotificationItem {
  id: number;
  type: AppNotificationType;
  message: string;
  durationMs: number;
  visible: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AppNotificationService {
  private static readonly MAX_VISIBLE_NOTIFICATIONS = 5;
  private static readonly DEFAULT_SUCCESS_DURATION_MS = 4200;
  private static readonly DEFAULT_ERROR_DURATION_MS = 6200;

  private readonly items = signal<AppNotificationItem[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 1;

  readonly notifications = computed(() => this.items().filter(item => item.visible));

  showSuccess(message: string, durationMs: number = AppNotificationService.DEFAULT_SUCCESS_DURATION_MS): void {
    this.enqueue('success', message, durationMs);
  }

  showError(message: string, durationMs: number = AppNotificationService.DEFAULT_ERROR_DURATION_MS): void {
    this.enqueue('error', message, durationMs);
  }

  dismiss(id: number): void {
    const removedItem = this.items().find(item => item.id === id);
    if (!removedItem) {
      return;
    }

    this.clearTimer(id);
    this.items.update(items => items.filter(item => item.id !== id));

    if (removedItem.visible) {
      this.promoteQueuedItems();
    }
  }

  private enqueue(type: AppNotificationType, message: string, durationMs: number): void {
    const item: AppNotificationItem = {
      id: this.nextId++,
      type,
      message,
      durationMs,
      visible: this.visibleCount() < AppNotificationService.MAX_VISIBLE_NOTIFICATIONS
    };

    this.items.update(items => [...items, item]);

    if (item.visible) {
      this.startTimer(item);
    }
  }

  private promoteQueuedItems(): void {
    const promotedItems: AppNotificationItem[] = [];

    this.items.update(items => {
      let remainingSlots = AppNotificationService.MAX_VISIBLE_NOTIFICATIONS - items.filter(item => item.visible).length;

      if (remainingSlots <= 0) {
        return items;
      }

      return items.map(item => {
        if (item.visible || remainingSlots <= 0) {
          return item;
        }

        const promotedItem = { ...item, visible: true };
        promotedItems.push(promotedItem);
        remainingSlots--;
        return promotedItem;
      });
    });

    promotedItems.forEach(item => this.startTimer(item));
  }

  private startTimer(item: AppNotificationItem): void {
    this.clearTimer(item.id);
    this.timers.set(
      item.id,
      setTimeout(() => this.dismiss(item.id), item.durationMs)
    );
  }

  private clearTimer(id: number): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
  }

  private visibleCount(): number {
    return this.items().filter(item => item.visible).length;
  }
}
