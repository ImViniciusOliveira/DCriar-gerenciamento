import { Component, OnInit, inject } from '@angular/core';
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
import { Observable, lastValueFrom } from 'rxjs';
import { startWith, map } from 'rxjs/operators';

import { LoteMateriaPrima, LoteMateriaPrimaRequest } from '../../models/lote-materia-prima.model';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { TipoMateriaPrimaService } from '../../services/tipo-materia-prima.service';
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

export function requireMatchUnidade(options: UnidadeOption[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) { return null; }
    const valueAsString = typeof value === 'string' ? value : value.name;
    const match = options.some(option => option.name === valueAsString);
    return match ? null : { requireMatchUnidade: true };
  };
}

@Component({
  selector: 'app-lote-materia-prima-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatAutocompleteModule, MatSelectModule, MatIconModule,
    MatProgressSpinnerModule, InfiniteScrollDirective
  ],
  templateUrl: './lote-materia-prima-form.html',
  styleUrls: ['./lote-materia-prima-form.scss']
})
export class LoteMateriaPrimaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaForm>);
  private readonly loteMateriaPrimaService = inject(LoteMateriaPrimaService);
  private readonly tipoMateriaPrimaService = inject(TipoMateriaPrimaService);
  private readonly entityDialog = inject(EntityDialogService);
  public readonly data: LoteMateriaPrimaFormData = inject(MAT_DIALOG_DATA);

  form!: FormGroup;
  isEditMode = false;

  // Para Tipo de Matéria-Prima
  tiposMateriaPrima: TipoMateriaPrima[] = [];
  tiposMateriaPrimaSearchUrl: string | null = null;
  currentPageTipos = 0;
  pageSizeTipos = 20;
  totalElementsTipos = 0;
  isSearchingTipos = false;

  searchForm: FormGroup; // Formulário de busca para tipos de matéria-prima

  // Para Unidade de Estoque
  unidades$: Observable<UnidadeOption[]> = new Observable<UnidadeOption[]>();
  unidades: UnidadeOption[] = [];

  constructor() {
    this.isEditMode = !!this.data.template.id;

    this.searchForm = this.fb.group({
      searchName: [''],
      searchUnit: ['']
    });
  }

  async ngOnInit(): Promise<void> {
    this.form = this.fb.group({
      tipoMateriaPrimaId: [this.data.template?.tipoMateriaPrimaId || '', Validators.required],
      unidadeDeEstoque: ['', [Validators.required]],
      quantidadeInicial: [this.data.template?.saldoEstoque || '', [Validators.required, Validators.min(0.01)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01)]],
      motivo: [this.data.template?.motivo || '', Validators.required],
      atributos: this.fb.array([])
    });

    await this.initializeForm();

    this.unidades$ = this.form.get('unidadeDeEstoque')!.valueChanges.pipe(
      startWith(''),
      map(value => (typeof value === 'string' ? this._filterUnidades(value) : this.unidades.slice()))
    );
  }

  async initializeForm(): Promise<void> {
    // Obter a URL de busca de tipos de matéria-prima do template
    const url = this.data.template?._links?.['tipos-materia-prima']?.href;
    if (url) {
      this.tiposMateriaPrimaSearchUrl = url.split('{')[0];
    } else {
      console.error("URL para busca de matéria-prima não pôde ser determinada.");
      // Desabilitar o campo de seleção se a URL não for encontrada
      this.form.get('tipoMateriaPrimaId')?.disable();
    }

    await this.performSearchTipos(); // Realiza a busca inicial de tipos

    // Se estiver em modo de edição e o tipo já estiver selecionado, garante que ele esteja na lista
    if (this.isEditMode && this.data.template.tipoMateriaPrimaId) {
      // Busca o tipo de matéria-prima selecionado individualmente para garantir que ele esteja na lista
      const tipoSelecionado = await lastValueFrom(this.tipoMateriaPrimaService.findById(this.data.template.tipoMateriaPrimaId));
      if (tipoSelecionado && !this.tiposMateriaPrima.some(t => t.id === tipoSelecionado.id)) {
        this.tiposMateriaPrima = [tipoSelecionado, ...this.tiposMateriaPrima];
      }
    }

    this.loadUnidadesDeEstoque();

    // Preencher atributos se estiver em modo de edição
    if (this.isEditMode && this.data.template.atributos) {
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        this.addAtributo(key, value as string);
      });
    }

    // Preencher unidade de estoque se estiver em modo de edição
    if (this.isEditMode && this.data.template.unidadeDeEstoque) {
      const unidadeInicial = this.unidades.find(u => u.name === this.data.template.unidadeDeEstoque);
      this.form.get('unidadeDeEstoque')?.setValue(unidadeInicial);
    }
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

  // Lógica para Tipo de Matéria-Prima (com busca e infinite scroll)
  async performSearchTipos(): Promise<void> {
    this.isSearchingTipos = true;
    try {
      this.currentPageTipos = 0;
      this.tiposMateriaPrima = []; // Limpa a lista antes de uma nova busca

      if (!this.tiposMateriaPrimaSearchUrl) {
        console.error('Não é possível buscar matérias-primas: URL não encontrada.');
        return;
      }

      const filters = { searchName: this.searchForm.value.searchName, searchUnit: this.searchForm.value.searchUnit };

      const response = await lastValueFrom(
        this.tipoMateriaPrimaService.findAll(
          this.currentPageTipos,
          this.pageSizeTipos,
          'nome',
          'asc',
          filters.searchName,
          filters.searchUnit
        )
      );

      this.tiposMateriaPrima = response._embedded?.['tipos-materia-prima'] || [];
      this.totalElementsTipos = response.page?.totalElements || 0;
    } catch (err) {
      console.error('Erro na busca por matéria-prima:', err);
      this.entityDialog.showErrorSnackbar('Falha ao buscar tipos de matéria-prima.');
    } finally {
      this.isSearchingTipos = false;
    }
  }

  async loadMoreTiposMateriaPrima(): Promise<void> {
    if (this.isSearchingTipos || this.tiposMateriaPrima.length >= this.totalElementsTipos) {
      return;
    }

    this.isSearchingTipos = true;
    this.currentPageTipos++;

    try {
      if (!this.tiposMateriaPrimaSearchUrl) return;

      const filters = { searchName: this.searchForm.value.searchName, searchUnit: this.searchForm.value.searchUnit };

      const response = await lastValueFrom(this.tipoMateriaPrimaService.findAll(
        this.currentPageTipos,
        this.pageSizeTipos,
        'nome',
        'asc',
        filters.searchName,
        filters.searchUnit
      ));

      const newTipos = response._embedded?.['tipos-materia-prima'] || [];
      this.tiposMateriaPrima = [...this.tiposMateriaPrima, ...newTipos];
    } catch (err) {
      console.error('Erro ao carregar mais matérias-primas:', err);
      this.entityDialog.showErrorSnackbar('Falha ao carregar mais tipos de matéria-prima.');
    } finally {
      this.isSearchingTipos = false;
    }
  }

  compareTiposMateriaPrima(o1: TipoMateriaPrima | number, o2: TipoMateriaPrima | number): boolean {
    if (typeof o1 === 'number' && typeof o2 === 'number') {
      return o1 === o2;
    }
    if (typeof o1 === 'object' && typeof o2 === 'object') {
      return o1 && o2 ? o1.id === o2.id : o1 === o2;
    }
    return false;
  }

  // Lógica para Unidade de Estoque
  loadUnidadesDeEstoque(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href;
    if (url) {
      this.http.get<any>(url).subscribe(response => {
        const embedded = response._embedded;
        if (embedded && embedded.unidadesDeMedida) {
          this.unidades = embedded.unidadesDeMedida.map((item: any) => ({ name: item.name, descricao: item.descricao }));
          this.form.get('unidadeDeEstoque')?.setValidators([Validators.required, requireMatchUnidade(this.unidades)]);
          if (this.isEditMode && this.data.template.unidadeDeEstoque) {
            const unidadeInicial = this.unidades.find(u => u.name === this.data.template.unidadeDeEstoque);
            this.form.get('unidadeDeEstoque')?.setValue(unidadeInicial);
          }
          this.form.get('unidadeDeEstoque')?.updateValueAndValidity();
        }
      });
    }
  }

  _filterUnidades(value: string): UnidadeOption[] {
    const filterValue = value.toLowerCase();
    return this.unidades.filter(unidade => unidade.descricao.toLowerCase().includes(filterValue));
  }

  displayUnidade(unidade: UnidadeOption): string {
    return unidade?.descricao || '';
  }

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

    const operation = this.isEditMode
      ? this.loteMateriaPrimaService.update(this.data.template._links!['update']!.href, request)
      : this.loteMateriaPrimaService.create(request);

    operation.subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(this.isEditMode ? 'Lote atualizado com sucesso!' : 'Lote cadastrado com sucesso!');
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
