import {CommonModule} from '@angular/common';
import { ChangeDetectorRef, Component, Inject, OnInit, WritableSignal, inject, signal, Signal, computed } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Product } from '../../models/product.model';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
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
import { MaterialType } from '../../../stock/models/material-type.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressSpinnerModule, MateriaPrimaSearchComponent],
  providers: [provideNgxMask()],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss']
})
export class ProductFormComponent implements OnInit {
  private static readonly CONFIRM_CHANGE_TITLE = 'Confirmar Alteração';
  private static readonly CONFIRM_CHANGE_MESSAGE = (original: string, novo: string) =>
    `Deseja realmente alterar a matéria-prima de "${original}" para "${novo}"?`;
  private static readonly CONFIRM_DELETE_SPEC_TITLE = 'Confirmar Remoção';
  private static readonly CONFIRM_DELETE_SPEC_MESSAGE = (key: string) =>
    `Deseja realmente remover a característica "${key}"?`;

  readonly product: WritableSignal<Product>;
  isEditMode: boolean;

  productForm: FormGroup;
  private readonly productService = inject(ProductService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);

  selectedFile: File | null = null;
  previewUrl = signal<string | null>(null);
  isUploading = signal(false);
  readonly safeImageSrc: Signal<string | null>;

  private initialSpecifications: { [key: string]: string } = {};

  constructor(
    public dialogRef: MatDialogRef<ProductFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProductFormData,
    private readonly fb: FormBuilder,
  ) {
    this.product = signal(data.product);
    this.isEditMode = data.isEditMode;
    this.safeImageSrc = computed(() => this.previewUrl() ?? this.product()?.fotoPrincipalUrl ?? null);

    const currentProduct = this.product();
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

    this.setupFormControlsBasedOnProductType(currentProduct.tipoProduto || 'CORTE', false);

    this.productForm.get('tipoProduto')?.valueChanges.subscribe(type => {
      this.setupFormControlsBasedOnProductType(type, true);
    });
  }

  ngOnInit(): void {
    if (this.data.isCreationMode) {
      return;
    }

    const selfUrl = this.product()?._links?.['self']?.href;
    if (selfUrl) {
      lastValueFrom(this.productService.getProductByUrl(selfUrl))
        .then(fullProduct => {
          if (fullProduct) {
            this.product.set(fullProduct);
            if (fullProduct.materiaPrima) {
              this.productForm.get('materiaPrima')?.patchValue(fullProduct.materiaPrima);
            }
            this.especificacoes.clear();
            const specs = fullProduct['especificacoes' as keyof Product] as { [key: string]: string } | undefined;
            if (specs) {
              this.initialSpecifications = { ...specs };
              Object.entries(specs).forEach(([chave, valor]) => {
                this.especificacoes.push(this.fb.group({
                  chave: [chave, Validators.required],
                  valor: [valor, Validators.required],
                  isNew: [false]
                }));
              });
            }
            this.cdr.detectChanges();
          }
        })
        .catch(err => console.error("Falha ao buscar detalhes completos do produto:", err));
    }
  }

  get especificacoes(): FormArray {
    return this.productForm.get('especificacoes') as FormArray;
  }

  get especificacoesControls(): FormGroup[] {
    return (this.productForm.get('especificacoes') as FormArray).controls as FormGroup[];
  }

  get materiaPrimaControl(): FormControl {
    return this.productForm.get('materiaPrima') as FormControl;
  }

  addEspecificacao(): void {
    this.especificacoes.push(this.fb.group({
      chave: ['', Validators.required],
      valor: ['', Validators.required],
      isNew: [true]
    }));
    this.cdr.detectChanges();

    setTimeout(() => {
      const dialogContent = (this.dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
      if (dialogContent) {
        dialogContent.scrollTop = dialogContent.scrollHeight;
      }
    }, 100);
  }

  async removeEspecificacao(index: number): Promise<void> {
    const specGroup = this.especificacoes.at(index);
    const isNew = specGroup.get('isNew')?.value;

    if (isNew) {
      this.especificacoes.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      this.cdr.detectChanges();
      return;
    }

    const key = specGroup.get('chave')?.value;
    const dialogData: ConfirmDialogData = {
      title: ProductFormComponent.CONFIRM_DELETE_SPEC_TITLE,
      message: ProductFormComponent.CONFIRM_DELETE_SPEC_MESSAGE(key || 'esta característica')
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      this.especificacoes.removeAt(index);
      this.productForm.get('especificacoes')?.markAsDirty();
      this.cdr.detectChanges();
    }
  }

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
    corControl?.updateValueAndValidity();
    this.productForm.get('dimensoes.larguraCm')?.updateValueAndValidity();
    this.productForm.get('dimensoes.comprimentoCm')?.updateValueAndValidity();
  }

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      return;
    }

    this.isUploading.set(true);
    try {
      if (this.isEditMode) {
        await this.handleEditSubmit();
      } else {
        await this.handleCreateSubmit();
      }
    } catch (error) {
      console.error('Falha no envio do formulário:', error);
      // Exibe um snackbar de erro para o usuário.
      this.dialogRef.close(false);
    } finally {
      this.isUploading.set(false);
    }
  }

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

  private async handleEditSubmit(): Promise<void> {
    let hasChanged = false;

    if (this.selectedFile) {
      const updated = await this.uploadImage(this.product());
      if (updated) {
        this.product.set(updated);
        hasChanged = true;
      }
    }

    if (!this.product()?.id) {
      console.error('ID do produto não encontrado, não é possível atualizar.', this.product());
      return;
    }

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
      Object.keys(this.initialSpecifications).forEach(initialKey => {
        if (!currentSpecs.hasOwnProperty(initialKey)) {
          specsPayload[initialKey] = null;
        }
      });
      dirtyValues['especificacoes'] = specsPayload;
    }

    if (Object.keys(dirtyValues).length > 0) {
      await lastValueFrom(this.productService.patchProduct(this.product().id, dirtyValues));
      hasChanged = true;
    }

    this.dialogRef.close(hasChanged);
  }

  private async handleCreateSubmit(): Promise<void> {
    const formValue = this.getProcessedFormValue();
    const newProduct = await lastValueFrom(this.productService.createProduct(formValue as Partial<Product>));

    if (this.selectedFile && newProduct) {
      await this.uploadImage(newProduct);
    }

    this.dialogRef.close(true);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.previewUrl.set(URL.createObjectURL(this.selectedFile));
      this.cdr.detectChanges();
    }
  }

  private async uploadImage(product: Product): Promise<Product | null> {
    const uploadUrl = product?._links?.['upload-foto']?.href;
    if (!this.selectedFile || !uploadUrl) {
      return product;
    }

    const updatedProduct = await lastValueFrom(
      this.productService.uploadProductPhoto(uploadUrl, this.selectedFile)
    );
    if (updatedProduct) {
      this.previewUrl.set(updatedProduct.fotoPrincipalUrl);
      this.product.set({ ...product, ...updatedProduct });
    }
    this.selectedFile = null;
    return updatedProduct;
  }

  getFormattedDimensions(dimensions: { larguraCm?: number; comprimentoCm?: number } | null | undefined): string {
    if (dimensions && typeof dimensions.larguraCm === 'number' && typeof dimensions.comprimentoCm === 'number') {
      return `${dimensions.larguraCm} x ${dimensions.comprimentoCm} cm`;
    }
    return 'N/A';
  }

  async onMaterialTypeChange(event: MatSelectChange): Promise<void> {
    const newSelection = event.value as MaterialType;
    const originalSelection = this.product().materiaPrima;

    if (!originalSelection || !newSelection || originalSelection.id === newSelection.id) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: ProductFormComponent.CONFIRM_CHANGE_TITLE,
      message: ProductFormComponent.CONFIRM_CHANGE_MESSAGE(originalSelection.nome, newSelection.nome)
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (!confirmed) {
      this.productForm.get('materiaPrima')?.setValue(originalSelection);
    }
  }
}

export interface ProductFormData {
  product: Product;
  isEditMode: boolean;
  isCreationMode?: boolean;
  title: string;
}
