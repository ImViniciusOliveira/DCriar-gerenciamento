import { Component, DestroyRef, effect, inject, input, OnInit, output, signal, WritableSignal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { debounceTime, distinctUntilChanged, take, filter } from 'rxjs/operators';
import { MaterialType } from '../../../features/stock/models/material-type.model';
import { MaterialTypeService } from '../../../features/stock/services/material-type.service';
import { EnumOption, EnumService } from '../../../core/services/enum.service';

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
  /** Quando informado, fixa o contexto do dropdown em CORTE ou CONSUMO. */
  fixedProductType = input<'CORTE' | 'CONSUMO' | null>(null);
  /** URL da coleção de unidades para complementar as opções faltantes. */
  unitsUrl = input<string | null>(null);
  /** Emite o evento de seleção para o componente pai. */
  selectionChange = output<MatSelectChange>();

  // --- Injeção de Dependências ---
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly enumService = inject(EnumService);
  private readonly destroyRef = inject(DestroyRef);
  private hasInitialized = false;
  private lastAppliedProductType: 'CORTE' | 'CONSUMO' | null = null;

  // --- Controles de Formulário Internos ---
  // O searchControl pode conter o texto digitado (string) ou o objeto selecionado (MaterialType)
  searchControl = new FormControl<string | MaterialType | null>('');
  productTypeControl = new FormControl<'CORTE' | 'CONSUMO'>('CORTE', { nonNullable: true });
  consumptionUnitControl = new FormControl<string | null>(null);

  // --- Estado Interno ---
  materialTypes: WritableSignal<MaterialType[]> = signal([]);
  unitOptions: WritableSignal<EnumOption[]> = signal([]);
  private readonly corteMaterialTypes = signal<MaterialType[]>([]);
  private readonly consumoMaterialTypes = signal<MaterialType[]>([]);
  private readonly backendUnitMap = signal<Map<string, EnumOption>>(new Map());

  readonly productTypeOptions = [
    { value: 'CORTE' as const, label: 'Matérias-primas de Corte' },
    { value: 'CONSUMO' as const, label: 'Matérias-primas de Consumo' }
  ];
  private readonly allUnitsOption: EnumOption = {
    value: 'TODOS',
    viewValue: 'Todos'
  };

  constructor() {
    effect(() => {
      const productType = this.getSelectedProductType();
      const previousProductType = this.lastAppliedProductType;
      this.lastAppliedProductType = productType;

      if (this.fixedProductType()) {
        this.productTypeControl.setValue(productType, { emitEvent: false });
      }

      this.syncUnitOptions();

      if (this.hasInitialized && previousProductType && previousProductType !== productType) {
        this.resetSelection();
      }
    });
  }

  ngOnInit(): void {
    const ctrl = this.control();
    this.loadBackendUnits();
    this.loadMaterialTypes();

    ctrl.valueChanges.pipe(
      filter((value: string | MaterialType | null): value is MaterialType => !!value && typeof value !== 'string'),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(selectedMaterialType => {
      this.searchControl.setValue(selectedMaterialType, { emitEvent: false });
      this.materialTypes.set([selectedMaterialType]);
      this.syncUnitSelection(selectedMaterialType.unidadeDeConsumo, false);
    });

    // Sincroniza o valor inicial do pai com o input de busca
    if (ctrl.value) {
      this.searchControl.setValue(ctrl.value);
      this.materialTypes.set([ctrl.value]);
      this.syncUnitSelection(ctrl.value.unidadeDeConsumo, false);
    } else if (this.isEditMode()) {
      ctrl.valueChanges.pipe(
        filter((value: string | MaterialType | null): value is MaterialType => !!value && typeof value !== 'string'),
        take(1),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(initialValue => {
        this.searchControl.setValue(initialValue);
        this.materialTypes.set([initialValue]);
        this.syncUnitSelection(initialValue.unidadeDeConsumo, false);
      });
    }

    // Busca ao digitar no input (apenas se for string, ou seja, usuário digitando)
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (typeof value === 'string') {
        this.applyFilters(value);
      }
    });

    this.productTypeControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(tipoProduto => {
      if (this.fixedProductType() && tipoProduto !== this.fixedProductType()) {
        this.productTypeControl.setValue(this.fixedProductType()!, { emitEvent: false });
        return;
      }

      this.syncUnitOptions();
      this.resetSelection();
    });

    this.consumptionUnitControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(selectedUnit => {
      const selectedMaterial = this.control().value;
      if (selectedMaterial && typeof selectedMaterial !== 'string' && selectedMaterial.unidadeDeConsumo !== selectedUnit) {
        this.control().setValue(null);
      }
      this.applyFilters(typeof this.searchControl.value === 'string' ? this.searchControl.value : '');
    });

    this.hasInitialized = true;
  }

  private getSelectedProductType(): 'CORTE' | 'CONSUMO' {
    return this.fixedProductType() ?? this.productTypeControl.value;
  }

  private loadMaterialTypes(): void {
    this.materialTypeService.searchByProductType({
      tipoProduto: 'CORTE',
      page: 0,
      size: 200,
      sort: 'nome,asc'
    }).pipe(take(1), takeUntilDestroyed(this.destroyRef)).subscribe(response => {
      this.corteMaterialTypes.set(response._embedded?.['tipos-materia-prima'] ?? []);
      this.syncUnitOptions();
      this.applyFilters(typeof this.searchControl.value === 'string' ? this.searchControl.value : '');
    });

    this.materialTypeService.searchByProductType({
      tipoProduto: 'CONSUMO',
      page: 0,
      size: 200,
      sort: 'nome,asc'
    }).pipe(take(1), takeUntilDestroyed(this.destroyRef)).subscribe(response => {
      this.consumoMaterialTypes.set(response._embedded?.['tipos-materia-prima'] ?? []);
      this.syncUnitOptions();
      this.applyFilters(typeof this.searchControl.value === 'string' ? this.searchControl.value : '');
    });
  }

  private loadBackendUnits(): void {
    const url = this.unitsUrl();
    if (!url) {
      return;
    }

    this.enumService.getConsumptionUnitsMap(url).pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(unitsMap => {
      this.backendUnitMap.set(unitsMap);
      this.syncUnitOptions();
      this.applyFilters(typeof this.searchControl.value === 'string' ? this.searchControl.value : '');
    });
  }

  private syncUnitOptions(): void {
    const unitOptionsMap = new Map<string, EnumOption>();
    const backendUnits = this.backendUnitMap();

    this.getActiveMaterialTypes().forEach(item => {
      if (!item.unidadeDeConsumo || unitOptionsMap.has(item.unidadeDeConsumo)) {
        return;
      }

      unitOptionsMap.set(item.unidadeDeConsumo, {
        value: item.unidadeDeConsumo,
        viewValue: item.unidadeDescricao ?? item.unidadeDeConsumo
      });

      const compatibleInputUnit = backendUnits.get(item.unidadeDeConsumo)?.compatibleInputUnit;
      if (compatibleInputUnit && !unitOptionsMap.has(compatibleInputUnit)) {
        const compatibleOption = backendUnits.get(compatibleInputUnit);
        unitOptionsMap.set(compatibleInputUnit, {
          value: compatibleInputUnit,
          viewValue: compatibleOption?.viewValue ?? compatibleInputUnit
        });
      }
    });

    const options = [
      this.allUnitsOption,
      ...Array.from(unitOptionsMap.values())
    ];

    this.unitOptions.set(options);

    const currentUnit = this.consumptionUnitControl.value;
    const hasCurrentUnit = !!currentUnit && options.some(option => option.value === currentUnit);
    if (!hasCurrentUnit) {
      this.consumptionUnitControl.setValue(this.allUnitsOption.value, { emitEvent: false });
    }
  }

  private syncUnitSelection(unit: string | null | undefined, emitEvent = false): void {
    if (!unit) {
      return;
    }

    const available = this.unitOptions();
    if (available.some(option => option.value === unit)) {
      this.consumptionUnitControl.setValue(unit, { emitEvent });
    }
  }

  private getActiveMaterialTypes(): MaterialType[] {
    return this.getSelectedProductType() === 'CORTE'
      ? this.corteMaterialTypes()
      : this.consumoMaterialTypes();
  }

  private applyFilters(searchTerm = ''): void {
    const normalizedSearch = searchTerm.trim().toLowerCase();
    const selectedUnit = this.consumptionUnitControl.value;
    const filtered = this.getActiveMaterialTypes().filter(item => {
      const matchesUnit = !selectedUnit || selectedUnit === this.allUnitsOption.value || item.unidadeDeConsumo === selectedUnit;
      const matchesSearch = !normalizedSearch || item.nome.toLowerCase().includes(normalizedSearch);
      return matchesUnit && matchesSearch;
    });

    this.materialTypes.set(filtered);
  }

  private resetSelection(): void {
    this.control().setValue(null);
    this.searchControl.setValue('', { emitEvent: false });
    this.consumptionUnitControl.setValue(this.allUnitsOption.value, { emitEvent: false });
    this.applyFilters('');
  }

  displayFn(materialType: MaterialType): string {
    return materialType && materialType.nome ? materialType.nome : '';
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as MaterialType;
    this.syncUnitSelection(selected.unidadeDeConsumo, false);
    this.control().setValue(selected);
    // Emite um evento compatível com MatSelectChange para manter compatibilidade
    this.selectionChange.emit({ source: null as any, value: selected });
  }
}
