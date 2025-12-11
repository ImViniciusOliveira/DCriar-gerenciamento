import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { HttpClient } from '@angular/common/http';
import { Observable, startWith, map } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { TipoMateriaPrima, TipoMateriaPrimaRequest } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';

export interface MaterialTypeFormData {
  template: TipoMateriaPrima;
  title: string;
}

export interface UnidadeOption {
  name: string;
  descricao: string;
}

/**
 * Validador customizado para garantir que o valor do autocomplete
 * corresponde a uma das opções da lista.
 */
export function requireMatch(options: UnidadeOption[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) { return null; }
    const valueAsString = typeof value === 'string' ? value : value.name;
    const match = options.some(option => option.name === valueAsString);
    return match ? null : { requireMatch: true };
  };
}

@Component({
  selector: 'app-material-type-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatAutocompleteModule, MatDialogModule
  ],
  templateUrl: './material-type-form.html',
  styleUrls: ['./material-type-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MaterialTypeForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<MaterialTypeForm>);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef); // Injetado para controle manual
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  unidades$!: Observable<UnidadeOption[]>;
  unidades = signal<UnidadeOption[]>([]);
  isEditMode = signal(false);

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      nome: [this.data.template?.nome || '', Validators.required],
      unidadeDeConsumo: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadUnidadesDeMedida();

    this.unidades$ = this.form.get('unidadeDeConsumo')!.valueChanges.pipe(
      takeUntilDestroyed(),
      startWith(''),
      map(value => (typeof value === 'string' ? this._filterUnidades(value) : this.unidades().slice()))
    );
  }

  /**
   * Carrega as unidades de medida a partir do link HATEOAS do template.
   */
  loadUnidadesDeMedida(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href;
    if (url) {
      this.http.get<any>(url).subscribe(response => {
        const embedded = response._embedded;
        if (embedded && embedded.unidadesDeMedida) {
          const unidades: UnidadeOption[] = embedded.unidadesDeMedida.map((item: any) => ({ name: item.name, descricao: item.descricao }));
          this.unidades.set(unidades);

          // Re-aplica validadores e valor inicial após carregar as opções
          this.form.get('unidadeDeConsumo')?.setValidators([Validators.required, requireMatch(unidades)]);
          if (this.data.template?.unidadeDeConsumo) {
            const unidadeInicial = unidades.find(u => u.name === this.data.template.unidadeDeConsumo);
            this.form.get('unidadeDeConsumo')?.setValue(unidadeInicial);
          }
          this.form.get('unidadeDeConsumo')?.updateValueAndValidity();

          // AVISA o Angular para verificar este componente, pois uma operação assíncrona terminou.
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

  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = { ...this.form.value };
    formValue.unidadeDeConsumo = formValue.unidadeDeConsumo.name;
    const request: TipoMateriaPrimaRequest = formValue;

    const operation = this.isEditMode()
      ? this.materialTypeService.update(this.data.template._links!['update']!.href, request)
      : this.materialTypeService.create(request);

    operation.subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Falha ao salvar matéria-prima:', err);
        this.entityDialog.showErrorSnackbar('Falha ao salvar. Verifique os dados e tente novamente.');
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
