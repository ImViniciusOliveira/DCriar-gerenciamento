import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, Signal, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom } from 'rxjs';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { filter, map, switchMap } from 'rxjs/operators';

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

export interface BatchFormData {
  template: Batch;
  title: string;
}

/**
 * Formulário inteligente para Lotes de Matéria-Prima.
 * Adapta-se dinamicamente para garantir a conformidade com as regras de negócio do backend,
 * como a exigência de largura para materiais em rolo e a compatibilidade entre unidades.
 */
@Component({
  selector: 'app-batch-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MaterialTypeSearch, MatSelectModule
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
  requiresWidth: Signal<boolean>;

  private readonly allMeasurementUnits: Signal<EnumOption[]>;
  /** Opções de 'Unidade de Estoque' filtradas com base na matéria-prima selecionada para evitar combinações inválidas. */
  stockUnitOptions: Signal<EnumOption[]>;

  private readonly unitsUrl = signal<string | null>(null);
  /** Signal que armazena a matéria-prima atualmente selecionada para alimentar a lógica reativa. */
  private materialTypeSignal = signal<MaterialType | null>(null);

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
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      materiaPrima: [null, Validators.required],
      unidadeDeEstoque: [null, Validators.required],
      quantidadeInicial: [this.data.template?.saldoEstoque || '', [Validators.required, Validators.min(0.01)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01)]],
      motivo: [this.data.template?.motivo || '', Validators.required],
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

    // Filtra as opções de unidade de estoque para prevenir erros de negócio no backend.
    this.stockUnitOptions = computed(() => {
      const materialType = this.materialTypeSignal();
      const allUnits = this.allMeasurementUnits();

      // REGRA: A unidade de estoque METRO_LINEAR só é permitida se a unidade de consumo da matéria-prima for CENTIMETRO_QUADRADO.
      if (materialType && materialType.unidadeDeConsumo !== 'CENTIMETRO_QUADRADO') {
        return allUnits.filter(u => u.value !== 'METRO_LINEAR');
      }

      return allUnits;
    });

    // Garante que, se as opções de unidade mudarem e o valor selecionado se tornar inválido, o campo seja limpo.
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

    // Inicia o carregamento dos dados do formulário sem bloquear a inicialização do componente.
    this.initializeForm().catch(err => console.error('Erro na inicialização do formulário:', err));
  }

  async initializeForm(): Promise<void> {
    if (this.isEditMode() && this.data.template.tipoMateriaPrimaId) {
      try {
        const materialType = await lastValueFrom(this.materialTypeService.findById(this.data.template.tipoMateriaPrimaId));
        this.materialTypeSignal.set(materialType);
        this.form.patchValue({
          materiaPrima: materialType,
          unidadeDeEstoque: this.data.template.unidadeDeEstoque
        });

        if (this.data.template.unidadeDeEstoque) {
          this.updateWidthValidation(this.data.template.unidadeDeEstoque);
        }

      } catch (error) {
        console.error("Falha ao carregar dados iniciais", error);
        this.entityDialog.showErrorSnackbar(BatchForm.Texts.LOAD_ERROR);
      }
    }

    if (this.isEditMode() && this.data.template.atributos) {
      this.attributes.clear();
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        if (key === 'larguraMm') {
          this.form.get('larguraMm')?.setValue(value);
        } else {
          this.addAttribute(key, value as string, false);
        }
      });
    }
    this.cdr.markForCheck();
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
    this.materialTypeSignal.set(materialType);
    this.form.patchValue({
      materiaPrima: materialType,
      unidadeDeEstoque: materialType.unidadeDeConsumo
    });
  }

  private updateWidthValidation(unidade: string | null): void {
    const widthControl = this.form.get('larguraMm');
    if (unidade === 'METRO_LINEAR') {
      widthControl?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      widthControl?.clearValidators();
      widthControl?.reset();
    }
    widthControl?.updateValueAndValidity();
  }

  addAttribute(key: string = '', value: string = '', isNew: boolean = true): void {
    this.attributes.push(this.fb.group({
      chave: [key, Validators.required],
      valor: [value, Validators.required],
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
      attributesMap['larguraMm'] = formValue.larguraMm;
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
