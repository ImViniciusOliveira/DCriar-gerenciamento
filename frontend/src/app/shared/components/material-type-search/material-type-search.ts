import { Component, DestroyRef, effect, inject, input, OnInit, output, signal, Signal, WritableSignal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { of } from 'rxjs';
import { filter, map, switchMap, debounceTime, distinctUntilChanged, catchError, take } from 'rxjs/operators';
import { ApiRoot } from '../../../core/services/api-root';
import { EnumOption, EnumService } from '../../../core/services/enum.service';
import { ApiResponseMaterialTypes, MaterialType } from '../../../features/stock/models/material-type.model';
import { MaterialTypeService } from '../../../features/stock/services/material-type.service';

/**
 * Componente genérico para busca e seleção de um Tipo de Matéria-Prima.
 * Utiliza Autocomplete para combinar busca e seleção em um único campo, otimizando o espaço.
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
    MatAutocompleteModule
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
  /** Emite o evento de seleção para o componente pai. */
  selectionChange = output<MatSelectChange>();

  // --- Injeção de Dependências ---
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly apiRoot = inject(ApiRoot);
  private readonly enumService = inject(EnumService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Controles de Formulário Internos ---
  // O searchControl pode conter o texto digitado (string) ou o objeto selecionado (MaterialType)
  searchControl = new FormControl<string | MaterialType | null>('');
  unitControl = new FormControl<string | null>('');

  // --- Estado Interno ---
  materialTypes: WritableSignal<MaterialType[]> = signal([]);
  isSearching = signal(false);
  totalElements = signal(0);

  // --- Paginação ---
  private currentPage = 0;
  private readonly pageSize = 20;

  // --- Sinais Computados e de Suporte ---
  private readonly unitsUrl = signal<string | null>(null);
  readonly consumptionUnits: Signal<EnumOption[]>;

  constructor() {
    const getUrl = (link: string) =>
      this.apiRoot.endpoints()?._links?.[link]?.href?.split('{')[0];
    const materialTypesSearchUrl = getUrl('tipos-materia-prima') ?? null;

    if (!materialTypesSearchUrl) {
      console.error('URL para busca de matéria-prima não pôde ser determinada.');
    }

    // Desabilita o controle se a URL da API não for encontrada.
    effect(() => {
       if (!materialTypesSearchUrl) {
         this.control().disable();
         this.searchControl.disable();
       }
    });

    // Busca as unidades de consumo.
    const consumptionUnits$ = toObservable(this.unitsUrl).pipe(
      filter((url): url is string => !!url),
      switchMap(url => this.enumService.getConsumptionUnitsMap(url)),
      map(unitsMap => Array.from(unitsMap.values())),
    );
    this.consumptionUnits = toSignal(consumptionUnits$, { initialValue: [] });

    const materialTypesResponse = toSignal(
      this.materialTypeService.getMaterialTypes().pipe(catchError(() => of(undefined)))
    );

    // Reage à resposta do serviço.
    effect(() => {
      this.isSearching.set(false);
      const response: ApiResponseMaterialTypes | undefined = materialTypesResponse();
      if (response) {
        const newItems = response._embedded?.['tipos-materia-prima'] ?? [];
        if (response.page.number === 0) {
          this.materialTypes.set(newItems);
        } else {
          this.materialTypes.update(current => [...current, ...newItems]);
        }
        this.totalElements.set(response.page.totalElements);

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

    // Sincroniza o valor inicial do pai com o input de busca
    if (ctrl.value) {
      this.searchControl.setValue(ctrl.value);
      this.materialTypes.set([ctrl.value]);
    } else if (this.isEditMode()) {
      ctrl.valueChanges.pipe(
        filter(value => !!value),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((initialValue: MaterialType) => {
        this.searchControl.setValue(initialValue);
        this.materialTypes.update(currentTypes => {
            const exists = currentTypes.some(t => t.id === initialValue.id);
            return exists ? currentTypes : [initialValue, ...currentTypes];
        });
      });
    }

    // Busca ao digitar no input (apenas se for string, ou seja, usuário digitando)
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (typeof value === 'string') {
        this.performSearch(value, this.unitControl.value || undefined);
      }
    });

    // Busca ao mudar o filtro de unidade
    this.unitControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(unit => {
      const searchValue = typeof this.searchControl.value === 'string' ? this.searchControl.value : '';
      this.performSearch(searchValue, unit || undefined);
    });

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

  displayFn(materialType: MaterialType): string {
    return materialType && materialType.nome ? materialType.nome : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as MaterialType;
    this.control().setValue(selected);
    // Emite um evento compatível com MatSelectChange para manter compatibilidade
    this.selectionChange.emit({ source: null as any, value: selected });
  }
}
