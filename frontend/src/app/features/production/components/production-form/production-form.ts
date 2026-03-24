import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { lastValueFrom, take } from 'rxjs';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ProductionOrder } from '../../models/production.model';
import { Product } from '../../../products/models/product.model';
import { ProductStockSearch } from '../../../../shared/components/product-stock-search/product-stock-search';
import { CreateCutOrderRequest, CreateConsumptionOrderRequest, ProductionService, SimulationRequest, VerificationRequest } from '../../services/production.service';
import { SimulationResult, SimulationCutResult, SimulationConsumptionResult } from '../../models/simulation.model';
import { BatchSearch } from '../../../../shared/components/batch-search/batch-search';
import {Batch} from '../../../stock/models/batch.model';
import { ChannelService } from '../../../stock/services/channel.service';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { RethalboBadgeComponent, RetalhoValue } from '../retalho-badge/retalho-badge.component';
import { EnumService } from '../../../../core/services/enum.service';
import { ProductService } from '../../../products/services/product.service';
import { BatchService } from '../../../stock/services/batch.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';

export interface ProductionFormData {
  template?: ProductionOrder;
  title: string;
  isViewMode?: boolean;
}

@Component({
  selector: 'app-production-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    ProductStockSearch,
    MatIconModule,
    BatchSearch,
    MatCheckboxModule,
    RethalboBadgeComponent
  ],
  templateUrl: './production-form.html',
  styleUrls: ['./production-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProductionForm>);
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly productionService = inject(ProductionService);
  private readonly channelService = inject(ChannelService);
  private readonly enumService = inject(EnumService);
  private readonly productService = inject(ProductService);
  private readonly batchService = inject(BatchService);
  private readonly entityDialog = inject(EntityDialogService);
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  @ViewChild(ProductStockSearch) private productStockSearchComponent!: ProductStockSearch;
  @ViewChild(BatchSearch) private batchSearchComponent!: BatchSearch;

  form: FormGroup;
  isEditMode = signal(false);
  isSaving = signal(false);
  isSimulating = signal(false);
  isVerifying = signal(false);

  produto = signal<Product | null>(null);
  loteSelecionado = signal<Batch | null>(null);
  showRetalhoPreview = signal(false);
  currentOrder = signal<ProductionOrder | null>(this.data.template ?? null);

  // Carrega os canais reais da API usando o novo serviço
  channels = toSignal(this.channelService.getAllChannels(), { initialValue: [] });

  // Signal com tipo forte para armazenar o resultado da simulação.
  simulationResult = signal<SimulationResult | null>(null);
  simulationFormSnapshot = signal<any | null>(null);
  consumptionSimulationSnapshot = signal<{ quantidade: number; unidadesPorProduto: number } | null>(null);

  // Signal para controlar se a verificação é necessária (dados alterados após simulação)
  needsVerification = signal(false);

  // Snapshot para restaurar o formulário em caso de cancelamento da verificação
  private formSnapshot: any;
  private persistedSnapshot: any;

  // Formata a string da dimensão rotacionada de forma segura
  rotatedDimensionString = computed(() => {
    const result = this.simulationResult();
    if (!result || result.tipoSimulacao !== 'CORTE' || !result.dimensaoProduto) {
      return '';
    }
    return result.dimensaoProduto.split('x').map(s => s.trim()).reverse().join(' x ');
  });

  readonly blockWithMarginsString = computed(() => {
    const result = this.simulationResult();
    if (!result || result.tipoSimulacao !== 'CORTE') {
      return '';
    }

    const larguraBase = Number(result.larguraBlocoProdutosCm ?? 0);
    const comprimentoBase = Number(result.comprimentoBlocoProdutosCm ?? 0);
    const margens = this.form.getRawValue().margens ?? {};

    const larguraFinal = larguraBase + Number(margens.esquerda || 0) + Number(margens.direita || 0);
    const comprimentoFinal = comprimentoBase + Number(margens.superior || 0) + Number(margens.inferior || 0);

    return `${larguraFinal}cm x ${comprimentoFinal}cm`;
  });

  // Monta a lista completa do preview com total por linha, ex.: [10] [5] [1] [R] = 16
  retalhoPreviewRows = computed(() => {
    const result = this.simulationResult();
    if (!result || result.tipoSimulacao !== 'CORTE') {
      return [] as Array<{ items: RetalhoValue[]; total: number }>;
    }

    const cut = result as SimulationCutResult;
    const capacidadeLinha = Number(cut.produtosPorLinha || 0);
    if (capacidadeLinha <= 0) {
      return [] as Array<{ items: RetalhoValue[]; total: number }>;
    }

    // Passo 1: Montar array com total de produtos por linha
    const rowTotals: number[] = [];
    for (let i = 0; i < Number(cut.numeroLinhasCompletas || 0); i += 1) {
      rowTotals.push(capacidadeLinha);
    }

    const ultimaLinha = Number(cut.produtosNaUltimaLinha || 0);
    if (ultimaLinha > 0) {
      rowTotals.push(ultimaLinha);
    }

    const totalLinhas = rowTotals.length;
    if (totalLinhas === 0) return [];

    // Passo 2: Definir o gabarito visual baseado na PRIMEIRA LINHA (índice 0)
    // Se a primeira linha for completa, o gabarito é o número de tokens dela.
    // Se for incompleta, é tokens + 1 (o R).
    const tokensPrimeiraLinha = this.buildCompactProductTokens(rowTotals[0]);
    const primeiraLinhaCompleta = rowTotals[0] === capacidadeLinha;
    const gabaritoQuadrados = tokensPrimeiraLinha.length + (primeiraLinhaCompleta ? 0 : 1);

    // Passo 3: Montar as linhas aplicando as regras
    return rowTotals.map((rowTotal, lineIndex) => {
      const items = this.buildCompactProductTokens(rowTotal);
      const isUltimaLinha = lineIndex === totalLinhas - 1;
      const linhaCompleta = rowTotal === capacidadeLinha;

      // Regra: Linha completa nunca tem R
      if (linhaCompleta) {
        return { items, total: rowTotal };
      }

      // Regra: Última linha tenta preencher até o gabarito
      if (isUltimaLinha) {
        const quadradosAtuais = items.length;

        if (quadradosAtuais >= gabaritoQuadrados) {
          // Se já tem mais ou igual ao gabarito, coloca apenas 1 R
          items.push('R');
        } else {
          // Se tem menos, preenche com R até igualar o gabarito
          const rsNecessarios = gabaritoQuadrados - quadradosAtuais;
          for (let i = 0; i < rsNecessarios; i++) {
            items.push('R');
          }
        }
        return { items, total: rowTotal };
      }

      // Regra: Qualquer outra linha incompleta (primeira ou meio) sempre tem apenas 1 R
      items.push('R');
      return { items, total: rowTotal };
    });
  });

  // Total de linhas do preview atual
  totalLinhasPreview = computed(() => this.retalhoPreviewRows().length);

  // Quantas linhas ficam ocultas quando compacta (>10)
  linhasOcultasPreview = computed(() => {
    const total = this.retalhoPreviewRows().length;
    return total > 10 ? total - 10 : 0;
  });

  // Estado para expandir/ocultar linhas ocultas
  linhasOcultasExpandido = signal(false);

  // Regra: penúltima (9) por padrão; última (10) só se a última linha for igual à anterior.
  penultimaIndexPreview = computed(() => {
    const previewRows = this.retalhoPreviewRows();
    const totalRows = previewRows.length;
    if (totalRows <= 10) return totalRows;
    // Se última linha é igual à primeira, botão na 10ª linha
    const ultimaIgualPrimeira = previewRows[totalRows - 1].total === previewRows[0].total;
    return ultimaIgualPrimeira ? 10 : 9;
  });

  // Linhas visíveis: até penultimaIndexPreview; se expandido, mostra todas
  retalhoPreviewRowsVisiveis = computed(() => {
    const allRows = this.retalhoPreviewRows();
    const totalRows = allRows.length;

    // Se estiver expandido ou se não houver linhas suficientes para precisar de compactação, mostra tudo.
    if (this.linhasOcultasExpandido() || totalRows <= 10) {
      return allRows;
    }

    const breakIndex = this.penultimaIndexPreview(); // Será 9 ou 10

    // Se o breakIndex for 10, significa que todas as linhas são iguais (ou não há o suficiente para esconder).
    // Mostramos as 10 primeiras.
    if (breakIndex === 10) {
      return allRows.slice(0, 10);
    }

    // Se o breakIndex for 9, significa que a última linha é diferente.
    // Mostramos as 9 primeiras e a última de todas para dar o contexto completo.
    const firstPart = allRows.slice(0, 9);
    const lastRow = allRows[totalRows - 1];
    return [...firstPart, lastRow];
  });

  // Toggle para expandir/ocultar linhas
  public toggleLinhasOcultas() {
    this.linhasOcultasExpandido.set(!this.linhasOcultasExpandido());
  }

  // Índice da linha onde o resumo (+N) deve aparecer no preview compactado.
  // Regra: penúltima (8) por padrão; última (9) só se a última linha for igual à anterior.
// Propriedades computadas para controlar a visibilidade das seções de input.
  showCorteInputs = computed(() => this.produto()?.tipoProduto === 'CORTE');
  showConsumoInputs = computed(() => this.produto()?.tipoProduto === 'CONSUMO');

  // Signal reativo para o valor do controle de canal de venda
  canalVendaIdValue;

  // Propriedade computada para exibir o nome do canal selecionado.
  selectedChannelName;


  // Signals para guardar os valores originais dos modos
  automaticoDimensoes = signal<{ largura: number | null, comprimento: number | null }>({ largura: null, comprimento: null });
  manualDimensoes = signal<{ largura: number | null, comprimento: number | null }>({ largura: null, comprimento: null });

  private ignoreDimensoesUpdate = false;

  private static readonly Texts = {
    LOAD_ERROR: 'Não foi possível carregar os dados da ordem de produção.',
    SAVE_ERROR: 'Falha ao salvar a ordem de produção. Verifique os dados e tente novamente.'
  };

  constructor() {
    this.form = this.fb.group({
      // ETAPA 1: SELEÇÃO
      tipoProducao: [''],
      produtoId: [null, Validators.required],
      quantidade: [null, [Validators.required, Validators.min(1)]],
      loteId: [null],

      // ETAPA 3: FORMULÁRIO REAL
      modoCalculo: ['AUTOMATICO', Validators.required],
      larguraBlocoProdutosCm: [{ value: null, disabled: true }],
      comprimentoBlocoProdutosCm: [{ value: null, disabled: true }],
      margens: this.fb.group({
        superior: [null],
        inferior: [null],
        esquerda: [null],
        direita: [null]
      }),
      canalVendaId: [''],
      motivo: ['', Validators.maxLength(255)]
    });

    // Inicializa o signal reativo após a criação do formulário
    this.canalVendaIdValue = toSignal(this.canalVendaIdControl.valueChanges, { initialValue: '' });

    // Inicializa o computed signal que depende do signal reativo
    this.selectedChannelName = computed(() => {
      const channelId = this.canalVendaIdValue();
      if (channelId === '' || channelId === null) {
        return 'Nenhum';
      }
      return this.channels().find(c => c.id === channelId)?.nome || 'Nenhum';
    });

    // Monitora mudanças nos campos críticos para exigir nova verificação
    this.setupVerificationTriggers();

    // Atualiza manualDimensoes sempre que o usuário edita os campos no modo MANUAL
    this.larguraBlocoProdutosCmControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(val => {
      if (this.modoCalculoControl.value === 'MANUAL' && !this.ignoreDimensoesUpdate) {
        this.manualDimensoes.set({
          largura: val !== null ? Number(val) : null,
          comprimento: this.comprimentoBlocoProdutosCmControl.value !== null ? Number(this.comprimentoBlocoProdutosCmControl.value) : null
        });
      }
    });
    this.comprimentoBlocoProdutosCmControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(val => {
      if (this.modoCalculoControl.value === 'MANUAL' && !this.ignoreDimensoesUpdate) {
        this.manualDimensoes.set({
          largura: this.larguraBlocoProdutosCmControl.value !== null ? Number(this.larguraBlocoProdutosCmControl.value) : null,
          comprimento: val !== null ? Number(val) : null
        });
      }
    });
  }

  formatProductUnit(quantity: number | null | undefined): string {
    return Number(quantity ?? 0) === 1 ? 'unidade' : 'unidades';
  }

  getCutPieceOrientationLabel(rotacionado: boolean | null | undefined): string {
    return rotacionado ? 'rotacionado' : 'não rotacionado';
  }

  getBlockMarginsUsageLabel(): string {
    const margens = this.form.getRawValue().margens ?? {};
    const hasMargins = Number(margens.superior || 0) > 0
      || Number(margens.inferior || 0) > 0
      || Number(margens.esquerda || 0) > 0
      || Number(margens.direita || 0) > 0;

    return hasMargins ? 'com margens do usuário' : 'usuário não usou margens';
  }

  formatConsumptionUnit(result: SimulationConsumptionResult, quantity: number | null | undefined): string {
    return this.enumService.formatQuantityWithUnit(quantity ?? 0, {
      viewValue: result.unidadeDescricao,
      pluralViewValue: result.unidadeDescricaoPlural,
      simbolo: result.unidadeSimbolo,
      displayQuantityWithSymbol: result.exibirQuantidadeComSimbolo
    });
  }

  ngOnInit(): void {
    this.isEditMode.set(!!this.data.template?.id && !this.data.isViewMode);

    if (this.isEditMode()) {
      this.tipoProducaoControl.disable({ emitEvent: false });
      this.initializeEditForm().catch(() => {
        this.entityDialog.showErrorSnackbar(ProductionForm.Texts.LOAD_ERROR);
        this.dialogRef.close(false);
      });
    }
  }

  private async initializeEditForm(): Promise<void> {
    const selfUrl = this.data.template?._links?.['self']?.href;
    if (!selfUrl) {
      throw new Error('Link self da ordem não encontrado.');
    }

    const order = await lastValueFrom(this.productionService.findByUrl(selfUrl));
    this.currentOrder.set(order);

    const [product, batch] = await Promise.all([
      lastValueFrom(this.productService.findById(order.produtoId)),
      order.lotesConsumidosIds?.length
        ? lastValueFrom(this.batchService.findById(order.lotesConsumidosIds[0]))
        : Promise.resolve(null)
    ]);

    this.applyOrderToForm(order, product, batch);
    this.loadEditState(order, product);
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  private applyOrderToForm(order: ProductionOrder, product: Product, batch: Batch | null): void {
    const isAutomaticCutOrder = order.tipoProduto === 'CORTE' && order.modoCalculo === 'AUTOMATICO';

    this.syncProductSelection(product, false);
    this.syncBatchSelection(batch, false);

    this.form.patchValue({
      tipoProducao: order.tipoProduto ?? '',
      produtoId: order.produtoId,
      quantidade: order.quantidadeProduzida,
      loteId: batch?.id ?? order.lotesConsumidosIds?.[0] ?? null,
      modoCalculo: order.modoCalculo ?? 'AUTOMATICO',
      larguraBlocoProdutosCm: isAutomaticCutOrder ? null : (order.larguraFinalCm ?? null),
      comprimentoBlocoProdutosCm: order.comprimentoFinalCm ?? null,
      margens: {
        superior: order.margens?.superior ?? null,
        inferior: order.margens?.inferior ?? null,
        esquerda: order.margens?.esquerda ?? null,
        direita: order.margens?.direita ?? null
      },
      canalVendaId: order.canalVendaDestinoId ?? '',
      motivo: order.motivo ?? ''
    }, { emitEvent: false });

    this.automaticoDimensoes.set({
      largura: isAutomaticCutOrder ? null : (order.larguraFinalCm ?? null),
      comprimento: order.comprimentoFinalCm ?? null
    });
    this.manualDimensoes.set({
      largura: isAutomaticCutOrder ? null : (order.larguraFinalCm ?? null),
      comprimento: order.comprimentoFinalCm ?? null
    });

    this.onModoCalculoChange();
    this.syncSelectedSearchInputs(product, batch);
    this.cdr.markForCheck();
  }

  private loadEditState(order: ProductionOrder, product: Product): void {
    if (order.tipoProduto === 'CORTE') {
      const result = this.buildEditCutSimulation(order, product);
      this.simulationResult.set(result);
      this.form.patchValue({
        larguraBlocoProdutosCm: result.larguraBlocoProdutosCm ?? null,
        comprimentoBlocoProdutosCm: result.comprimentoBlocoProdutosCm ?? null
      }, { emitEvent: false });
      this.automaticoDimensoes.set({
        largura: result.larguraBlocoProdutosCm ?? null,
        comprimento: result.comprimentoBlocoProdutosCm ?? null
      });

      if (order.modoCalculo === 'MANUAL') {
        this.manualDimensoes.set({
          largura: result.larguraBlocoProdutosCm ?? null,
          comprimento: result.comprimentoBlocoProdutosCm ?? null
        });
      }
    } else {
      const result = this.buildEditConsumptionSimulation(order, product);
      this.simulationResult.set(result);
      this.consumptionSimulationSnapshot.set({
        quantidade: order.quantidadeProduzida,
        unidadesPorProduto: Number(product.unidadesPorProduto || 1)
      });
    }

    this.formSnapshot = this.form.getRawValue();
    this.persistedSnapshot = this.form.getRawValue();
    this.simulationFormSnapshot.set(this.formSnapshot);
    this.needsVerification.set(false);
    this.cdr.markForCheck();
  }

  private buildEditCutSimulation(order: ProductionOrder, product: Product): SimulationCutResult {
    const productCuts = (order.cortesRealizados ?? []).filter(cut => cut.tipo === 'PRODUTO');
    const lateralRetalho = (order.cortesRealizados ?? []).find(cut => cut.tipo === 'RETALHO' && cut.retalhoCategoria === 'LATERAL');
    const inferiorRetalho = (order.cortesRealizados ?? []).find(cut => cut.tipo === 'RETALHO' && cut.retalhoCategoria === 'FINAL');

    const pieceWidth = order.rotacionado ? Number(product.dimensoes?.comprimentoCm ?? 0) : Number(product.dimensoes?.larguraCm ?? 0);
    const pieceLength = order.rotacionado ? Number(product.dimensoes?.larguraCm ?? 0) : Number(product.dimensoes?.comprimentoCm ?? 0);
    const productsPerLine = Math.max(...productCuts.map(cut => Number(cut.quantidade || 0)), 0);
    const totalRows = productCuts.length || (order.quantidadeProduzida > 0 && productsPerLine > 0 ? Math.ceil(order.quantidadeProduzida / productsPerLine) : 0);
    const productsOnLastLine = totalRows > 0 && productsPerLine > 0
      ? order.quantidadeProduzida - (Math.max(totalRows - 1, 0) * productsPerLine)
      : order.quantidadeProduzida;
    const completedRows = totalRows > 0 && productsOnLastLine > 0 && productsOnLastLine < productsPerLine
      ? totalRows - 1
      : totalRows;

    const pureBlockWidth = order.modoCalculo === 'AUTOMATICO'
      ? Math.max(0, Number(order.larguraFinalCm ?? 0) - Number(order.margens?.esquerda ?? 0) - Number(order.margens?.direita ?? 0) - Number(lateralRetalho?.larguraCm ?? 0))
      : Number(order.larguraFinalCm ?? 0);
    const pureBlockLength = order.modoCalculo === 'AUTOMATICO'
      ? Math.max(0, Number(order.comprimentoFinalCm ?? 0) - Number(order.margens?.superior ?? 0) - Number(order.margens?.inferior ?? 0) - Number(inferiorRetalho?.comprimentoCm ?? 0))
      : Number(order.comprimentoFinalCm ?? 0);

    return {
      tipoSimulacao: 'CORTE',
      modoCalculo: order.modoCalculo ?? 'AUTOMATICO',
      larguraFinalCm: Number(order.larguraFinalCm ?? 0),
      comprimentoFinalCm: Number(order.comprimentoFinalCm ?? 0),
      consumoEstimado: 0,
      rotacionado: !!order.rotacionado,
      produtosPorLinha: productsPerLine,
      numeroLinhasCompletas: Math.max(completedRows, 0),
      produtosNaUltimaLinha: totalRows > 0 && productsOnLastLine < productsPerLine ? Math.max(productsOnLastLine, 0) : 0,
      sobraLateral: lateralRetalho ? `${lateralRetalho.larguraCm}cm x ${lateralRetalho.comprimentoCm}cm` : '',
      sobraInferior: inferiorRetalho ? `${inferiorRetalho.larguraCm}cm x ${inferiorRetalho.comprimentoCm}cm` : '',
      dimensaoProduto: `${pieceWidth}cm x ${pieceLength}cm`,
      consumoTotal: `${order.larguraFinalCm}cm x ${order.comprimentoFinalCm}cm`,
      larguraBlocoProdutosCm: pureBlockWidth,
      comprimentoBlocoProdutosCm: pureBlockLength
    };
  }

  private buildEditConsumptionSimulation(order: ProductionOrder, product: Product): SimulationConsumptionResult {
    const lotId = Number(order.lotesConsumidosIds?.[0] ?? 0);
    const unidadeDescricao = product.materiaPrima?.unidadeDescricao ?? product.materiaPrima?.unidadeDeConsumo ?? 'unidade';

    return {
      tipoSimulacao: 'CONSUMO',
      consumoTotalEstimado: Number(order.quantidadeProduzida || 0) * Number(product.unidadesPorProduto || 1),
      unidadeDeConsumo: product.materiaPrima?.unidadeDeConsumo ?? '',
      unidadeDescricao,
      unidadeDescricaoPlural: unidadeDescricao,
      unidadeSimbolo: '',
      exibirQuantidadeComSimbolo: false,
      planoDeConsumo: [
        {
          loteId: lotId,
          motivoLote: '',
          quantidadeAConsumir: Number(order.quantidadeProduzida || 0) * Number(product.unidadesPorProduto || 1)
        }
      ],
      saldoRestante: lotId ? { [lotId]: 0 } : {}
    };
  }

  private syncSelectedSearchInputs(product: Product | null, batch: Batch | null): void {
    setTimeout(() => {
      if (product && this.productStockSearchComponent) {
        this.productStockSearchComponent.setSelectedProduct(product);
      }
      if (batch && this.batchSearchComponent) {
        this.batchSearchComponent.setSelectedBatch(batch);
      }
    });
  }

  private setupVerificationTriggers(): void {
    // Lista de controles que invalidam a simulação
    const criticalControls = [
      this.form.get('quantidade'),
      this.larguraBlocoProdutosCmControl,
      this.comprimentoBlocoProdutosCmControl,
      this.form.get('margens.superior'),
      this.form.get('margens.inferior'),
      this.form.get('margens.esquerda'),
      this.form.get('margens.direita')
    ];

    criticalControls.forEach(control => {
      control?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => {
          if (this.simulationResult()) {
            // Verificação Inteligente: só ativa se houver mudança real em relação ao snapshot
            this.needsVerification.set(this.checkIfVerificationIsNeeded());
          }
        });
    });
  }

  /**
   * Verifica se o estado atual do formulário difere do último snapshot estável,
   * considerando apenas os campos relevantes para o modo de cálculo atual.
   */
  private checkIfVerificationIsNeeded(): boolean {
    if (!this.formSnapshot) return false;

    const current = this.form.getRawValue();
    const snapshot = this.formSnapshot;

    // 1. Quantidade é crítica em ambos os modos
    if (Number(current.quantidade || 0) !== Number(snapshot.quantidade || 0)) return true;

    // 2. Validação específica por modo
    if (current.modoCalculo === 'AUTOMATICO') {
      const m1 = current.margens;
      const m2 = snapshot.margens;
      return (
        Number(m1.superior || 0) !== Number(m2.superior || 0) ||
        Number(m1.inferior || 0) !== Number(m2.inferior || 0) ||
        Number(m1.esquerda || 0) !== Number(m2.esquerda || 0) ||
        Number(m1.direita || 0) !== Number(m2.direita || 0)
      );
    } else {
      return (
        Number(current.larguraBlocoProdutosCm || 0) !== Number(snapshot.larguraBlocoProdutosCm || 0) ||
        Number(current.comprimentoBlocoProdutosCm || 0) !== Number(snapshot.comprimentoBlocoProdutosCm || 0)
      );
    }
  }

  hasEditChanges(): boolean {
    if (!this.isEditMode() || !this.persistedSnapshot) {
      return true;
    }

    const current = this.form.getRawValue();
    const snapshot = this.persistedSnapshot;

    if (Number(current.quantidade || 0) !== Number(snapshot.quantidade || 0)) return true;
    if (String(current.modoCalculo || '') !== String(snapshot.modoCalculo || '')) return true;
    if (String(current.canalVendaId ?? '') !== String(snapshot.canalVendaId ?? '')) return true;
    if (String(current.motivo ?? '') !== String(snapshot.motivo ?? '')) return true;

    if (current.modoCalculo === 'AUTOMATICO') {
      const m1 = current.margens ?? {};
      const m2 = snapshot.margens ?? {};
      return (
        Number(m1.superior || 0) !== Number(m2.superior || 0) ||
        Number(m1.inferior || 0) !== Number(m2.inferior || 0) ||
        Number(m1.esquerda || 0) !== Number(m2.esquerda || 0) ||
        Number(m1.direita || 0) !== Number(m2.direita || 0)
      );
    }

    return (
      Number(current.larguraBlocoProdutosCm || 0) !== Number(snapshot.larguraBlocoProdutosCm || 0) ||
      Number(current.comprimentoBlocoProdutosCm || 0) !== Number(snapshot.comprimentoBlocoProdutosCm || 0)
    );
  }

  /**
   * Chamado apenas quando o usuário muda manualmente a seleção do tipo de produção.
   * Notifica o componente de busca que os filtros foram alterados pelo usuário.
   */
  onTipoProducaoManualChange(): void {
    if (this.productStockSearchComponent) {
      this.productStockSearchComponent.markFiltersAsDirty();
    }
  }

  /**
   * Getter para o FormControl de tipo de produção
   */
  get tipoProducaoControl(): FormControl {
    return this.form.get('tipoProducao') as FormControl;
  }

  /**
   * Getter para o FormControl de produto, seguindo padrão de MaterialTypeSearch
   */
  get produtoControl(): FormControl {
    return this.form.get('produtoId') as FormControl;
  }

  /**
   * Getter para o FormControl de loteId
   */
  get loteIdControl(): FormControl {
    return this.form.get('loteId') as FormControl;
  }

  /**
   * Getter para o FormControl de modoCalculo
   */
  get modoCalculoControl(): FormControl {
    return this.form.get('modoCalculo') as FormControl;
  }

  /**
   * Getter para o FormControl de larguraBlocoProdutosCm
   */
  get larguraBlocoProdutosCmControl(): FormControl {
    return this.form.get('larguraBlocoProdutosCm') as FormControl;
  }

  /**
   * Getter para o FormControl de comprimentoBlocoProdutosCm
   */
  get comprimentoBlocoProdutosCmControl(): FormControl {
    return this.form.get('comprimentoBlocoProdutosCm') as FormControl;
  }

  /**
   * Getter para o FormControl de canalVendaId
   */
  get canalVendaIdControl(): FormControl {
    return this.form.get('canalVendaId') as FormControl;
  }

  /**
   * Getter para o FormControl de motivo
   */
  get motivoControl(): FormControl {
    return this.form.get('motivo') as FormControl;
  }

  /**
   * Callback quando um produto é selecionado no componente de busca.
   * Atualiza o formulário com os dados do produto selecionado.
   */
  onProdutoChange(event: MatSelectChange): void {
    const produto = event.value as Product;
    this.syncProductSelection(produto);
  }

  /**
   * Callback quando um lote é selecionado no componente de busca de lotes.
   */
  onLoteChange(lote: Batch): void {
    this.syncBatchSelection(lote);
  }

  private syncProductSelection(produto: Product | null, resetState = true): void {
    this.produto.set(produto);

    if (resetState) {
      this.simulationResult.set(null);
      this.simulationFormSnapshot.set(null);
      this.consumptionSimulationSnapshot.set(null);
      this.loteSelecionado.set(null);

      if (this.batchSearchComponent) {
        this.batchSearchComponent.reset();
      }
    }

    this.form.patchValue({
      produtoId: produto?.id ?? null,
      tipoProducao: produto?.tipoProduto ?? ''
    }, { emitEvent: false });

    if (produto?.tipoProduto === 'CORTE' || produto?.tipoProduto === 'CONSUMO') {
      this.loteIdControl.addValidators(Validators.required);
    } else {
      this.loteIdControl.removeValidators(Validators.required);
    }
    this.loteIdControl.updateValueAndValidity({ emitEvent: false });
    this.cdr.markForCheck();
  }

  private syncBatchSelection(lote: Batch | null, resetState = true): void {
    this.loteSelecionado.set(lote);
    this.loteIdControl.setValue(lote?.id ?? null, { emitEvent: false });

    if (resetState) {
      this.simulationResult.set(null);
      this.simulationFormSnapshot.set(null);
      this.consumptionSimulationSnapshot.set(null);
    }
  }

  /**
   * Callback quando o modo de cálculo é alterado.
   * Atualiza os validators das dimensões conforme o modo escolhido.
   * - AUTOMATICO: dimensões readonly (vêm da simulação), margens opcionais
   * - MANUAL: dimensões obrigatórias (usuário digita), margens não aparecem
   */
  onModoCalculoChange(): void {
    const modo = this.modoCalculoControl.value;
    const larguraControl = this.larguraBlocoProdutosCmControl;
    const comprimentoControl = this.comprimentoBlocoProdutosCmControl;

    this.ignoreDimensoesUpdate = true;
    if (modo === 'MANUAL') {
      // Se manualDimensoes está vazio/null, inicializa com valores do automático
      const dimensoesManual = this.manualDimensoes();
      if ((dimensoesManual.largura == null || dimensoesManual.comprimento == null)) {
        const dimensoesAuto = this.automaticoDimensoes();
        this.manualDimensoes.set({
          largura: dimensoesAuto.largura,
          comprimento: dimensoesAuto.comprimento
        });
      }
      // Modo MANUAL: habilita os campos e adiciona validadores
      larguraControl.enable();
      comprimentoControl.enable();
      larguraControl.setValidators([Validators.required, Validators.min(0.1)]);
      comprimentoControl.setValidators([Validators.required, Validators.min(0.1)]);
      // Restaura os valores do manual
      const dimensoesManualAtual = this.manualDimensoes();
      this.form.patchValue({
        larguraBlocoProdutosCm: dimensoesManualAtual.largura,
        comprimentoBlocoProdutosCm: dimensoesManualAtual.comprimento
      }, { emitEvent: false });
    } else {
      // Modo AUTOMATICO: desabilita os campos (remove validadores implicitamente)
      larguraControl.disable();
      comprimentoControl.disable();
      larguraControl.clearValidators();
      comprimentoControl.clearValidators();
      // Restaura os valores do automático
      const dimensoesAuto = this.automaticoDimensoes();
      this.form.patchValue({
        larguraBlocoProdutosCm: dimensoesAuto.largura,
        comprimentoBlocoProdutosCm: dimensoesAuto.comprimento
      }, { emitEvent: false });
    }
    this.ignoreDimensoesUpdate = false;

    larguraControl.updateValueAndValidity({ emitEvent: false });
    comprimentoControl.updateValueAndValidity({ emitEvent: false });

    // Atualiza needsVerification após alternância de modo
    this.needsVerification.set(this.checkIfVerificationIsNeeded());

    // Scroll automático para o final para garantir visibilidade das margens ou botões
    this.scrollToBottom();

    this.cdr.markForCheck();
  }

  /**
   * Executa a simulação de produção inicial.
   */
  onSimulate(): void {
    if (!this.produto() || this.form.get('quantidade')?.invalid || this.form.get('loteId')?.invalid) {
      return;
    }

    const url = this.produto()?._links?.["simulate"]?.href;
    if (!url) {
      console.error('URL de simulação não encontrada para o produto.');
      return;
    }

    const produto = this.produto()!;
    const quantidade = Number(this.form.value.quantidade);
    const loteId = Number(this.form.value.loteId);

    this.isSimulating.set(true);
    this.needsVerification.set(false);
    this.simulationResult.set(null);
    this.simulationFormSnapshot.set(null);
    this.consumptionSimulationSnapshot.set(null);

    const payload: SimulationRequest = {
      produtoId: produto.id,
      quantidade: quantidade,
      loteId: loteId,
      ordemId: this.isEditMode() ? this.currentOrder()?.id : undefined
    };

    if (produto.tipoProduto === 'CORTE') {
      console.log('%c[DEBUG] Payload ENVIADO para Simulação (CORTE):', 'color: blue; font-weight: bold;', payload);
      this.productionService.simulateProduction(url, payload)
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            console.log('%c[DEBUG] Resposta RECEBIDA da Simulação (CORTE):', 'color: green; font-weight: bold;', response);
            this.simulationResult.set(response as SimulationResult);
            if (response.tipoSimulacao === 'CORTE') {
              this.form.patchValue({
                larguraBlocoProdutosCm: response.larguraBlocoProdutosCm,
                comprimentoBlocoProdutosCm: response.comprimentoBlocoProdutosCm
              }, { emitEvent: false });
              this.automaticoDimensoes.set({
                largura: response.larguraBlocoProdutosCm ?? null,
                comprimento: response.comprimentoBlocoProdutosCm ?? null
              });
            }
            this.formSnapshot = this.form.getRawValue();
            this.simulationFormSnapshot.set(this.formSnapshot);
            this.isSimulating.set(false);
            this.scrollToBottom();
          },
          error: (err) => {
            console.error('%c[DEBUG] Erro na Simulação (CORTE):', 'color: red; font-weight: bold;', err);
            this.simulationResult.set(null);
            this.simulationFormSnapshot.set(null);
            this.isSimulating.set(false);
          }
        });
    } else if (produto.tipoProduto === 'CONSUMO') {
      console.log('%c[DEBUG] Payload ENVIADO para Simulação (CONSUMO):', 'color: purple; font-weight: bold;', payload);
      this.productionService.simulateConsumption(url, payload)
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            console.log('%c[DEBUG] Resposta RECEBIDA da Simulação (CONSUMO):', 'color: green; font-weight: bold;', response);
            this.simulationResult.set(response as SimulationResult);
            this.consumptionSimulationSnapshot.set({
              quantidade,
              unidadesPorProduto: Number(produto.unidadesPorProduto || 1)
            });
            this.formSnapshot = this.form.getRawValue();
            this.simulationFormSnapshot.set(this.formSnapshot);
            this.isSimulating.set(false);
            this.scrollToBottom();
          },
          error: (err) => {
            console.error('%c[DEBUG] Erro na Simulação (CONSUMO):', 'color: red; font-weight: bold;', err);
            this.simulationResult.set(null);
            this.simulationFormSnapshot.set(null);
            this.consumptionSimulationSnapshot.set(null);
            this.isSimulating.set(false);
          }
        });
    } else {
      this.isSimulating.set(false);
    }
  }

  onPrimaryAction(): void {
    if (this.isEditMode()) {
      this.onVerify();
      return;
    }

    this.onSimulate();
  }

  isPrimaryActionDisabled(): boolean {
    if (this.isEditMode()) {
      return !this.needsVerification() || this.isVerifying() || this.form.invalid || !this.hasEditChanges();
    }

    return this.isSimulating();
  }

  /**
   * Monta o objeto de margens a partir dos valores do formulário.
   * Retorna undefined se o modo não for AUTOMATICO.
   */
  private buildMargensPayload(formValue: any): { superior: number; inferior: number; esquerda: number; direita: number } | undefined {
    if (formValue.modoCalculo !== 'AUTOMATICO') {
      return undefined;
    }

    return {
      superior: formValue.margens.superior ? Number(formValue.margens.superior) : 0,
      inferior: formValue.margens.inferior ? Number(formValue.margens.inferior) : 0,
      esquerda: formValue.margens.esquerda ? Number(formValue.margens.esquerda) : 0,
      direita: formValue.margens.direita ? Number(formValue.margens.direita) : 0
    };
  }

  /**
   * Executa a verificação dos dados (re-simulação).
   */
  onVerify(): void {
    const formValue = this.form.getRawValue();
    const currentResult = this.simulationResult();

    if (!currentResult) return;

    if (currentResult.tipoSimulacao === 'CONSUMO') {
      const produto = this.produto();
      const url = produto?._links?.["simulate"]?.href;

      if (!produto || !url) {
        return;
      }

      this.isVerifying.set(true);

      const payload: SimulationRequest = {
        produtoId: Number(formValue.produtoId),
        quantidade: Number(formValue.quantidade),
        loteId: Number(formValue.loteId),
        ordemId: this.isEditMode() ? this.currentOrder()?.id : undefined
      };

      this.productionService.simulateConsumption(url, payload)
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            this.simulationResult.set(response);
            this.consumptionSimulationSnapshot.set({
              quantidade: Number(formValue.quantidade),
              unidadesPorProduto: Number(produto.unidadesPorProduto || 1)
            });
            this.formSnapshot = this.form.getRawValue();
            this.simulationFormSnapshot.set(this.formSnapshot);
            this.needsVerification.set(false);
            this.isVerifying.set(false);
            this.scrollToBottom();
          },
          error: (err) => {
            this.isVerifying.set(false);
            console.error('Erro na verificação de consumo:', err);
          }
        });
      return;
    }

    const oldResult = currentResult as SimulationCutResult;

    // Montar payload para a API de verificação
    let payload: VerificationRequest;
    if (formValue.modoCalculo === 'MANUAL') {
      payload = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidade: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
        ordemId: this.isEditMode() ? this.currentOrder()?.id : undefined,
        larguraBlocoProdutosCm: Number(formValue.larguraBlocoProdutosCm),
        comprimentoBlocoProdutosCm: Number(formValue.comprimentoBlocoProdutosCm)
      };
    } else {
      payload = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidade: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
        ordemId: this.isEditMode() ? this.currentOrder()?.id : undefined,
        larguraBlocoProdutosCm: oldResult.larguraBlocoProdutosCm,
        comprimentoBlocoProdutosCm: oldResult.comprimentoBlocoProdutosCm
      };
      const margens = this.buildMargensPayload(formValue);
      if (margens) {
        payload.margens = margens;
      }
    }

    this.isVerifying.set(true);
    console.log('%c[DEBUG] Payload ENVIADO para Verificação:', 'color: orange; font-weight: bold;', payload);

    this.productionService.verifyCutLayout(payload)
      .pipe(take(1))
      .subscribe({
        next: (newResult) => {
          console.log('%c[DEBUG] Resposta RECEBIDA da Verificação:', 'color: purple; font-weight: bold;', newResult);
          this.isVerifying.set(false);
          this.showVerificationDialog(oldResult, newResult, formValue);
        },
        error: (err) => {
          this.isVerifying.set(false);
          // TODO: Mostrar alerta de erro (ex: dimensões insuficientes no manual)
          console.error('Erro na verificação:', err);
        }
      });
  }

  toggleRetalhoPreview(): void {
    this.showRetalhoPreview.set(!this.showRetalhoPreview());
  }


  /**
   * Compacta a quantidade de produtos em tokens grandes para reduzir ruido visual.
   * Ex.: 23 -> [10, 10, 1, 1, 1]
   */
  private buildCompactProductTokens(count: number): RetalhoValue[] {
    const tokens: RetalhoValue[] = [];
    const groups: Array<{ value: RetalhoValue; size: number }> = [
      { value: '100', size: 100 },
      { value: '50', size: 50 },
      { value: '10', size: 10 },
      { value: '5', size: 5 },
      { value: '1', size: 1 }
    ];

    let remaining = Math.max(0, Math.floor(count));

    for (const group of groups) {
      while (remaining >= group.size) {
        tokens.push(group.value);
        remaining -= group.size;
      }
    }

    return tokens;
  }

  /**
   * Exibe o diálogo de confirmação com a comparação real entre o estado atual e o verificado.
   */
  private showVerificationDialog(oldR: SimulationCutResult, newR: SimulationCutResult, formValue: any): void {
    const oldQtd = this.formSnapshot?.quantidade || 0;
    const newQtd = formValue.quantidade;
    const oldModo = this.formSnapshot?.modoCalculo || 'AUTOMATICO';
    const newModo = formValue.modoCalculo;

    // Comparação de dimensões no modo MANUAL
    let dimensaoLine = '';
    if (newModo === 'MANUAL') {
      const oldLargura = this.formSnapshot?.larguraBlocoProdutosCm || '';
      const oldComprimento = this.formSnapshot?.comprimentoBlocoProdutosCm || '';
      const newLargura = formValue.larguraBlocoProdutosCm || '';
      const newComprimento = formValue.comprimentoBlocoProdutosCm || '';
      if (oldLargura !== newLargura || oldComprimento !== newComprimento) {
        dimensaoLine = `Dimensões: ${oldLargura} x ${oldComprimento} → ${newLargura} x ${newComprimento}`;
      }
    }

    // Formatação das linhas de comparação
    const infoLine = oldQtd !== newQtd ? `Informação: ${oldQtd} ${oldQtd === 1 ? 'produto' : 'produtos'} → ${newQtd} ${newQtd === 1 ? 'produto' : 'produtos'}.` : '';
    const oldLayoutStr = this.formatLayoutShortString(oldR, oldQtd);
    const newLayoutStr = this.formatLayoutShortString(newR, newQtd);
    const layoutLine = oldLayoutStr !== newLayoutStr ? `Produtos: ${oldLayoutStr} → ${newLayoutStr}` : '';
    const oldSobras = this.formatSobrasString(oldR);
    const newSobras = this.formatSobrasString(newR);
    const sobrasLine = oldSobras !== newSobras ? `Sobras: ${oldSobras} → ${newSobras}` : '';
    const consumoLine = oldR.consumoTotal !== newR.consumoTotal ? `Consumo total: ${oldR.consumoTotal} → ${newR.consumoTotal}` : '';
    const rotacaoLine = oldR.rotacionado !== newR.rotacionado ? `Rotação: ${oldR.rotacionado ? 'Sim' : 'Não'} → ${newR.rotacionado ? 'Sim' : 'Não'}` : '';
    const oldM = this.formSnapshot?.margens;
    const newM = formValue.margens;
    // Margens: mostra só as que mudaram, agrupando 2 por linha
    let margensMsg = '';
    if (oldM && newM) {
      const margensDiff: string[] = [];
      if (oldM.superior !== newM.superior) {
        margensDiff.push(`Superior:  ${oldM.superior || 0} → ${newM.superior || 0}`);
      }
      if (oldM.inferior !== newM.inferior) {
        margensDiff.push(`Inferior:  ${oldM.inferior || 0} → ${newM.inferior || 0}`);
      }
      if (oldM.esquerda !== newM.esquerda) {
        margensDiff.push(`Esquerda:  ${oldM.esquerda || 0} → ${newM.esquerda || 0}`);
      }
      if (oldM.direita !== newM.direita) {
        margensDiff.push(`Direita:   ${oldM.direita || 0} → ${newM.direita || 0}`);
      }
      if (margensDiff.length > 0) {
        // Agrupa 2 por linha
        const margensLines: string[] = [];
        for (let i = 0; i < margensDiff.length; i += 2) {
          margensLines.push(margensDiff.slice(i, i + 2).join('  '));
        }
        margensMsg = `Margens:\n${margensLines.join('\n')}`;
      }
    }

    let message: string;
    if (oldModo === 'AUTOMATICO' && newModo === 'MANUAL') {
      message = `As alterações mudaram o plano de produção:\n\n` +
                [infoLine, dimensaoLine, sobrasLine, consumoLine, 'modo automatico → modo manual']
                  .filter(Boolean)
                  .join('\n') +
                `\n\nDeseja aplicar estas mudanças?`;
    } else if (oldModo === 'MANUAL' && newModo === 'AUTOMATICO') {
      message = `As alterações mudaram o plano de produção:\n\n` +
                [infoLine, dimensaoLine, layoutLine, sobrasLine, consumoLine, rotacaoLine, margensMsg]
                  .filter(Boolean)
                  .join('\n') +
                `\n\nDeseja aplicar estas mudanças?`;
    } else if (oldModo === newModo && newModo === 'MANUAL') {
      message = `As alterações mudaram o plano de produção:\n\n` +
                [infoLine, dimensaoLine, sobrasLine, consumoLine]
                  .filter(Boolean)
                  .join('\n') +
                `\n\nDeseja aplicar estas mudanças?`;
    } else {
      // AUTOMATICO → AUTOMATICO
      message = `As alterações mudaram o plano de produção:\n\n` +
                [infoLine, layoutLine, sobrasLine, consumoLine, rotacaoLine, margensMsg]
                  .filter(Boolean)
                  .join('\n') +
                `\n\nDeseja aplicar estas mudanças?`;
    }

    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: { title: 'Confirmar Alterações', message: message },
      width: '600px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        // ACEITAR: Atualiza o estado estável
        this.simulationResult.set(newR);
        this.formSnapshot = this.form.getRawValue();
        this.simulationFormSnapshot.set(this.formSnapshot);
        this.needsVerification.set(false);

        // Se for automático, atualiza os campos de dimensão com os novos valores calculados
        if (formValue.modoCalculo === 'AUTOMATICO') {
          this.form.patchValue({
            larguraBlocoProdutosCm: newR.larguraBlocoProdutosCm,
            comprimentoBlocoProdutosCm: newR.comprimentoBlocoProdutosCm
          }, { emitEvent: false });
        }
      } else {
        // CANCELAR: Restaura o último estado estável
        if (this.formSnapshot) {
          const restoreData = { ...this.formSnapshot };
          restoreData.canalVendaId = this.form.get('canalVendaId')?.value;
          restoreData.motivo = this.form.get('motivo')?.value;
          this.form.patchValue(restoreData, { emitEvent: false });
          this.onModoCalculoChange();
          this.needsVerification.set(false);
        }
      }
      this.cdr.markForCheck();
    });
  }

  /**
   * Formata uma string curta descrevendo o layout (usado no alerta).
   */
  private formatLayoutShortString(r: SimulationCutResult, qtd: number): string {
    const totalLinhas = r.numeroLinhasCompletas + (r.produtosNaUltimaLinha > 0 ? 1 : 0);
    if (totalLinhas === 1) {
      return `${qtd} na única linha`;
    }
    return `${r.produtosPorLinha} por linha`;
  }

  /**
   * Formata uma string descrevendo as sobras (usado no alerta).
   */
  private formatSobrasString(r: SimulationCutResult): string {
    const parts: string[] = [];
    if (r.sobraLateral) parts.push(`Lateral ${r.sobraLateral}`);
    if (r.sobraInferior) parts.push(`Inferior ${r.sobraInferior}`);
    return parts.length > 0 ? parts.join(' | ') : 'Nenhuma';
  }

  /**
   * Rola o conteúdo do diálogo para o final de forma suave.
   */
  private scrollToBottom(): void {
    setTimeout(() => {
      const dialogContent = document.querySelector('mat-dialog-content');
      if (dialogContent) {
        dialogContent.scrollTo({ top: dialogContent.scrollHeight, behavior: 'smooth' });
      }
    }, 100);
  }

  onSave(): void {
    const simulation = this.simulationResult();
    const createUrl = simulation?._links?.['create-order']?.href;
    const updateUrl = this.currentOrder()?._links?.['update']?.href;

    if (!simulation || (!this.isEditMode() && !createUrl) || (this.isEditMode() && !updateUrl)) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();

    if (simulation.tipoSimulacao === 'CORTE') {
      const payload: CreateCutOrderRequest = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidadeProduzida: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
        larguraBlocoProdutosCm: Number(formValue.larguraBlocoProdutosCm),
        comprimentoBlocoProdutosCm: Number(formValue.comprimentoBlocoProdutosCm),
        canalVendaDestinoId: formValue.canalVendaId ? Number(formValue.canalVendaId) : null,
        motivo: formValue.motivo || null,
        margens: this.buildMargensPayload(formValue)
      };

      const operation = this.isEditMode()
        ? this.productionService.updateCutOrder(updateUrl!, payload)
        : this.productionService.createCutOrder(createUrl!, payload);

      console.log('%c[DEBUG] Payload FINAL ENVIADO para salvar Ordem:', 'color: #bada55; font-weight: bold;', payload);

      operation
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            console.log('%c[DEBUG] Ordem de Produção salva com SUCESSO:', 'color: green; font-weight: bold;', response);
            this.isSaving.set(false);
            this.dialogRef.close(true);
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Erro ao salvar ordem de produção por corte:', err);
            this.entityDialog.showErrorSnackbar(err.error?.detail || err.error?.message || ProductionForm.Texts.SAVE_ERROR);
            this.isSaving.set(false);
            this.cdr.markForCheck();
          }
        });
      return;
    }

    if (simulation.tipoSimulacao === 'CONSUMO') {
      const payload: CreateConsumptionOrderRequest = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidadeProduzida: Number(formValue.quantidade),
        canalVendaDestinoId: formValue.canalVendaId ? Number(formValue.canalVendaId) : null,
        motivo: formValue.motivo || null
      };

      const operation = this.isEditMode()
        ? this.productionService.updateConsumptionOrder(updateUrl!, payload)
        : this.productionService.createConsumptionOrder(createUrl!, payload);

      console.log('%c[DEBUG] Payload FINAL ENVIADO para salvar Ordem (CONSUMO):', 'color: #bada55; font-weight: bold;', payload);

      operation
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            console.log('%c[DEBUG] Ordem de Produção salva com SUCESSO (CONSUMO):', 'color: green; font-weight: bold;', response);
            this.isSaving.set(false);
            this.dialogRef.close(true);
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Erro ao salvar ordem de produção por consumo:', err);
            this.entityDialog.showErrorSnackbar(err.error?.detail || err.error?.message || ProductionForm.Texts.SAVE_ERROR);
            this.isSaving.set(false);
            this.cdr.markForCheck();
          }
        });
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
