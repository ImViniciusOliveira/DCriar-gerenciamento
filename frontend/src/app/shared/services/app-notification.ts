import { Injectable, computed, signal } from '@angular/core';

export type AppNotificationType = 'success' | 'error' | 'info';

export interface AppNotificationItem {
  id: number;
  type: AppNotificationType;
  message: string;
  durationMs: number;
  remainingMs: number;
  startedAt: number | null;
  paused: boolean;
  visible: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AppNotificationService {
  private static readonly MAX_VISIBLE_NOTIFICATIONS = 5;
  private static readonly DEFAULT_SUCCESS_DURATION_MS = 4200;
  private static readonly DEFAULT_ERROR_DURATION_MS = 6200;
  private static readonly DEFAULT_INFO_DURATION_MS = 4200;

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

  showInfo(message: string, durationMs: number = AppNotificationService.DEFAULT_INFO_DURATION_MS): void {
    this.enqueue('info', message, durationMs);
  }

  pause(id: number): void {
    const item = this.items().find(currentItem => currentItem.id === id);
    if (!item || item.paused) {
      return;
    }

    const now = Date.now();
    const elapsedMs = item.startedAt ? Math.max(now - item.startedAt, 0) : 0;
    const remainingMs = Math.max(item.remainingMs - elapsedMs, 0);

    this.clearTimer(id);
    this.items.update(items => items.map(currentItem => {
      if (currentItem.id !== id) {
        return currentItem;
      }

      return {
        ...currentItem,
        remainingMs,
        startedAt: null,
        paused: true
      };
    }));
  }

  resume(id: number): void {
    const item = this.items().find(currentItem => currentItem.id === id);
    if (!item || !item.paused) {
      return;
    }

    const resumedItem: AppNotificationItem = {
      ...item,
      paused: false
    };

    this.items.update(items => items.map(currentItem => currentItem.id === id ? resumedItem : currentItem));
    this.startTimer(resumedItem);
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
      remainingMs: durationMs,
      startedAt: null,
      paused: false,
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
    const startedAt = Date.now();

    this.items.update(items => items.map(currentItem => {
      if (currentItem.id !== item.id) {
        return currentItem;
      }

      return {
        ...currentItem,
        startedAt,
        paused: false
      };
    }));

    this.timers.set(
      item.id,
      setTimeout(() => this.dismiss(item.id), item.remainingMs)
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
