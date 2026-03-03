import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
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
import { ProductionService, SimulationRequest } from '../../services/production.service';
import { SimulationResult } from '../../models/simulation.model';
import { BatchSearch } from '../../../../shared/components/batch-search/batch-search';
import {Batch} from '../../../stock/models/batch.model';
import { ChannelService } from '../../../stock/services/channel.service';

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
    MatCheckboxModule
  ],
  templateUrl: './production-form.html',
  styleUrls: ['./production-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProductionForm>);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly productionService = inject(ProductionService);
  private readonly channelService = inject(ChannelService);
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  @ViewChild(ProductStockSearch) private productStockSearchComponent!: ProductStockSearch;

  form: FormGroup;
  isSaving = signal(false);
  isSimulating = signal(false);
  isVerifying = signal(false);

  produto = signal<Product | null>(null);
  loteSelecionado = signal<Batch | null>(null);

  // Carrega os canais reais da API usando o novo serviço
  channels = toSignal(this.channelService.getAllChannels(), { initialValue: [] });

  // Signal com tipo forte para armazenar o resultado da simulação.
  simulationResult = signal<SimulationResult | null>(null);

  // Signal para controlar se a verificação é necessária (dados alterados após simulação)
  needsVerification = signal(false);

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
      larguraFinalCm: [{ value: null, disabled: true }],
      comprimentoFinalCm: [{ value: null, disabled: true }],
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
    // A lógica de reação a eventos foi movida para métodos específicos.
  }

  private setupVerificationTriggers(): void {
    // Lista de controles que invalidam a simulação
    const criticalControls = [
      this.form.get('quantidade'),
      this.larguraFinalCmControl,
      this.comprimentoFinalCmControl,
      this.form.get('margens.superior'),
      this.form.get('margens.inferior'),
      this.form.get('margens.esquerda'),
      this.form.get('margens.direita')
    ];

    criticalControls.forEach(control => {
      control?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => {
          // Só marca como necessário verificar se já houver um resultado de simulação
          if (this.simulationResult()) {
            this.needsVerification.set(true);
          }
        });
    });
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
   * Getter para o FormControl de larguraFinalCm
   */
  get larguraFinalCmControl(): FormControl {
    return this.form.get('larguraFinalCm') as FormControl;
  }

  /**
   * Getter para o FormControl de comprimentoFinalCm
   */
  get comprimentoFinalCmControl(): FormControl {
    return this.form.get('comprimentoFinalCm') as FormControl;
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
    const larguraControl = this.larguraFinalCmControl;
    const comprimentoControl = this.comprimentoFinalCmControl;

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
          larguraFinalCm: result.larguraFinalCm,
          comprimentoFinalCm: result.comprimentoFinalCm
        }, { emitEvent: false });
      }
    }

    larguraControl.updateValueAndValidity();
    comprimentoControl.updateValueAndValidity();
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


    this.isSimulating.set(true);
    this.productionService.simulateProduction(url, payload)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.simulationResult.set(response as SimulationResult);
          this.isSimulating.set(false);
          this.needsVerification.set(false);

          // Popula os campos do formulário com os dados da simulação
          if (response.tipoSimulacao === 'CORTE') {
            this.form.patchValue({
              larguraFinalCm: response.larguraFinalCm,
              comprimentoFinalCm: response.comprimentoFinalCm
            }, { emitEvent: false });
          }
        },
        error: (_err) => {
          this.simulationResult.set(null);
          this.isSimulating.set(false);
          // TODO: Mostrar uma notificação de erro para o usuário.
        }
      });
  }

  /**
   * Executa a verificação dos dados (re-simulação).
   */
  onVerify(): void {
    const formValue = this.form.getRawValue();
    const modo = formValue.modoCalculo;

    const payload: any = {
      produtoId: Number(formValue.produtoId),
      loteId: Number(formValue.loteId),
      quantidade: Number(formValue.quantidade),
      modoCalculo: modo,
      larguraFinalCm: Number(formValue.larguraFinalCm),
      comprimentoFinalCm: Number(formValue.comprimentoFinalCm),
    };

    if (modo === 'AUTOMATICO') {
      payload.margens = {
        superior: formValue.margens.superior ? Number(formValue.margens.superior) : 0,
        inferior: formValue.margens.inferior ? Number(formValue.margens.inferior) : 0,
        esquerda: formValue.margens.esquerda ? Number(formValue.margens.esquerda) : 0,
        direita: formValue.margens.direita ? Number(formValue.margens.direita) : 0
      };
    }

    console.log('Payload para verificação:', payload);
  }

  onSave(): void {
    // Validar que existe simulação antes de criar a ordem
    if (!this.simulationResult()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    // Montar payload para criar a ordem de produção
    const formValue = this.form.getRawValue();
    const modo = formValue.modoCalculo;

    const payload: any = {
      produtoId: Number(formValue.produtoId),
      lotePrincipalId: Number(formValue.loteId),
      quantidadeProduzida: Number(formValue.quantidade),
      modoCalculo: modo,
      larguraFinalCm: Number(formValue.larguraFinalCm),
      comprimentoFinalCm: Number(formValue.comprimentoFinalCm),
      canalVendaId: formValue.canalVendaId ? Number(formValue.canalVendaId) : null, // Renomeado
      motivo: formValue.motivo || null
    };

    // Margens apenas no modo AUTOMATICO
    if (modo === 'AUTOMATICO') {
      payload.margens = {
        superior: formValue.margens.superior ? Number(formValue.margens.superior) : 0,
        inferior: formValue.margens.inferior ? Number(formValue.margens.inferior) : 0,
        esquerda: formValue.margens.esquerda ? Number(formValue.margens.esquerda) : 0,
        direita: formValue.margens.direita ? Number(formValue.margens.direita) : 0
      };
    }

    console.log('Payload final para criação:', payload);

    // TODO: Chamar ProductionService.createCutOrder(payload)
    // Por enquanto, apenas simular sucesso
    setTimeout(() => {
      this.isSaving.set(false);
      this.dialogRef.close(true);
      this.cdr.markForCheck();
    }, 500);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
