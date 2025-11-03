import { InfiniteScrollDirective } from '../../../stock/services/infinite-scroll.directive';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Inject, OnInit, WritableSignal, computed, inject, signal, Signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Product } from '../../models/products.model';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductsService } from '../../services/products';
import { MaterialTypeService } from '../../../stock/services/material-type.service';
import { MaterialType } from '../../../stock/models/material-type.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog/confirm-dialog';
import { Observable, lastValueFrom, of, map } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { provideNgxMask } from 'ngx-mask';
import { ApiRoot } from '../../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule, InfiniteScrollDirective, MatProgressSpinnerModule],
  providers: [provideNgxMask()],
  templateUrl: './product-form.html',
  styleUrls: ['./product-form.scss']
})
export class ProductFormComponent implements OnInit { // Removido OnDestroy, pois destroy$ não é mais necessário
  private static readonly CONFIRM_CHANGE_TITLE = 'Confirmar Alteração';
  private static readonly CONFIRM_CHANGE_MESSAGE = (original: string, novo: string) =>
    `Deseja realmente alterar a matéria-prima de "${original}" para "${novo}"?`;

  product!: Product;
  isEditMode: boolean;

  productForm: FormGroup;
  private readonly productsService = inject(ProductsService);
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

  private readonly currentPage = signal(0);
  private readonly pageSize = 20;
  private readonly totalElements = signal(0);
  private readonly materialTypesSearchUrl: string | null;

  readonly consumptionUnits$: Observable<EnumOption[]>;
  readonly consumptionUnits: Signal<EnumOption[]>;
  private readonly consumptionUnitsMap = computed(() => new Map(this.consumptionUnits().map(u => [u.value, u.viewValue])));

  constructor(
    public dialogRef: MatDialogRef<ProductFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProductFormData,
    private readonly fb: FormBuilder,
  ) {
    this.product = data.product;
    this.isEditMode = data.isEditMode;

    // Lógica HATEOAS robusta:
    // 1. Tenta obter o link do objeto do produto (cenário ideal).
    // 2. Se não encontrar, busca o link na raiz da API (fallback).
    const getUrl = (link: string) => this.product?._links?.[link]?.href?.split('{')[0]
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
      console.error("URL para unidades de medida não pôde ser determinada.");
      this.consumptionUnits$ = of([]); // Define um array vazio se a URL não for encontrada
    }

    this.consumptionUnits = toSignal(this.consumptionUnits$, { initialValue: [] });

    this.productForm = this.fb.group({
      nome: [this.product.nome, Validators.required],
      sku: [this.product.sku, Validators.required],
      descricao: [this.product.descricao],
      cor: [this.product.cor],
      unidadesPorProduto: [this.product.unidadesPorProduto, [Validators.required, Validators.min(1)]],
      ativo: [this.product.ativo],
      materiaPrima: [this.product.materiaPrima, Validators.required],
      dimensoes: this.fb.group({
        larguraCm: [this.product.dimensoes?.larguraCm, [Validators.required, Validators.min(0.1)]],
        comprimentoCm: [this.product.dimensoes?.comprimentoCm, [Validators.required, Validators.min(0.1)]]
      })
    });

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: ['']
    });

    if (!this.materialTypesSearchUrl) {
      this.productForm.disable();
    }
  }

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      return;
    }

    if (this.isEditMode) {
      await this.handleEditSubmit();
    } else { // Modo de Criação
      await this.handleCreateSubmit();
    }
  }

  private async handleEditSubmit(): Promise<void> {
     this.isUploading.set(true); // Ativa o spinner antes das operações
     try {
       let hasChanged = false;

       if (this.selectedFile) {
         // Apenas faz o upload. Não precisamos da resposta aqui, pois a lista será recarregada.
         const updated = await this.uploadImage();
         if (updated) {
           this.product = updated; // garante que o produto local está atualizado
           hasChanged = true;
         }
       }

       if (!this.product?.id) {
         console.error('ID do produto não encontrado, não é possível atualizar.', this.product);
         return;
       }
      const dirtyValues = this.getDirtyValues(this.productForm);

      if (Object.keys(dirtyValues).length > 0) {
        await lastValueFrom(this.productsService.patchProduct(this.product.id, dirtyValues));
        hasChanged = true;
      }

      // Se houve qualquer alteração (upload ou patch), fechamos o diálogo para forçar o recarregamento.
      // Se não houve alteração, podemos simplesmente fechar sem sinalizar sucesso.
      // Desconecta o componente da detecção de mudanças ANTES de fechar.
      this.cdr.detach();
      this.dialogRef.close(hasChanged);
    } catch (error) {
      console.error('Erro ao atualizar o produto:', error instanceof Error ? error.message : error);
     } finally {
       this.isUploading.set(false); // Garante que o spinner seja desativado
     }
   }

  private async handleCreateSubmit(): Promise<void> {
    try {
      // Desconecta o componente da detecção de mudanças ANTES de fechar.
      this.cdr.detach();
      const formValue = this.productForm.getRawValue();
      await lastValueFrom(this.productsService.createProduct(formValue as Partial<Product>));
      this.dialogRef.close(true);
    } catch (error) {
      console.error('Erro ao criar o produto:', error instanceof Error ? error.message : error);
    }
  }

  // Utilitário: detecta se uma URL é provavelmente o endpoint de upload do produto
  private isUploadEndpoint(url: string | undefined | null): boolean {
    if (!url) return false;
    try {
      const parsed = new URL(url, window.location.origin);
      // Se a URL contém o padrão /api/v1/produtos/{id}/foto ou termina com /foto, consideramos upload endpoint
      return /\/api\/v1\/produtos\/[0-9]+\/foto(\/)?$/.test(parsed.pathname) || /\/api\/v1\/produtos\/.*\/foto/.test(parsed.pathname);
    } catch (e) {
      // fallback: regex direto na string (mais permissivo)
      return /\/api\/v1\/produtos\/[0-9]+\/foto(\/)?$/.test(String(url)) || /\/api\/v1\/produtos\/.*\/foto/.test(String(url));
    }
  }

  // Retorna a URL que deve ser usada no <img src="...">. Prioriza preview local (createObjectURL)
  // e só usa a URL do produto se ela for a URL pública de download (não o endpoint de upload).
  displayImageUrl(): string | null {
    const preview = this.previewUrl();
    if (preview) return preview;
    const prodUrl = this.product?.fotoPrincipalUrl;
    if (prodUrl) {
      // Normaliza e garante que não é o endpoint de upload
      const normalized = this.normalizeDownloadUrl(prodUrl);
      if (!this.isUploadEndpoint(normalized)) return normalized;
    }
    return null;
  }

  // Normaliza URLs recebidas do backend: encode espaços e caracteres inválidos para uso em src
  private normalizeDownloadUrl(url: string | null | undefined): string | null {
    if (!url) return null;
    try {
      // Se já é uma URL absoluta
      const u = new URL(url, window.location.origin);
      // Garantir que o path seja adequadamente encoded (evita espaços no caminho)
      const pathname = u.pathname.split('/').map(segment => encodeURIComponent(decodeURIComponent(segment))).join('/');
      // Reconstruir sem alterar other parts
      const normalized = `${u.protocol}//${u.host}${pathname}${u.search}${u.hash}`;
      return normalized;
    } catch (e) {
      // Se não é um URL válida, tenta um encode simples
      try {
        return encodeURI(url);
      } catch (ex) {
        return url;
      }
    }
  }

  ngOnInit(): void {
    // No modo de edição, pré-populamos a lista apenas com a matéria-prima atual do produto
    // para que ela já apareça selecionada. A busca completa só ocorrerá com a interação do usuário.
    if (this.isEditMode && this.product.materiaPrima) {
      this.materialTypes.set([this.product.materiaPrima]);
    }

    // Se o produto já tiver uma foto, define a URL de preview
    if (this.isEditMode && this.product.fotoPrincipalUrl) {
      // Segurança: evita usar acidentalmente uma URL de upload (POST) como src de imagem.
      const normalized = this.normalizeDownloadUrl(this.product.fotoPrincipalUrl);
      if (normalized && !this.isUploadEndpoint(normalized)) {
        this.previewUrl.set(normalized);
      } else {
        // Se a URL parecer apontar para o endpoint de upload, não a usa como src.
        this.previewUrl.set(null);
      }
    }
    // Nos modos de criação e visualização, a lista de matérias-primas começa vazia,
    // aguardando a ação do usuário para ser populada.
  }

  async performSearch(): Promise<void> {
    this.isSearching.set(true);

    try {
      this.currentPage.set(0);
      // Sempre limpa os tipos de matéria-prima existentes para uma nova busca
      this.materialTypes.set([]);
      // Limpa a seleção atual do dropdown para evitar que um valor antigo seja mantido.
      this.productForm.get('materiaPrima')?.reset();

      if (!this.materialTypesSearchUrl) {
        console.error("Não é possível buscar matérias-primas: URL não encontrada no produto.");
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

      // Acesso seguro: se a busca não retornar nada (`_embedded` for undefined), `newMaterials` será um array vazio.
      const newMaterials = response?._embedded?.['tipos-materia-prima'] || [];

      // A lista de resultados agora contém apenas o que foi retornado pela busca.
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

      // Acesso seguro também no `loadMore`.
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
      // Gera uma URL local para a pré-visualização da imagem
      this.previewUrl.set(URL.createObjectURL(this.selectedFile));
    }
  }

  private async uploadImage(): Promise<Product | null> {
    const uploadUrl = this.product?._links?.['upload-foto']?.href;
    if (!this.selectedFile || !uploadUrl) {
      // Se não houver arquivo ou URL, retorna o produto atual sem alterações.
      return this.product;
    }

    this.isUploading.set(true);
    try {
      const updatedProduct = await lastValueFrom(
        this.productsService.uploadProductPhoto(uploadUrl, this.selectedFile)
      );
      // Atualiza o produto local com a nova URL da imagem para feedback visual imediato.
      // Atualiza o estado do componente com o produto retornado pelo backend
      if (updatedProduct) {
        // Sanitiza a fotoPrincipalUrl retornada pelo backend antes de sobrescrever o produto local.
        const rawUrl = updatedProduct.fotoPrincipalUrl;
        const normalized = this.normalizeDownloadUrl(rawUrl);
        if (normalized && !this.isUploadEndpoint(normalized) && normalized.includes('/uploads/')) {
          // URL válida de download -> usa
          updatedProduct.fotoPrincipalUrl = normalized;
          this.previewUrl.set(normalized);
        } else {
          // Backend retornou algo inesperado (ex.: endpoint de upload).
          // Não sobrescreve a url de foto existente do produto local para evitar que o <img> tente um GET em /produtos/{id}/foto.
          // Mantemos a URL anterior (ou string vazia) sem logs de depuração.
          updatedProduct.fotoPrincipalUrl = this.product?.fotoPrincipalUrl ?? '';
         }

        // Atualiza o produto local com os demais campos (mantendo fotoPrincipalUrl sanitizada)
        this.product = { ...this.product, ...updatedProduct } as Product;
      }
      this.selectedFile = null; // Limpa o arquivo selecionado
      return updatedProduct;
    } catch (err) {
      // Erro no upload: será tratado pelo chamador
      throw err; // Re-lança o erro para ser pego pelo bloco catch do onSubmit
    } finally {
      this.isUploading.set(false);
    }
  }

  private getDirtyValues(form: FormGroup | FormArray): { [key: string]: any } {
    const dirtyValues: { [key: string]: any } = {};
    for (const key of Object.keys(form.controls)) {
      const control = (form.controls as any)[key];

      // Skip controls that were not modified to reduce nesting
      if (!control.dirty) {
        continue;
      }

      // Handle composite controls (groups/arrays)
      if (control instanceof FormGroup || control instanceof FormArray) {
        const nestedDirtyValues = this.getDirtyValues(control);
        if (Object.keys(nestedDirtyValues).length === 0) {
          continue;
        }

        // Special case for 'dimensoes': if any child changed, send the whole object
        if (key === 'dimensoes') {
          dirtyValues[key] = control.value;
        } else {
          dirtyValues[key] = nestedDirtyValues;
        }
        continue;
      }

      // Simple control: include its value
      dirtyValues[key] = control.value;
    }
    return dirtyValues;
  }

  getConsumptionUnitViewValue(value: string): string {
    return this.consumptionUnitsMap().get(value) ?? value;
  }

  compareMaterialTypes(o1: MaterialType, o2: MaterialType): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  async onMaterialTypeChange(event: { value: MaterialType }): Promise<void> {
    const newSelection = event.value;
    const originalSelection = this.product.materiaPrima;

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

  // Retorna a URL segura para usar em <img src>
  getSafeImageSrc(): string | null {
    // Prioriza preview local (createObjectURL)
    const preview = this.previewUrl();
    if (preview) return preview;

    const prodUrl = this.product?.fotoPrincipalUrl;
    if (!prodUrl) return null;

    const normalized = this.normalizeDownloadUrl(prodUrl);
    if (!normalized) return null;

    // Apenas permite URLs públicas que contenham '/uploads/' (onde os arquivos são servidos)
    if (normalized.includes('/uploads/')) {
      return normalized;
    }

    // Se o backend devolveu algo inesperado (ex.: o endpoint de upload), logamos para investigação
    // Ignora URL não válida de download sem log.
     return null;
   }
 }

export interface ProductFormData {
  product: Product;
  isEditMode: boolean;
  isCreationMode?: boolean;
  title: string;
}
