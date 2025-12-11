import { Component, OnInit, inject, effect, ChangeDetectionStrategy, ChangeDetectorRef, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';
import { Observable, lastValueFrom, of } from 'rxjs';
import { startWith, map, debounceTime, distinctUntilChanged, catchError } from 'rxjs/operators';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { LoteMateriaPrima, LoteMateriaPrimaRequest } from '../../models/lote-materia-prima.model';
import { TipoMateriaPrima } from '../../models/material-type.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InfiniteScrollDirective } from '../../services/infinite-scroll.directive';

export interface LoteMateriaPrimaFormData {
  template: LoteMateriaPrima;
  title: string;
}

export interface UnidadeOption {
  name: string;
  descricao: string;
}

/**
 * Validador para garantir que a unidade selecionada é válida.
 */
export function requireMatchUnidade(options: UnidadeOption[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) { return null; }
    const valueAsString = typeof value === 'string' ? value : value.name;
    const match = options.some(option => option.name === valueAsString);
    return match ? null : { requireMatchUnidade: true };
  };
}

/**
 * Formulário para criação e edição de Lotes de Matéria-Prima.
 *
 * Utiliza arquitetura reativa com Signals e OnPush.
 * Gerencia a busca paginada de tipos de matéria-prima e o carregamento dinâmico de unidades.
 */
@Component({
  selector: 'app-lote-materia-prima-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatAutocompleteModule, MatSelectModule, MatIconModule,
    MatProgressSpinnerModule, InfiniteScrollDirective
  ],
  templateUrl: './lote-materia-prima-form.html',
  styleUrls: ['./lote-materia-prima-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoteMateriaPrimaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaForm>);
  private readonly loteMateriaPrimaService = inject(LoteMateriaPrimaService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef); // Injetado para limpeza
  public readonly data: LoteMateriaPrimaFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isEditMode = signal(false);

  // --- Estado Reativo para Tipos de Matéria-Prima ---
  tiposMateriaPrima = signal<TipoMateriaPrima[]>([]);
  searchForm: FormGroup;
  isLoadingTipos = signal(false);
  totalElementsTipos = signal(0);
  private currentPage = 0;
  private readonly pageSize = 20;

  // --- Estado Reativo para Unidades ---
  unidades$!: Observable<UnidadeOption[]>;
  unidades = signal<UnidadeOption[]>([]);

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: ['']
    });

    this.form = this.fb.group({
      tipoMateriaPrimaId: [this.data.template?.tipoMateriaPrimaId || '', Validators.required],
      unidadeDeEstoque: ['', [Validators.required]],
      quantidadeInicial: [this.data.template?.saldoEstoque || '', [Validators.required, Validators.min(0.01)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01)]],
      motivo: [this.data.template?.motivo || '', Validators.required],
      atributos: this.fb.array([])
    });

    // Reage às mudanças na lista de tipos de matéria-prima do serviço
    const tiposResponse = toSignal(
      this.materialTypeService.getTiposMateriaPrima().pipe(
        catchError(() => {
          this.entityDialog.showErrorSnackbar('Falha ao carregar tipos de matéria-prima.');
          return of(undefined);
        })
      )
    );

    effect(() => {
      this.isLoadingTipos.set(false);
      const response = tiposResponse();
      if (response) {
        const newItems = response._embedded?.['tipos-materia-prima'] ?? [];
        if (response.page.number === 0) {
          this.tiposMateriaPrima.set(newItems);
        } else {
          this.tiposMateriaPrima.update(current => [...current, ...newItems]);
        }
        this.totalElementsTipos.set(response.page.totalElements);
      }
    });
  }

  async ngOnInit(): Promise<void> {
    await this.initializeForm();

    this.unidades$ = this.form.get('unidadeDeEstoque')!.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef), // Limpeza automática
      startWith(''),
      map(value => (typeof value === 'string' ? this._filterUnidades(value) : this.unidades().slice()))
    );

    // Conecta a busca de tipos ao serviço
    this.searchForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(values => {
      this.performSearchTipos(values.searchName, values.searchUnit);
    });
  }

  async initializeForm(): Promise<void> {
    this.performSearchTipos(); // Busca inicial

    // Se estiver editando, garante que o tipo selecionado esteja na lista
    if (this.isEditMode() && this.data.template.tipoMateriaPrimaId) {
      const tipoSelecionado = await lastValueFrom(this.materialTypeService.findById(this.data.template.tipoMateriaPrimaId));
      if (tipoSelecionado && !this.tiposMateriaPrima().some(t => t.id === tipoSelecionado.id)) {
        this.tiposMateriaPrima.update(current => [tipoSelecionado, ...current]);
      }
    }

    this.loadUnidadesDeEstoque();

    if (this.isEditMode() && this.data.template.atributos) {
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        this.addAtributo(key, value as string);
      });
    }

    // A unidade de estoque será setada após o carregamento das opções em loadUnidadesDeEstoque
  }

  get atributos(): FormArray {
    return this.form.get('atributos') as FormArray;
  }

  get atributosControls(): FormGroup[] {
    return (this.form.get('atributos') as FormArray).controls as FormGroup[];
  }

  addAtributo(chave: string = '', valor: string = ''): void {
    this.atributos.push(this.fb.group({
      chave: [chave, Validators.required],
      valor: [valor, Validators.required]
    }));
  }

  removeAtributo(index: number): void {
    this.atributos.removeAt(index);
  }

  // --- Lógica para Tipo de Matéria-Prima ---

  performSearchTipos(nome?: string, unidade?: string): void {
    this.isLoadingTipos.set(true);
    this.currentPage = 0;
    this.materialTypeService.updateSearchParams({
      page: this.currentPage,
      size: this.pageSize,
      sort: 'nome,asc',
      nome: nome,
      unidadeDeConsumo: unidade
    });
  }

  loadMoreTiposMateriaPrima(): void {
    if (this.isLoadingTipos() || this.tiposMateriaPrima().length >= this.totalElementsTipos()) {
      return;
    }

    this.isLoadingTipos.set(true);
    this.currentPage++;
    this.materialTypeService.updateSearchParams({ page: this.currentPage });
  }

  compareTiposMateriaPrima(o1: TipoMateriaPrima | number, o2: TipoMateriaPrima | number): boolean {
    const id1 = typeof o1 === 'number' ? o1 : o1?.id;
    const id2 = typeof o2 === 'number' ? o2 : o2?.id;
    return id1 === id2;
  }

  // --- Lógica para Unidade de Estoque ---

  loadUnidadesDeEstoque(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href;
    if (url) {
      this.http.get<any>(url).subscribe(response => {
        const embedded = response._embedded;
        if (embedded && embedded.unidadesDeMedida) {
          const unidades: UnidadeOption[] = embedded.unidadesDeMedida.map((item: any) => ({ name: item.name, descricao: item.descricao }));
          this.unidades.set(unidades);

          this.form.get('unidadeDeEstoque')?.setValidators([Validators.required, requireMatchUnidade(unidades)]);

          if (this.isEditMode() && this.data.template.unidadeDeEstoque) {
            const unidadeInicial = unidades.find(u => u.name === this.data.template.unidadeDeEstoque);
            this.form.get('unidadeDeEstoque')?.setValue(unidadeInicial);
          }
          this.form.get('unidadeDeEstoque')?.updateValueAndValidity();

          // Importante: Notifica o Angular para atualizar a view após a resposta assíncrona
          this.cdr.markForCheck();
        }
      });
    }
  }

  private _filterUnidades(value: string): UnidadeOption[] {
    const filterValue = value.toLowerCase();
    return this.unidades().filter(unidade => unidade.descricao.toLowerCase().includes(filterValue));
  }

  displayUnidade(unidade: UnidadeOption): string {
    return unidade?.descricao || '';
  }

  // --- Ações do Formulário ---

  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = this.form.getRawValue();
    const atributosMap: { [key: string]: string } = {};
    (formValue.atributos || []).forEach((attr: { chave: string; valor: string }) => {
      if (attr.chave) {
        atributosMap[attr.chave] = attr.valor;
      }
    });
    formValue.atributos = atributosMap;
    formValue.unidadeDeEstoque = formValue.unidadeDeEstoque.name;

    const request: LoteMateriaPrimaRequest = formValue;

    const operation = this.isEditMode()
      ? this.loteMateriaPrimaService.update(this.data.template._links!['update']!.href, request)
      : this.loteMateriaPrimaService.create(request);

    operation.subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(this.isEditMode() ? 'Lote atualizado com sucesso!' : 'Lote cadastrado com sucesso!');
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Falha ao salvar lote:', err);
        this.entityDialog.showErrorSnackbar('Falha ao salvar. Verifique os dados e tente novamente.');
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
