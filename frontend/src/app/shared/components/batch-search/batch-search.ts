import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';

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

  // --- Controles de Formulário Internos ---
  searchControl = new FormControl<string | Batch | null>('');

  // --- Estado Interno ---
  foundBatches = toSignal(this.batchService.batches$, {
    initialValue: {
      _embedded: { 'lotes-materia-prima': [] },
      page: { size: 0, totalElements: 0, totalPages: 0, number: 0 },
      _links: {}
    } as ApiResponseBatches
  });
  readonly isSearching = this.batchService.isSearching;
  private readonly unitsUrl = signal<string | null>(null);

  // Subject para controlar quando disparar a busca
  private readonly searchTrigger$ = new Subject<void>();

  readonly measurementUnitOptions = toSignal(
    toObservable(this.unitsUrl).pipe(
      distinctUntilChanged(),
      filter((url): url is string => !!url),
      switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))
    ),
    { initialValue: [] }
  );

  constructor() {
    // Reage a mudanças no `tipoMateriaPrimaId` (vindo do pai) para disparar uma nova busca.
    effect(() => {
      this.tipoMateriaPrimaId();
      this.searchTrigger$.next();
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
    ).subscribe(() => {
      this.searchTrigger$.next();
    });

    // Processa o trigger de busca centralizado
    this.searchTrigger$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.triggerSearchNow();
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
  private triggerSearchNow(): void {
    if (this.disabled()) {
      return;
    }
    const searchTerm = typeof this.searchControl.value === 'string' ? this.searchControl.value : null;

    this.batchService.updateSearchParams({
      nome: searchTerm,
      tipoMateriaPrimaId: this.tipoMateriaPrimaId(),
      page: 0
    });
  }

  /**
   * Formata como o lote é exibido no input e nas opções do autocomplete.
   */
  displayFn = (value: Batch | string | null): string => {
    if (!value) return '';
    if (typeof value === 'string') return value;

    const batch = value;
    const unidadeApresentacao = batch.unidadeCadastroEstoque ?? batch.unidadeDeEstoque;

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
      if (batch.saldoEstoque && larguraMmVal) {
        // Comprimento em mm: saldoEstoque (m²) * 1_000_000 / larguraMm (mm)
        const comprimentoMmVal = (batch.saldoEstoque * 1000000) / larguraMmVal;
        comprimentoMm = nf.format(comprimentoMmVal);
        comprimentoCm = nf.format(comprimentoMmVal / 10);
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
    this.control().setValue(selected.id);
    this.selectionChange.emit(selected);
  }

  public setSelectedBatch(batch: Batch | null): void {
    this.control().setValue(batch?.id ?? null);
    this.searchControl.setValue(batch, { emitEvent: false });
  }

  /**
   * Método público para resetar o componente ao estado inicial.
   * Limpa o campo de busca e o controle do formulário pai.
   */
  public reset(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.control().setValue(null);
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
