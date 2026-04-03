import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  TemplateRef,
  ViewChild,
  computed,
  effect,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { Sort } from '@angular/material/sort';
import { catchError, debounceTime, distinctUntilChanged, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { DetailsDialog } from '../../../../shared/components/details-dialog/details-dialog';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import { ApiResponseStockHistory, StockHistoryItem, StockMovementTypeOption } from '../../models/stock-history.model';
import { StockService } from '../../services/stock.service';

type StockSectionKey = 'consultas' | 'ajustes' | 'historico';
type HistoryRangeKey = '1d' | '1m' | '6m' | '1a' | 'all';

interface StockSection {
  key: StockSectionKey;
  title: string;
  buttonLabel: string;
  subtitle: string;
}

interface HistoryRangeOption {
  key: HistoryRangeKey;
  label: string;
}

@Component({
  selector: 'app-stock-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    BaseTable
  ],
  templateUrl: './stock-home.html',
  styleUrl: './stock-home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class StockHome implements AfterViewInit {
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly stockService = inject(StockService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly pagination = inject(PaginationHandler);

  protected readonly sections: StockSection[] = [
    {
      key: 'ajustes',
      title: 'Ajustes',
      buttonLabel: 'Ajustes',
      subtitle: 'Correcao do estoque fisico total e redistribuicao de saldo por canal'
    },
    {
      key: 'consultas',
      title: 'Consultas',
      buttonLabel: 'Consultas',
      subtitle: 'Consultas de estoque fisico, distribuicao por canal e visoes consolidadas'
    },
    {
      key: 'historico',
      title: 'Historico',
      buttonLabel: 'Historico',
      subtitle: 'Acompanhamento de mudancas, conferencias e auditoria do estoque'
    }
  ];

  protected readonly activeSection = signal<StockSection>(this.sections[0]);
  protected readonly isHistorySection = computed(() => this.activeSection().key === 'historico');
  protected readonly selectedRange = signal<HistoryRangeKey>('1d');
  protected readonly historyItems = signal<StockHistoryItem[]>([]);
  protected readonly movementTypes = signal<StockMovementTypeOption[]>([]);
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly movementTypeControl = new FormControl('', { nonNullable: true });
  protected readonly productSearch = signal('');
  protected readonly selectedMovementType = signal('');
  protected readonly rangeOptions: HistoryRangeOption[] = [
    { key: '1d', label: '1D' },
    { key: '1m', label: '1M' },
    { key: '6m', label: '6M' },
    { key: '1a', label: '1A' },
    { key: 'all', label: 'Todo período' }
  ];

  tableColumns: TableColumn<StockHistoryItem>[] = [];

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('produtoTemplate') produtoTemplate!: TemplateRef<any>;
  @ViewChild('skuTemplate') skuTemplate!: TemplateRef<any>;
  @ViewChild('movementTemplate') movementTemplate!: TemplateRef<any>;
  @ViewChild('quantityTemplate') quantityTemplate!: TemplateRef<any>;
  @ViewChild('reasonTemplate') reasonTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;

  constructor() {
    this.pagination.initialize('stock-history', { active: 'data', direction: 'desc' });

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.productSearch.set(value.trim());
      this.resetHistoryPage();
    });

    this.movementTypeControl.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.selectedMovementType.set(value);
      this.resetHistoryPage();
    });

    const historyResponse = toSignal(
      this.stockService.getHistory().pipe(
        catchError(() => of(undefined))
      )
    );

    const movementTypesResponse = toSignal(
      this.stockService.getMovementTypes().pipe(
        catchError(() => of([]))
      ),
      { initialValue: [] }
    );

    effect(() => {
      const response = historyResponse();
      if (response) {
        this.applyHistoryResponse(response);
      }
    });

    effect(() => {
      this.movementTypes.set(movementTypesResponse());
    });

    effect(() => {
      const isHistoryActive = this.isHistorySection();
      const page = this.pagination.pageIndex();
      const size = this.pagination.pageSize();
      const sort = this.pagination.sortString();
      const periodo = this.selectedRange();
      const nomeProduto = this.productSearch();
      const tipoMovimentacao = this.selectedMovementType();

      if (!isHistoryActive) {
        return;
      }

      this.stockService.updateHistorySearchParams({
        page,
        size,
        sort,
        periodo,
        nomeProduto,
        tipoMovimentacao
      });
    });
  }

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'data', header: 'Data da movimentação', sortable: true, className: 'col-created', widthPx: 132, cellTemplate: this.dataTemplate },
      { key: 'produtoNome', header: 'Produto', sortable: true, sortKey: 'produto.nome', sortType: 'text', className: 'col-product-history', cellTemplate: this.produtoTemplate },
      { key: 'produtoSku', header: 'SKU', sortable: true, sortType: 'text', className: 'col-sku-history', widthPx: 190, cellTemplate: this.skuTemplate },
      { key: 'tipo', header: 'Movimentação', sortable: false, className: 'col-movement-history', widthPx: 150, cellTemplate: this.movementTemplate },
      { key: 'quantidade', header: 'Quantidade', sortable: true, className: 'col-quantity-history', widthPx: 95, cellTemplate: this.quantityTemplate },
      { key: 'motivo', header: 'Motivo', sortable: false, className: 'col-reason-history', widthPx: 220, cellTemplate: this.reasonTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, className: 'col-actions', widthPx: 125, cellTemplate: this.actionsTemplate }
    ];
    this.cdr.detectChanges();
  }

  protected setActiveSection(section: StockSection): void {
    this.activeSection.set(section);
    if (section.key === 'historico') {
      this.stockService.refreshHistory();
    }
  }

  protected setHistoryRange(range: HistoryRangeKey): void {
    this.selectedRange.set(range);
    this.resetHistoryPage();
  }

  protected onPageChange(event: PageEvent): void {
    this.pagination.handlePageEvent(event);
  }

  protected onSortChange(sort: Sort): void {
    this.pagination.handleSortChange(sort);
  }

  protected hasProductNameChanged(item: StockHistoryItem): boolean {
    return item.produtoNome !== item.produtoNomeSnapshot;
  }

  protected hasProductSkuChanged(item: StockHistoryItem): boolean {
    return item.produtoSku !== item.produtoSkuSnapshot;
  }

  protected formatSignedQuantity(quantidade: number): string {
    return `${quantidade > 0 ? '+' : '-'}${Math.abs(quantidade)}`;
  }

  protected isPositiveQuantity(quantidade: number): boolean {
    return quantidade >= 0;
  }

  protected getTruncatedReason(motivo: string): string {
    if (motivo.length <= 100) {
      return motivo;
    }

    return `${motivo.slice(0, 97)}...`;
  }

  protected openReasonDetails(item: StockHistoryItem): void {
    const details = [
      this.hasProductNameChanged(item)
        ? { label: 'Nome registrado', value: `${item.produtoNomeSnapshot} -> Nome atual: ${item.produtoNome}` }
        : { label: 'Nome atual', value: item.produtoNome },
      this.hasProductSkuChanged(item)
        ? { label: 'SKU registrado', value: `${item.produtoSkuSnapshot} -> SKU atual: ${item.produtoSku}` }
        : { label: 'SKU atual', value: item.produtoSku },
      { label: 'Movimentação', value: item.tipoDescricao || item.tipo },
      { label: 'Quantidade', value: this.formatSignedQuantity(item.quantidade) },
      { label: 'Data da movimentação', value: new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short'
      }).format(new Date(item.data)) },
      { label: 'Motivo', value: item.motivo }
    ];

    if (item.ordemProducaoId) {
      details.push({ label: 'Ordem de produção', value: `#${item.ordemProducaoId}` });
    }

    if (item.vendaId) {
      details.push({ label: 'Venda', value: `#${item.vendaId}` });
    }

    this.dialog.open(DetailsDialog, {
      data: {
        title: `Motivo da movimentação #${item.id}`,
        items: details,
        showLabels: true
      },
      width: '680px',
      maxWidth: '90vw',
      autoFocus: false
    });
  }

  private applyHistoryResponse(response: ApiResponseStockHistory): void {
    this.historyItems.set(response._embedded?.historicoEstoqueConsolidadoResponseDTOList ?? []);
    this.pagination.updateTotalElements(response.page?.totalElements ?? 0);
  }

  private resetHistoryPage(): void {
    this.pagination.handlePageEvent({
      pageIndex: 0,
      pageSize: this.pagination.pageSize(),
      length: this.pagination.totalElements(),
      previousPageIndex: this.pagination.pageIndex()
    });
  }
}
