import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs/operators';

import { MaterialType, MaterialTypeRequest } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { EnumService } from '../../../../core/services/enum.service';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';

export interface MaterialTypeFormData {
  template: MaterialType;
  title: string;
}

export interface UnitOption {
  name: string;
  descricao: string;
}

/**
 * Validador customizado para garantir que o valor do autocomplete corresponde a uma das opções da lista.
 */
export function requireMatch(options: UnitOption[]): ValidatorFn {
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
  private readonly dialogRef = inject(MatDialogRef<MaterialTypeForm>);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly enumService = inject(EnumService);
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  private static readonly Texts = {
    saveError: 'Falha ao salvar. Verifique os dados.',
    loadUnitsError: 'Erro ao carregar unidades.',
    unitsUrlError: 'URL de unidades-de-medida não encontrada.'
  };

  form: FormGroup;
  matcher = new InstantErrorStateMatcher();

  allUnits = signal<UnitOption[]>([]);
  filterValue = signal<string>('');

  /** Signal computado que filtra as unidades com base no valor digitado. */
  filteredUnits = computed(() => {
    const filter = this.filterValue().toLowerCase();
    const units = this.allUnits();
    return units.filter(unit =>
      unit.descricao.toLowerCase().includes(filter)
    );
  });

  isEditMode = signal(false);

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      nome: [this.data.template?.nome || '', [Validators.required, Validators.maxLength(150)]],
      unidadeDeConsumo: ['', [Validators.required]]
    });

    const valueChanges$ = this.form.get('unidadeDeConsumo')!.valueChanges.pipe(startWith(''));
    const valueSignal = toSignal(valueChanges$, { initialValue: '' });

    // Sincroniza o valor do input com o signal de filtro para o autocomplete.
    effect(() => {
      const value = valueSignal();
      const stringValue = typeof value === 'string' ? value : '';
      this.filterValue.set(stringValue);
    });

    this.applyFieldLocks();
  }

  ngOnInit(): void {
    this.loadMeasurementUnits();
  }

  /**
   * Carrega as unidades de medida a partir do link HATEOAS para popular o autocomplete.
   */
  loadMeasurementUnits(): void {
    const url = this.data.template?._links?.['unidades-de-medida']?.href;

    if (!url) {
      return;
    }

    this.enumService.getEnumOptions(url, 'unidadesDeMedida').subscribe({
      next: (options) => {
        const units: UnitOption[] = options.map(option => ({
          name: option.value,
          descricao: option.viewValue
        }));

        this.allUnits.set(units);
        this.form.get('unidadeDeConsumo')?.setValidators([Validators.required, requireMatch(units)]);

        if (this.data.template?.unidadeDeConsumo) {
          const initialUnit = units.find(u => u.name === this.data.template.unidadeDeConsumo);
          this.form.get('unidadeDeConsumo')?.setValue(initialUnit);
        }

        this.form.get('unidadeDeConsumo')?.updateValueAndValidity();
        this.applyFieldLocks();
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Define como o valor do autocomplete será exibido no campo de input.
   */
  displayUnit(unit: UnitOption): string {
    return unit?.descricao || '';
  }

  onFocus(): void {
    const currentValue = this.form.get('unidadeDeConsumo')?.value;
    if (typeof currentValue !== 'string') {
        this.filterValue.set('');
    }
  }

  protected isFieldLocked(field: string): boolean {
    return hasLockedField(this.data.template, field);
  }

  protected getFieldLockReason(field: string): string | null {
    return getLockedFieldReason(this.data.template, field);
  }

  /**
   * Envia os dados do formulário para criação ou atualização do Tipo de Matéria-Prima.
   */
  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = { ...this.form.value };
    formValue.unidadeDeConsumo = formValue.unidadeDeConsumo.name;
    const request: MaterialTypeRequest = formValue;

    const operation = this.isEditMode()
      ? this.materialTypeService.update(this.data.template._links!['update']!.href, request)
      : this.materialTypeService.create(request);

    operation.subscribe({
      next: () => this.dialogRef.close(true),
      error: () => this.entityDialog.showErrorSnackbar(MaterialTypeForm.Texts.saveError)
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private applyFieldLocks(): void {
    if (!this.isEditMode()) {
      return;
    }

    if (this.isFieldLocked('unidadeDeConsumo')) {
      this.form.get('unidadeDeConsumo')?.disable({ emitEvent: false });
    }
  }
}
