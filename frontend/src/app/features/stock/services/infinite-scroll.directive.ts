import { Directive, Output, EventEmitter, AfterViewInit, inject, DestroyRef } from '@angular/core';
import { fromEvent } from 'rxjs';
import { debounceTime, map, distinctUntilChanged, filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/**
 * Diretiva para detectar scroll infinito em painéis do Angular Material (mat-select).
 *
 * Necessária porque o `mat-select` renderiza seu painel de opções (`.mat-mdc-select-panel`)
 * dentro de um contêiner de overlay no final do `body`, fora da hierarquia normal do componente.
 *
 * Utiliza `MutationObserver` para detectar quando o painel é criado no DOM e anexa
 * um ouvinte de evento de scroll a ele.
 */
@Directive({
  selector: '[appInfiniteScroll]',
  standalone: true,
})
export class InfiniteScrollDirective implements AfterViewInit {
  @Output() infiniteScroll = new EventEmitter<void>();

  private readonly destroyRef = inject(DestroyRef);
  private observer: MutationObserver | null = null;

  ngAfterViewInit(): void {
    this.setupPanelObserver();
  }

  private setupPanelObserver(): void {
    this.observer = new MutationObserver(mutations => {
      for (const mutation of mutations) {
        // Procura pelo painel do select nas adições ao DOM
        const selectPanel = Array.from(mutation.addedNodes).find(
          (node): node is HTMLElement =>
            node instanceof HTMLElement && node.classList.contains('mat-mdc-select-panel')
        );

        if (selectPanel) {
          this.attachScrollListener(selectPanel);
          // Uma vez encontrado e anexado, não precisamos mais observar o body
          this.observer?.disconnect();
          break;
        }
      }
    });

    // Observa o body por adições de nós filhos (onde o overlay do Material será injetado)
    this.observer.observe(document.body, { childList: true, subtree: true });

    // Garante que o observer seja desconectado quando a diretiva for destruída
    this.destroyRef.onDestroy(() => this.observer?.disconnect());
  }

  private attachScrollListener(element: HTMLElement): void {
    fromEvent(element, 'scroll')
      .pipe(
        takeUntilDestroyed(this.destroyRef), // Gerenciamento automático de memória
        debounceTime(200),
        map(event => event.target as HTMLElement),
        // Dispara quando o scroll chega a 20px do final
        filter(target => target.scrollTop + target.clientHeight >= target.scrollHeight - 20),
        distinctUntilChanged()
      )
      .subscribe(() => this.infiniteScroll.emit());
  }
}
