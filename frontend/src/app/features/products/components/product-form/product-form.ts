import { InfiniteScrollDirective } from '../../../stock/services/infinite-scroll.directive';
import {CommonModule, NgOptimizedImage} from '@angular/common';
                          import { ChangeDetectorRef, Component, Inject, OnInit, WritableSignal, inject, signal, Signal, computed } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Product } from '../../models/product.model';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../services/product';
import { MaterialTypeService } from '../../../stock/services/material-type.service';
import { MaterialType } from '../../../stock/models/material-type.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Observable, lastValueFrom, of, map } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { provideNgxMask } from 'ngx-mask';
import { ApiRoot } from '../../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule, InfiniteScrollDirective, MatProgressSpinnerModule, NgOptimizedImage],
  providers: [provideNgxMask()],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss']
})
export class ProductFormComponent implements OnInit {
  private static readonly CONFIRM_CHANGE_TITLE = 'Confirmar Alteração';
  private static readonly CONFIRM_CHANGE_MESSAGE = (original: string, novo: string) =>
    `Deseja realmente alterar a matéria-prima de "${original}" para "${novo}"?`;

  readonly product: WritableSignal<Product>;
  isEditMode: boolean;

  productForm: FormGroup;
  // Nome da variável injetada corrigido
  private readonly productService = inject(ProductService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);

  searchForm: FormGroup;
  materialTypes: WritableSignal<MaterialType[]> = signal([]);
  selectedFile: File | null = null;
  previewUrl = signal<string | null>(null);
  isSearching = signal(false);
  isUploading = signal(false);
  readonly safeImageSrc: Signal<string | null>;

  private readonly currentPage = signal(0);
  private readonly pageSize = 20;
  private readonly totalElements = signal(0);
  private readonly materialTypesSearchUrl: string | null;

  readonly consumptionUnits$: Observable<EnumOption[]>;
  readonly consumptionUnits: Signal<EnumOption[]>;
  private readonly consumptionUnitsMap: Map<string, string | undefined>;

  constructor(
    public dialogRef: MatDialogRef<ProductFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProductFormData,
    private readonly fb: FormBuilder,
  ) {
    this.product = signal(data.product);
    this.isEditMode = data.isEditMode;
    this.safeImageSrc = computed(() => this.previewUrl() ?? this.product()?.fotoPrincipalUrl ?? null);

    // Obtém a URL do endpoint HATEOAS, priorizando o link do produto e usando a raiz da API como fallback.
    const getUrl = (link: string) => this.product()?._links?.[link]?.href?.split('{')[0]
                                  || this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];

    const searchUrl = getUrl('buscar-tipos-materia-prima');
    const unitsUrl = getUrl('unidades-de-medida');

    this.materialTypesSearchUrl = searchUrl ?? null;

    if (!this.materialTypesSearchUrl) {
      console.error("URL para busca de matéria-prima não pôde ser determinada. O formulário será desabilitado.");
    }

    if (unitsUrl) {
      this.consumptionUnits$ = this.enumService.getConsumptionUnitsMap(unitsUrl).pipe(
        map(unitsMap => Array.from(unitsMap.values()))
      );
    } else {
      console.error('URL para unidades de medida não pôde ser determinada.');
      this.consumptionUnits$ = of([]);
    }

    this.consumptionUnits = toSignal(this.consumptionUnits$, { initialValue: [] });
    this.consumptionUnitsMap = new Map(this.consumptionUnits().map(u => [u.value, u.viewValue]));

    const currentProduct = this.product();
    this.productForm = this.fb.group({
      tipoProduto: [currentProduct.tipoProduto || 'CORTE', Validators.required],
      nome: [currentProduct.nome, Validators.required],
      sku: [currentProduct.sku, Validators.required],
      descricao: [currentProduct.descricao],
      unidadesPorProduto: [currentProduct.unidadesPorProduto, [Validators.required, Validators.min(1)]],
      ativo: [currentProduct.ativo],
      materiaPrima: [currentProduct.materiaPrima, Validators.required],

      // Campos de ProdutoDeCorte
      cor: [currentProduct.cor],
      dimensoes: this.fb.group({
        larguraCm: [currentProduct.dimensoes?.larguraCm, [Validators.required, Validators.min(0.1)]],
        comprimentoCm: [currentProduct.dimensoes?.comprimentoCm, [Validators.required, Validators.min(0.1)]]
      }),

      // Campos de ProdutoDeConsumoDireto
      codigoFabricante: [currentProduct.codigoFabricante],
      especificacoes: this.fb.group({
        // Inicialização vazia, pode ser preenchido dinamicamente se necessário
      })
    });

    this.setupFormControlsBasedOnProductType(currentProduct.tipoProduto || 'CORTE', false);

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: ['']
    });

    if (!this.materialTypesSearchUrl) {
      this.productForm.get('materiaPrima')?.disable();
    }

    this.productForm.get('tipoProduto')?.valueChanges.subscribe(type => {
      this.setupFormControlsBasedOnProductType(type, true);
    });
  }

  private setupFormControlsBasedOnProductType(type: 'CORTE' | 'CONSUMO_DIRETO', resetOppositeControls: boolean): void {
    const corteControls = ['cor', 'dimensoes'];
    const consumoControls = ['codigoFabricante', 'especificacoes'];

    if (type === 'CORTE') {
      corteControls.forEach(name => {
        this.productForm.get(name)?.enable();
        if (name === 'dimensoes') {
          this.productForm.get('dimensoes.larguraCm')?.setValidators([Validators.required, Validators.min(0.1)]);
          this.productForm.get('dimensoes.comprimentoCm')?.setValidators([Validators.required, Validators.min(0.1)]);
        }
      });
      consumoControls.forEach(name => {
        const control = this.productForm.get(name);
        control?.disable();
        if (resetOppositeControls) {
          control?.reset();
        }
      });
    } else { // CONSUMO_DIRETO
      consumoControls.forEach(name => this.productForm.get(name)?.enable());
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
  }

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      return;
    }

    if (this.isEditMode) {
      await this.handleEditSubmit();
    } else {
      await this.handleCreateSubmit();
    }
  }

  private async handleEditSubmit(): Promise<void> {
     this.isUploading.set(true);
     try {
       let hasChanged = false;

       if (this.selectedFile) {
         const updated = await this.uploadImage();
         if (updated) {
           this.product.set(updated);
           hasChanged = true;
         }
       }

       if (!this.product()?.id) {
         console.error('ID do produto não encontrado, não é possível atualizar.', this.product());
         return;
       }
      const dirtyValues = this.getDirtyValues(this.productForm);

      if (Object.keys(dirtyValues).length > 0) {
        await lastValueFrom(this.productService.patchProduct(this.product().id, dirtyValues));
        hasChanged = true;
      }

      this.cdr.detach();
      this.dialogRef.close(hasChanged);
    } catch (error) {
      console.error('Erro ao atualizar o produto:', error instanceof Error ? error.message : error);
     }
   }

  private async handleCreateSubmit(): Promise<void> {
    try {
      this.cdr.detach();
      const formValue = this.productForm.getRawValue();
      await lastValueFrom(this.productService.createProduct(formValue as Partial<Product>));
      this.dialogRef.close(true);
    } catch (error) {
      console.error('Erro ao criar o produto:', error instanceof Error ? error.message : error);
    }
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
            this.productForm.patchValue(fullProduct);
            if (fullProduct.materiaPrima) {
              this.materialTypes.set([fullProduct.materiaPrima as MaterialType]);
            }
          }
        })
        .catch(err => console.error("Falha ao buscar detalhes completos do produto:", err));
    }
  }

  async performSearch(): Promise<void> {
    this.isSearching.set(true);

    try {
      this.currentPage.set(0);
      this.materialTypes.set([]);
      this.productForm.get('materiaPrima')?.reset();

      if (!this.materialTypesSearchUrl) {
        console.error('Não é possível buscar matérias-primas: URL não encontrada no produto.');
        return;
      }

      const filters = { nome: this.searchForm.value.searchName, unidadeDeConsumo: this.searchForm.value.searchUnit };

      const response = await lastValueFrom(
        this.materialTypeService.searchMaterialTypes(
          this.materialTypesSearchUrl,
          filters,
          this.currentPage(),
          this.pageSize
        )
      );

      const newMaterials = response?._embedded?.['tipos-materia-prima'] || [];

      this.materialTypes.set(newMaterials);
      this.totalElements.set(response.page.totalElements);
    } catch (err) {
      console.error('Erro na busca por matéria-prima:', err);
    } finally {
      this.isSearching.set(false);
    }
  }

  async loadMore(): Promise<void> {
    if (this.isSearching() || this.materialTypes().length >= this.totalElements()) {
      return;
    }

    this.isSearching.set(true);

    try {
      this.currentPage.update(page => page + 1);

      if (!this.materialTypesSearchUrl) return;

      const filters = { nome: this.searchForm.value.searchName, unidadeDeConsumo: this.searchForm.value.searchUnit };

      const response = await lastValueFrom(this.materialTypeService.searchMaterialTypes(this.materialTypesSearchUrl, filters, this.currentPage(), this.pageSize));

      const newMaterials = response?._embedded?.['tipos-materia-prima'] || [];
      this.materialTypes.update(currentTypes => [...currentTypes, ...newMaterials]);
    } catch (err) {
      console.error('Erro ao carregar mais matérias-primas:', err);
    } finally {
      this.isSearching.set(false);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.previewUrl.set(URL.createObjectURL(this.selectedFile));
    }
  }

  private async uploadImage(): Promise<Product | null> {
    const uploadUrl = this.product()?._links?.['upload-foto']?.href;
    if (!this.selectedFile || !uploadUrl) {
      return this.product();
    }

    this.isUploading.set(true);
    try {
      const updatedProduct = await lastValueFrom(
        this.productService.uploadProductPhoto(uploadUrl, this.selectedFile)
      );
      if (updatedProduct) {
        this.previewUrl.set(updatedProduct.fotoPrincipalUrl);
        this.product.set({ ...this.product(), ...updatedProduct });
      }
      this.selectedFile = null;
      return updatedProduct;
    } catch (err) {
      console.error('Falha durante o upload da imagem:', err);
      throw err;
    } finally {
      this.isUploading.set(false);
    }
  }

  private getDirtyValues(form: FormGroup | FormArray): { [key: string]: any } {
    const dirtyValues: { [key: string]: any } = {};
    for (const key of Object.keys(form.controls)) {
      const control = (form.controls as any)[key];

      if (!control.dirty) {
        continue;
      }

      if (control instanceof FormGroup || control instanceof FormArray) {
        const nestedDirtyValues = this.getDirtyValues(control);
        if (Object.keys(nestedDirtyValues).length === 0) {
          continue;
        }

        dirtyValues[key] = nestedDirtyValues;
        continue;
      }

      dirtyValues[key] = control.value;
    }

    return dirtyValues;
  }

  getConsumptionUnitViewValue(value: string): string {
    return this.consumptionUnitsMap.get(value) ?? value;
  }

  getFormattedDimensions(dimensions: { larguraCm?: number; comprimentoCm?: number } | null | undefined): string {
    if (dimensions && typeof dimensions.larguraCm === 'number' && typeof dimensions.comprimentoCm === 'number') {
      return `${dimensions.larguraCm} x ${dimensions.comprimentoCm} cm`;
    }
    return 'N/A';
  }

  compareMaterialTypes(o1: MaterialType, o2: MaterialType): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  async onMaterialTypeChange(event: { value: MaterialType }): Promise<void> {
    const newSelection = event.value;
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
