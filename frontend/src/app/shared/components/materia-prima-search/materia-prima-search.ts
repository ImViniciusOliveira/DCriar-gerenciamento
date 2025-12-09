import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal, Signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { lastValueFrom } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { ApiRoot } from '../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../core/services/enum.service';
import { Product } from '../../../features/products/models/product.model';
import { ApiResponseMaterialTypes, MaterialType } from '../../../features/stock/models/material-type.model';
import { InfiniteScrollDirective } from '../../../features/stock/services/infinite-scroll.directive';
import { MaterialTypeService } from '../../../features/stock/services/material-type.service';

/**
 * Componente de busca de matéria-prima que opera de forma dinâmica via HATEOAS.
 * A lógica principal envolve descobrir a URL para os filtros de busca a partir
 * de uma busca inicial, tornando o componente adaptável às URLs fornecidas pela API.
 */
@Component({
  selector: 'app-materia-prima-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    InfiniteScrollDirective,
  ],
  templateUrl: './materia-prima-search.html',
  styleUrls: ['./materia-prima-search.scss'],
})
export class MateriaPrimaSearchComponent implements OnInit {
  @Input({ required: true }) product!: Product;
  @Input({ required: true }) formControl!: any;
  @Input() isEditMode = false;
  @Output() selectionChange = new EventEmitter<MatSelectChange>();

  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);
  private readonly fb = inject(FormBuilder);

  searchForm: FormGroup;
  materialTypes: WritableSignal<MaterialType[]> = signal([]);
  isSearching = signal(false);

  readonly safeImageSrc: Signal<string | null>;

  private readonly currentPage = signal(0);
  private readonly pageSize = 20;
  private readonly totalElements = signal(0);
  private readonly materialTypesSearchUrl: string | null;

  // 1. Um signal para armazenar a URL das unidades, descoberta dinamicamente.
  // Inicia como nulo e será preenchido após a primeira busca.
  private readonly unitsUrl = signal<string | null>(null);
  readonly consumptionUnits: Signal<EnumOption[]>;
  private readonly consumptionUnitsMap: Signal<Map<string, string | undefined>>;
  originalMateriaPrima?: MaterialType;

  constructor() {
    this.safeImageSrc = computed(() => this.product?.fotoPrincipalUrl ?? null);

    const getUrl = (link: string) =>
      this.product?._links?.[link]?.href?.split('{')[0] ||
      this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];

    this.materialTypesSearchUrl = getUrl('tipos-materia-prima') ?? null;

    if (!this.materialTypesSearchUrl) {
      console.error('URL para busca de matéria-prima não pôde ser determinada. O formulário será desabilitado.');
    }

    // 2. Converte o signal `unitsUrl` em um Observable.
    // Agora, qualquer mudança no signal emitirá um novo valor neste fluxo.
    const consumptionUnits$ = toObservable(this.unitsUrl).pipe(
      // 3. `filter`: Garante que o fluxo só prossiga se a URL for uma string válida (não nula).
      // Isso previne uma chamada de API desnecessária na inicialização.
      filter((url): url is string => !!url),

      // 4. `switchMap`: O operador principal para a reatividade.
      // Quando uma nova URL válida chega, ele cancela qualquer requisição anterior
      // e "troca" para um novo Observable que busca os dados das unidades.
      switchMap(url => this.enumService.getConsumptionUnitsMap(url)),

      // 5. `map`: Transforma o `Map` retornado pelo serviço em um array de opções,
      // que é o formato esperado pelo template.
      map(unitsMap => Array.from(unitsMap.values())),
    );

    // 6. Converte o resultado final do Observable de volta para um signal.
    // O template usará `consumptionUnits` para renderizar as opções do filtro.
    this.consumptionUnits = toSignal(consumptionUnits$, { initialValue: [] });

    // Cria um mapa para fácil acesso à descrição da unidade, recalculado sempre que `consumptionUnits` mudar.
    this.consumptionUnitsMap = computed(() => {
      const units = this.consumptionUnits();
      return new Map(units.map(u => [u.value, u.viewValue]));
    });

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: [''],
    });

    if (!this.materialTypesSearchUrl) {
      this.formControl?.disable();
    }
  }

  ngOnInit(): void {
    this.originalMateriaPrima = this.product.materiaPrima;
    if (this.product.materiaPrima) {
      this.materialTypes.set([this.product.materiaPrima as MaterialType]);
    }
    // Dispara a busca inicial que também descobrirá os links para os filtros.
    void this.performSearch(true);
  }

  /**
   * Processa a resposta da busca, extraindo o link HATEOAS para as unidades de consumo.
   */
  private handleSearchResponse(response: ApiResponseMaterialTypes | null) {
    if (!response) {
      return;
    }

    // Ponto-chave do HATEOAS: O link para o filtro é descoberto a partir da resposta da API.
    const firstMaterial = response._embedded?.['tipos-materia-prima']?.[0];
    const newUnitsUrl = firstMaterial?._links?.['unidades-de-medida']?.href;

    // Ao atualizar este signal, a cadeia reativa no construtor é acionada,
    // o que resulta na busca e população do dropdown de unidades.
    if (newUnitsUrl && this.unitsUrl() !== newUnitsUrl) {
      this.unitsUrl.set(newUnitsUrl);
    }

    const newMaterials = response._embedded?.['tipos-materia-prima'] || [];
    this.totalElements.set(response.page.totalElements);

    return newMaterials;
  }

  async performSearch(isInitialSearch = false): Promise<void> {
    this.isSearching.set(true);

    try {
      this.currentPage.set(0);
      if (!isInitialSearch) {
        this.formControl.reset();
      }

      if (!this.materialTypesSearchUrl) {
        console.error('Não é possível buscar matérias-primas: URL não encontrada.');
        return;
      }

      const filters = {
        nome: this.searchForm.value.searchName,
        unidadeDeConsumo: this.searchForm.value.searchUnit,
      };

      const response = await lastValueFrom(
        this.materialTypeService.searchMaterialTypes(this.materialTypesSearchUrl, filters, this.currentPage(), this.pageSize),
      );

      const newMaterials = this.handleSearchResponse(response);
      this.materialTypes.set(newMaterials || []);
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

      const filters = {
        nome: this.searchForm.value.searchName,
        unidadeDeConsumo: this.searchForm.value.searchUnit,
      };

      const response = await lastValueFrom(
        this.materialTypeService.searchMaterialTypes(this.materialTypesSearchUrl, filters, this.currentPage(), this.pageSize),
      );

      const newMaterials = this.handleSearchResponse(response) || [];
      this.materialTypes.update(currentTypes => [...currentTypes, ...newMaterials]);
    } catch (err) {
      console.error('Erro ao carregar mais matérias-primas:', err);
    } finally {
      this.isSearching.set(false);
    }
  }

  getConsumptionUnitViewValue(value: string): string {
    return this.consumptionUnitsMap().get(value) ?? value;
  }

  compareMaterialTypes(o1: MaterialType, o2: MaterialType): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  onSelectionChange(event: MatSelectChange): void {
    this.selectionChange.emit(event);
  }
}
