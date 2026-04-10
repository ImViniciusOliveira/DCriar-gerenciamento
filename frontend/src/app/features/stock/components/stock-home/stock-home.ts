import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { Sort } from '@angular/material/sort';
import { Observable, catchError, debounceTime, distinctUntilChanged, map, of } from 'rxjs';

import { EnumOption, EnumService } from '../../../../core/services/enum.service';
import { environment } from '../../../../core/services/environment';
import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';
import {
  AdjustmentChannelSummary,
  AdjustmentLotSummary,
  AdjustmentProductSummary
} from '../../models/stock-adjustment.model';
import { Channel } from '../../models/channel.model';
import { ApiResponseStockHistory, StockHistoryItem, StockMovementTypeOption } from '../../models/stock-history.model';
import { BatchService } from '../../services/batch.service';
import { ChannelService } from '../../services/channel.service';
import { StockService } from '../../services/stock.service';
import { StockConsultationSummary } from '../../models/stock-consultation.model';
import { BatchForm } from '../batch-form/batch-form';
import { ChannelStockAdjustmentForm } from '../channel-stock-adjustment-form/channel-stock-adjustment-form';
import { ProductStockAdjustmentForm } from '../product-stock-adjustment-form/product-stock-adjustment-form';
import { StockProductHistoryDialog, StockProductHistoryDialogData } from '../stock-product-history-dialog/stock-product-history-dialog';
import { ProductService } from '../../../products/services/product.service';
import { Product } from '../../../products/models/product.model';

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

interface ConsultationTableRow {
  rowType: 'pontual';
  point: StockConsultationSummary;
}

@Component({
  selector: 'app-stock-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatAutocompleteModule,
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
  private readonly channelService = inject(ChannelService);
  private readonly productService = inject(ProductService);
  private readonly enumService = inject(EnumService);
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
  protected readonly isConsultationsSection = computed(() => this.activeSection().key === 'consultas');
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
  protected readonly adjustmentSearchControl = new FormControl('', { nonNullable: true });
  protected readonly adjustmentChannelControl = new FormControl<number | ''>('', { nonNullable: true });
  protected readonly adjustmentUnitControl = new FormControl('', { nonNullable: true });
  protected readonly adjustmentSearch = signal('');
  protected readonly selectedAdjustmentChannelId = signal<number | null>(null);
  protected readonly selectedAdjustmentUnit = signal('');
  protected readonly channels = signal<Channel[]>([]);
  protected readonly lotMeasurementUnits = signal<EnumOption[]>([]);
  protected readonly adjustmentRefreshVersion = signal(0);
  protected readonly consultationItems = signal<ConsultationTableRow[]>([]);
  protected readonly consultationTotalElements = signal(0);
  protected readonly consultationPageSize = signal(10);
  protected readonly consultationPageIndex = signal(0);
  protected readonly consultationSortActive = signal('nomeProduto');
  protected readonly consultationSortDirection = signal<Sort['direction']>('asc');
  protected readonly consultationProductControl = new FormControl<string | Partial<Product>>('', { nonNullable: true });
  protected readonly consultationChannelControl = new FormControl<number | ''>('', { nonNullable: true });
  protected readonly consultationProductOptions = signal<Partial<Product>[]>([]);
  protected readonly selectedConsultationProduct = signal<Partial<Product> | null>(null);
  protected readonly selectedConsultationChannelId = signal<number | null>(null);
  protected readonly consultationRequest = signal<{
    produtoId?: number;
    nomeProduto?: string;
    canalVendaId?: number;
  } | null>({});

  historyTableColumns: TableColumn<StockHistoryItem>[] = [];
  adjustmentTableColumns: TableColumn<AdjustmentTableRow>[] = [];
  consultationTableColumns: TableColumn<ConsultationTableRow>[] = [];

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
  @ViewChild('consultationNameTemplate') consultationNameTemplate!: TemplateRef<any>;
  @ViewChild('consultationSkuTemplate') consultationSkuTemplate!: TemplateRef<any>;
  @ViewChild('consultationChannelNameTemplate') consultationChannelNameTemplate!: TemplateRef<any>;
  @ViewChild('consultationChannelQuantityTemplate') consultationChannelQuantityTemplate!: TemplateRef<any>;
  @ViewChild('consultationPhysicalStockTemplate') consultationPhysicalStockTemplate!: TemplateRef<any>;
  @ViewChild('consultationDistributedStockTemplate') consultationDistributedStockTemplate!: TemplateRef<any>;
  @ViewChild('consultationAvailableStockTemplate') consultationAvailableStockTemplate!: TemplateRef<any>;
  @ViewChild('consultationDivergenceTemplate') consultationDivergenceTemplate!: TemplateRef<any>;
  @ViewChild('consultationActionsTemplate') consultationActionsTemplate!: TemplateRef<any>;
  @ViewChild('consultationProductTrigger', { read: MatAutocompleteTrigger }) consultationProductTrigger?: MatAutocompleteTrigger;

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
    const channelsResponse = toSignal(
      this.channelService.getAllChannels().pipe(
        catchError(() => of([]))
      ),
      { initialValue: [] }
    );
    const lotMeasurementUnitsResponse = toSignal(
      this.enumService.getEnumOptions(
        `${environment.apiVersionPath}/enums/stock/unidades-de-medida`,
        'unidadesDeMedida'
      ).pipe(
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
      this.channels.set(channelsResponse());
    });

    effect(() => {
      this.lotMeasurementUnits.set(lotMeasurementUnitsResponse());
    });

    this.adjustmentSearchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.adjustmentSearch.set(value.trim());
      this.resetAdjustmentPage();
    });

    this.adjustmentChannelControl.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.selectedAdjustmentChannelId.set(value === '' ? null : Number(value));
      this.resetAdjustmentPage();
    });

    this.adjustmentUnitControl.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.selectedAdjustmentUnit.set(value);
      if (!value && this.activeAdjustmentView().key === 'lotes' && this.adjustmentSortActive() === 'saldoEstoque') {
        const defaults = this.getAdjustmentDefaultSort('lotes');
        this.adjustmentSortActive.set(defaults.active);
        this.adjustmentSortDirection.set(defaults.direction);
      }
      this.resetAdjustmentPage();
    });

    this.consultationProductControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((previous, current) => this.resolveConsultationProductTerm(previous) === this.resolveConsultationProductTerm(current)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (typeof value !== 'string') {
        this.consultationProductOptions.set([]);
        return;
      }

      this.selectedConsultationProduct.set(null);
      this.consultationRequest.set(null);
      this.consultationItems.set([]);
      this.consultationTotalElements.set(0);
      const term = value.trim();
      this.productService.searchProducts(term, undefined, false, 100).pipe(
        catchError(() => of([]))
      ).subscribe(products => {
        this.consultationProductOptions.set(products);
        this.cdr.markForCheck();
      });

      this.runPointConsultation();
    });

    this.consultationChannelControl.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.selectedConsultationChannelId.set(value === '' ? null : Number(value));
      this.runPointConsultation();
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
      const search = this.adjustmentSearch();
      const channelId = this.selectedAdjustmentChannelId();
      const unit = this.selectedAdjustmentUnit();
      this.adjustmentRefreshVersion();

      this.updateAdjustmentColumns(view.key);

      if (!isAdjustmentsActive) {
        this.adjustmentItems.set([]);
        this.adjustmentTotalElements.set(0);
        return;
      }

      const sort = sortDirection ? `${sortActive},${sortDirection}` : sortActive;
      const subscription = this.getAdjustmentRows$(view.key, pageIndex, pageSize, sort, search, channelId, unit)
        .subscribe({
          next: result => {
            this.adjustmentItems.set(result.items);
            this.adjustmentTotalElements.set(result.total);
            this.cdr.markForCheck();
          },
          error: err => {
            this.handleAdjustmentSearchError(view.key, err);
          }
        });

      onCleanup(() => subscription.unsubscribe());
    });

    effect((onCleanup) => {
      const isConsultationsActive = this.isConsultationsSection();
      const request = this.consultationRequest();
      const pageIndex = this.consultationPageIndex();
      const pageSize = this.consultationPageSize();
      const sortActive = this.consultationSortActive();
      const sortDirection = this.consultationSortDirection();

      this.updateConsultationColumns();

      if (!isConsultationsActive) {
        this.consultationItems.set([]);
        this.consultationTotalElements.set(0);
        return;
      }

      if (!request) {
        this.runPointConsultation();
        return;
      }

      const sort = sortDirection ? `${sortActive},${sortDirection}` : sortActive;

      const subscription = this.stockService.searchConsultations({
        page: pageIndex,
        size: pageSize,
        sort,
        produtoId: request.produtoId,
        nomeProduto: request.nomeProduto,
        canalVendaId: request.canalVendaId
      }).subscribe({
        next: result => {
          this.consultationItems.set(result.items.map(point => ({ rowType: 'pontual' as const, point })));
          this.consultationTotalElements.set(result.total);
          this.cdr.markForCheck();
        },
        error: err => {
          this.handleConsultationError(err);
        }
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
    this.updateConsultationColumns();

    this.cdr.detectChanges();
  }

  protected setActiveSection(section: StockSection): void {
    this.activeSection.set(section);
    if (section.key === 'historico') {
      this.stockService.refreshHistory();
      return;
    }

    if (section.key === 'consultas') {
      this.consultationProductControl.setValue('', { emitEvent: false });
      this.consultationChannelControl.setValue('', { emitEvent: false });
      this.consultationProductOptions.set([]);
      this.selectedConsultationProduct.set(null);
      this.selectedConsultationChannelId.set(null);
      this.resetConsultationTable();
    }
  }

  protected setActiveAdjustmentView(view: AdjustmentViewOption): void {
    this.activeAdjustmentView.set(view);
    if (view.key !== 'canais') {
      this.adjustmentChannelControl.setValue('', { emitEvent: false });
      this.selectedAdjustmentChannelId.set(null);
    }
    if (view.key !== 'lotes') {
      this.adjustmentUnitControl.setValue('', { emitEvent: false });
      this.selectedAdjustmentUnit.set('');
    }
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
    this.resetAdjustmentPage();
  }

  protected onConsultationPageChange(event: PageEvent): void {
    this.consultationPageSize.set(event.pageSize);
    this.consultationPageIndex.set(event.pageIndex);
  }

  protected onConsultationSortChange(sort: Sort): void {
    const defaults = this.getConsultationDefaultSort();
    this.consultationSortActive.set(sort.direction ? sort.active : defaults.active);
    this.consultationSortDirection.set(sort.direction || defaults.direction);
    this.resetConsultationPage();
  }

  protected getAdjustmentSearchLabel(): string {
    switch (this.activeAdjustmentView().key) {
      case 'lotes':
        return 'Buscar lote';
      case 'produtos':
      case 'canais':
        return 'Buscar produto';
    }
  }

  protected getAdjustmentSearchPlaceholder(): string {
    switch (this.activeAdjustmentView().key) {
      case 'lotes':
        return 'Digite o nome da matéria-prima ou LT/RT';
      case 'produtos':
      case 'canais':
        return 'Digite o nome ou SKU do produto';
    }
  }

  protected displayConsultationProduct(value: string | Partial<Product> | null): string {
    if (!value) {
      return '';
    }

    if (typeof value === 'string') {
      return value;
    }

    const nome = value?.nome ?? '';
    const sku = value?.sku ? ` (${value.sku})` : '';
    return `${nome}${sku}`;
  }

  protected selectConsultationProduct(product: Partial<Product>): void {
    this.selectedConsultationProduct.set(product);
    this.consultationProductControl.setValue(product, { emitEvent: false });
    this.consultationProductOptions.set([]);
    this.runPointConsultation();
  }

  protected openConsultationProductOptions(): void {
    const currentValue = this.consultationProductControl.value;
    const term = typeof currentValue === 'string' ? currentValue.trim() : '';

    this.productService.searchProducts(term, undefined, false, 100).pipe(
      catchError(() => of([]))
    ).subscribe(products => {
      this.consultationProductOptions.set(products);
      this.consultationProductTrigger?.openPanel();
      this.cdr.markForCheck();
    });
  }

  protected runPointConsultation(): void {
    const selectedProduct = this.selectedConsultationProduct();
    const typedValue = typeof this.consultationProductControl.value === 'string'
      ? this.consultationProductControl.value.trim()
      : '';
    const channelId = this.selectedConsultationChannelId();

    this.resetConsultationPage();
    this.consultationRequest.set({
      produtoId: selectedProduct?.id,
      nomeProduto: selectedProduct?.id ? undefined : (typedValue || undefined),
      canalVendaId: channelId ?? undefined
    });
  }

  protected isLotSaldoSortEnabled(): boolean {
    return this.activeAdjustmentView().key === 'lotes' && !!this.selectedAdjustmentUnit();
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

  protected openProductHistory(item: StockHistoryItem): void {
    const dialogData: StockProductHistoryDialogData = {
      produtoId: item.produtoId,
      nomeProduto: item.produtoNome,
      skuProduto: item.produtoSku
    };

    this.openStockProductHistoryDialog(dialogData);
  }

  protected openConsultationProductHistory(point: StockConsultationSummary): void {
    const dialogData: StockProductHistoryDialogData = {
      produtoId: point.produtoId,
      nomeProduto: point.nomeProduto,
      skuProduto: point.skuProduto
    };

    this.openStockProductHistoryDialog(dialogData);
  }

  private openStockProductHistoryDialog(dialogData: StockProductHistoryDialogData): void {
    this.dialog.open(StockProductHistoryDialog, {
      data: dialogData,
      width: '1100px',
      maxWidth: '95vw',
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

  protected formatConsultationChannelQuantity(row: ConsultationTableRow): string {
    return this.formatDecimal(row.point.quantidadeNoCanal ?? 0);
  }

  protected isConsultationConsistent(row: ConsultationTableRow): boolean {
    return row.point.statusDivergencia !== 'INCONSISTENTE';
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
        return row.lot?.statusDivergencia !== 'INCONSISTENTE';
      case 'produtos':
        return row.product?.statusDivergencia !== 'INCONSISTENTE';
      case 'canais':
        return row.channel?.statusDivergencia !== 'INCONSISTENTE';
    }
  }

  private resetAdjustmentTable(view: AdjustmentViewKey): void {
    const defaultSort = this.getAdjustmentDefaultSort(view);
    this.adjustmentPageSize.set(10);
    this.adjustmentSortActive.set(defaultSort.active);
    this.adjustmentSortDirection.set(defaultSort.direction);
    this.resetAdjustmentPage();
  }

  private resetAdjustmentPage(): void {
    this.adjustmentPageIndex.set(0);
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
          { key: 'saldo', header: 'Saldo Atual', sortable: this.isLotSaldoSortEnabled(), sortKey: 'saldoEstoque', widthPx: 200, className: 'col-adjustment-balance', cellTemplate: this.adjustmentSaldoTemplate },
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

  private updateConsultationColumns(): void {
    this.consultationTableColumns = [
      { key: 'nome', header: 'Produto', sortable: true, sortKey: 'nomeProduto', sortType: 'text', className: 'col-adjustment-name', cellTemplate: this.consultationNameTemplate },
      { key: 'sku', header: 'SKU', sortable: true, sortKey: 'skuProduto', sortType: 'text', widthPx: 250, className: 'col-adjustment-sku', cellTemplate: this.consultationSkuTemplate },
      { key: 'canal', header: 'Canal', sortable: true, sortKey: 'nomeCanalVenda', widthPx: 200, className: 'col-adjustment-channel-name', cellTemplate: this.consultationChannelNameTemplate },
      { key: 'quantidadeNoCanal', header: 'No Canal', sortable: true, sortKey: 'quantidadeNoCanal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.consultationChannelQuantityTemplate },
      { key: 'estoqueFisicoTotal', header: 'Físico', sortable: true, sortKey: 'estoqueFisicoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.consultationPhysicalStockTemplate },
      { key: 'estoqueDistribuidoTotal', header: 'Distribuído', sortable: true, sortKey: 'estoqueDistribuidoTotal', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.consultationDistributedStockTemplate },
      { key: 'estoqueDisponivelParaAlocar', header: 'Disponível', sortable: true, sortKey: 'estoqueDisponivelParaAlocar', widthPx: 150, className: 'col-adjustment-stock', cellTemplate: this.consultationAvailableStockTemplate },
      { key: 'divergencia', header: 'Divergência', sortable: false, widthPx: 170, className: 'col-adjustment-status', cellTemplate: this.consultationDivergenceTemplate },
      { key: 'acoes', header: 'Ações', sortable: false, widthPx: 90, className: 'col-trigger col-fit-center', cellTemplate: this.consultationActionsTemplate }
    ];
    this.cdr.markForCheck();
  }

  private getAdjustmentRows$(
    view: AdjustmentViewKey,
    page: number,
    size: number,
    sort: string,
    search: string,
    channelId: number | null,
    unit: string
  ): Observable<{ items: AdjustmentTableRow[]; total: number }> {
    switch (view) {
      case 'lotes':
        return this.stockService.searchAdjustmentLots({
          page,
          size,
          sort,
          nomeMateriaPrima: search || undefined,
          unidadeDeMedida: unit || undefined
        }).pipe(
          map(response => ({
            items: response.items.map(lot => ({ rowType: 'lotes' as const, lot })),
            total: response.total
          }))
        );
      case 'produtos':
        return this.stockService.searchAdjustmentProducts({
          page,
          size,
          sort,
          nomeProduto: search || undefined
        }).pipe(
          map(response => ({
            items: response.items.map(product => ({ rowType: 'produtos' as const, product })),
            total: response.total
          }))
        );
      case 'canais':
        return this.stockService.searchAdjustmentChannels({
          page,
          size,
          sort,
          nomeProduto: search || undefined,
          canalVendaId: channelId ?? undefined
        }).pipe(
          map(response => ({
            items: response.items.map(channel => ({ rowType: 'canais' as const, channel })),
            total: response.total
          }))
        );
    }
    return of({ items: [], total: 0 });
  }

  private handleAdjustmentSearchError(view: AdjustmentViewKey, error: unknown): void {
    this.adjustmentItems.set([]);
    this.adjustmentTotalElements.set(0);

    const sortCorrigido = this.tryRecoverInvalidAdjustmentSort(view, error);
    this.cdr.markForCheck();

    this.entityDialog.showApiErrorSnackbar(
      error,
      'Não foi possível atualizar a listagem de ajustes.'
    );

    if (sortCorrigido) {
      return;
    }
  }

  private handleConsultationError(error: unknown): void {
    this.consultationItems.set([]);
    this.consultationTotalElements.set(0);
    this.cdr.markForCheck();
    this.entityDialog.showApiErrorSnackbar(
      error,
      'Não foi possível consultar o estoque informado.'
    );
  }

  private getConsultationDefaultSort(): Sort {
    return { active: 'nomeProduto', direction: 'asc' };
  }

  private resetConsultationPage(): void {
    this.consultationPageIndex.set(0);
  }

  private resetConsultationTable(): void {
    const defaultSort = this.getConsultationDefaultSort();
    this.consultationSortActive.set(defaultSort.active);
    this.consultationSortDirection.set(defaultSort.direction);
    this.consultationRequest.set({});
    this.consultationItems.set([]);
    this.consultationTotalElements.set(0);
    this.consultationPageIndex.set(0);
  }

  private tryRecoverInvalidAdjustmentSort(view: AdjustmentViewKey, error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse)) {
      return false;
    }

    const details = error.error?.details;
    const recurso = details?.recurso;
    const campoOrdenacao = details?.campoOrdenacao;

    if (recurso !== 'ajustes-lotes' || view !== 'lotes') {
      return false;
    }

    if ((campoOrdenacao === 'saldoEstoque' || campoOrdenacao === 'saldoAtual') && !this.selectedAdjustmentUnit()) {
      const defaults = this.getAdjustmentDefaultSort('lotes');
      this.adjustmentSortActive.set(defaults.active);
      this.adjustmentSortDirection.set(defaults.direction);
      return true;
    }

    return false;
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

  private resolveConsultationProductTerm(value: string | Partial<Product>): string {
    if (typeof value === 'string') {
      return value.trim();
    }

    const nome = value?.nome ?? '';
    const sku = value?.sku ?? '';
    return `${nome}|${sku}`;
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
