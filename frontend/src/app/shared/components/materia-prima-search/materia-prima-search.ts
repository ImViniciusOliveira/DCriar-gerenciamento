import { Component, computed, effect, EventEmitter, inject, Input, OnInit, Output, signal, Signal, WritableSignal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { of } from 'rxjs';
import { filter, map, switchMap, debounceTime, distinctUntilChanged, catchError } from 'rxjs/operators';
import { ApiRoot } from '../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../core/services/enum.service';
import { Product } from '../../../features/products/models/product.model';
import { TipoMateriaPrima } from '../../../features/stock/models/material-type.model';
import { InfiniteScrollDirective } from '../../../features/stock/services/infinite-scroll.directive';
import { MaterialTypeService } from '../../../features/stock/services/material-type.service';

@Component({
  selector: 'app-materia-prima-search',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    InfiniteScrollDirective
  ],
  templateUrl: './materia-prima-search.html',
  styleUrls: ['./materia-prima-search.scss'],
})
export class MateriaPrimaSearchComponent implements OnInit {
  // Aceita um Produto parcial, pois só precisamos dos _links.
  @Input({ required: true }) product!: Partial<Product>;
  @Input({ required: true }) formControl!: any;
  @Input() isEditMode = false;
  @Output() selectionChange = new EventEmitter<MatSelectChange>();

  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);
  private readonly fb = inject(FormBuilder);

  searchForm: FormGroup;
  materialTypes: WritableSignal<TipoMateriaPrima[]> = signal([]);
  isSearching = signal(false);
  totalElements = signal(0);
  private currentPage = 0;
  private readonly pageSize = 20;

  readonly safeImageSrc: Signal<string | null>;
  private readonly unitsUrl = signal<string | null>(null);
  readonly consumptionUnits: Signal<EnumOption[]>;
  private readonly consumptionUnitsMap: Signal<Map<string, string | undefined>>;
  originalMateriaPrima?: TipoMateriaPrima;

  constructor() {
    this.safeImageSrc = computed(() => this.product?.fotoPrincipalUrl ?? null);

    const getUrl = (link: string) =>
      this.product?._links?.[link]?.href?.split('{')[0] ||
      this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];

    const materialTypesSearchUrl = getUrl('tipos-materia-prima') ?? null;

    if (!materialTypesSearchUrl) {
      console.error('URL para busca de matéria-prima não pôde ser determinada. O formulário será desabilitado.');
    }

    const consumptionUnits$ = toObservable(this.unitsUrl).pipe(
      filter((url): url is string => !!url),
      switchMap(url => this.enumService.getConsumptionUnitsMap(url)),
      map(unitsMap => Array.from(unitsMap.values())),
    );

    this.consumptionUnits = toSignal(consumptionUnits$, { initialValue: [] });
    this.consumptionUnitsMap = computed(() => new Map(this.consumptionUnits().map((u: EnumOption) => [u.value, u.viewValue])));

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: [''],
    });

    if (!materialTypesSearchUrl) {
      this.formControl?.disable();
    }

    // --- Reação às mudanças no serviço ---
    const materialTypesResponse = toSignal(
      this.materialTypeService.getTiposMateriaPrima().pipe(
        catchError(() => {
          this.isSearching.set(false);
          return of(undefined);
        })
      )
    );

    effect(() => {
      this.isSearching.set(false);
      const response = materialTypesResponse();
      if (response) {
        const newItems = response._embedded?.['tipos-materia-prima'] ?? [];
        if (response.page.number === 0) {
          this.materialTypes.set(newItems);
        } else {
          this.materialTypes.update(current => [...current, ...newItems]);
        }
        this.totalElements.set(response.page.totalElements);

        // Descobre a URL das unidades a partir da primeira resposta
        const firstMaterial = newItems[0];
        const newUnitsUrl = firstMaterial?._links?.['unidades-de-medida']?.href;
        if (newUnitsUrl && this.unitsUrl() !== newUnitsUrl) {
          this.unitsUrl.set(newUnitsUrl);
        }
      }
    });
  }

  ngOnInit(): void {
    this.originalMateriaPrima = this.formControl.value;
    if (this.formControl.value) {
      this.materialTypes.set([this.formControl.value as TipoMateriaPrima]);
    }

    // Conecta o formulário de busca ao serviço
    this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(values => {
      this.performSearch(values.searchName, values.searchUnit);
    });

    // Busca inicial
    this.performSearch();
  }

  performSearch(nome?: string, unidadeDeConsumo?: string): void {
    this.isSearching.set(true);
    this.currentPage = 0;
    this.materialTypeService.updateSearchParams({
      page: this.currentPage,
      size: this.pageSize,
      sort: 'nome,asc',
      nome: nome,
      unidadeDeConsumo: unidadeDeConsumo
    });
  }

  loadMore(): void {
    if (this.isSearching() || this.materialTypes().length >= this.totalElements()) {
      return;
    }

    this.isSearching.set(true);
    this.currentPage++;
    this.materialTypeService.updateSearchParams({ page: this.currentPage });
  }

  getConsumptionUnitViewValue(value: string): string {
    return this.consumptionUnitsMap().get(value) ?? value;
  }

  compareMaterialTypes(o1: TipoMateriaPrima, o2: TipoMateriaPrima): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  onSelectionChange(event: MatSelectChange): void {
    this.selectionChange.emit(event);
  }
}
