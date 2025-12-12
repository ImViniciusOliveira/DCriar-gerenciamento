import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, Signal } from '@angular/core';
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
 * Formulário para criação e edição de Lotes de Matéria-Prima.
 * Adapta-se dinamicamente à matéria-prima selecionada, exigindo atributos
 * específicos como 'larguraMm' quando a unidade de estoque é METRO_LINEAR.
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
  /** Signal que determina se o campo 'larguraMm' deve ser exibido e obrigatório. */
  requiresLargura: Signal<boolean>;

  /** Signal que contém a lista de opções para o select 'Unidade de Estoque'. */
  unidadesDeMedida: Signal<EnumOption[]>;
  private readonly unitsUrl = signal<string | null>(null);

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
    // Reage à seleção da unidade de estoque para determinar se a largura é necessária.
    this.requiresLargura = toSignal(unidadeEstoque$.pipe(map(unidade => unidade === 'METRO_LINEAR')), { initialValue: false });

    // Converte o signal da URL em um Observable para que o `switchMap` possa reagir a ele.
    const unitsUrl$ = toObservable(this.unitsUrl).pipe(
      filter((url): url is string => !!url)
    );

    // Busca as unidades de medida de forma reativa assim que a URL for descoberta.
    this.unidadesDeMedida = toSignal(
      unitsUrl$.pipe(
        switchMap(url => this.enumService.getEnumOptions(url, 'unidadesDeMedida'))
      ), { initialValue: [] }
    );

    // Ajusta a validação do campo de largura sempre que a unidade de estoque mudar.
    unidadeEstoque$.pipe(takeUntilDestroyed()).subscribe(unidade => {
      this.updateLarguraValidation(unidade);
    });
  }

  ngOnInit(): void {
    // Pega a URL para as unidades de medida do link HATEOAS fornecido pelo backend.
    const url = this.data.template._links?.['unidades-de-medida']?.href;
    if (url) {
      this.unitsUrl.set(url);
    } else {
      console.error("URL para 'unidades-de-medida' não encontrada no template do lote. O select de unidades ficará vazio.");
    }

    this.initializeForm();
  }

  /**
   * Prepara o formulário, populando-o com dados existentes se estiver em modo de edição.
   */
  async initializeForm(): Promise<void> {
    if (this.isEditMode() && this.data.template.tipoMateriaPrimaId) {
      try {
        const tipoMateriaPrima = await lastValueFrom(this.materialTypeService.findById(this.data.template.tipoMateriaPrimaId));
        this.form.get('materiaPrima')?.setValue(tipoMateriaPrima);

        const unidadeEstoque = this.data.template.unidadeDeEstoque;
        this.form.get('unidadeDeEstoque')?.setValue(unidadeEstoque);
        if (unidadeEstoque) {
          this.updateLarguraValidation(unidadeEstoque);
        }

      } catch (error) {
        console.error("Falha ao carregar dados iniciais", error);
        this.entityDialog.showErrorSnackbar("Não foi possível carregar os dados do lote.");
      }
    }

    if (this.isEditMode() && this.data.template.atributos) {
      this.atributos.clear();
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        // Separa o atributo 'larguraMm' para o campo dedicado.
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

  /**
   * Ao selecionar uma matéria-prima, define a 'unidadeDeEstoque' sugerida
   * com base na 'unidadeDeConsumo' do material.
   */
  onMaterialTypeChange(event: MatSelectChange): void {
    const materialType = event.value as TipoMateriaPrima;
    this.form.get('unidadeDeEstoque')?.setValue(materialType.unidadeDeConsumo);
  }

  /**
   * Adiciona ou remove o validador 'required' do campo 'larguraMm'
   * com base na unidade de estoque selecionada.
   */
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

  /**
   * Adiciona um novo atributo dinâmico ao formulário.
   */
  addAtributo(chave: string = '', valor: string = '', isNew: boolean = true): void {
    this.atributos.push(this.fb.group({
      chave: [chave, Validators.required],
      valor: [valor, Validators.required],
      isNew: [isNew] // Controle interno para a lógica de remoção
    }));

    if (isNew) {
      // Scroll automático para o novo elemento adicionado.
      setTimeout(() => {
        const dialogContent = (this.dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
        if (dialogContent) {
          dialogContent.scrollTop = dialogContent.scrollHeight;
        }
      }, 100);
    }
  }

  /**
   * Remove um atributo, pedindo confirmação se ele já existia.
   */
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

  /**
   * Processa e salva os dados do formulário.
   */
  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = this.form.getRawValue();
    const materiaPrima: TipoMateriaPrima = formValue.materiaPrima;

    // Junta os atributos dinâmicos com o atributo de largura, se aplicável.
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
