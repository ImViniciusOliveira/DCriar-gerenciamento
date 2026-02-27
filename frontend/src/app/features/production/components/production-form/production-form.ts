import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { take } from 'rxjs';

import { ProductionOrder } from '../../models/production.model';
import { Product } from '../../../products/models/product.model';
import { ProductStockSearch } from '../../../../shared/components/product-stock-search/product-stock-search';
import { ProductionService, SimulationRequest } from '../../services/production.service';

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
    MatIconModule
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
  public readonly data: ProductionFormData = inject(MAT_DIALOG_DATA);

  @ViewChild(ProductStockSearch) private productStockSearchComponent!: ProductStockSearch;

  form: FormGroup;
  isSaving = signal(false);
  isSimulating = signal(false);
  produto = signal<Product | null>(null);

  constructor() {
    this.form = this.fb.group({
      // O tipo de produção não é mais obrigatório, pois é sincronizado automaticamente.
      tipoProducao: [''],
      // O produto passa a ser obrigatório.
      produtoId: [null, Validators.required],
      quantidade: [null, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    // A lógica de reação a eventos foi movida para métodos específicos.
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
   * Callback quando um produto é selecionado no componente de busca.
   * Atualiza o formulário com os dados do produto selecionado.
   */
  onProdutoChange(event: MatSelectChange): void {
    const produto = event.value as Product;
    this.produto.set(produto);

    // Atualiza o formulário com o ID e também sincroniza o dropdown de tipo de produção.
    this.form.patchValue({
      produtoId: produto.id,
      tipoProducao: produto.tipoProduto
    });

    // Força a detecção de mudanças para garantir que o mat-select-trigger seja atualizado.
    this.cdr.markForCheck();
  }

  /**
   * Executa a simulação de produção.
   */
  onSimulate(): void {
    if (!this.produto() || !this.form.value.quantidade) {
      console.warn('Produto e quantidade são necessários para simular.');
      return;
    }

    const url = this.produto()?._links?.["simulate"]?.href;
    if (!url) {
      console.error('Link de simulação (HATEOAS) não encontrado no objeto do produto.');
      return;
    }

    const payload: SimulationRequest = {
      produtoId: this.produto()!.id,
      quantidade: Number(this.form.value.quantidade)
    };

    this.isSimulating.set(true);
    this.productionService.simulateProduction(url, payload)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          console.log('✅ Resposta da Simulação:', response);
          this.isSimulating.set(false);
          // TODO: Exibir os resultados em um diálogo ou em uma nova seção da UI.
        },
        error: (err) => {
          console.error('Erro ao simular produção:', err);
          this.isSimulating.set(false);
          // TODO: Mostrar uma notificação de erro para o usuário.
        }
      });
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.getRawValue();
    const payload = {
      produtoId: formValue.produtoId,
      quantidade: Number(formValue.quantidade)
    };

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
