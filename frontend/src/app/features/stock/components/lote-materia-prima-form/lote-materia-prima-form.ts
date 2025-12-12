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

import { LoteMateriaPrima, LoteMateriaPrimaRequest } from '../../models/lote-materia-prima.model';
import { TipoMateriaPrima } from '../../models/material-type.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MateriaPrimaSearchComponent } from '../../../../shared/components/materia-prima-search/materia-prima-search';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { EnumOption, EnumService } from '../../../../core/services/enum.service';

export interface LoteMateriaPrimaFormData {
  template: LoteMateriaPrima;
  title: string;
}

/**
 * Formulário inteligente para Lotes de Matéria-Prima.
 * Adapta-se dinamicamente para garantir a conformidade com as regras de negócio do backend,
 * como a exigência de largura para materiais em rolo e a compatibilidade entre unidades.
 */
@Component({
  selector: 'app-lote-materia-prima-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MateriaPrimaSearchComponent, MatSelectModule
  ],
  templateUrl: './lote-materia-prima-form.html',
  styleUrls: ['./lote-materia-prima-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoteMateriaPrimaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaForm>);
  private readonly dialog = inject(MatDialog);
  private readonly loteMateriaPrimaService = inject(LoteMateriaPrimaService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly enumService = inject(EnumService);
  public readonly data: LoteMateriaPrimaFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isEditMode = signal(false);
  requiresLargura: Signal<boolean>;

  private readonly todasUnidadesDeMedida: Signal<EnumOption[]>;
  /** Opções de 'Unidade de Estoque' filtradas com base na matéria-prima selecionada para evitar combinações inválidas. */
  opcoesUnidadeEstoque: Signal<EnumOption[]>;

  private readonly unitsUrl = signal<string | null>(null);
  /** Signal que armazena a matéria-prima atualmente selecionada para alimentar a lógica reativa. */
  private materiaPrimaSignal = signal<TipoMateriaPrima | null>(null);

  private static readonly Texts = {
    CONFIRM_DELETE_ATTR_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_ATTR_MESSAGE: (key: string) => `Deseja realmente remover o atributo "${key}"?`
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
    this.requiresLargura = toSignal(unidadeEstoque$.pipe(map(unidade => unidade === 'METRO_LINEAR')), { initialValue: false });

    const unitsUrl$ = toObservable(this.unitsUrl).pipe(filter((url): url is string => !!url));
    this.todasUnidadesDeMedida = toSignal(
      unitsUrl$.pipe(switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))),
      { initialValue: [] }
    );

    // Filtra as opções de unidade de estoque para prevenir erros de negócio no backend.
    this.opcoesUnidadeEstoque = computed(() => {
      const materiaPrima = this.materiaPrimaSignal();
      const todasUnidades = this.todasUnidadesDeMedida();

      // REGRA: A unidade de estoque METRO_LINEAR só é permitida se a unidade de consumo da matéria-prima for CENTIMETRO_QUADRADO.
      if (materiaPrima && materiaPrima.unidadeDeConsumo !== 'CENTIMETRO_QUADRADO') {
        return todasUnidades.filter(u => u.value !== 'METRO_LINEAR');
      }

      return todasUnidades;
    });

    // Garante que, se as opções de unidade mudarem e o valor selecionado se tornar inválido, o campo seja limpo.
    effect(() => {
      const opcoes = this.opcoesUnidadeEstoque();
      const control = this.form.get('unidadeDeEstoque');
      const valorAtual = control?.value;

      if (valorAtual && opcoes.length > 0 && !opcoes.some(opt => opt.value === valorAtual)) {
        control.setValue(null, { emitEvent: false });
      }
    });

    unidadeEstoque$.pipe(takeUntilDestroyed()).subscribe(unidade => {
      this.updateLarguraValidation(unidade);
    });
  }

  ngOnInit(): void {
    const url = this.data.template._links?.['unidades-de-medida']?.href;
    if (url) {
      this.unitsUrl.set(url);
    } else {
      console.error("URL para 'unidades-de-medida' não encontrada no template do lote.");
    }

    // Inicia o carregamento dos dados do formulário sem bloquear a inicialização do componente.
    this.initializeForm().catch(err => console.error('Erro na inicialização do formulário:', err));
  }

  async initializeForm(): Promise<void> {
    if (this.isEditMode() && this.data.template.tipoMateriaPrimaId) {
      try {
        const tipoMateriaPrima = await lastValueFrom(this.materialTypeService.findById(this.data.template.tipoMateriaPrimaId));
        this.materiaPrimaSignal.set(tipoMateriaPrima);
        this.form.patchValue({
          materiaPrima: tipoMateriaPrima,
          unidadeDeEstoque: this.data.template.unidadeDeEstoque
        });

        if (this.data.template.unidadeDeEstoque) {
          this.updateLarguraValidation(this.data.template.unidadeDeEstoque);
        }

      } catch (error) {
        console.error("Falha ao carregar dados iniciais", error);
        this.entityDialog.showErrorSnackbar("Não foi possível carregar os dados do lote.");
      }
    }

    if (this.isEditMode() && this.data.template.atributos) {
      this.atributos.clear();
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        if (key === 'larguraMm') {
          this.form.get('larguraMm')?.setValue(value);
        } else {
          this.addAtributo(key, value as string, false);
        }
      });
    }
    this.cdr.markForCheck();
  }

  get atributos(): FormArray {
    return this.form.get('atributos') as FormArray;
  }

  get atributosControls(): FormGroup[] {
    return (this.form.get('atributos') as FormArray).controls as FormGroup[];
  }

  get materiaPrimaControl(): FormControl {
    return this.form.get('materiaPrima') as FormControl;
  }

  onMaterialTypeChange(event: MatSelectChange): void {
    const materialType = event.value as TipoMateriaPrima;
    this.materiaPrimaSignal.set(materialType);
    this.form.patchValue({
      materiaPrima: materialType,
      unidadeDeEstoque: materialType.unidadeDeConsumo
    });
  }

  private updateLarguraValidation(unidade: string | null): void {
    const larguraControl = this.form.get('larguraMm');
    if (unidade === 'METRO_LINEAR') {
      larguraControl?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      larguraControl?.clearValidators();
      larguraControl?.reset();
    }
    larguraControl?.updateValueAndValidity();
  }

  addAtributo(chave: string = '', valor: string = '', isNew: boolean = true): void {
    this.atributos.push(this.fb.group({
      chave: [chave, Validators.required],
      valor: [valor, Validators.required],
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

  async removeAtributo(index: number): Promise<void> {
    const attrGroup = this.atributos.at(index);
    const isNew = attrGroup.get('isNew')?.value;

    if (isNew) {
      this.atributos.removeAt(index);
      this.form.get('atributos')?.markAsDirty();
      return;
    }

    const key = attrGroup.get('chave')?.value;
    const dialogData: ConfirmDialogData = {
      title: LoteMateriaPrimaForm.Texts.CONFIRM_DELETE_ATTR_TITLE,
      message: LoteMateriaPrimaForm.Texts.CONFIRM_DELETE_ATTR_MESSAGE(key || 'este atributo')
    };

    const dialogRef = this.dialog.open(ConfirmDialog, { data: dialogData });
    const confirmed = await lastValueFrom(dialogRef.afterClosed());

    if (confirmed) {
      this.atributos.removeAt(index);
      this.form.get('atributos')?.markAsDirty();
      this.cdr.markForCheck();
    }
  }

  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = this.form.getRawValue();
    const materiaPrima: TipoMateriaPrima = formValue.materiaPrima;

    const atributosMap: { [key: string]: any } = {};
    (formValue.atributos || []).forEach((attr: { chave: string; valor: string }) => {
      if (attr.chave) {
        atributosMap[attr.chave] = attr.valor;
      }
    });

    if (this.requiresLargura()) {
      atributosMap['larguraMm'] = formValue.larguraMm;
    }

    const request: LoteMateriaPrimaRequest = {
      tipoMateriaPrimaId: materiaPrima.id,
      unidadeDeEstoque: formValue.unidadeDeEstoque,
      quantidadeInicial: formValue.quantidadeInicial,
      custoTotalLote: formValue.custoTotalLote,
      motivo: formValue.motivo,
      atributos: atributosMap
    };

    const operation = this.isEditMode()
      ? this.loteMateriaPrimaService.update(this.data.template._links!['update']!.href, request)
      : this.loteMateriaPrimaService.create(request);

    operation.subscribe({
      next: () => {
        this.entityDialog.showSuccessSnackbar(this.isEditMode() ? 'Lote atualizado com sucesso!' : 'Lote cadastrado com sucesso!');
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
