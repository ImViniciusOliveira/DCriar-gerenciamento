import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject, signal, Signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Product } from '../../models/product.model';
import { FormArray, FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../services/product';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule, MatSelectChange } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { lastValueFrom } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { provideNgxMask } from 'ngx-mask';
import { MateriaPrimaSearchComponent } from '../../../../shared/components/materia-prima-search/materia-prima-search';
import { TipoMateriaPrima } from '../../../stock/models/material-type.model';

/**
 * Componente de formulário para criação e edição de produtos.
 *
 * Gerencia a lógica complexa de dois tipos de produtos:
 * - **CORTE**: Requer dimensões (largura/comprimento) e cor.
 * - **CONSUMO_DIRETO**: Requer especificações técnicas dinâmicas e código de fabricante.
 *
 * Utiliza Signals para gerenciamento de estado e ChangeDetection.OnPush para performance.
 */
@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressSpinnerModule, MateriaPrimaSearchComponent],
  providers: [provideNgxMask()],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductFormComponent implements OnInit {
  // --- Injeções de Dependência ---
  private readonly productService = inject(ProductService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  public readonly dialogRef = inject(MatDialogRef<ProductFormComponent>);
  public readonly data: ProductFormData = inject(MAT_DIALOG_DATA);

  // --- Estado Reativo (Signals) ---
  readonly product = signal<Product>(this.data.product);
  readonly isEditMode = signal<boolean>(this.data.isEditMode);

  /**
   * URL segura para exibição da imagem.
   * Computada automaticamente: prioriza o preview local (upload pendente),
   * senão usa a URL da foto existente no produto.
   */
  readonly safeImageSrc: Signal<string | null>;

  selectedFile = signal<File | null>(null);
  previewUrl = signal<string | null>(null);
  isUploading = signal(false);

  // --- Formulário ---
  productForm: FormGroup;

  /**
   * Armazena as especificações originais para comparação durante a edição.
   * Usado para determinar quais especificações foram removidas ou alteradas.
   */
  private initialSpecifications = signal<{ [key: string]: string }>({});

  // --- Constantes de Texto ---
  private static readonly Texts = {
    CONFIRM_CHANGE_TITLE: 'Confirmar Alteração',
    CONFIRM_CHANGE_MESSAGE: (original: string, novo: string) => `Deseja realmente alterar a matéria-prima de "${original}" para "${novo}"?`,
    CONFIRM_DELETE_SPEC_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_SPEC_MESSAGE: (key: string) => `Deseja realmente remover a característica "${key}"?`
  };

  constructor() {
    this.safeImageSrc = computed(() => this.previewUrl() ?? this.product()?.fotoPrincipalUrl ?? null);

    const currentProduct = this.product();

    // Inicialização do formulário com validadores padrão
    this.productForm = this.fb.group({
      tipoProduto: [currentProduct.tipoProduto || 'CORTE', Validators.required],
      nome: [currentProduct.nome, Validators.required],
      sku: [currentProduct.sku, Validators.required],
      descricao: [currentProduct.descricao],
      unidadesPorProduto: [currentProduct.unidadesPorProduto, [Validators.required, Validators.min(1)]],
      ativo: [currentProduct.ativo],
      materiaPrima: [currentProduct.materiaPrima, Validators.required],
      cor: [currentProduct.cor],
      dimensoes: this.fb.group({
        larguraCm: [currentProduct.dimensoes?.larguraCm, [Validators.required, Validators.min(0.1)]],
        comprimentoCm: [currentProduct.dimensoes?.comprimentoCm, [Validators.required, Validators.min(0.1)]]
      }),
      codigoFabricante: [currentProduct.codigoFabricante],
      especificacoes: this.fb.array([])
    });

    // Configura o estado inicial dos campos baseados no tipo
    this.setupFormControlsBasedOnProductType(currentProduct.tipoProduto || 'CORTE', false);

    // Reage dinamicamente à mudança do tipo de produto
    this.productForm.get('tipoProduto')?.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(type => {
        this.setupFormControlsBasedOnProductType(type, true);
      });
  }

  ngOnInit(): void {
    if (this.data.isCreationMode) {
      return;
    }

    // Em modo de edição, buscamos os detalhes completos (incluindo especificações e links)
    const selfUrl = this.product()?._links?.['self']?.href;
    if (selfUrl) {
      lastValueFrom(this.productService.getProductByUrl(selfUrl))
        .then(fullProduct => {
          if (fullProduct) {
            this.product.set(fullProduct);

            if (fullProduct.materiaPrima) {
              this.productForm.get('materiaPrima')?.patchValue(fullProduct.materiaPrima);
            }

            // Popula o FormArray de especificações dinâmicas
            this.especificacoes.clear();
            const specs = fullProduct.especificacoes;
            if (specs) {
              this.initialSpecifications.set({ ...specs });
              Object.entries(specs).forEach(([chave, valor]) => {
                this.especificacoes.push(this.fb.group({
                  chave: [chave, Validators.required],
                  valor: [valor, Validators.required],
                  isNew: [false] // Marca como existente para controle de remoção
                }));
              });
            }
            // Força verificação de mudanças pois a atualização é assíncrona
            this.cdr.markForCheck();
          }
        })
        .catch(err => console.error("Falha ao buscar detalhes completos do produto:", err));
    }
  }

  // --- Getters Auxiliares ---

  get especificacoes(): FormArray {
    return this.productForm.get('especificacoes') as FormArray;
  }

  get especificacoesControls(): FormGroup[] {
    return (this.productForm.get('especificacoes') as FormArray).controls as FormGroup[];
  }

  get materiaPrimaControl(): FormControl {
    return this.productForm.get('materiaPrima') as FormControl;
  }

  // --- Manipulação do Formulário ---

  /**
   * Adiciona uma nova linha de especificação técnica ao formulário.
   * Faz o scroll automático para o final da lista para melhor UX.
   */
  addEspecificacao(): void {
    this.especificacoes.push(this.fb.group({
      chave: ['', Validators.required],
      valor: ['', Validators.required],
      isNew: [true]
    }));

    setTimeout(() => {
      const dialogContent = (this.dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
      if (dialogContent) {
        dialogContent.scrollTop = dialogContent.scrollHeight;
      }
    }, 100);
  }

  /**
   * Remove uma especificação da lista.
   *
   * - Se for um item novo (ainda não salvo), remove imediatamente.
   * - Se for um item existente, solicita confirmação do usuário para evitar perda acidental de dados.
   *
   * @param index Índice do item no FormArray
   */
  async removeEspecificacao(index: number): Promise<void> {
    const specGroup = this.especificacoes.at(index);
    const isNew = specGroup.get('isNew')?.value;

    if (isNew) {
      this.especificacoes.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      return;
    }

    const key = specGroup.get('chave')?.value;
    const dialogData: ConfirmDialogData = {
      title: ProductFormComponent.Texts.CONFIRM_DELETE_SPEC_TITLE,
      message: ProductFormComponent.Texts.CONFIRM_DELETE_SPEC_MESSAGE(key || 'esta característica')
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      this.especificacoes.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      this.cdr.markForCheck();
    }
  }

  /**
   * Ajusta a validação e o estado (habilitado/desabilitado) dos campos
   * com base no tipo de produto selecionado.
   *
   * - **CORTE**: Habilita 'cor' e 'dimensoes'. Desabilita 'codigoFabricante' e 'especificacoes'.
   * - **CONSUMO_DIRETO**: Inverso do acima.
   *
   * @param type Tipo do produto selecionado
   * @param resetOppositeControls Se true, limpa os valores dos campos desabilitados (útil na criação)
   */
  private setupFormControlsBasedOnProductType(type: 'CORTE' | 'CONSUMO_DIRETO', resetOppositeControls: boolean): void {
    const corteControls = ['cor', 'dimensoes'];
    const consumoControls = ['codigoFabricante', 'especificacoes'];
    const corControl = this.productForm.get('cor');

    if (type === 'CORTE') {
      corteControls.forEach(name => {
        this.productForm.get(name)?.enable();
        if (name === 'dimensoes') {
          this.productForm.get('dimensoes.larguraCm')?.setValidators([Validators.required, Validators.min(0.1)]);
          this.productForm.get('dimensoes.comprimentoCm')?.setValidators([Validators.required, Validators.min(0.1)]);
        }
      });
      corControl?.setValidators(Validators.required);

      consumoControls.forEach(name => {
        const control = this.productForm.get(name);
        control?.disable();
        if (resetOppositeControls) {
          if (name === 'especificacoes') {
            this.especificacoes.clear();
          } else {
            control?.reset();
          }
        }
      });
    } else { // CONSUMO_DIRETO
      consumoControls.forEach(name => this.productForm.get(name)?.enable());
      corControl?.clearValidators();

      corteControls.forEach(name => {
        const control = this.productForm.get(name);
        control?.disable();
        if (resetOppositeControls) {
          control?.reset();
        }
        if (name === 'dimensoes') {
          this.productForm.get('dimensoes.larguraCm')?.clearValidators();
          this.productForm.get('dimensoes.comprimentoCm')?.clearValidators();
        }
      });
    }
    // Atualiza a validação para refletir as mudanças
    corControl?.updateValueAndValidity();
    this.productForm.get('dimensoes.larguraCm')?.updateValueAndValidity();
    this.productForm.get('dimensoes.comprimentoCm')?.updateValueAndValidity();
  }

  // --- Submissão ---

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      return;
    }

    this.isUploading.set(true);
    try {
      if (this.isEditMode()) {
        await this.handleEditSubmit();
      } else {
        await this.handleCreateSubmit();
      }
    } catch (error) {
      console.error('Falha no envio do formulário:', error);
      this.dialogRef.close(false);
    } finally {
      this.isUploading.set(false);
    }
  }

  /**
   * Prepara o objeto de valores do formulário, convertendo o array de especificações
   * de volta para um mapa (objeto) chave-valor esperado pelo backend.
   */
  private getProcessedFormValue(): any {
    const formValue = this.productForm.getRawValue();
    const especificacoesMap: { [key: string]: string } = {};
    (formValue.especificacoes || []).forEach((spec: { chave: string; valor: string }) => {
      if (spec.chave) {
        especificacoesMap[spec.chave] = spec.valor;
      }
    });
    formValue.especificacoes = especificacoesMap;
    return formValue;
  }

  /**
   * Lógica de atualização (PATCH).
   * Envia apenas os campos que foram alterados (dirty) para economizar banda e evitar conflitos.
   * Trata especificamente o mapa de especificações para enviar null nas chaves removidas.
   */
  private async handleEditSubmit(): Promise<void> {
    const hasImageChanged = !!this.selectedFile();
    const dirtyValues = this.getDirtyValues();
    const hasFormChanged = Object.keys(dirtyValues).length > 0;

    if (!hasImageChanged && !hasFormChanged) {
      this.dialogRef.close(false); // Nenhuma mudança, fecha sem atualizar
      return;
    }

    // Se houver imagem e formulário, o upload pula o refresh, que será feito pelo patch.
    if (hasImageChanged) {
      const updated = await this.uploadImage(this.product(), hasFormChanged);
      if (updated) {
        this.product.set(updated);
      }
    }

    if (hasFormChanged) {
      if (!this.product()?.id) {
        console.error('ID do produto não encontrado, não é possível atualizar.', this.product());
        return;
      }
      // O patch sempre faz o refresh final.
      await lastValueFrom(this.productService.patchProduct(this.product().id, dirtyValues));
    }

    this.dialogRef.close(true); // Indica que houve mudança
  }

  /**
   * Extrai apenas os campos "sujos" (dirty) do formulário para o payload do PATCH.
   * Inclui a lógica especial para o mapa de especificações.
   */
  private getDirtyValues(): { [key: string]: any } {
    const dirtyValues: { [key: string]: any } = {};
    let especificacoesIsDirty = false;

    Object.keys(this.productForm.controls).forEach(key => {
      const control = this.productForm.get(key);
      if (control && control.dirty) {
        if (key === 'especificacoes') {
          especificacoesIsDirty = true;
        } else {
          dirtyValues[key] = control.value;
        }
      }
    });

    if (especificacoesIsDirty) {
      const currentSpecs: { [key: string]: string } = {};
      (this.productForm.get('especificacoes')?.value || []).forEach((spec: { chave: string; valor: string }) => {
        if (spec.chave) {
          currentSpecs[spec.chave] = spec.valor;
        }
      });

      const specsPayload: { [key: string]: string | null } = { ...currentSpecs };
      Object.keys(this.initialSpecifications()).forEach(initialKey => {
        if (!currentSpecs.hasOwnProperty(initialKey)) {
          specsPayload[initialKey] = null;
        }
      });
      dirtyValues['especificacoes'] = specsPayload;
    }
    return dirtyValues;
  }


  private async handleCreateSubmit(): Promise<void> {
    const formValue = this.getProcessedFormValue();
    // Na criação, o primeiro post pula o refresh, pois o upload (se houver) o fará.
    const hasImageToUpload = !!this.selectedFile();
    const newProduct = await lastValueFrom(this.productService.createProduct(formValue as Partial<Product>, hasImageToUpload));

    if (hasImageToUpload && newProduct) {
      // O upload é a última operação, então ele dispara o refresh.
      await this.uploadImage(newProduct, false);
    }

    this.dialogRef.close(true);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.selectedFile.set(file);
      this.previewUrl.set(URL.createObjectURL(file));
    }
  }

  private async uploadImage(product: Product, skipRefresh: boolean): Promise<Product | null> {
    const uploadUrl = product?._links?.['upload-foto']?.href;
    const file = this.selectedFile();
    if (!file || !uploadUrl) {
      return product;
    }

    const updatedProduct = await lastValueFrom(
      this.productService.uploadProductPhoto(uploadUrl, file, skipRefresh)
    );
    if (updatedProduct) {
      this.previewUrl.set(updatedProduct.fotoPrincipalUrl);
      this.product.set({ ...product, ...updatedProduct });
    }
    this.selectedFile.set(null);
    return updatedProduct;
  }

  getFormattedDimensions(dimensions: { larguraCm?: number; comprimentoCm?: number } | null | undefined): string {
    if (dimensions && typeof dimensions.larguraCm === 'number' && typeof dimensions.comprimentoCm === 'number') {
      return `${dimensions.larguraCm} x ${dimensions.comprimentoCm} cm`;
    }
    return 'N/A';
  }

  /**
   * Verifica se o usuário realmente deseja trocar a matéria-prima,
   * pois isso pode impactar custos e estoque.
   */
  async onMaterialTypeChange(event: MatSelectChange): Promise<void> {
    const newSelection = event.value as TipoMateriaPrima;
    const originalSelection = this.product().materiaPrima;

    if (!originalSelection || !newSelection || originalSelection.id === newSelection.id) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: ProductFormComponent.Texts.CONFIRM_CHANGE_TITLE,
      message: ProductFormComponent.Texts.CONFIRM_CHANGE_MESSAGE(originalSelection.nome, newSelection.nome)
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (!confirmed) {
      this.productForm.get('materiaPrima')?.setValue(originalSelection);
      this.cdr.markForCheck();
    }
  }
}

export interface ProductFormData {
  product: Product;
  isEditMode: boolean;
  isCreationMode?: boolean;
  title: string;
}
