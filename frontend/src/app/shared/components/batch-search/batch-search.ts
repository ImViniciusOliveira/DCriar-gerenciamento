import { Component, DestroyRef, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { Subject, of, EMPTY } from 'rxjs';

import { EnumService } from '../../../core/services/enum.service';
import { ApiResponseBatches, Batch } from '../../../features/stock/models/batch.model';
import { BatchService } from '../../../features/stock/services/batch.service';

/**
 * Componente genérico para busca e seleção de Lotes de Matéria-Prima.
 * Utiliza Autocomplete para combinar busca e seleção em um único campo.
 */
@Component({
  selector: 'app-batch-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './batch-search.html',
  styleUrls: ['./batch-search.scss']
})
export class BatchSearch {
  // --- Entradas e Saídas ---
  control = input.required<FormControl>();
  tipoMateriaPrimaId = input<number | null>(null);
  disabled = input(false);
  selectionChange = output<Batch>();

  // --- Injeção de Dependências ---
  private readonly batchService = inject(BatchService);
  private readonly enumService = inject(EnumService);
  private readonly destroyRef = inject(DestroyRef);
  private previousTipoMateriaPrimaId: number | null | undefined = undefined;
  private readonly filtersAreDirty = signal(false);

  // --- Controles de Formulário Internos ---
  searchControl = new FormControl<string | Batch | null>('');

  // --- Estado Interno ---
  private readonly emptyResponse: ApiResponseBatches = {
    _embedded: { 'lotes-materia-prima': [] },
    page: { size: 0, totalElements: 0, totalPages: 0, number: 0 },
    _links: {}
  };
  protected readonly foundBatches = signal<ApiResponseBatches>(this.emptyResponse);
  readonly isSearching = signal(false);
  private readonly unitsUrl = signal<string | null>(null);
  protected readonly selectedBatch = signal<Batch | null>(null);
  protected readonly availableBatches = computed(() =>
    [...this.foundBatches()._embedded['lotes-materia-prima']]
      .filter(batch => this.getAvailableInternalBalance(batch) > 0)
      .sort((left, right) => this.getAvailableInternalBalance(right) - this.getAvailableInternalBalance(left))
  );
  private readonly searchRequests$ = new Subject<{ term: string; tipoMateriaPrimaId: number | null }>();
  private activeSearchKey: string | null = null;
  private lastLoadedSearchKey: string | null = null;

  readonly measurementUnitOptions = toSignal(
    toObservable(this.unitsUrl).pipe(
      distinctUntilChanged(),
      filter((url): url is string => !!url),
      switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))
    ),
    { initialValue: [] }
  );

  constructor() {
    // Reage a mudanças no contexto externo e apenas marca o autocomplete como desatualizado.
    effect(() => {
      const currentTipoMateriaPrimaId = this.tipoMateriaPrimaId();

      if (this.previousTipoMateriaPrimaId !== undefined && this.previousTipoMateriaPrimaId !== currentTipoMateriaPrimaId) {
        this.filtersAreDirty.set(true);
        this.foundBatches.set(this.emptyResponse);
        this.lastLoadedSearchKey = null;
        this.previousTipoMateriaPrimaId = currentTipoMateriaPrimaId;
        return;
      }

      this.previousTipoMateriaPrimaId = currentTipoMateriaPrimaId;
    });

    effect(() => {
      const batches = this.foundBatches()._embedded['lotes-materia-prima'];
      const url = batches[0]?._links?.['unidades-de-medida']?.href ?? null;

      if (url && this.unitsUrl() !== url) {
        this.unitsUrl.set(url);
      }
    });

    // Gatilho para busca ao digitar no campo principal.
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (!value || typeof value === 'string') {
        this.selectedBatch.set(null);
        if (this.control().value !== null) {
          this.control().setValue(null);
        }
      }

      if (typeof value === 'string') {
        this.queueSearch(value);
      }
    });

    this.searchRequests$.pipe(
      switchMap(({ term, tipoMateriaPrimaId }) => {
        if (this.disabled() || !tipoMateriaPrimaId) {
          this.isSearching.set(false);
          return of(this.emptyResponse);
        }

        const searchKey = `${tipoMateriaPrimaId}:${term}`;
        if (this.activeSearchKey === searchKey) {
          return EMPTY;
        }

        this.activeSearchKey = searchKey;
        this.isSearching.set(true);
        return this.batchService.search({
          nome: term || null,
          tipoMateriaPrimaId,
          page: 0
        });
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: response => {
        this.foundBatches.set(response);
        this.lastLoadedSearchKey = this.activeSearchKey;
        this.activeSearchKey = null;
        this.isSearching.set(false);
      },
      error: () => {
        this.foundBatches.set(this.emptyResponse);
        this.activeSearchKey = null;
        this.isSearching.set(false);
      }
    });

    effect(() => {
      if (this.disabled()) {
        this.searchControl.disable({ emitEvent: false });
      } else {
        this.searchControl.enable({ emitEvent: false });
      }
    });
  }


  /**
   * Verifica se o FormControl externo, passado para o componente, é obrigatório.
   * Isso é usado para exibir o asterisco (*) no mat-form-field interno.
   */
  get isRequired(): boolean {
    return this.control().hasValidator(Validators.required);
  }

  /**
   * Centraliza a lógica de busca, lendo os valores atuais dos controles
   * e enviando-os para o serviço.
   */
  private queueSearch(searchTerm = ''): void {
    const tipoMateriaPrimaId = this.tipoMateriaPrimaId();

    if (!tipoMateriaPrimaId) {
      this.foundBatches.set(this.emptyResponse);
      this.lastLoadedSearchKey = null;
      this.isSearching.set(false);
      return;
    }

    const searchKey = `${tipoMateriaPrimaId}:${searchTerm}`;
    if (!this.filtersAreDirty() && this.lastLoadedSearchKey === searchKey) {
      return;
    }

    this.searchRequests$.next({
      term: searchTerm,
      tipoMateriaPrimaId
    });
  }

  /**
   * Limpa o texto antigo apenas quando o usuário volta a interagir com o autocomplete
   * depois de uma troca externa de tipo de matéria-prima.
   */
  onAutocompleteOpened(): void {
    if (this.disabled()) {
      return;
    }

    if (this.filtersAreDirty()) {
      this.selectedBatch.set(null);
      this.searchControl.setValue('', { emitEvent: false });
      if (this.control().value !== null) {
        this.control().setValue(null);
      }
      this.filtersAreDirty.set(false);
    }

    this.queueSearch(typeof this.searchControl.value === 'string' ? this.searchControl.value : '');
  }

  public markFiltersAsDirty(): void {
    this.selectedBatch.set(null);
    this.filtersAreDirty.set(true);
    this.lastLoadedSearchKey = null;
  }

  /**
   * Formata como o lote é exibido no input e nas opções do autocomplete.
   */
  displayFn = (value: Batch | string | null): string => {
    if (!value) return '';
    if (typeof value === 'string') return value;

    const batch = value;
    const unidadeApresentacao = batch.unidadeCadastroEstoque ?? batch.unidadeDeEstoque;
    const saldoDisponivel = this.getAvailableInternalBalance(batch) > 0;

    // SE for uma unidade de medida de CORTE, usa a formatação original
    if (unidadeApresentacao === 'METRO_QUADRADO' || unidadeApresentacao === 'METRO_LINEAR') {
      const nf = new Intl.NumberFormat('pt-BR');
      // Saldo em m²
      const saldoM2 = batch.saldoEstoque !== undefined ? nf.format(batch.saldoEstoque) : '';
      // Saldo em cm²
      const saldoCm2 = batch.unidadeSimbolo === 'm²' && typeof batch.saldoEstoque === 'number' ? nf.format(batch.saldoEstoque * 10000) : '';
      // Largura em mm e cm
      const larguraMmVal = batch.atributos?.['larguraMm'] ? Number(batch.atributos['larguraMm']) : 0;
      const larguraMm = larguraMmVal ? nf.format(larguraMmVal) : '';
      const larguraCm = larguraMmVal ? nf.format(larguraMmVal / 10) : '';
      // Comprimento calculado a partir do saldo e largura
      let comprimentoMm = '';
      let comprimentoCm = '';
      if (saldoDisponivel && batch.saldoEstoque && larguraMmVal) {
        // Comprimento em mm: saldoEstoque (m²) * 1_000_000 / larguraMm (mm)
        const comprimentoMmVal = (batch.saldoEstoque * 1000000) / larguraMmVal;
        comprimentoMm = nf.format(comprimentoMmVal);
        comprimentoCm = nf.format(comprimentoMmVal / 10);
      }
      if (!saldoDisponivel) {
        return `Saldo: ${saldoM2}${batch.unidadeSimbolo || ''}`;
      }
      // Monta string final
      return `Saldo: ${saldoM2}m² (${saldoCm2}cm²) | Largura: ${larguraMm}mm (${larguraCm}cm) | Comprimento: ${comprimentoMm}mm (${comprimentoCm}cm)`;
    } else {
      // PARA TODAS AS OUTRAS UNIDADES (LITRO, UNIDADE, etc.)
      return `Saldo: ${this.getDisplayUnit(batch)}`;
    }
  };

  /**
   * Chamado quando uma opção é selecionada no autocomplete.
   */
  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    if (this.disabled()) {
      return;
    }
    const selected = event.option.value as Batch;
    this.selectedBatch.set(selected);
    this.control().setValue(selected.id);
    this.selectionChange.emit(selected);
  }

  public setSelectedBatch(batch: Batch | null): void {
    this.selectedBatch.set(batch);
    this.control().setValue(batch?.id ?? null);
    this.searchControl.setValue(batch, { emitEvent: false });
  }

  /**
   * Método público para resetar o componente ao estado inicial.
   * Limpa o campo de busca e o controle do formulário pai.
   */
  public reset(): void {
    this.selectedBatch.set(null);
    this.searchControl.setValue('', { emitEvent: false });
    this.control().setValue(null);
    this.activeSearchKey = null;
    this.lastLoadedSearchKey = null;
    this.filtersAreDirty.set(false);
    this.foundBatches.set(this.emptyResponse);
  }

  protected getSelectedBatchSubtitle(): string | null {
    const batch = this.selectedBatch();
    if (!batch) {
      return null;
    }

    const identifier = batch.identificadorPublico ?? String(batch.id);
    return batch.nomeTipoMateriaPrima
      ? `${batch.nomeTipoMateriaPrima} • ${identifier}`
      : identifier;
  }

  private getAvailableInternalBalance(batch: Batch): number {
    if (typeof batch.saldoInternoAtual === 'number') {
      return batch.saldoInternoAtual;
    }

    return typeof batch.saldoEstoque === 'number' ? batch.saldoEstoque : 0;
  }

  private getDisplayUnit(batch: Batch): string {
    const unidadeApresentacao = batch.unidadeCadastroEstoque ?? batch.unidadeDeEstoque;
    const matchedUnit = this.measurementUnitOptions().find(unit => unit.value === unidadeApresentacao);

    if (matchedUnit) {
      return this.enumService.formatQuantityWithUnit(batch.saldoEstoque || 0, matchedUnit);
    }

    const fallbackUnit = this.enumService.buildFallbackUnitOption(
      batch.unidadeDescricao ?? unidadeApresentacao ?? '',
      undefined,
      batch.unidadeSimbolo,
      !!batch.unidadeSimbolo && batch.unidadeSimbolo !== 'un' && batch.unidadeSimbolo !== 'fl'
    );
    if (fallbackUnit) {
      return this.enumService.formatQuantityWithUnit(batch.saldoEstoque || 0, fallbackUnit);
    }

    return this.enumService.formatQuantityWithUnit(batch.saldoEstoque || 0);
  }

}
