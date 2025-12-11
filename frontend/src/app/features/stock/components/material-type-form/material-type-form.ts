import {
  Component,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  computed,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs/operators';

import { TipoMateriaPrima, TipoMateriaPrimaRequest } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { ApiRoot } from '../../../../core/services/api-root';

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

/**
 * Formulário para criação e edição de Tipos de Matéria-Prima.
 * Utiliza uma arquitetura reativa com Signals para gerenciar o estado do autocomplete.
 */
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
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly apiRoot = inject(ApiRoot);
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;

  // Signal que armazena a lista completa de unidades carregada da API.
  allUnidades = signal<UnidadeOption[]>([]);

  // Signal que armazena o valor digitado pelo usuário no campo de autocomplete.
  filterValue = signal<string>('');

  // Signal computado que filtra as unidades com base no valor digitado.
  // É recalculado automaticamente sempre que `filterValue` ou `allUnidades` mudam.
  filteredUnidades = computed(() => {
    const filter = this.filterValue().toLowerCase();
    const unidades = this.allUnidades();
    return unidades.filter(unidade =>
      unidade.descricao.toLowerCase().includes(filter)
    );
  });

  isEditMode = signal(false);

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      nome: [this.data.template?.nome || '', Validators.required],
      unidadeDeConsumo: ['', [Validators.required]]
    });

    // Converte o Observable de `valueChanges` do campo em um signal.
    const valueChanges$ = this.form.get('unidadeDeConsumo')!.valueChanges.pipe(startWith(''));
    const valueSignal = toSignal(valueChanges$, { initialValue: '' });

    // Efeito que sincroniza o valor do input (que pode ser um objeto ou string)
    // com o signal de filtro (que é sempre uma string).
    effect(() => {
      const value = valueSignal();
      const stringValue = (typeof value === 'string' ? value : value?.descricao || '');
      this.filterValue.set(stringValue);
    });
  }

  ngOnInit(): void {
    this.loadUnidadesDeMedida();
  }

  /**
   * Carrega as unidades de medida a partir do link HATEOAS.
   * Usa o link do template (edição) ou do ApiRoot (criação).
   */
  loadUnidadesDeMedida(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href || this.apiRoot.endpoints()?._links?.['unidades-de-medida']?.href;

    if (!url) {
      console.error('URL de unidades-de-medida não encontrada.');
      return;
    }

    this.http.get<any>(url).subscribe({
      next: (response) => {
        const embedded = response._embedded;
        if (embedded && embedded.unidadesDeMedida) {
          const unidades: UnidadeOption[] = embedded.unidadesDeMedida.map((item: any) => ({ name: item.name, descricao: item.descricao }));

          this.allUnidades.set(unidades);

          this.form.get('unidadeDeConsumo')?.setValidators([Validators.required, requireMatch(unidades)]);

          if (this.data.template?.unidadeDeConsumo) {
            const unidadeInicial = unidades.find(u => u.name === this.data.template.unidadeDeConsumo);
            this.form.get('unidadeDeConsumo')?.setValue(unidadeInicial);
          }

          this.form.get('unidadeDeConsumo')?.updateValueAndValidity();

          // Notifica o Angular para verificar o componente, pois a chamada HTTP é assíncrona.
          this.cdr.markForCheck();
        }
      },
      error: (err) => console.error('Erro ao carregar unidades:', err)
    });
  }

  /**
   * Função para o `[displayWith]` do autocomplete, garantindo que o campo
   * mostre a descrição da unidade em vez do objeto.
   */
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
      next: () => this.dialogRef.close(true),
      error: () => this.entityDialog.showErrorSnackbar('Falha ao salvar. Verifique os dados.')
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
