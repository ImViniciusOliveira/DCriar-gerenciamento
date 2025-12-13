import { Component, computed, DestroyRef, effect, inject, input, OnInit, output, signal, Signal, WritableSignal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { of } from 'rxjs';
import { filter, map, switchMap, debounceTime, distinctUntilChanged, catchError, take } from 'rxjs/operators';
import { ApiRoot } from '../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../core/services/enum.service';
import { ApiResponseMaterialTypes, MaterialType } from '../../../features/stock/models/material-type.model';
import { InfiniteScrollDirective } from '../../../features/stock/services/infinite-scroll.directive';
import { MaterialTypeService } from '../../../features/stock/services/material-type.service';

/**
 * Componente genérico para busca e seleção de um Tipo de Matéria-Prima.
 * Inclui filtros, scroll infinito e lida com o estado de carregamento e seleção.
 */
@Component({
  selector: 'app-material-type-search',
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
  templateUrl: './material-type-search.html',
  styleUrls: ['./material-type-search.scss'],
})
export class MaterialTypeSearch implements OnInit {
  // --- Entradas e Saídas ---
  /** O FormControl do formulário pai que este componente irá controlar. */
  control = input.required<FormControl>();
  /** Flag para indicar se o componente está em modo de edição, para lidar com o valor inicial. */
  isEditMode = input(false);
  /** Emite o evento de seleção do `mat-select` para o componente pai. */
  selectionChange = output<MatSelectChange>();

  // --- Injeção de Dependências ---
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // --- Estado Interno ---
  searchForm: FormGroup;
  materialTypes: WritableSignal<MaterialType[]> = signal([]);
  isSearching = signal(false);
  totalElements = signal(0);
  /** Armazena a matéria-prima original para garantir que ela sempre apareça na lista em modo de edição. */
  originalMateriaPrima = signal<MaterialType | undefined>(undefined);

  // --- Paginação ---
  private currentPage = 0;
  private readonly pageSize = 20;

  // --- Sinais Computados e de Suporte ---
  private readonly unitsUrl = signal<string | null>(null);
  readonly consumptionUnits: Signal<EnumOption[]>;
  private readonly consumptionUnitsMap: Signal<Map<string, string | undefined>>;

  constructor() {
    const getUrl = (link: string) =>
      this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];
    const materialTypesSearchUrl = getUrl('tipos-materia-prima') ?? null;

    if (!materialTypesSearchUrl) {
      console.error('URL para busca de matéria-prima não pôde ser determinada.');
    }

    // Desabilita o controle se a URL da API não for encontrada, prevenindo erros.
    effect(() => {
       if (!materialTypesSearchUrl) this.control().disable();
    });

    // Busca as unidades de consumo de forma reativa assim que a URL de enums for descoberta.
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

    const materialTypesResponse = toSignal(
      this.materialTypeService.getMaterialTypes().pipe(catchError(() => of(undefined)))
    );

    // Reage à resposta do serviço, atualizando a lista de itens e o estado de carregamento.
    effect(() => {
      this.isSearching.set(false);
      const response: ApiResponseMaterialTypes | undefined = materialTypesResponse();
      if (response) {
        const newItems = response._embedded?.['tipos-materia-prima'] ?? [];
        // Lógica de paginação: substitui na primeira página, concatena nas seguintes.
        if (response.page.number === 0) {
          this.materialTypes.set(newItems);
        } else {
          this.materialTypes.update(current => [...current, ...newItems]);
        }
        this.totalElements.set(response.page.totalElements);

        // Descobre a URL das unidades de medida a partir da primeira resposta da API.
        // Isso evita a necessidade de buscar a raiz da API novamente.
        const firstMaterial = newItems[0];
        const newUnitsUrl = firstMaterial?._links?.['unidades-de-medida']?.href;
        if (newUnitsUrl && this.unitsUrl() !== newUnitsUrl) {
          this.unitsUrl.set(newUnitsUrl);
        }
      }
    });
  }

  ngOnInit(): void {
    const ctrl = this.control();

    // Lida com o valor inicial do FormControl, que pode ser síncrono ou assíncrono.
    if (ctrl.value) {
      // Caso 1: O valor já existe na inicialização (ex: formulário de criação com template).
      this.originalMateriaPrima.set(ctrl.value);
      this.materialTypes.set([ctrl.value]);
    } else if (this.isEditMode()) {
      // Caso 2: Modo de edição, mas o valor virá depois (carregamento assíncrono).
      // Escuta a primeira emissão de valor válido para defini-lo como o original.
      ctrl.valueChanges.pipe(
        filter(value => !!value), // Ignora valores nulos ou vazios.
        take(1), // Pega apenas o primeiro valor e encerra a subscrição.
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((initialValue: MaterialType) => {
        if (!this.originalMateriaPrima()) {
          this.originalMateriaPrima.set(initialValue);
          // Garante que o valor inicial esteja na lista de opções do select.
          this.materialTypes.update(currentTypes => {
            const exists = currentTypes.some(t => t.id === initialValue.id);
            return exists ? currentTypes : [initialValue, ...currentTypes];
          });
        }
      });
    }

    // Conecta o formulário de filtros ao serviço de busca com debounce para performance.
    this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(values => {
      this.performSearch(values.searchName, values.searchUnit);
    });

    // Realiza a busca inicial ao carregar o componente.
    this.performSearch();
  }

  /** Dispara uma nova busca, resetando a paginação e aplicando os filtros. */
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

  /** Carrega a próxima página de resultados para o scroll infinito. */
  loadMore(): void {
    if (this.isSearching() || this.materialTypes().length >= this.totalElements()) {
      return;
    }
    this.isSearching.set(true);
    this.currentPage++;
    this.materialTypeService.updateSearchParams({ page: this.currentPage });
  }

  /** Retorna o nome de exibição de uma unidade de consumo a partir do seu valor. */
  getConsumptionUnitViewValue(value: string): string {
    return this.consumptionUnitsMap().get(value) ?? value;
  }

  /** Função para o `mat-select` comparar objetos e saber qual está selecionado. */
  compareMaterialTypes(o1: MaterialType, o2: MaterialType): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }
}
