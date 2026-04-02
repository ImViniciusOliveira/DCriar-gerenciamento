import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, ReactiveFormsModule, Validators, ValidationErrors, AbstractControl, ValidatorFn } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { ErrorStateMatcher } from '@angular/material/core';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Sale, SaleRequest } from '../../models/sales.model';
import { SalesService } from '../../services/sales.service';
import { ChannelService } from '../../../stock/services/channel.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { ProductSearch } from '../../../../shared/components/product-search/product-search';
import { Product } from '../../../products/models/product.model';
import { ProductService } from '../../../products/services/product.service';

type SalePriceType = 'PRECO_PADRAO' | 'PRECO_ALTERADO' | 'DESCONTO_TOTAL';

/**
 * Validador que verifica se a parte inteira de um número excede um máximo de dígitos.
 * Usado para validar campos numéricos como quantidade.
 */
export function maxIntegerDigits(maxDigits: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    const value = String(control.value);
    const integerPart = value.split('.')[0].replace(/^-/, '');

    if (integerPart.length > maxDigits) {
      return { maxIntegerDigits: { requiredDigits: maxDigits, actualDigits: integerPart.length } };
    }
    return null;
  };
}

/**
 * Define quando os erros de um campo de formulário devem ser exibidos.
 * A regra é: mostrar o erro se o campo for inválido E (o usuário já digitou nele OU já saiu dele).
 * Permite que a validação apareça imediatamente ao digitar (dirty).
 */
export class ImmediateErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}

export interface SalesFormData {
  template?: Sale;
  title: string;
  isViewMode?: boolean;
}

/**
 * Validador global para verificar se a soma das quantidades de um mesmo produto excede o estoque disponível.
 */
function stockAvailabilityValidator(formArray: AbstractControl): ValidationErrors | null {
  if (!(formArray instanceof FormArray)) return null;

  const productQuantities = new Map<number, number>();
  const productStocks = new Map<number, number>();
  let hasError = false;

  // Passo 1: Calcular totais por produto
  formArray.controls.forEach((control) => {
    const group = control as FormGroup;
    const productId = group.get('produtoId')?.value;
    const quantity = group.get('quantidade')?.value || 0;
    const stock = group.get('estoqueDisponivel')?.value;

    if (productId) {
      const currentTotal = (productQuantities.get(productId) || 0) + quantity;
      productQuantities.set(productId, currentTotal);

      if (stock !== undefined && stock !== null) {
        productStocks.set(productId, stock);
      }
    }
  });

  // Passo 2: Validar e marcar erros nos controles individuais
  formArray.controls.forEach((control) => {
    const group = control as FormGroup;
    const productId = group.get('produtoId')?.value;
    const qtdControl = group.get('quantidade');

    if (productId && qtdControl) {
      const total = productQuantities.get(productId) || 0;
      const stock = productStocks.get(productId);

      if (stock !== undefined && total > stock) {
        const currentErrors = qtdControl.errors || {};
        qtdControl.setErrors({ ...currentErrors, stockExceeded: { total, stock } });
        hasError = true;
      } else {
        if (qtdControl.hasError('stockExceeded')) {
           const { stockExceeded, ...otherErrors } = qtdControl.errors || {};
           qtdControl.setErrors(Object.keys(otherErrors).length ? otherErrors : null);
        }
      }
    }
  });

  return hasError ? { stockExceeded: true } : null;
}

@Component({
  selector: 'app-sales-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatAutocompleteModule,
    ProductSearch
  ],
  templateUrl: './sales-form.html',
  styleUrls: ['./sales-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SalesForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<SalesForm>);
  private readonly salesService = inject(SalesService);
  private readonly channelService = inject(ChannelService);
  private readonly productService = inject(ProductService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  public readonly data: SalesFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isSaving = signal(false);
  isEditMode = signal(false);
  isViewMode = signal(false);
  matcher = new ImmediateErrorStateMatcher();

  // Mapa público para ser acessado pelo template
  public originalQuantities = new Map<number, number>();

  // Carrega os canais reais da API usando o novo serviço
  channels = toSignal(this.channelService.getAllChannels(), { initialValue: [] });

  private static readonly Texts = {
    SAVE_SUCCESS: 'Venda registrada com sucesso!',
    UPDATE_SUCCESS: 'Venda atualizada com sucesso!',
    SAVE_ERROR: 'Falha ao registrar a venda. Verifique os dados e tente novamente.',
    LOAD_ERROR: 'Não foi possível carregar os dados iniciais.'
  };

  constructor() {
    this.isEditMode.set(!!this.data.template?.id && !this.data.isViewMode);
    this.isViewMode.set(!!this.data.isViewMode);

    // Não cria o formulário em modo de visualização
    if (this.isViewMode()) {
      this.form = this.fb.group({});
      return;
    }

    this.form = this.fb.group({
      canalVendaId: [null, Validators.required],
      itens: this.fb.array([], stockAvailabilityValidator)
    });

    // Escuta mudanças no canal de venda para limpar os itens
    this.form.get('canalVendaId')?.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.form.get('canalVendaId')?.dirty) {
           this.items.clear();
           if (this.form.get('canalVendaId')?.valid) {
             this.addItem();
           }
        } else if (!this.isEditMode() && this.items.length === 0 && this.form.get('canalVendaId')?.value) {
             this.addItem();
        }
      });
  }

  ngOnInit(): void {
    if (this.isEditMode() && this.data.template) {
      this.initializeForm(this.data.template);
    }
  }

  private async initializeForm(sale: Sale): Promise<void> {
    this.form.patchValue({
      canalVendaId: sale.canalVendaId
    }, { emitEvent: false });

    // Preenche os itens e popula o mapa de quantidades originais
    if (sale.itens && sale.itens.length > 0) {
      for (const item of sale.itens) {
        // Armazena a quantidade original deste produto
        const currentOriginal = this.originalQuantities.get(item.produtoId) || 0;
        this.originalQuantities.set(item.produtoId, currentOriginal + item.quantidade);

        const itemGroup = this.createItemControl();

        itemGroup.patchValue({
          produtoId: item.produtoId,
          produtoNome: { id: item.produtoId, nome: item.nomeProduto, sku: item.produtoSku } as any,
          quantidade: item.quantidade,
          precoComercialOriginal: item.precoComercialOriginal,
          precoAplicado: item.precoUnitario,
          precoTotal: item.precoTotal,
          tipoPrecoAplicado: item.tipoPrecoAplicado,
          motivoAlteracaoPreco: item.motivoAlteracaoPreco ?? ''
        }, { emitEvent: false });

        // Desabilita a troca de produto para itens existentes
        itemGroup.get('produtoNome')?.disable();

        this.items.push(itemGroup);

        this.loadStockForItem(item.produtoId, sale.canalVendaId, itemGroup);
      }
    }
  }

  private loadStockForItem(productId: number, channelId: number, group: FormGroup): void {
     const sku = group.get('produtoNome')?.value?.sku;
     if (sku) {
        // Busca com includeZeroStock=true para garantir que encontre o produto
        this.productService.searchProducts(sku, channelId, true).subscribe(products => {
           const match = products.find(p => p.id === productId);
           if (match && match.estoqueDisponivel !== undefined) {
              // Aqui somamos manualmente porque estamos pegando o dado cru do serviço
              const originalQty = this.originalQuantities.get(productId) || 0;
              const adjustedStock = match.estoqueDisponivel + originalQty;

              group.patchValue({ estoqueDisponivel: adjustedStock });
              this.items.updateValueAndValidity();
              this.cdr.markForCheck();
           }
        });
     }
  }

  get items(): FormArray {
    return this.form.get('itens') as FormArray;
  }

  get itemsControls(): FormGroup[] {
    return this.items.controls as FormGroup[];
  }

  /**
   * Cria o FormGroup de um item.
   */
  private createItemControl(): FormGroup {
    const group = this.fb.group({
      produtoId: [null, Validators.required],
      produtoNome: ['', Validators.required],
      estoqueDisponivel: [null],
      precoComercialOriginal: [null],
      precoAplicado: [null, [Validators.required, Validators.min(0), Validators.pattern(/^\d+([.,]\d{1,4})?$/)]],
      precoTotal: [null, [Validators.required, Validators.min(0), Validators.pattern(/^\d+([.,]\d{1,2})?$/)]],
      tipoPrecoAplicado: ['PRECO_PADRAO' as SalePriceType, Validators.required],
      motivoAlteracaoPreco: [''],
      quantidade: [1, [
        Validators.required,
        Validators.min(1),
        maxIntegerDigits(15),
        Validators.pattern(/^-?\d*([.,]\d+)?$/)
      ]]
    });

    group.get('quantidade')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recalculateItemPricing(group, 'quantidade'));

    group.get('motivoAlteracaoPreco')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.syncMotivoRequirement(group));

    return group;
  }

  /**
   * Adiciona uma nova linha de item ao formulário.
   */
  addItem(): void {
    this.items.push(this.createItemControl());
  }

  /**
   * Remove um item da lista.
   */
  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  /**
   * Callback chamado quando um produto é selecionado no Autocomplete.
   * Preenche os campos ocultos e visuais do item.
   */
  onProductSelected(product: Product, index: number): void {
    const itemGroup = this.items.at(index);
    if (itemGroup) {
      // O produto vindo do ProductSearch já tem o estoque ajustado (Físico + Original)
      // graças à lógica de 'stockAdjustments' no componente filho.
      // Portanto, usamos o valor diretamente.
      itemGroup.patchValue({
        produtoId: product.id,
        produtoNome: product,
        estoqueDisponivel: product.estoqueDisponivel,
        precoComercialOriginal: product.precoComercial ?? null
      });
      this.recalculateItemPricing(itemGroup as FormGroup, 'produto');
      this.items.updateValueAndValidity();
    }
  }

  onPrecoAplicadoChanged(index: number): void {
    this.recalculateItemPricing(this.items.at(index) as FormGroup, 'precoAplicado');
  }

  onPrecoTotalChanged(index: number): void {
    this.recalculateItemPricing(this.items.at(index) as FormGroup, 'precoTotal');
  }

  onTipoPrecoChanged(index: number): void {
    this.recalculateItemPricing(this.items.at(index) as FormGroup, 'tipo');
  }

  private recalculateItemPricing(group: FormGroup, source: 'produto' | 'quantidade' | 'precoAplicado' | 'precoTotal' | 'tipo'): void {
    const quantidade = Number(group.get('quantidade')?.value || 0);
    const precoComercialOriginal = this.toMoneyNumber(group.get('precoComercialOriginal')?.value);
    const precoAplicadoAtual = this.toMoneyNumber(group.get('precoAplicado')?.value);
    const precoTotalAtual = this.toMoneyNumber(group.get('precoTotal')?.value);
    const tipoAtual = (group.get('tipoPrecoAplicado')?.value || 'PRECO_PADRAO') as SalePriceType;

    if (!quantidade || !precoComercialOriginal) {
      return;
    }

    const totalPadrao = this.roundMoney(precoComercialOriginal * quantidade);

    if (source === 'produto') {
      group.patchValue({
        precoAplicado: precoComercialOriginal,
        precoTotal: totalPadrao,
        tipoPrecoAplicado: 'PRECO_PADRAO',
        motivoAlteracaoPreco: ''
      }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    if (source === 'precoAplicado') {
      const precoAplicado = this.roundUnitPrice(precoAplicadoAtual);
      const total = this.roundMoney(precoAplicado * quantidade);
      const isPadrao = precoAplicado === precoComercialOriginal;
      group.patchValue({
        precoAplicado,
        precoTotal: total,
        tipoPrecoAplicado: isPadrao ? 'PRECO_PADRAO' : 'PRECO_ALTERADO',
        motivoAlteracaoPreco: isPadrao ? '' : group.get('motivoAlteracaoPreco')?.value
      }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    if (source === 'precoTotal') {
      const total = this.roundMoney(precoTotalAtual);
      const precoAplicado = this.roundUnitPrice(total / quantidade);

      const isPadrao = precoAplicado === precoComercialOriginal && total === totalPadrao;
      group.patchValue({
        precoAplicado,
        precoTotal: total,
        tipoPrecoAplicado: isPadrao ? 'PRECO_PADRAO' : 'DESCONTO_TOTAL',
        motivoAlteracaoPreco: isPadrao ? '' : group.get('motivoAlteracaoPreco')?.value
      }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    if (source === 'tipo') {
      if (tipoAtual === 'DESCONTO_TOTAL') {
        const total = precoTotalAtual || totalPadrao;
        const precoAplicado = this.roundUnitPrice(total / quantidade);
        group.patchValue({
          precoAplicado,
          precoTotal: this.roundMoney(total),
          motivoAlteracaoPreco: group.get('motivoAlteracaoPreco')?.value
        }, { emitEvent: false });
        this.syncMotivoRequirement(group);
        return;
      }

      if (tipoAtual === 'PRECO_ALTERADO') {
        const precoAplicado = precoAplicadoAtual || precoComercialOriginal;
        group.patchValue({
          precoAplicado: this.roundUnitPrice(precoAplicado),
          precoTotal: this.roundMoney(precoAplicado * quantidade),
          motivoAlteracaoPreco: group.get('motivoAlteracaoPreco')?.value
        }, { emitEvent: false });
        this.syncMotivoRequirement(group);
        return;
      }

      group.patchValue({
        precoAplicado: precoComercialOriginal,
        precoTotal: totalPadrao,
        motivoAlteracaoPreco: ''
      }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    if (tipoAtual === 'DESCONTO_TOTAL') {
      const total = this.roundMoney(precoTotalAtual);
      const precoAplicado = this.roundUnitPrice(total / quantidade);
      group.patchValue({ precoAplicado, precoTotal: total }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    if (tipoAtual === 'PRECO_ALTERADO') {
      const precoAplicado = this.roundUnitPrice(precoAplicadoAtual);
      group.patchValue({
        precoAplicado,
        precoTotal: this.roundMoney(precoAplicado * quantidade)
      }, { emitEvent: false });
      this.syncMotivoRequirement(group);
      return;
    }

    group.patchValue({
      precoAplicado: precoComercialOriginal,
      precoTotal: totalPadrao,
      tipoPrecoAplicado: 'PRECO_PADRAO',
      motivoAlteracaoPreco: ''
    }, { emitEvent: false });
    this.syncMotivoRequirement(group);
  }

  private roundMoney(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  private roundUnitPrice(value: number): number {
    return Math.round((value + Number.EPSILON) * 10000) / 10000;
  }

  private toMoneyNumber(value: unknown): number {
    const normalized = String(value ?? '0').replace(',', '.');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private syncMotivoRequirement(group: FormGroup): void {
    const motivoControl = group.get('motivoAlteracaoPreco');
    const tipoPrecoAplicado = group.get('tipoPrecoAplicado')?.value as SalePriceType;
    if (!motivoControl) {
      return;
    }

    if (tipoPrecoAplicado === 'PRECO_PADRAO') {
      motivoControl.setErrors(null);
      return;
    }

    const motivo = String(motivoControl.value ?? '').trim();
    if (!motivo) {
      motivoControl.setErrors({ ...(motivoControl.errors || {}), required: true });
      return;
    }

    if (motivoControl.hasError('required')) {
      const { required, ...otherErrors } = motivoControl.errors || {};
      motivoControl.setErrors(Object.keys(otherErrors).length ? otherErrors : null);
    }
  }

  /**
   * Helper para obter o FormControl de nome do produto de um item específico.
   * Necessário para passar para o componente ProductSearch.
   */
  getProductControl(index: number): FormControl {
    return this.items.at(index).get('produtoNome') as FormControl;
  }

  getPriceTypeLabel(value: SalePriceType): string {
    switch (value) {
      case 'PRECO_PADRAO':
        return 'Preço padrão';
      case 'PRECO_ALTERADO':
        return 'Preço aplicado';
      case 'DESCONTO_TOTAL':
        return 'Desconto no total';
      default:
        return value;
    }
  }

  isPrecoAplicadoLocked(itemGroup: FormGroup): boolean {
    return itemGroup.get('tipoPrecoAplicado')?.value !== 'PRECO_ALTERADO';
  }

  isPrecoTotalLocked(itemGroup: FormGroup): boolean {
    return itemGroup.get('tipoPrecoAplicado')?.value !== 'DESCONTO_TOTAL';
  }

  isMotivoRequired(itemGroup: FormGroup): boolean {
    return itemGroup.get('tipoPrecoAplicado')?.value !== 'PRECO_PADRAO';
  }

  /**
   * Retorna o nome do canal de venda em modo de visualização.
   */
  getChannelName(): string {
    const channelId = this.data.template?.canalVendaId;
    const channel = this.channels().find(c => c.id === channelId);
    return channel?.nome || 'N/A';
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();

    const request: SaleRequest = {
      canalVendaId: formValue.canalVendaId,
      itens: formValue.itens.map((item: any) => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade,
        precoAplicado: this.roundUnitPrice(Number(item.precoAplicado)),
        precoTotal: this.roundMoney(Number(item.precoTotal)),
        tipoPrecoAplicado: item.tipoPrecoAplicado,
        motivoAlteracaoPreco: item.motivoAlteracaoPreco?.trim() || null
      }))
    };

    const operation = this.isEditMode() && this.data.template?._links?.['update']
      ? this.salesService.update(this.data.template._links['update'].href, request)
      : this.salesService.create(request);

    operation.subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(
          this.isEditMode() ? SalesForm.Texts.UPDATE_SUCCESS : SalesForm.Texts.SAVE_SUCCESS
        );
        this.dialogRef.close(true);
      },
      error: (err) => {
        const errorMsg = err.error?.detail || SalesForm.Texts.SAVE_ERROR;
        this.entityDialog.showErrorSnackbar(errorMsg);
        this.isSaving.set(false);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
