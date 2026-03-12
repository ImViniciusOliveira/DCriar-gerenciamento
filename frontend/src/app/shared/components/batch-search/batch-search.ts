import { Component, DestroyRef, effect, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';

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
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './batch-search.html',
  styleUrls: ['./batch-search.scss']
})
export class BatchSearch {
  // --- Entradas e Saídas ---
  control = input.required<FormControl>();
  tipoMateriaPrimaId = input<number | null>(null);
  selectionChange = output<Batch>();

  // --- Injeção de Dependências ---
  private readonly batchService = inject(BatchService);
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

  // Subject para controlar quando disparar a busca
  private readonly searchTrigger$ = new Subject<void>();

  constructor() {
    // Reage a mudanças no `tipoMateriaPrimaId` (vindo do pai) para disparar uma nova busca.
    effect(() => {
      this.tipoMateriaPrimaId();
      this.searchTrigger$.next();
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
  displayFn(batch: Batch): string {
    if (!batch) return '';
    const nf = new Intl.NumberFormat('pt-BR');
    // Saldo em m²
    const saldoM2 = batch.saldoEstoque !== undefined ? nf.format(batch.saldoEstoque) : '';
    // Saldo em cm²
    const saldoCm2 = batch.unidadeSimbolo === 'm²' && typeof batch.saldoEstoque === 'number' ? nf.format(batch.saldoEstoque * 10000) : '';
    // Largura em mm e cm
    const larguraMm = batch.atributos?.['larguraMm'] ? batch.atributos['larguraMm'] : '';
    const larguraCm = larguraMm ? nf.format(larguraMm / 10) : '';
    // Comprimento calculado a partir do saldo e largura
    let comprimentoMm = '';
    let comprimentoCm = '';
    if (batch.saldoEstoque && larguraMm) {
      // Comprimento em mm: saldoEstoque (m²) * 1_000_000 / larguraMm (mm)
      const comprimentoMmVal = (batch.saldoEstoque * 1000000) / larguraMm;
      comprimentoMm = nf.format(comprimentoMmVal);
      comprimentoCm = nf.format(comprimentoMmVal / 10);
    }
    // Monta string final
    return `Saldo: ${saldoM2}m² (${saldoCm2}cm²) | Largura: ${larguraMm}mm (${larguraCm}cm) | Comprimento: ${comprimentoMm}mm (${comprimentoCm}cm)`;
  }

  /**
   * Chamado quando uma opção é selecionada no autocomplete.
   */
  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const selected = event.option.value as Batch;
    this.control().setValue(selected.id);
    this.selectionChange.emit(selected);
  }

  /**
   * Método público para resetar o componente ao estado inicial.
   * Limpa o campo de busca e o controle do formulário pai.
   */
  public reset(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.control().setValue(null);
  }
}
