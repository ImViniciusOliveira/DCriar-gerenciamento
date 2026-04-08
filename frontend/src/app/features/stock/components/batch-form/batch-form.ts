import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, Signal, effect, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, TitleCasePipe } from '@angular/common';
import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom } from 'rxjs';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { filter, map, switchMap } from 'rxjs/operators';

import { Batch, BatchMovement, BatchRequest } from '../../models/batch.model';
import { MaterialType } from '../../models/material-type.model';
import { BatchService } from '../../services/batch.service';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { MaterialTypeSearch } from '../../../../shared/components/material-type-search/material-type-search';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';
import { BatchAdjustmentForm } from '../batch-adjustment-form/batch-adjustment-form';
import { InstantErrorStateMatcher } from '../../../../shared/utils/error-state-matchers';
import { POSITIVE_DECIMAL_4_PATTERN } from '../../../../shared/utils/number-patterns';
import { getLockedFieldReason, hasLockedField } from '../../../../shared/utils/field-locks';
import { analyzeLogicalMapKeys, normalizeLogicalMapKey } from '../../../../shared/utils/logical-map-key';
import { toggleControlError } from '../../../../shared/utils/control-errors';
import { scrollDialogToElement } from '../../../../shared/utils/dialog-scroll';

/**
 * Validador que verifica se a parte inteira de um número excede um máximo de dígitos.
 */
export function maxIntegerDigits(maxDigits: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    const value = String(control.value);
    const integerPart = value.split('.')[0].replace(/^-/, '');

    if (integerPart.length > maxDigits) {
      return { maxIntegerDigits: { requiredDigits: maxDigits, actualDigits: integerPart.length } };
    }
    return null;
  };
}

export interface BatchFormData {
  template: Batch;
  title: string;
  isViewMode?: boolean;
}

/**
 * Formulário para criação e edição de Lotes de Matéria-Prima.
 * Contém lógicas complexas para validação condicional e atributos dinâmicos.
 */
@Component({
  selector: 'app-batch-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MaterialTypeSearch, MatSelectModule, BatchAdjustmentForm,
    CurrencyPipe, TitleCasePipe
  ],
  templateUrl: './batch-form.html',
  styleUrls: ['./batch-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchForm implements OnInit {
  private static readonly GEOMETRIC_UNITS = new Set([
    'METRO_LINEAR',
    'CENTIMETRO_LINEAR',
    'METRO_QUADRADO',
    'CENTIMETRO_QUADRADO'
  ]);

  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<BatchForm>);
  private readonly dialog = inject(MatDialog);
  private readonly batchService = inject(BatchService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly enumService = inject(EnumService);
  public readonly data: BatchFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isEditMode = signal(false);
  isViewMode = signal(false);
  requiresWidth: Signal<boolean>;
  matcher = new InstantErrorStateMatcher();

  readonly batch = signal<Batch>(this.data.template);
  readonly movements = signal<BatchMovement[]>([]);
  readonly isLoadingMovements = signal(false);
  readonly showMovements = signal(false);
  private readonly allMeasurementUnits: Signal<EnumOption[]>;
  stockUnitOptions: Signal<EnumOption[]>;
  readonly unitsUrl = signal<string | null>(null);

  // Signal público e gravável para a matéria-prima, usado pela lógica e pelo template.
  materialType = signal<MaterialType | null | undefined>(undefined);

  private static readonly Texts = {
    CONFIRM_DELETE_ATTR_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_ATTR_MESSAGE: (key: string) => `Deseja realmente remover o atributo "${key}"?`,
    SAVE_SUCCESS_CREATE: 'Lote cadastrado com sucesso!',
    SAVE_SUCCESS_UPDATE: 'Lote atualizado com sucesso!',
    SAVE_ERROR: 'Falha ao salvar. Verifique os dados e tente novamente.',
    NO_CHANGES: 'Nenhuma alteração detectada.',
    FORM_VALIDATION_ERROR: 'Corrija os campos inválidos antes de continuar.',
    LOAD_ERROR: 'Não foi possível carregar os dados do lote.',
    UNITS_URL_ERROR: "URL para 'unidades-de-medida' não encontrada no template do lote.",
    DUPLICATE_ATTRIBUTE_KEY_INLINE_ERROR: 'Existe outro atributo equivalente preenchido.',
  };

  constructor() {
    this.isEditMode.set(!!this.data.template.id && !this.data.isViewMode);
    this.isViewMode.set(!!this.data.isViewMode);
    const initialUnitsUrl = this.data.template?._links?.['unidades-de-medida']?.href?.split('{')[0] ?? null;
    if (initialUnitsUrl) {
      this.unitsUrl.set(initialUnitsUrl);
    }

    this.form = this.fb.group({
      materiaPrima: [null, Validators.required],
      unidadeDeEstoque: [{ value: null, disabled: true }, Validators.required],
      quantidadeInicial: [{ value: this.data.template?.saldoEstoque || '', disabled: this.isEditMode() }, [Validators.required, Validators.min(0.01), maxIntegerDigits(15), Validators.pattern(POSITIVE_DECIMAL_4_PATTERN)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01), maxIntegerDigits(15), Validators.pattern(POSITIVE_DECIMAL_4_PATTERN)]],
      motivo: [this.data.template?.motivo || '', [Validators.required, Validators.maxLength(100)]],
      larguraMm: [null],
      atributos: this.fb.array([])
    });
    this.attributes.addValidators(this.validateAttributeKeys.bind(this));

    const unidadeEstoque$ = this.form.get('unidadeDeEstoque')!.valueChanges;
    this.requiresWidth = toSignal(
      unidadeEstoque$.pipe(map(unidade => BatchForm.GEOMETRIC_UNITS.has(unidade))),
      { initialValue: false }
    );

    const unitsUrl$ = toObservable(this.unitsUrl).pipe(filter((url): url is string => !!url));
    this.allMeasurementUnits = toSignal(
      unitsUrl$.pipe(switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))),
      { initialValue: [] }
    );

    this.stockUnitOptions = computed(() => {
      const mt = this.materialType();
      const allUnits = this.allMeasurementUnits();
      if (!mt || allUnits.length === 0) {
        return [];
      }

      const mainUnit = allUnits.find(unit => unit.value === mt.unidadeDeConsumo);
      if (!mainUnit) {
        return [];
      }

      const options = [mainUnit];
      if (mainUnit.compatibleInputUnit) {
        const compatibleUnit = allUnits.find(unit => unit.value === mainUnit.compatibleInputUnit);
        if (compatibleUnit) {
          options.push(compatibleUnit);
        }
      }

      return options.filter((option, index, self) => self.findIndex(item => item.value === option.value) === index);
    });

    effect(() => {
      const options = this.stockUnitOptions();
      const control = this.form.get('unidadeDeEstoque');
      const currentValue = control?.value;
      if (currentValue && options.length > 0 && !options.some(opt => opt.value === currentValue)) {
        control.setValue(null, { emitEvent: false });
      }
    });

    unidadeEstoque$.pipe(takeUntilDestroyed()).subscribe(unidade => {
      this.updateWidthValidation(unidade);
      this.attributes.updateValueAndValidity();
    });

    this.syncStockUnitControlState();
    this.applyFieldLocks();
  }

  ngOnInit(): void {
    this.initializeForm().catch(() => {});
  }

  async initializeForm(): Promise<void> {
    if ((this.isEditMode() || this.isViewMode()) && this.data.template.id) {
      try {
        const selfLink = this.data.template._links?.['self']?.href;
        if (!selfLink) {
          return;
        }
        const fullBatch = await lastValueFrom(this.batchService.findByUrl(selfLink));
        this.batch.set(fullBatch);

        if (fullBatch.tipoMateriaPrimaId) {
          const mt = await lastValueFrom(this.materialTypeService.findById(fullBatch.tipoMateriaPrimaId));
          this.materialType.set(mt);
          const unidadeApresentacao = fullBatch.unidadeCadastroEstoque ?? fullBatch.unidadeDeEstoque;
          this.form.patchValue({
            materiaPrima: mt,
            unidadeDeEstoque: unidadeApresentacao,
            motivo: fullBatch.motivo,
            custoTotalLote: fullBatch.custoTotalLote
          });
        }

        if (fullBatch.unidadeCadastroEstoque ?? fullBatch.unidadeDeEstoque) {
          this.updateWidthValidation(fullBatch.unidadeCadastroEstoque ?? fullBatch.unidadeDeEstoque ?? null);
        }

        if (fullBatch.atributos) {
          this.attributes.clear();
          Object.entries(fullBatch.atributos).forEach(([key, value]) => {
            if (normalizeLogicalMapKey(key) === 'larguramm') {
              this.form.get('larguraMm')?.setValue(value);
            } else {
              this.addAttribute(key, value as string, false);
            }
          });
        }
        await this.loadMovements(fullBatch);
        this.syncStockUnitControlState();
        this.applyFieldLocks();
        this.cdr.markForCheck();
      } catch {
        this.entityDialog.showErrorSnackbar(BatchForm.Texts.LOAD_ERROR);
      }
    }
  }

  getUnitDescription(value: string | undefined): string {
    if (!value) return 'N/A';
    const unit = this.allMeasurementUnits().find(u => u.value === value);
    return unit?.viewValue ?? value;
  }

  protected getSelectedStockUnitSuffix(): string {
    const selectedUnit = this.form.get('unidadeDeEstoque')?.value;
    if (!selectedUnit) {
      return '';
    }

    const option = this.stockUnitOptions().find(unit => unit.value === selectedUnit);
    return option?.simbolo || option?.viewValue || selectedUnit;
  }

  get attributes(): FormArray {
    return this.form.get('atributos') as FormArray;
  }

  get attributesControls(): FormGroup[] {
    return (this.form.get('atributos') as FormArray).controls as FormGroup[];
  }

  protected getAttributeInlineError(control: AbstractControl | null): string | null {
    if (!control?.hasError('logicalDuplicateKey')) {
      return null;
    }

    return BatchForm.Texts.DUPLICATE_ATTRIBUTE_KEY_INLINE_ERROR;
  }

  get materialTypeControl(): FormControl {
    return this.form.get('materiaPrima') as FormControl;
  }

  protected shouldShowControlError(control: FormControl | null): boolean {
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  protected isFieldLocked(...fields: string[]): boolean {
    return fields.some(field => hasLockedField(this.batch(), field));
  }

  protected getFieldLockReason(...fields: string[]): string | null {
    return getLockedFieldReason(this.batch(), ...fields);
  }

  onMaterialTypeChange(event: MatSelectChange): void {
    const materialType = event.value as MaterialType;
    this.materialType.set(materialType);
    this.form.patchValue({
      materiaPrima: materialType,
      unidadeDeEstoque: materialType.unidadeDeConsumo
    });
    this.syncStockUnitControlState();
  }

  async onAdjustmentApplied(updatedBatch: Batch): Promise<void> {
    const selfLink = updatedBatch._links?.['self']?.href ?? this.batch()._links?.['self']?.href;
    if (!selfLink) {
      this.batch.set(updatedBatch);
      this.cdr.markForCheck();
      return;
    }

    try {
      const freshBatch = await lastValueFrom(this.batchService.findByUrl(selfLink));
      this.batch.set(freshBatch);
      await this.loadMovements(freshBatch);
      this.cdr.markForCheck();
    } catch {
      this.batch.set(updatedBatch);
      await this.loadMovements(updatedBatch);
      this.cdr.markForCheck();
    }
  }

  toggleMovements(): void {
    this.showMovements.update(current => !current);
  }

  movementLabel(tipo: string): string {
    switch (tipo) {
      case 'ENTRADA_COMPRA':
        return 'Entrada de compra';
      case 'SAIDA_PRODUCAO':
        return 'Saída para produção';
      case 'AJUSTE_INVENTARIO':
        return 'Ajuste de inventário';
      case 'PERDA_DESCARTE':
        return 'Perda / Descarte';
      case 'ENTRADA_SOBRA':
        return 'Entrada de retalho';
      default:
        return tipo.replaceAll('_', ' ').toLowerCase().replace(/^\w/, char => char.toUpperCase());
    }
  }

  formatMovementQuantity(movement: BatchMovement): string {
    const unit = this.batch().unidadeSimbolo ?? 'un';
    const prefix = movement.quantidade > 0 ? '+' : '';
    const quantity = new Intl.NumberFormat('pt-BR', {
      maximumFractionDigits: 4
    }).format(movement.quantidade);
    return `${prefix}${quantity}${unit}`;
  }

  private async loadMovements(batch: Batch): Promise<void> {
    const movementsUrl = batch._links?.['movimentacoes']?.href;
    if (!movementsUrl) {
      this.movements.set([]);
      return;
    }

    this.isLoadingMovements.set(true);

    try {
      const response = await lastValueFrom(this.batchService.findMovements(movementsUrl));
      this.movements.set(response._embedded?.movimentacoes ?? []);
    } catch {
      this.movements.set([]);
    } finally {
      this.isLoadingMovements.set(false);
    }
  }

  private updateWidthValidation(unidade: string | null): void {
    const widthControl = this.form.get('larguraMm');
    if (unidade && BatchForm.GEOMETRIC_UNITS.has(unidade)) {
      widthControl?.setValidators([Validators.required, Validators.min(1), maxIntegerDigits(10), Validators.pattern(POSITIVE_DECIMAL_4_PATTERN)]);
    } else {
      widthControl?.clearValidators();
      widthControl?.reset();
    }
    widthControl?.updateValueAndValidity();
  }

  addAttribute(key: string = '', value: string = '', isNew: boolean = true): void {
    this.attributes.push(this.fb.group({
      chave: [key, [Validators.required, Validators.maxLength(50)]],
      valor: [value, [Validators.required, Validators.maxLength(50)]],
      isNew: [isNew]
    }));
    this.attributes.updateValueAndValidity();

    if (isNew) {
      scrollDialogToElement(this.dialogRef, '.add-attribute-button');
    }
  }

  async removeAttribute(index: number): Promise<void> {
    const attrGroup = this.attributes.at(index);
    const isNew = attrGroup.get('isNew')?.value;

    if (isNew) {
      this.attributes.removeAt(index);
      this.form.get('atributos')?.markAsDirty();
      this.attributes.updateValueAndValidity();
      return;
    }

    const key = attrGroup.get('chave')?.value;
    const dialogData: ConfirmDialogData = {
      title: BatchForm.Texts.CONFIRM_DELETE_ATTR_TITLE,
      message: BatchForm.Texts.CONFIRM_DELETE_ATTR_MESSAGE(key || 'este atributo')
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      this.attributes.removeAt(index);
      this.form.get('atributos')?.markAsDirty();
      this.attributes.updateValueAndValidity();
      this.cdr.markForCheck();
    }
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.entityDialog.showErrorSnackbar(BatchForm.Texts.FORM_VALIDATION_ERROR);
      return;
    }

    const formValue = this.form.getRawValue();
    const materialType: MaterialType = formValue.materiaPrima;

    const attributesMap: { [key: string]: any } = {};
    (formValue.atributos || []).forEach((attr: { chave: string; valor: string }) => {
      if (attr.chave) {
        attributesMap[attr.chave] = attr.valor;
      }
    });

    if (this.requiresWidth()) {
      attributesMap['larguraMm'] = formValue.larguraMm ? this.parseDecimal(formValue.larguraMm) : null;
    }

    const request: BatchRequest = {
      tipoMateriaPrimaId: materialType.id,
      unidadeDeEstoque: formValue.unidadeDeEstoque,
      unidadeCadastroEstoque: this.batch().unidadeCadastroEstoque ?? formValue.unidadeDeEstoque,
      quantidadeInicial: this.parseDecimal(formValue.quantidadeInicial),
      custoTotalLote: this.parseDecimal(formValue.custoTotalLote),
      motivo: formValue.motivo,
      atributos: attributesMap
    };

    if (this.isEditMode() && this.isNoOpUpdate(request)) {
      this.entityDialog.showInfoSnackbar(BatchForm.Texts.NO_CHANGES);
      return;
    }

    const operation = this.isEditMode()
      ? this.batchService.update(this.data.template._links!['update']!.href, request)
      : this.batchService.create(request);

    operation.subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.entityDialog.showErrorSnackbar(err?.error?.detail || err?.error?.message || BatchForm.Texts.SAVE_ERROR);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private parseDecimal(value: unknown): number {
    const normalized = String(value ?? '0').trim().replace(',', '.');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private isNoOpUpdate(request: BatchRequest): boolean {
    const currentBatch = this.batch();
    if (!currentBatch?.id) {
      return false;
    }

    const currentDisplayUnit = currentBatch.unidadeCadastroEstoque ?? currentBatch.unidadeDeEstoque;

    const currentAttributes = this.normalizeAttributesForComparison(currentBatch.atributos ?? {});
    const requestAttributes = this.normalizeAttributesForComparison(request.atributos ?? {});
    const currentReason = this.normalizeText(currentBatch.motivo);
    const requestReason = this.normalizeText(request.motivo);

    return String(currentBatch.tipoMateriaPrimaId ?? '') === String(request.tipoMateriaPrimaId ?? '')
      && String(currentDisplayUnit ?? '') === String(request.unidadeDeEstoque ?? '')
      && String(currentDisplayUnit ?? '') === String(request.unidadeCadastroEstoque ?? '')
      && this.sameNumericValue(currentBatch.custoTotalLote, request.custoTotalLote)
      && currentReason === requestReason
      && this.deepEquals(currentAttributes, requestAttributes);
  }

  private normalizeAttributesForComparison(value: unknown): unknown {
    if (Array.isArray(value)) {
      return value.map(item => this.normalizeAttributesForComparison(item));
    }

    if (value && typeof value === 'object') {
      return Object.entries(value as Record<string, unknown>)
        .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))
        .reduce<Record<string, unknown>>((accumulator, [key, entryValue]) => {
          const normalizedKey = this.normalizeText(key);
          if (!normalizedKey) {
            return accumulator;
          }
          accumulator[normalizedKey] = this.normalizeAttributesForComparison(entryValue);
          return accumulator;
        }, {});
    }

    if (value === null || value === undefined) {
      return null;
    }

    return this.normalizeText(String(value));
  }

  private normalizeText(value: unknown): string | null {
    const normalized = String(value ?? '').trim();
    return normalized ? normalized : null;
  }

  private sameNumericValue(currentValue: unknown, requestValue: unknown): boolean {
    const currentNumber = currentValue === null || currentValue === undefined || currentValue === ''
      ? null
      : Number(currentValue);
    const requestNumber = requestValue === null || requestValue === undefined || requestValue === ''
      ? null
      : Number(requestValue);

    if (currentNumber === null || requestNumber === null) {
      return currentNumber === requestNumber;
    }

    return currentNumber === requestNumber;
  }

  private deepEquals(left: unknown, right: unknown): boolean {
    if (left === right) {
      return true;
    }

    if (Array.isArray(left) && Array.isArray(right)) {
      if (left.length !== right.length) {
        return false;
      }

      return left.every((item, index) => this.deepEquals(item, right[index]));
    }

    if (left && right && typeof left === 'object' && typeof right === 'object') {
      const leftEntries = Object.entries(left as Record<string, unknown>);
      const rightEntries = Object.entries(right as Record<string, unknown>);

      if (leftEntries.length !== rightEntries.length) {
        return false;
      }

      return leftEntries.every(([key, value]) => this.deepEquals(value, (right as Record<string, unknown>)[key]));
    }

    return false;
  }

  private applyFieldLocks(): void {
    if (!this.isEditMode()) {
      return;
    }

    if (this.isFieldLocked('tipoMateriaPrimaId')) {
      this.materialTypeControl.disable({ emitEvent: false });
    }

    if (this.isFieldLocked('unidadeDeEstoque', 'unidadeCadastroEstoque')) {
      this.form.get('unidadeDeEstoque')?.disable({ emitEvent: false });
    }

    if (this.isFieldLocked('custoTotalLote')) {
      this.form.get('custoTotalLote')?.disable({ emitEvent: false });
    }

    if (this.isFieldLocked('atributos.larguraMm')) {
      this.form.get('larguraMm')?.disable({ emitEvent: false });
    }
  }

  private syncStockUnitControlState(): void {
    const stockUnitControl = this.form.get('unidadeDeEstoque');
    if (!stockUnitControl) {
      return;
    }

    if (this.isEditMode() && this.isFieldLocked('unidadeDeEstoque', 'unidadeCadastroEstoque')) {
      stockUnitControl.disable({ emitEvent: false });
      return;
    }

    if (!this.materialType()) {
      stockUnitControl.disable({ emitEvent: false });
      stockUnitControl.setValue(null, { emitEvent: false });
      return;
    }

    stockUnitControl.enable({ emitEvent: false });
  }

  private validateAttributeKeys(control: AbstractControl): ValidationErrors | null {
    const entries = Array.isArray(control.value) ? control.value : [];
    const reservedKeys = this.requiresWidth && this.requiresWidth() ? ['larguraMm'] : [];
    const analysis = analyzeLogicalMapKeys(entries, reservedKeys);

    this.attributesControls.forEach((attrGroup, index) => {
      toggleControlError(attrGroup.get('chave'), 'logicalDuplicateKey', analysis.duplicateIndexes.has(index));
    });
    toggleControlError(
      this.form.get('larguraMm'),
      'logicalDuplicateKey',
      analysis.conflictingReservedKeys.has('larguraMm')
    );

    if (!analysis.conflict) {
      return null;
    }

    return {
      logicalDuplicateKey: analysis.conflict
    };
  }
}
