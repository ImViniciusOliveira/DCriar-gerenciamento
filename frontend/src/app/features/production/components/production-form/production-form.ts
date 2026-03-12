import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { take } from 'rxjs';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ProductionOrder } from '../../models/production.model';
import { Product } from '../../../products/models/product.model';
import { ProductStockSearch } from '../../../../shared/components/product-stock-search/product-stock-search';
import { CreateCutOrderRequest, ProductionService, SimulationRequest, VerificationRequest } from '../../services/production.service';
import { SimulationResult, SimulationCutResult } from '../../models/simulation.model';
import { BatchSearch } from '../../../../shared/components/batch-search/batch-search';
import {Batch} from '../../../stock/models/batch.model';
import { ChannelService } from '../../../stock/services/channel.service';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { RethalboBadgeComponent, RetalhoValue } from '../retalho-badge/retalho-badge.component';

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
    MatProgressSpinnerModule,
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
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  @ViewChild(ProductStockSearch) private productStockSearchComponent!: ProductStockSearch;
  @ViewChild(BatchSearch) private batchSearchComponent!: BatchSearch;

  form: FormGroup;
  isSaving = signal(false);
  isSimulating = signal(false);
  isVerifying = signal(false);

  produto = signal<Product | null>(null);
  loteSelecionado = signal<Batch | null>(null);
  showRetalhoPreview = signal(false);

  // Carrega os canais reais da API usando o novo serviço
  channels = toSignal(this.channelService.getAllChannels(), { initialValue: [] });

  // Signal com tipo forte para armazenar o resultado da simulação.
  simulationResult = signal<SimulationResult | null>(null);

  // Signal para controlar se a verificação é necessária (dados alterados após simulação)
  needsVerification = signal(false);

  // Snapshot para restaurar o formulário em caso de cancelamento da verificação
  private formSnapshot: any;

  // Formata a string da dimensão rotacionada de forma segura
  rotatedDimensionString = computed(() => {
    const result = this.simulationResult();
    if (!result || result.tipoSimulacao !== 'CORTE' || !result.dimensaoProduto) {
      return '';
    }
    return result.dimensaoProduto.split('x').map(s => s.trim()).reverse().join(' x ');
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
  showConsumoInputs = computed(() => this.produto()?.tipoProduto === 'CONSUMO_DIRETO');

  // Signal reativo para o valor do controle de canal de venda
  canalVendaIdValue;

  // Propriedade computada para exibir o nome do canal selecionado.
  selectedChannelName;


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
  }

  ngOnInit(): void {
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
    this.produto.set(produto);
    this.simulationResult.set(null);
    this.loteSelecionado.set(null);

    // Atualiza o formulário com o ID e também sincroniza o dropdown de tipo de produção.
    this.form.patchValue({
      produtoId: produto.id,
      tipoProducao: produto.tipoProduto
    });

    // Reseta o componente BatchSearch usando seu método público (mantém encapsulamento)
    if (this.batchSearchComponent) {
      this.batchSearchComponent.reset();
    }

    // Adiciona ou remove o validador 'required' para loteId com base no tipo de produto
    if (produto.tipoProduto === 'CORTE') {
      this.loteIdControl.addValidators(Validators.required);
    } else {
      this.loteIdControl.removeValidators(Validators.required);
    }
    this.loteIdControl.updateValueAndValidity();

    // Força a detecção de mudanças para garantir que o mat-select-trigger seja atualizado.
    this.cdr.markForCheck();
  }

  /**
   * Callback quando um lote é selecionado no componente de busca de lotes.
   */
  onLoteChange(lote: Batch): void {
    this.loteSelecionado.set(lote);
    this.loteIdControl.setValue(lote.id);
    this.simulationResult.set(null);
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

    if (modo === 'MANUAL') {
      // Modo MANUAL: habilita os campos e adiciona validadores
      larguraControl.enable();
      comprimentoControl.enable();
      larguraControl.setValidators([Validators.required, Validators.min(0.1)]);
      comprimentoControl.setValidators([Validators.required, Validators.min(0.1)]);
    } else {
      // Modo AUTOMATICO: desabilita os campos (remove validadores implicitamente)
      larguraControl.disable();
      comprimentoControl.disable();
      larguraControl.clearValidators();
      comprimentoControl.clearValidators();

      // Restaura os valores originais da simulação se disponíveis
      const result = this.simulationResult();
      if (result && result.tipoSimulacao === 'CORTE') {
        this.form.patchValue({
          larguraBlocoProdutosCm: result.larguraBlocoProdutosCm,
          comprimentoBlocoProdutosCm: result.comprimentoBlocoProdutosCm
        }, { emitEvent: false });
      }
    }

    // Verificação Inteligente: avalia se o novo estado exige verificação
    this.needsVerification.set(this.checkIfVerificationIsNeeded());

    larguraControl.updateValueAndValidity({ emitEvent: false });
    comprimentoControl.updateValueAndValidity({ emitEvent: false });

    // Scroll automático para o final para garantir visibilidade das margens ou botões
    this.scrollToBottom();

    this.cdr.markForCheck();
  }

  /**
   * Executa a simulação de produção inicial.
   */
  onSimulate(): void {
    if (!this.produto() || !this.form.value.quantidade) {
      return;
    }

    const url = this.produto()?._links?.["simulate"]?.href;
    if (!url) {
      return;
    }

    let payload: SimulationRequest = {
      produtoId: this.produto()!.id,
      quantidade: Number(this.form.value.quantidade)
    };

    // Adiciona loteId ao payload se for um produto de CORTE
    if (this.produto()?.tipoProduto === 'CORTE' && this.loteSelecionado()) {
      payload = { ...payload, loteId: Number(this.loteSelecionado()!.id) };
    }
    // TODO: Adicionar lotesConsumidosIds para CONSUMO_DIRETO

    // --- DEBUG INÍCIO ---
    console.log('%c[DEBUG] Payload ENVIADO para Simulação:', 'color: blue; font-weight: bold;', payload);
    // --- DEBUG FIM ---

    this.isSimulating.set(true);
    this.needsVerification.set(false); // Reseta o estado de verificação ao iniciar nova simulação

    this.productionService.simulateProduction(url, payload)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          // --- DEBUG INÍCIO ---
          console.log('%c[DEBUG] Resposta RECEBIDA da Simulação:', 'color: green; font-weight: bold;', response);
          // --- DEBUG FIM ---

          this.simulationResult.set(response as SimulationResult);
          this.isSimulating.set(false);
          this.needsVerification.set(false);

          // Popula os campos do formulário com os dados da simulação
          if (response.tipoSimulacao === 'CORTE') {
            this.form.patchValue({
              larguraBlocoProdutosCm: response.larguraBlocoProdutosCm,
              comprimentoBlocoProdutosCm: response.comprimentoBlocoProdutosCm
            }, { emitEvent: false });
          }

          // Salva o estado atual do formulário para restauração futura
          this.formSnapshot = this.form.getRawValue();

          // Scroll automático para o final do diálogo para focar no feedback
          this.scrollToBottom();
        },
        error: (_err) => {
          // --- DEBUG INÍCIO ---
          console.error('%c[DEBUG] Erro na Simulação:', 'color: red; font-weight: bold;', _err);
          // --- DEBUG FIM ---
          this.simulationResult.set(null);
          this.isSimulating.set(false);
          // TODO: Mostrar uma notificação de erro para o usuário.
        }
      });
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

    if (!currentResult || currentResult.tipoSimulacao !== 'CORTE') return;
    const oldResult = currentResult as SimulationCutResult;

    // Montar payload para a API de verificação
    let payload: VerificationRequest;
    if (formValue.modoCalculo === 'MANUAL') {
      payload = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidade: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
        larguraBlocoProdutosCm: Number(formValue.larguraBlocoProdutosCm),
        comprimentoBlocoProdutosCm: Number(formValue.comprimentoBlocoProdutosCm)
      };
    } else {
      payload = {
        produtoId: Number(formValue.produtoId),
        loteId: Number(formValue.loteId),
        quantidade: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
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

    // Formatação das linhas de comparação
    const infoLine = `Informação: ${oldQtd} ${oldQtd === 1 ? 'produto' : 'produtos'} → ${newQtd} ${newQtd === 1 ? 'produto' : 'produtos'}.`;

    const oldLayoutStr = this.formatLayoutShortString(oldR, oldQtd);
    const newLayoutStr = this.formatLayoutShortString(newR, newQtd);
    const layoutLine = `Produtos: ${oldLayoutStr} → ${newLayoutStr}`;

    const oldSobras = this.formatSobrasString(oldR);
    const newSobras = this.formatSobrasString(newR);
    const sobrasLine = `Sobras: ${oldSobras} → ${newSobras}`;

    const consumoLine = `Consumo total: ${oldR.consumoTotal} → ${newR.consumoTotal}`;
    const rotacaoLine = `Rotação: ${oldR.rotacionado ? 'Sim' : 'Não'} → ${newR.rotacionado ? 'Sim' : 'Não'}`;

    // Margens com alinhamento profissional
    const oldM = this.formSnapshot?.margens;
    const newM = formValue.margens;
    const labelSup = "Superior:".padEnd(10);
    const labelInf = "Inferior:".padEnd(10);
    const labelEsq = "Esquerda:".padEnd(10);
    const labelDir = "Direita:".padEnd(10);
    const margensMsg = `Margens: ${labelSup} ${oldM?.superior || 0} → ${newM.superior || 0}  ${labelInf} ${oldM?.inferior || 0} → ${newM.inferior || 0}\n` +
                       `         ${labelEsq} ${oldM?.esquerda || 0} → ${newM.esquerda || 0}  ${labelDir} ${oldM?.direita || 0} → ${newM.direita || 0}`;

    const message = `As alterações mudaram o plano de produção:\n\n` +
                    `${infoLine}\n` +
                    `${layoutLine}\n` +
                    `${sobrasLine}\n` +
                    `${consumoLine}\n` +
                    `${rotacaoLine}\n\n` +
                    `${margensMsg}\n\n` +
                    `Deseja aplicar estas mudanças?`;

    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: { title: 'Confirmar Alterações', message: message },
      width: '600px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        // ACEITAR: Atualiza o estado estável
        this.simulationResult.set(newR);
        this.formSnapshot = this.form.getRawValue();
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
    const url = simulation?._links?.['create-order']?.href;

    if (!simulation || !url) {
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
        lotePrincipalId: Number(formValue.loteId),
        quantidadeProduzida: Number(formValue.quantidade),
        modoCalculo: formValue.modoCalculo,
        larguraFinalCm: Number(formValue.larguraBlocoProdutosCm),
        comprimentoFinalCm: Number(formValue.comprimentoBlocoProdutosCm),
        canalVendaDestinoId: formValue.canalVendaId ? Number(formValue.canalVendaId) : null,
        motivo: formValue.motivo || null,
        margens: this.buildMargensPayload(formValue)
      };

      console.log('%c[DEBUG] Payload FINAL ENVIADO para Criar Ordem:', 'color: #bada55; font-weight: bold;', payload);

      this.productionService.createCutOrder(url, payload)
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            console.log('%c[DEBUG] Ordem de Produção criada com SUCESSO:', 'color: green; font-weight: bold;', response);
            this.isSaving.set(false);
            this.dialogRef.close(true);
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Erro ao criar ordem de produção por corte:', err);
            this.isSaving.set(false);
            this.cdr.markForCheck();
          }
        });
    }
    // TODO: Implementar a lógica para 'CONSUMO_DIRETO'
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
