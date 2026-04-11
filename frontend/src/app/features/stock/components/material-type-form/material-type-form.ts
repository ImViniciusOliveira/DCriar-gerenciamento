import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { lastValueFrom } from 'rxjs';
import { startWith } from 'rxjs/operators';

import { MaterialType, MaterialTypeRequest } from '../../models/material-type.model';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { EnumService } from '../../../../core/services/enum.service';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';
import { applyApiFieldErrors, clearApiFieldErrors } from '../../../../shared/utils/api-errors';
import { clearControlError } from '../../../../shared/utils/control-errors';
import { stockApiErrorOptions } from '../../utils/stock-api-errors';
import { POSITIVE_DECIMAL_4_PATTERN } from '../../../../shared/utils/number-patterns';

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

function maxIntegerDigits(maxDigits: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const integerPart = String(control.value).split(/[.,]/)[0].replace(/^-/, '');
    return integerPart.length > maxDigits
      ? { maxIntegerDigits: { requiredDigits: maxDigits, actualDigits: integerPart.length } }
      : null;
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
  private static readonly NO_CHANGES_MESSAGE = 'Nenhuma alteração detectada.';
  private static readonly BACKEND_FIELD_MAP: Record<string, string> = {
    nome: 'nome',
    unidadeDeConsumo: 'unidadeDeConsumo',
    estoqueCritico: 'estoqueCritico',
    estoqueAceitavel: 'estoqueAceitavel'
  };

  private static readonly BACKEND_ERROR_FIELDS = Object.values(MaterialTypeForm.BACKEND_FIELD_MAP);

  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<MaterialTypeForm>);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly enumService = inject(EnumService);
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  private static readonly Texts = {
    saveError: 'Falha ao salvar. Verifique os dados.',
    loadError: 'Não foi possível carregar os dados completos da matéria-prima.',
    loadUnitsError: 'Erro ao carregar unidades.',
    unitsUrlError: 'URL de unidades-de-medida não encontrada.',
    formValidationError: 'Revise os campos destacados.'
  };

  form: FormGroup;
  matcher = new InstantErrorStateMatcher();

  allUnits = signal<UnitOption[]>([]);
  filterValue = signal<string>('');
  currentMaterialType = signal<MaterialType>(this.data.template);
  isInitializing = signal(false);

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
      unidadeDeConsumo: ['', [Validators.required]],
      estoqueCritico: [
        this.formatDecimal(this.data.template?.estoqueCritico),
        [Validators.min(0), maxIntegerDigits(15), Validators.pattern(POSITIVE_DECIMAL_4_PATTERN)]
      ],
      estoqueAceitavel: [
        this.formatDecimal(this.data.template?.estoqueAceitavel),
        [Validators.min(0), maxIntegerDigits(15), Validators.pattern(POSITIVE_DECIMAL_4_PATTERN)]
      ]
    });

    if (this.isEditMode()) {
      this.isInitializing.set(true);
    }

    const valueChanges$ = this.form.get('unidadeDeConsumo')!.valueChanges.pipe(startWith(''));
    const valueSignal = toSignal(valueChanges$, { initialValue: '' });

    // Sincroniza o valor do input com o signal de filtro para o autocomplete.
    effect(() => {
      const value = valueSignal();
      const stringValue = typeof value === 'string' ? value : '';
      this.filterValue.set(stringValue);
    });

    MaterialTypeForm.BACKEND_ERROR_FIELDS.forEach(controlPath => {
      this.form.get(controlPath)?.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe(() => clearControlError(this.form.get(controlPath), 'backend'));
    });

    this.applyFieldLocks();
  }

  ngOnInit(): void {
    this.initializeForm().catch(() => undefined);
  }

  private async initializeForm(): Promise<void> {
    try {
      if (this.isEditMode() && this.currentMaterialType().id) {
        const fullMaterialType = await lastValueFrom(this.materialTypeService.findById(this.currentMaterialType().id));
        this.currentMaterialType.set(fullMaterialType);
        this.form.patchValue({
          nome: fullMaterialType.nome,
          estoqueCritico: this.formatDecimal(fullMaterialType.estoqueCritico),
          estoqueAceitavel: this.formatDecimal(fullMaterialType.estoqueAceitavel)
        }, { emitEvent: false });
      }

      await this.loadMeasurementUnits();
      this.applyFieldLocks();
      this.cdr.markForCheck();
    } catch {
      this.entityDialog.showErrorSnackbar(MaterialTypeForm.Texts.loadError);
    } finally {
      this.isInitializing.set(false);
    }
  }

  /**
   * Carrega as unidades de medida a partir do link HATEOAS para popular o autocomplete.
   */
  async loadMeasurementUnits(): Promise<void> {
    const materialType = this.currentMaterialType();
    const url = materialType?._links?.['unidades-de-medida']?.href ?? this.data.template?._links?.['unidades-de-medida']?.href;

    if (!url) {
      return;
    }

    const options = await lastValueFrom(this.enumService.getEnumOptions(url, 'unidadesDeMedida'));
    const units: UnitOption[] = options.map(option => ({
      name: option.value,
      descricao: option.viewValue
    }));

    this.allUnits.set(units);
    this.form.get('unidadeDeConsumo')?.setValidators([Validators.required, requireMatch(units)]);

    if (materialType?.unidadeDeConsumo) {
      const initialUnit = units.find(u => u.name === materialType.unidadeDeConsumo);
      this.form.get('unidadeDeConsumo')?.setValue(initialUnit);
    }

    this.form.get('unidadeDeConsumo')?.updateValueAndValidity();
    this.applyFieldLocks();
    this.cdr.markForCheck();
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
    return hasLockedField(this.currentMaterialType(), field);
  }

  protected getFieldLockReason(field: string): string | null {
    return getLockedFieldReason(this.currentMaterialType(), field);
  }

  /**
   * Envia os dados do formulário para criação ou atualização do Tipo de Matéria-Prima.
   */
  onSave(): void {
    if (this.isInitializing()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.entityDialog.showErrorSnackbar(MaterialTypeForm.Texts.formValidationError);
      return;
    }

    const formValue = this.form.getRawValue();
    const unidadeSelecionada = typeof formValue.unidadeDeConsumo === 'string'
      ? formValue.unidadeDeConsumo
      : formValue.unidadeDeConsumo?.name;
    const request: Partial<MaterialTypeRequest> = {
      nome: String(formValue.nome ?? '').trim(),
      estoqueCritico: this.parseDecimal(formValue.estoqueCritico),
      estoqueAceitavel: this.parseDecimal(formValue.estoqueAceitavel)
    };

    if (!this.isEditMode() || !this.isFieldLocked('unidadeDeConsumo')) {
      request.unidadeDeConsumo = unidadeSelecionada || this.currentMaterialType().unidadeDeConsumo;
    }

    if (this.isEditMode() && this.isNoOpUpdate(request as MaterialTypeRequest)) {
      this.entityDialog.showInfoSnackbar(MaterialTypeForm.NO_CHANGES_MESSAGE);
      return;
    }

    const operation = this.isEditMode()
      ? this.materialTypeService.update(this.currentMaterialType()._links!['update']!.href, request as MaterialTypeRequest)
      : this.materialTypeService.create(request as MaterialTypeRequest);

    operation.subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        clearApiFieldErrors(this.form, MaterialTypeForm.BACKEND_ERROR_FIELDS);
        const hasFieldErrors = applyApiFieldErrors(this.form, err, {
          fieldMap: MaterialTypeForm.BACKEND_FIELD_MAP,
          ...stockApiErrorOptions
        });
        if (hasFieldErrors) {
          this.entityDialog.showErrorSnackbar('Revise os campos destacados.');
        } else {
          this.entityDialog.showApiErrorSnackbar(err, MaterialTypeForm.Texts.saveError, stockApiErrorOptions);
        }
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private applyFieldLocks(): void {
    const unidadeDeConsumoControl = this.form.get('unidadeDeConsumo');

    if (!this.isEditMode() || !unidadeDeConsumoControl) {
      return;
    }

    if (this.isFieldLocked('unidadeDeConsumo')) {
      unidadeDeConsumoControl.disable({ emitEvent: false });
    }
  }

  private isNoOpUpdate(request: MaterialTypeRequest): boolean {
    const currentMaterialType = this.currentMaterialType();
    return this.normalizeText(currentMaterialType.nome) === this.normalizeText(request.nome)
      && (
        request.unidadeDeConsumo == null
        || String(currentMaterialType.unidadeDeConsumo ?? '') === String(request.unidadeDeConsumo ?? '')
      )
      && this.normalizeDecimal(currentMaterialType.estoqueCritico) === this.normalizeDecimal(request.estoqueCritico)
      && this.normalizeDecimal(currentMaterialType.estoqueAceitavel) === this.normalizeDecimal(request.estoqueAceitavel);
  }

  private normalizeText(value: unknown): string | null {
    const normalized = String(value ?? '').trim();
    return normalized ? normalized : null;
  }

  private formatDecimal(value: number | null | undefined): string {
    return value == null ? '' : String(value).replace('.', ',');
  }

  private parseDecimal(value: unknown): number | null {
    const normalized = this.normalizeDecimal(value);
    return normalized == null ? null : Number(normalized);
  }

  private normalizeDecimal(value: unknown): string | null {
    const normalized = String(value ?? '').trim().replace(',', '.');
    return normalized ? normalized : null;
  }
}
