import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, Signal, effect, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, TitleCasePipe } from '@angular/common';
import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom } from 'rxjs';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { filter, map, switchMap } from 'rxjs/operators';
import { ErrorStateMatcher } from '@angular/material/core';

import { Batch, BatchRequest } from '../../models/batch.model';
import { MaterialType } from '../../models/material-type.model';
import { BatchService } from '../../services/batch.service';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MaterialTypeSearch } from '../../../../shared/components/material-type-search/material-type-search';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';

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

/**
 * Define quando os erros de um campo de formulário devem ser exibidos.
 * A regra é: mostrar o erro se o campo for inválido E (o usuário já digitou nele OU já saiu dele).
 */
export class ImmediateErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
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
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MaterialTypeSearch, MatSelectModule,
    CurrencyPipe, TitleCasePipe
  ],
  templateUrl: './batch-form.html',
  styleUrls: ['./batch-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchForm implements OnInit {
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
  matcher = new ImmediateErrorStateMatcher();

  readonly batch = signal<Batch>(this.data.template);
  private readonly allMeasurementUnits: Signal<EnumOption[]>;
  stockUnitOptions: Signal<EnumOption[]>;
  private readonly unitsUrl = signal<string | null>(null);

  // Signal público e gravável para a matéria-prima, usado pela lógica e pelo template.
  materialType = signal<MaterialType | null | undefined>(undefined);

  private static readonly Texts = {
    CONFIRM_DELETE_ATTR_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_ATTR_MESSAGE: (key: string) => `Deseja realmente remover o atributo "${key}"?`,
    SAVE_SUCCESS_CREATE: 'Lote cadastrado com sucesso!',
    SAVE_SUCCESS_UPDATE: 'Lote atualizado com sucesso!',
    SAVE_ERROR: 'Falha ao salvar. Verifique os dados e tente novamente.',
    LOAD_ERROR: 'Não foi possível carregar os dados do lote.',
    UNITS_URL_ERROR: "URL para 'unidades-de-medida' não encontrada no template do lote."
  };

  constructor() {
    this.isEditMode.set(!!this.data.template.id && !this.data.isViewMode);
    this.isViewMode.set(!!this.data.isViewMode);

    this.form = this.fb.group({
      materiaPrima: [null, Validators.required],
      unidadeDeEstoque: [null, Validators.required],
      quantidadeInicial: [{ value: this.data.template?.saldoEstoque || '', disabled: this.isEditMode() }, [Validators.required, Validators.min(0.01), maxIntegerDigits(15), Validators.pattern(/^-?\d*(\.\d+)?$/)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01), maxIntegerDigits(15), Validators.pattern(/^-?\d*(\.\d+)?$/)]],
      motivo: [this.data.template?.motivo || '', [Validators.required, Validators.maxLength(100)]],
      larguraMm: [null],
      atributos: this.fb.array([])
    });

    const unidadeEstoque$ = this.form.get('unidadeDeEstoque')!.valueChanges;
    this.requiresWidth = toSignal(unidadeEstoque$.pipe(map(unidade => unidade === 'METRO_LINEAR')), { initialValue: false });

    const unitsUrl$ = toObservable(this.unitsUrl).pipe(filter((url): url is string => !!url));
    this.allMeasurementUnits = toSignal(
      unitsUrl$.pipe(switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))),
      { initialValue: [] }
    );

    // LÓGICA CORRIGIDA E FINAL
    this.stockUnitOptions = computed(() => {
      const mt = this.materialType();
      const allUnits = this.allMeasurementUnits();

      // Se a matéria-prima selecionada for compatível, retorna todas as unidades.
      if (mt && mt.unidadeDeConsumo === 'CENTIMETRO_QUADRADO') {
        return allUnits;
      }

      // Caso contrário (nenhuma selecionada ou uma incompatível), sempre remove "Metro Linear".
      return allUnits.filter(u => u.value !== 'METRO_LINEAR');
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
    });
  }

  ngOnInit(): void {
    const url = this.data.template._links?.['unidades-de-medida']?.href;
    if (url) {
      this.unitsUrl.set(url);
    } else {
      console.error(BatchForm.Texts.UNITS_URL_ERROR);
    }
    this.initializeForm().catch(err => console.error('Erro na inicialização do formulário:', err));
  }

  async initializeForm(): Promise<void> {
    if ((this.isEditMode() || this.isViewMode()) && this.data.template.id) {
      try {
        const selfLink = this.data.template._links?.['self']?.href;
        if (!selfLink) {
          console.error("Link 'self' não encontrado para carregar o lote.");
          return;
        }
        const fullBatch = await lastValueFrom(this.batchService.findByUrl(selfLink));
        this.batch.set(fullBatch);

        if (fullBatch.tipoMateriaPrimaId) {
          const mt = await lastValueFrom(this.materialTypeService.findById(fullBatch.tipoMateriaPrimaId));
          this.materialType.set(mt);
          this.form.patchValue({
            materiaPrima: mt,
            unidadeDeEstoque: fullBatch.unidadeDeEstoque,
            motivo: fullBatch.motivo,
            custoTotalLote: fullBatch.custoTotalLote
          });
        }

        if (fullBatch.unidadeDeEstoque) {
          this.updateWidthValidation(fullBatch.unidadeDeEstoque);
        }

        if (fullBatch.atributos) {
          this.attributes.clear();
          Object.entries(fullBatch.atributos).forEach(([key, value]) => {
            if (key === 'larguraMm') {
              this.form.get('larguraMm')?.setValue(value);
            } else {
              this.addAttribute(key, value as string, false);
            }
          });
        }
        this.cdr.markForCheck();
      } catch (error) {
        console.error("Falha ao carregar dados iniciais", error);
        this.entityDialog.showErrorSnackbar(BatchForm.Texts.LOAD_ERROR);
      }
    }
  }

  getUnitDescription(value: string | undefined): string {
    if (!value) return 'N/A';
    const unit = this.allMeasurementUnits().find(u => u.value === value);
    return unit?.viewValue ?? value;
  }

  get attributes(): FormArray {
    return this.form.get('atributos') as FormArray;
  }

  get attributesControls(): FormGroup[] {
    return (this.form.get('atributos') as FormArray).controls as FormGroup[];
  }

  get materialTypeControl(): FormControl {
    return this.form.get('materiaPrima') as FormControl;
  }

  onMaterialTypeChange(event: MatSelectChange): void {
    const materialType = event.value as MaterialType;
    this.materialType.set(materialType);
    this.form.patchValue({
      materiaPrima: materialType,
      unidadeDeEstoque: materialType.unidadeDeConsumo
    });
  }

  private updateWidthValidation(unidade: string | null): void {
    const widthControl = this.form.get('larguraMm');
    if (unidade === 'METRO_LINEAR') {
      // Reduzido maxIntegerDigits para 10 para evitar overflow de Integer no backend
      widthControl?.setValidators([Validators.required, Validators.min(1), maxIntegerDigits(10), Validators.pattern(/^-?\d*(\.\d+)?$/)]);
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

    if (isNew) {
      setTimeout(() => {
        const dialogContent = (this.dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
        if (dialogContent) {
          dialogContent.scrollTop = dialogContent.scrollHeight;
        }
      }, 100);
    }
  }

  async removeAttribute(index: number): Promise<void> {
    const attrGroup = this.attributes.at(index);
    const isNew = attrGroup.get('isNew')?.value;

    if (isNew) {
      this.attributes.removeAt(index);
      this.form.get('atributos')?.markAsDirty();
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
      this.cdr.markForCheck();
    }
  }

  onSave(): void {
    if (this.form.invalid) {
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
      // Converte para número para garantir tipo correto
      attributesMap['larguraMm'] = formValue.larguraMm ? Number(formValue.larguraMm) : null;
    }

    const request: BatchRequest = {
      tipoMateriaPrimaId: materialType.id,
      unidadeDeEstoque: formValue.unidadeDeEstoque,
      quantidadeInicial: formValue.quantidadeInicial,
      custoTotalLote: formValue.custoTotalLote,
      motivo: formValue.motivo,
      atributos: attributesMap
    };

    const operation = this.isEditMode()
      ? this.batchService.update(this.data.template._links!['update']!.href, request)
      : this.batchService.create(request);

    operation.subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(this.isEditMode() ? BatchForm.Texts.SAVE_SUCCESS_UPDATE : BatchForm.Texts.SAVE_SUCCESS_CREATE);
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Falha ao salvar lote:', err);
        this.entityDialog.showErrorSnackbar(BatchForm.Texts.SAVE_ERROR);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
