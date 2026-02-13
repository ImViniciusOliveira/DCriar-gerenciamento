import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject, signal, Signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Product } from '../../models/product.model';
import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, FormsModule, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ProductService } from '../../services/product.service';
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
import { MaterialTypeSearch } from '../../../../shared/components/material-type-search/material-type-search';
import { MaterialType } from '../../../stock/models/material-type.model';
import { ErrorStateMatcher } from '@angular/material/core';

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

export class ImmediateErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}

/**
 * Formulário para criação e edição de Produtos.
 * Gerencia a lógica complexa para os tipos 'CORTE' e 'CONSUMO_DIRETO',
 * incluindo campos condicionais, upload de imagem e especificações dinâmicas.
 */
@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressSpinnerModule, MaterialTypeSearch],
  providers: [provideNgxMask()],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductFormComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  public readonly dialogRef = inject(MatDialogRef<ProductFormComponent>);
  public readonly data: ProductFormData = inject(MAT_DIALOG_DATA);

  readonly product = signal<Product>(this.data.product);
  readonly isEditMode = signal<boolean>(this.data.isEditMode);
  matcher = new ImmediateErrorStateMatcher();

  /** URL segura para exibição da imagem, priorizando o preview local. */
  readonly safeImageSrc: Signal<string | null>;

  selectedFile = signal<File | null>(null);
  previewUrl = signal<string | null>(null);
  isUploading = signal(false);

  // Sinal para controlar se a foto foi marcada para remoção
  isPhotoRemoved = signal(false);

  productForm: FormGroup;

  /** Armazena as especificações originais para comparação e envio de `null` em campos removidos. */
  private initialSpecifications = signal<{ [key: string]: string }>({});

  private static readonly Texts = {
    CONFIRM_CHANGE_TITLE: 'Confirmar Alteração',
    CONFIRM_CHANGE_MESSAGE: (original: string, novo: string) => `Deseja realmente alterar a matéria-prima de "${original}" para "${novo}"?`,
    CONFIRM_DELETE_SPEC_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_SPEC_MESSAGE: (key: string) => `Deseja realmente remover a característica "${key}"?`,
    LOAD_ERROR: 'Falha ao buscar detalhes completos do produto:',
    SUBMIT_ERROR: 'Falha no envio do formulário:',
    UPDATE_ERROR: 'ID do produto não encontrado, não é possível atualizar.'
  };

  constructor() {
    this.safeImageSrc = computed(() => {
      if (this.isPhotoRemoved()) return null;
      return this.previewUrl() ?? this.product()?.fotoPrincipalUrl ?? null;
    });

    const currentProduct = this.product();

    this.productForm = this.fb.group({
      tipoProduto: [currentProduct.tipoProduto || 'CORTE', Validators.required],
      nome: [currentProduct.nome, [Validators.required, Validators.maxLength(100)]],
      sku: [currentProduct.sku, [Validators.required, Validators.maxLength(50)]],
      descricao: [currentProduct.descricao, Validators.maxLength(100)],
      unidadesPorProduto: [currentProduct.unidadesPorProduto, [Validators.required, Validators.min(1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]],
      ativo: [currentProduct.ativo],
      materiaPrima: [currentProduct.materiaPrima, Validators.required],
      cor: [currentProduct.cor, Validators.maxLength(50)],
      dimensoes: this.fb.group({
        larguraCm: [currentProduct.dimensoes?.larguraCm, [Validators.required, Validators.min(0.1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]],
        comprimentoCm: [currentProduct.dimensoes?.comprimentoCm, [Validators.required, Validators.min(0.1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]]
      }),
      codigoFabricante: [currentProduct.codigoFabricante, Validators.maxLength(50)],
      especificacoes: this.fb.array([])
    });

    this.setupFormControlsBasedOnProductType(currentProduct.tipoProduto || 'CORTE', false);

    // Reage dinamicamente à mudança do tipo de produto para ajustar a UI.
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
    this.initializeForm();
  }

  /**
   * Carrega os dados detalhados do produto para o formulário no modo de edição.
   * Isso é necessário para popular campos complexos como o FormArray de especificações.
   */
  initializeForm(): void {
    const selfUrl = this.product()?._links?.['self']?.href;
    if (!selfUrl) return;

    lastValueFrom(this.productService.getProductByUrl(selfUrl))
      .then(fullProduct => {
        if (fullProduct) {
          this.product.set(fullProduct);

          if (fullProduct.materiaPrima) {
            this.productForm.get('materiaPrima')?.patchValue(fullProduct.materiaPrima);
          }

          this.specifications.clear();
          const specs = fullProduct.especificacoes;
          if (specs) {
            this.initialSpecifications.set({ ...specs });
            Object.entries(specs).forEach(([chave, valor]) => {
              this.specifications.push(this.fb.group({
                chave: [chave, [Validators.required, Validators.maxLength(50)]],
                valor: [valor, [Validators.required, Validators.maxLength(100)]],
                isNew: [false] // Flag para controle de remoção
              }));
            });
          }
          this.cdr.markForCheck();
        }
      })
      .catch(err => console.error(ProductFormComponent.Texts.LOAD_ERROR, err));
  }

  get specifications(): FormArray {
    return this.productForm.get('especificacoes') as FormArray;
  }

  get specificationsControls(): FormGroup[] {
    return (this.productForm.get('especificacoes') as FormArray).controls as FormGroup[];
  }

  get materialTypeControl(): FormControl {
    return this.productForm.get('materiaPrima') as FormControl;
  }

  /**
   * Adiciona uma nova linha de especificação técnica ao formulário.
   */
  addSpecification(): void {
    this.specifications.push(this.fb.group({
      chave: ['', [Validators.required, Validators.maxLength(50)]],
      valor: ['', [Validators.required, Validators.maxLength(100)]],
      isNew: [true]
    }));

    // Scroll automático para o novo item, melhorando a experiência do usuário.
    setTimeout(() => {
      const dialogContent = (this.dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
      if (dialogContent) {
        dialogContent.scrollTop = dialogContent.scrollHeight;
      }
    }, 100);
  }

  /**
   * Remove uma especificação da lista, com confirmação para itens existentes.
   */
  async removeSpecification(index: number): Promise<void> {
    const specGroup = this.specifications.at(index);
    const isNew = specGroup.get('isNew')?.value;

    // Se for um item novo, remove sem confirmação.
    if (isNew) {
      this.specifications.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      return;
    }

    // Se for um item existente, pede confirmação.
    const key = specGroup.get('chave')?.value;
    const dialogData: ConfirmDialogData = {
      title: ProductFormComponent.Texts.CONFIRM_DELETE_SPEC_TITLE,
      message: ProductFormComponent.Texts.CONFIRM_DELETE_SPEC_MESSAGE(key || 'esta característica')
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      this.specifications.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      this.cdr.markForCheck();
    }
  }

  /**
   * Habilita/desabilita campos do formulário com base no tipo de produto,
   * garantindo que apenas os campos relevantes sejam validados e preenchidos.
   */
  private setupFormControlsBasedOnProductType(type: 'CORTE' | 'CONSUMO_DIRETO', resetOppositeControls: boolean): void {
    const corteControls = ['cor', 'dimensoes'];
    const consumoControls = ['codigoFabricante', 'especificacoes'];
    const corControl = this.productForm.get('cor');

    if (type === 'CORTE') {
      corteControls.forEach(name => {
        this.productForm.get(name)?.enable();
        if (name === 'dimensoes') {
          this.productForm.get('dimensoes.larguraCm')?.setValidators([Validators.required, Validators.min(0.1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]);
          this.productForm.get('dimensoes.comprimentoCm')?.setValidators([Validators.required, Validators.min(0.1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]);
        }
      });
      corControl?.setValidators([Validators.required, Validators.maxLength(50)]);

      consumoControls.forEach(name => {
        const control = this.productForm.get(name);
        control?.disable();
        if (resetOppositeControls) {
          if (name === 'especificacoes') {
            this.specifications.clear();
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
    corControl?.updateValueAndValidity();
    this.productForm.get('dimensoes.larguraCm')?.updateValueAndValidity();
    this.productForm.get('dimensoes.comprimentoCm')?.updateValueAndValidity();
  }

  /**
   * Ponto de entrada para a submissão do formulário.
   */
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
      console.error(ProductFormComponent.Texts.SUBMIT_ERROR, error);
      this.dialogRef.close(false);
    } finally {
      this.isUploading.set(false);
    }
  }

  /**
   * Prepara o payload final para a API, formatando especificações e removendo campos irrelevantes.
   */
  private getProcessedFormValue(): any {
    // Usa getRawValue() para incluir campos desabilitados, que serão limpos a seguir.
    const formValue = this.productForm.getRawValue();

    const especificacoesMap: { [key: string]: string } = {};
    (formValue.especificacoes || []).forEach((spec: { chave: string; valor: string }) => {
      if (spec.chave) {
        especificacoesMap[spec.chave] = spec.valor;
      }
    });
    formValue.especificacoes = especificacoesMap;

    // Limpa o payload para enviar apenas os dados pertinentes ao tipo do produto.
    if (formValue.tipoProduto === 'CORTE') {
      delete formValue.codigoFabricante;
      delete formValue.especificacoes;
    } else if (formValue.tipoProduto === 'CONSUMO_DIRETO') {
      delete formValue.cor;
      delete formValue.dimensoes;
    }

    return formValue;
  }

  /**
   * Lida com a submissão de edição (PATCH), enviando apenas os campos alterados.
   */
  private async handleEditSubmit(): Promise<void> {
    const hasImageChanged = !!this.selectedFile();
    const dirtyValues = this.getDirtyValues();
    const hasFormChanged = Object.keys(dirtyValues).length > 0;

    // Se a foto foi removida, adiciona null ao payload
    if (this.isPhotoRemoved()) {
        dirtyValues['fotoPrincipalUrl'] = null;
    }

    if (!hasImageChanged && !hasFormChanged && !this.isPhotoRemoved()) {
      this.dialogRef.close(false); // Nenhuma mudança, fecha sem atualizar.
      return;
    }

    // Se a imagem mudou, faz o upload primeiro.
    if (hasImageChanged) {
      const updated = await this.uploadImage(this.product(), hasFormChanged);
      if (updated) {
        this.product.set(updated);
      }
    }

    // Se o formulário mudou ou a foto foi removida, envia o PATCH.
    if (hasFormChanged || this.isPhotoRemoved()) {
      if (!this.product()?.id) {
        console.error(ProductFormComponent.Texts.UPDATE_ERROR, this.product());
        return;
      }
      await lastValueFrom(this.productService.patchProduct(this.product().id, dirtyValues));
    }

    this.dialogRef.close(true);
  }

  /**
   * Extrai apenas os campos "sujos" (dirty) do formulário para o payload do PATCH.
   * Lida com a complexidade do FormArray de especificações.
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

    // Lógica especial para especificações: compara o estado atual com o inicial
    // para enviar `null` para as chaves que foram removidas.
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
          specsPayload[initialKey] = null; // Marca para remoção no backend.
        }
      });
      dirtyValues['especificacoes'] = specsPayload;
    }
    return dirtyValues;
  }

  /**
   * Lida com a submissão de criação (POST).
   */
  private async handleCreateSubmit(): Promise<void> {
    const formValue = this.getProcessedFormValue();
    const hasImageToUpload = !!this.selectedFile();
    const newProduct = await lastValueFrom(this.productService.createProduct(formValue as Partial<Product>, hasImageToUpload));

    if (hasImageToUpload && newProduct) {
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
      this.isPhotoRemoved.set(false); // Se selecionou nova foto, cancela a remoção
    }
  }

  removePhoto(): void {
    this.selectedFile.set(null);
    this.previewUrl.set(null);
    this.isPhotoRemoved.set(true);
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
   * Confirma a intenção do usuário ao alterar a matéria-prima de um produto existente.
   */
  async onMaterialTypeChange(event: MatSelectChange): Promise<void> {
    const newSelection = event.value as MaterialType;
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
