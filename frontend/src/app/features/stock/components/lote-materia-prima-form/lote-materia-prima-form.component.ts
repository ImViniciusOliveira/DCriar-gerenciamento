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
import { Observable, startWith, map, lastValueFrom } from 'rxjs';
import { LoteMateriaPrima, LoteMateriaPrimaRequest } from '../../models/lote-materia-prima.model';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { TipoMateriaPrimaService } from '../../services/tipo-materia-prima.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { InfiniteScrollDirective } from '../../services/infinite-scroll.directive';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export interface LoteMateriaPrimaFormData {
  template: LoteMateriaPrima;
  title: string;
}

export interface AtributoOption {
  chave: string;
  valor: string;
}

export interface UnidadeOption {
  name: string;
  descricao: string;
}

// Validador customizado para Unidade de Estoque
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
    MatButtonModule, MatAutocompleteModule, MatSelectModule, MatIconModule, InfiniteScrollDirective,
    MatProgressSpinnerModule
  ],
  templateUrl: './lote-materia-prima-form.component.html',
  styleUrls: ['./lote-materia-prima-form.component.scss']
})
export class LoteMateriaPrimaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaFormComponent>);
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

  // Para Unidade de Estoque
  unidades$: Observable<UnidadeOption[]> = new Observable<UnidadeOption[]>();
  unidades: UnidadeOption[] = [];

  constructor() {
    this.isEditMode = !!this.data.template.id;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      tipoMateriaPrimaId: [this.data.template?.tipoMateriaPrimaId || '', Validators.required],
      unidadeDeEstoque: ['', [Validators.required]],
      quantidadeInicial: [this.data.template?.saldoEstoque || '', [Validators.required, Validators.min(0.01)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01)]],
      motivo: [this.data.template?.motivo || '', Validators.required],
      atributos: this.fb.array([])
    });

    this.loadTiposMateriaPrima();
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

  // Lógica para Tipo de Matéria-Prima
  async loadTiposMateriaPrima(): Promise<void> {
    const url = this.data.template?._links?.['tipos-materia-prima']?.href;
    if (url) {
      this.tiposMateriaPrimaSearchUrl = url.split('{')[0];
      const response = await lastValueFrom(this.tipoMateriaPrimaService.findAll(0, this.pageSizeTipos, 'nome', 'asc'));
      this.tiposMateriaPrima = response._embedded?.['tipos-materia-prima'] || [];
      this.totalElementsTipos = response.page?.totalElements || 0;

      // Se estiver em modo de edição, pré-seleciona o tipo de matéria-prima
      if (this.isEditMode && this.data.template.tipoMateriaPrimaId) {
        this.form.get('tipoMateriaPrimaId')?.setValue(this.data.template.tipoMateriaPrimaId);
      }
    }
  }

  async loadMoreTiposMateriaPrima(): Promise<void> {
    if (this.isSearchingTipos || this.tiposMateriaPrima.length >= this.totalElementsTipos) {
      return;
    }
    this.isSearchingTipos = true;
    this.currentPageTipos++;

    const response = await lastValueFrom(this.tipoMateriaPrimaService.findAll(this.currentPageTipos, this.pageSizeTipos, 'nome', 'asc'));
    const newTipos = response._embedded?.['tipos-materia-prima'] || [];
    this.tiposMateriaPrima = [...this.tiposMateriaPrima, ...newTipos];
    this.isSearchingTipos = false;
  }

  compareTiposMateriaPrima(o1: TipoMateriaPrima, o2: TipoMateriaPrima): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
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

  private _filterUnidades(value: string): UnidadeOption[] {
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
