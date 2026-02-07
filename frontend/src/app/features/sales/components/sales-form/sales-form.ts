import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators, ValidationErrors, AbstractControl } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Sale, SaleRequest } from '../../models/sales.model';
import { SalesService } from '../../services/sales.service';
import { ChannelService } from '../../../stock/services/channel.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { ProductSearch } from '../../../../shared/components/product-search/product-search';
import { Product } from '../../../products/models/product.model';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';

export interface SalesFormData {
  template?: Sale;
  title: string;
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
    MatProgressSpinnerModule,
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
  private readonly entityDialog = inject(EntityDialogService);
  public readonly data: SalesFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isSaving = signal(false);
  matcher = new InstantErrorStateMatcher();

  // Carrega os canais reais da API usando o novo serviço
  channels = toSignal(this.channelService.getAllChannels(), { initialValue: [] });

  private static readonly Texts = {
    SAVE_SUCCESS: 'Venda registrada com sucesso!',
    SAVE_ERROR: 'Falha ao registrar a venda. Verifique os dados e tente novamente.',
    LOAD_ERROR: 'Não foi possível carregar os dados iniciais.'
  };

  constructor() {
    this.form = this.fb.group({
      canalVendaId: [null, Validators.required],
      itens: this.fb.array([], stockAvailabilityValidator)
    });

    // Escuta mudanças no canal de venda para limpar os itens
    this.form.get('canalVendaId')?.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.items.clear();
        if (this.form.get('canalVendaId')?.valid) {
          this.addItem();
        }
      });
  }

  ngOnInit(): void {
  }

  get items(): FormArray {
    return this.form.get('itens') as FormArray;
  }

  get itemsControls(): FormGroup[] {
    return this.items.controls as FormGroup[];
  }

  /**
   * Adiciona uma nova linha de item ao formulário.
   */
  addItem(): void {
    const itemGroup = this.fb.group({
      produtoId: [null, Validators.required],
      produtoNome: ['', Validators.required],
      estoqueDisponivel: [null],
      quantidade: [1, [Validators.required, Validators.min(1)]]
    });

    this.items.push(itemGroup);
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
      itemGroup.patchValue({
        produtoId: product.id,
        produtoNome: product,
        estoqueDisponivel: product.estoqueDisponivel
      });
      // Dispara validação do FormArray após selecionar produto
      this.items.updateValueAndValidity();
    }
  }

  /**
   * Helper para obter o FormControl de nome do produto de um item específico.
   * Necessário para passar para o componente ProductSearch.
   */
  getProductControl(index: number): FormControl {
    return this.items.at(index).get('produtoNome') as FormControl;
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
        quantidade: item.quantidade
      }))
    };

    this.salesService.create(request).subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(SalesForm.Texts.SAVE_SUCCESS);
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Erro ao salvar venda:', err);
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
