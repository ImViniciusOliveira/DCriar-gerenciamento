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
import { Observable, catchError, debounceTime, distinctUntilChanged, map, of } from 'rxjs';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { DetailsDialog } from '../../../../shared/components/details-dialog/details-dialog';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import {
  AdjustmentChannelSummary,
  AdjustmentLotSummary,
  AdjustmentProductSummary
} from '../../models/stock-adjustment.model';
import { ApiResponseStockHistory, StockHistoryItem, StockMovementTypeOption } from '../../models/stock-history.model';
import { BatchService } from '../../services/batch.service';
import { StockService } from '../../services/stock.service';
import { BatchForm } from '../batch-form/batch-form';
import { ChannelStockAdjustmentForm } from '../channel-stock-adjustment-form/channel-stock-adjustment-form';
import { ProductStockAdjustmentForm } from '../product-stock-adjustment-form/product-stock-adjustment-form';

type StockSectionKey = 'consultas' | 'ajustes' | 'historico';
type HistoryRangeKey = '1d' | '1m' | '6m' | '1a' | 'all';
type AdjustmentViewKey = 'lotes' | 'produtos' | 'canais';

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

interface AdjustmentViewOption {
  key: AdjustmentViewKey;
  title: string;
  subtitle: string;
  buttonLabel: string;
}

interface AdjustmentTableRow {
  rowType: AdjustmentViewKey;
  lot?: AdjustmentLotSummary;
  product?: AdjustmentProductSummary;
  channel?: AdjustmentChannelSummary;
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
  styleUrls: ['./stock-home.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PaginationHandler]
})
export class StockHome implements AfterViewInit {
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly stockService = inject(StockService);
  private readonly batchService = inject(BatchService);
  private readonly entityDialog = inject(EntityDialogService);
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
  protected readonly isAdjustmentsSection = computed(() => this.activeSection().key === 'ajustes');
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
  protected readonly adjustmentViews: AdjustmentViewOption[] = [
    {
      key: 'lotes',
      title: 'Lotes',
      subtitle: 'Ajustes em lotes e retalhos de matéria-prima',
      buttonLabel: 'Lotes'
    },
    {
      key: 'produtos',
      title: 'Produtos',
      subtitle: 'Correção do estoque físico total do produto',
      buttonLabel: 'Produtos'
    },
    {
      key: 'canais',
      title: 'Canais',
      subtitle: 'Redistribuição do saldo dos produtos por canal de venda',
      buttonLabel: 'Canais'
    }
  ];
  protected readonly activeAdjustmentView = signal<AdjustmentViewOption>(this.adjustmentViews[0]);
  protected readonly adjustmentItems = signal<AdjustmentTableRow[]>([]);
  protected readonly adjustmentTotalElements = signal(0);
  protected readonly adjustmentPageSize = signal(10);
  protected readonly adjustmentPageIndex = signal(0);
  protected readonly adjustmentSortActive = signal('tipoMateriaPrima.nome');
  protected readonly adjustmentSortDirection = signal<Sort['direction']>('asc');
  protected readonly adjustmentRefreshVersion = signal(0);

  historyTableColumns: TableColumn<StockHistoryItem>[] = [];
  adjustmentTableColumns: TableColumn<AdjustmentTableRow>[] = [];

  @ViewChild('dataTemplate') dataTemplate!: TemplateRef<any>;
  @ViewChild('produtoTemplate') produtoTemplate!: TemplateRef<any>;
  @ViewChild('skuTemplate') skuTemplate!: TemplateRef<any>;
  @ViewChild('movementTemplate') movementTemplate!: TemplateRef<any>;
  @ViewChild('quantityTemplate') quantityTemplate!: TemplateRef<any>;
  @ViewChild('reasonTemplate') reasonTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate') actionsTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentNameTemplate') adjustmentNameTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentBatchTemplate') adjustmentBatchTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentSaldoTemplate') adjustmentSaldoTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentValueTemplate') adjustmentValueTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentUnitCostTemplate') adjustmentUnitCostTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentSkuTemplate') adjustmentSkuTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentPhysicalStockTemplate') adjustmentPhysicalStockTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentDistributedStockTemplate') adjustmentDistributedStockTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentAvailableStockTemplate') adjustmentAvailableStockTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentChannelNameTemplate') adjustmentChannelNameTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentChannelQuantityTemplate') adjustmentChannelQuantityTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentDivergenceTemplate') adjustmentDivergenceTemplate!: TemplateRef<any>;
  @ViewChild('adjustmentActionsTemplate') adjustmentActionsTemplate!: TemplateRef<any>;

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

    effect((onCleanup) => {
      const isAdjustmentsActive = this.isAdjustmentsSection();
      const view = this.activeAdjustmentView();
      const pageIndex = this.adjustmentPageIndex();
      const pageSize = this.adjustmentPageSize();
      const sortActive = this.adjustmentSortActive();
      const sortDirection = this.adjustmentSortDirection();
      this.adjustmentRefreshVersion();

      this.updateAdjustmentColumns(view.key);

      if (!isAdjustmentsActive) {
        this.adjustmentItems.set([]);
        this.adjustmentTotalElements.set(0);
        return;
      }

      const sort = sortDirection ? `${sortActive},${sortDirection}` : sortActive;
      const subscription = this.getAdjustmentRows$(view.key, pageIndex, pageSize, sort)
        .subscribe(result => {
          this.adjustmentItems.set(result.items);
          this.adjustmentTotalElements.set(result.total);
          this.cdr.markForCheck();
        });

      onCleanup(() => subscription.unsubscribe());
    });

  }

  ngAfterViewInit(): void {
    this.historyTableColumns = [
      { key: 'data', header: 'Data da movimentação', sortable: true, className: 'col-created', widthPx: 150, cellTemplate: this.dataTemplate },
      { key: 'produtoNome', header: 'Produto', sortable: true, sortKey: 'produto.nome', sortType: 'text', className: 'col-product-history', cellTemplate: this.produtoTemplate },
      { key: 'produtoSku', header: 'SKU', sortable: true, sortType: 'text', className: 'col-sku-history', widthPx: 250, cellTemplate: this.skuTemplate },
      { key: 'tipo', header: 'Movimentação', sortable: false, className: 'col-movement-history', widthPx: 200, cellTemplate: this.movementTemplate },
      { key: 'quantidade', header: 'Quantidade', sortable: true, className: 'col-quantity-history', widthPx: 200, cellTemplate: this.quantityTemplate },
      { key: 'motivo', header: 'Motivo', sortable: false, className: 'col-reason-history', widthPx: 250, cellTemplate: this.reasonTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, className: 'col-actions', widthPx: 75, cellTemplate: this.actionsTemplate }
    ];

    this.updateAdjustmentColumns(this.activeAdjustmentView().key);

    this.cdr.detectChanges();
  }

  protected setActiveSection(section: StockSection): void {
    this.activeSection.set(section);
    if (section.key === 'historico') {
      this.stockService.refreshHistory();
    }
  }

  protected setActiveAdjustmentView(view: AdjustmentViewOption): void {
    this.activeAdjustmentView.set(view);
    this.resetAdjustmentTable(view.key);
  }

  protected setActiveAdjustmentViewByKey(viewKey: AdjustmentViewKey): void {
    const view = this.adjustmentViews.find(option => option.key === viewKey);
    if (!view) {
      return;
    }
    this.setActiveAdjustmentView(view);
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

  protected onAdjustmentPageChange(event: PageEvent): void {
    this.adjustmentPageSize.set(event.pageSize);
    this.adjustmentPageIndex.set(event.pageIndex);
  }

  protected onAdjustmentSortChange(sort: Sort): void {
    const defaults = this.getAdjustmentDefaultSort(this.activeAdjustmentView().key);
    this.adjustmentSortActive.set(sort.direction ? sort.active : defaults.active);
    this.adjustmentSortDirection.set(sort.direction || defaults.direction);
    this.adjustmentPageIndex.set(0);
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

  protected formatBatchSaldo(row: AdjustmentTableRow): string {
    const saldo = row.lot?.saldoEstoque ?? 0;
    const unit = row.lot?.unidadeSimbolo || '';
    if (saldo === 0) {
      return '-';
    }
    return `${this.formatDecimal(saldo)}${unit ? ` ${unit}` : ''}`;
  }

  protected formatCurrency(value?: number | null): string {
    if ((value ?? 0) === 0) {
      return '-';
    }

    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value ?? 0);
  }

  protected formatProductStock(value?: number | null): string {
    return this.formatDecimal(value ?? 0);
  }

  protected formatChannelQuantity(row: AdjustmentTableRow): string {
    return this.formatDecimal(row.channel?.quantidadeNoCanal ?? 0);
  }

  protected getAdjustmentActionLabel(): string {
    switch (this.activeAdjustmentView().key) {
      case 'lotes':
        return 'Ajustar lote';
      case 'produtos':
        return 'Ajustar produto';
      case 'canais':
        return 'Ajustar canal';
    }
    return 'Ajustar';
  }

  protected openAdjustment(row: AdjustmentTableRow): void {
    switch (row.rowType) {
      case 'lotes':
        if (row.lot) {
          this.openLotAdjustment(row.lot);
        }
        break;
      case 'produtos':
        if (row.product) {
          this.openProductAdjustment(row.product);
        }
        break;
      case 'canais':
        if (row.channel) {
          this.openChannelAdjustment(row.channel);
        }
        break;
    }
  }

  protected isAdjustmentConsistent(row: AdjustmentTableRow): boolean {
    switch (row.rowType) {
      case 'lotes':
        return (row.lot?.camposBloqueados?.length ?? 0) === 0;
      case 'produtos':
        return (row.product?.camposBloqueados?.length ?? 0) === 0;
      case 'canais':
        return (row.channel?.camposBloqueados?.length ?? 0) === 0;
    }
  }

  private resetAdjustmentTable(view: AdjustmentViewKey): void {
    const defaultSort = this.getAdjustmentDefaultSort(view);
    this.adjustmentPageIndex.set(0);
    this.adjustmentPageSize.set(10);
    this.adjustmentSortActive.set(defaultSort.active);
    this.adjustmentSortDirection.set(defaultSort.direction);
  }

  private getAdjustmentDefaultSort(view: AdjustmentViewKey): Sort {
    switch (view) {
      case 'lotes':
        return { active: 'tipoMateriaPrima.nome', direction: 'asc' };
      case 'produtos':
        return { active: 'nome', direction: 'asc' };
      case 'canais':
        return { active: 'produto.nome', direction: 'asc' };
    }
    return { active: 'id', direction: 'asc' };
  }

  private updateAdjustmentColumns(view: AdjustmentViewKey): void {
    switch (view) {
      case 'lotes':
        this.adjustmentTableColumns = [
          { key: 'nome', header: 'Matéria-Prima', sortable: true, sortKey: 'tipoMateriaPrima.nome', sortType: 'text', className: 'col-adjustment-name', cellTemplate: this.adjustmentNameTemplate },
          { key: 'lote', header: 'Lote', sortable: false, widthPx: 200, className: 'col-adjustment-batch', cellTemplate: this.adjustmentBatchTemplate },
          { key: 'saldo', header: 'Saldo Atual', sortable: false, widthPx: 200, className: 'col-adjustment-balance', cellTemplate: this.adjustmentSaldoTemplate },
          { key: 'valorAtual', header: 'Valor Atual', sortable: true, sortKey: 'valorAtualLote', widthPx: 200, className: 'col-adjustment-value', cellTemplate: this.adjustmentValueTemplate },
          { key: 'custoUnitario', header: 'Custo Unitário', sortable: true, sortKey: 'custoUnitarioAtual', widthPx: 200, className: 'col-adjustment-unit-cost', cellTemplate: this.adjustmentUnitCostTemplate },
          { key: 'divergencia', header: 'Divergência', sortable: false, widthPx: 170, className: 'col-adjustment-status', cellTemplate: this.adjustmentDivergenceTemplate },
          { key: 'acoes', header: 'Ações', sortable: false, widthPx: 90, className: 'col-trigger col-fit-center', cellTemplate: this.adjustmentActionsTemplate }
        ];
        break;
      case 'produtos':
        this.adjustmentTableColumns = [
          { key: 'nome', header: 'Produto', sortable: true, sortType: 'text', className: 'col-adjustment-name', cellTemplate: this.adjustmentNameTemplate },
          { key: 'sku', header: 'SKU', sortable: true, sortType: 'text', widthPx: 250, className: 'col-adjustment-sku', cellTemplate: this.adjustmentSkuTemplate },
          { key: 'estoqueFisicoTotal', header: 'Físico', sortable: true, sortKey: 'estoqueFisicoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentPhysicalStockTemplate },
          { key: 'estoqueDistribuidoTotal', header: 'Distribuído', sortable: true, sortKey: 'estoqueDistribuidoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentDistributedStockTemplate },
          { key: 'estoqueDisponivelParaAlocar', header: 'Disponível', sortable: true, sortKey: 'estoqueDisponivelParaAlocar', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentAvailableStockTemplate },
          { key: 'divergencia', header: 'Divergência', sortable: false, widthPx: 170, className: 'col-adjustment-status', cellTemplate: this.adjustmentDivergenceTemplate },
          { key: 'acoes', header: 'Ações', sortable: false, widthPx: 90, className: 'col-trigger col-fit-center', cellTemplate: this.adjustmentActionsTemplate }
        ];
        break;
      case 'canais':
        this.adjustmentTableColumns = [
          { key: 'nome', header: 'Produto', sortable: true, sortKey: 'produto.nome', sortType: 'text', className: 'col-adjustment-name', cellTemplate: this.adjustmentNameTemplate },
          { key: 'sku', header: 'SKU', sortable: true, sortKey: 'produto.sku', sortType: 'text', widthPx: 250, className: 'col-adjustment-sku', cellTemplate: this.adjustmentSkuTemplate },
          { key: 'canal', header: 'Canal', sortable: false, widthPx: 200, className: 'col-adjustment-channel-name', cellTemplate: this.adjustmentChannelNameTemplate },
          { key: 'quantidadeNoCanal', header: 'No Canal', sortable: true, sortKey: 'quantidadeNoCanal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentChannelQuantityTemplate },
          { key: 'estoqueFisicoTotal', header: 'Físico', sortable: true, sortKey: 'estoqueFisicoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentPhysicalStockTemplate },
          { key: 'estoqueDistribuidoTotal', header: 'Distribuído', sortable: true, sortKey: 'estoqueDistribuidoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentDistributedStockTemplate },
          { key: 'estoqueDisponivelParaAlocar', header: 'Disponível', sortable: true, sortKey: 'estoqueDisponivelParaAlocar', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.adjustmentAvailableStockTemplate },
          { key: 'divergencia', header: 'Divergência', sortable: false, widthPx: 170, className: 'col-adjustment-status', cellTemplate: this.adjustmentDivergenceTemplate },
          { key: 'acoes', header: 'Ações', sortable: false, widthPx: 90, className: 'col-trigger col-fit-center', cellTemplate: this.adjustmentActionsTemplate }
        ];
        break;
    }
    this.cdr.markForCheck();
  }

  private getAdjustmentRows$(view: AdjustmentViewKey, page: number, size: number, sort: string): Observable<{ items: AdjustmentTableRow[]; total: number }> {
    switch (view) {
      case 'lotes':
        return this.stockService.searchAdjustmentLots({ page, size, sort }).pipe(
          map(response => ({
            items: response.items.map(lot => ({ rowType: 'lotes' as const, lot })),
            total: response.total
          })),
          catchError(() => of({ items: [] as AdjustmentTableRow[], total: 0 }))
        );
      case 'produtos':
        return this.stockService.searchAdjustmentProducts({ page, size, sort }).pipe(
          map(response => ({
            items: response.items.map(product => ({ rowType: 'produtos' as const, product })),
            total: response.total
          })),
          catchError(() => of({ items: [] as AdjustmentTableRow[], total: 0 }))
        );
      case 'canais':
        return this.stockService.searchAdjustmentChannels({ page, size, sort }).pipe(
          map(response => ({
            items: response.items.map(channel => ({ rowType: 'canais' as const, channel })),
            total: response.total
          })),
          catchError(() => of({ items: [] as AdjustmentTableRow[], total: 0 }))
        );
    }
    return of({ items: [], total: 0 });
  }

  private formatDecimal(value: number): string {
    if (value === 0) {
      return '-';
    }

    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
    }).format(value);
  }

  private openLotAdjustment(lot: AdjustmentLotSummary): void {
    this.batchService.findById(lot.loteId).subscribe({
      next: batch => {
        const dialogRef = this.dialog.open(BatchForm, {
          data: {
            template: batch,
            title: `Ajustar lote ${lot.identificadorPublico}`,
            isViewMode: true
          },
          width: '800px',
          maxWidth: '95vw',
          autoFocus: false
        });

        dialogRef.afterClosed().subscribe(() => this.refreshAdjustments());
      },
      error: err => {
        this.entityDialog.showApiErrorSnackbar(err, 'Não foi possível abrir o lote para ajuste.');
      }
    });
  }

  private openProductAdjustment(product: AdjustmentProductSummary): void {
    this.entityDialog.openFormDialog({
      component: ProductStockAdjustmentForm,
      formData: {
        template: product,
        title: `Ajustar produto ${product.nomeProduto}`
      },
      title: `Ajustar produto ${product.nomeProduto}`,
      width: '680px',
      maxWidth: '95vw'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar('Estoque físico do produto ajustado com sucesso.');
        this.refreshAdjustments();
      }
    });
  }

  private openChannelAdjustment(channel: AdjustmentChannelSummary): void {
    this.entityDialog.openFormDialog({
      component: ChannelStockAdjustmentForm,
      formData: {
        template: channel,
        title: `Ajustar canal ${channel.nomeCanalVenda}`
      },
      title: `Ajustar canal ${channel.nomeCanalVenda}`,
      width: '680px',
      maxWidth: '95vw'
    }).subscribe(saved => {
      if (saved) {
        this.entityDialog.showSuccessSnackbar('Estoque do canal ajustado com sucesso.');
        this.refreshAdjustments();
      }
    });
  }

  private refreshAdjustments(): void {
    this.adjustmentRefreshVersion.update(current => current + 1);
  }
}
