import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { HttpClient } from '@angular/common/http';
import { Observable, startWith, map } from 'rxjs';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';

// Definição dos dados que o diálogo espera receber
export interface MaterialTypeFormData {
  template: TipoMateriaPrima; // O "esqueleto" ou o item a ser editado
  title: string;
}

// Interface para o nosso objeto de unidade
export interface UnidadeOption {
  name: string;
  descricao: string;
}

// Validador customizado
export function requireMatch(options: UnidadeOption[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) {
      return null;
    }
    const valueAsString = typeof value === 'string' ? value : value.name;
    const match = options.some(option => option.name === valueAsString);
    return match ? null : { requireMatch: true };
  };
}

@Component({
  selector: 'app-material-type-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatAutocompleteModule,
    MatDialogModule
  ],
  templateUrl: './material-type-form.component.html',
  styleUrls: ['./material-type-form.component.scss']
})
export class MaterialTypeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly dialogRef = inject(MatDialogRef<MaterialTypeFormComponent>);
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  form!: FormGroup;
  unidades$!: Observable<UnidadeOption[]>;
  unidades: UnidadeOption[] = [];

  ngOnInit(): void {
    this.form = this.fb.group({
      nome: [this.data.template?.nome || '', Validators.required],
      unidadeDeConsumo: ['', [Validators.required]]
    });

    this.loadUnidadesDeMedida();

    this.unidades$ = this.form.get('unidadeDeConsumo')!.valueChanges.pipe(
      startWith(''),
      map(value => {
        // Se o valor for uma string, o usuário está digitando. Filtramos.
        if (typeof value === 'string') {
          return this._filterUnidades(value);
        }
        // Se não for string (é um objeto ou nulo), mostramos a lista completa.
        return this.unidades.slice();
      })
    );
  }

  loadUnidadesDeMedida(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href;
    if (url) {
      this.http.get<any>(url).subscribe(response => {
        const embedded = response._embedded;
        if (embedded && embedded.unidadesDeMedida) {
          this.unidades = embedded.unidadesDeMedida.map((item: any) => ({ name: item.name, descricao: item.descricao }));
          this.form.get('unidadeDeConsumo')?.setValidators([Validators.required, requireMatch(this.unidades)]);
          if (this.data.template?.unidadeDeConsumo) {
            const unidadeInicial = this.unidades.find(u => u.name === this.data.template.unidadeDeConsumo);
            this.form.get('unidadeDeConsumo')?.setValue(unidadeInicial);
          }
          this.form.get('unidadeDeConsumo')?.updateValueAndValidity();
        }
      });
    }
  }

  private _filterUnidades(value: string): UnidadeOption[] {
    const filterValue = value.toLowerCase();
    return this.unidades.filter(unidade => unidade.descricao.toLowerCase().includes(filterValue));
  }

  displayUnidade(unidade: UnidadeOption): string {
    return unidade && unidade.descricao ? unidade.descricao : '';
  }

  onSave(): void {
    if (this.form.valid) {
      const formValue = { ...this.form.value };
      // Garante que estamos enviando apenas a 'key' do enum, e não o objeto inteiro
      if (formValue.unidadeDeConsumo && typeof formValue.unidadeDeConsumo === 'object') {
        formValue.unidadeDeConsumo = formValue.unidadeDeConsumo.name;
      }
      this.dialogRef.close(formValue);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
