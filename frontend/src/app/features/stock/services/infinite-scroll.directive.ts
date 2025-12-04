import { Directive, Output, EventEmitter, AfterViewInit, OnDestroy } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';
import { debounceTime, map, distinctUntilChanged, filter } from 'rxjs/operators';

@Directive({
  selector: '[appInfiniteScroll]',
  standalone: true,
})
export class InfiniteScrollDirective implements AfterViewInit, OnDestroy {
  @Output() infiniteScroll = new EventEmitter<void>();

  private subscription: Subscription | null = null;
  private observer: MutationObserver | null = null;

  ngAfterViewInit(): void {
    // Usamos um MutationObserver para detectar quando ele é adicionado.
    this.observer = new MutationObserver(mutations => {
      for (const mutation of mutations) {
        const selectPanel = Array.from(mutation.addedNodes).find(
          (node): node is HTMLElement => node instanceof HTMLElement && node.classList.contains('mat-mdc-select-panel')
        );

        if (selectPanel) {
          this.subscription = fromEvent(selectPanel, 'scroll')
            .pipe(
              debounceTime(200),
              map(event => event.target as HTMLElement),
              filter(target => target.scrollTop + target.clientHeight >= target.scrollHeight - 20),
              distinctUntilChanged()
            )
            .subscribe(() => this.infiniteScroll.emit());
          this.observer?.disconnect();
          break;
        }
      }
    });

    this.observer.observe(document.body, { childList: true, subtree: true });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.observer?.disconnect();
  }
}
