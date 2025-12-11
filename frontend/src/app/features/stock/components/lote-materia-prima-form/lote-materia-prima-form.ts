import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { lastValueFrom } from 'rxjs';

import { LoteMateriaPrima, LoteMateriaPrimaRequest } from '../../models/lote-materia-prima.model';
import { TipoMateriaPrima } from '../../models/material-type.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeService } from '../../services/material-type.service';
import { EntityDialogService } from '../../../../shared/services/entity-dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MateriaPrimaSearchComponent } from '../../../../shared/components/materia-prima-search/materia-prima-search';
import { ConfirmDialog, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog';

export interface LoteMateriaPrimaFormData {
  template: LoteMateriaPrima;
  title: string;
}

/**
 * Formulário para criação e edição de Lotes de Matéria-Prima.
 * Inclui lógica de UX avançada para manipulação de atributos dinâmicos,
 * como scroll automático e confirmação de remoção.
 */
@Component({
  selector: 'app-lote-materia-prima-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MateriaPrimaSearchComponent
  ],
  templateUrl: './lote-materia-prima-form.html',
  styleUrls: ['./lote-materia-prima-form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoteMateriaPrimaForm implements OnInit {
  // --- Injeção de Dependências ---
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaForm>);
  private readonly dialog = inject(MatDialog);
  private readonly loteMateriaPrimaService = inject(LoteMateriaPrimaService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  public readonly data: LoteMateriaPrimaFormData = inject(MAT_DIALOG_DATA);

  // --- Estado do Componente ---
  form: FormGroup;
  isEditMode = signal(false);

  // --- Constantes de Texto ---
  private static readonly Texts = {
    CONFIRM_DELETE_ATTR_TITLE: 'Confirmar Remoção',
    CONFIRM_DELETE_ATTR_MESSAGE: (key: string) => `Deseja realmente remover o atributo "${key}"?`
  };

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      materiaPrima: [null, Validators.required],
      quantidadeInicial: [this.data.template?.saldoEstoque || '', [Validators.required, Validators.min(0.01)]],
      custoTotalLote: [this.data.template?.custoTotalLote || '', [Validators.required, Validators.min(0.01)]],
      motivo: [this.data.template?.motivo || '', Validators.required],
      atributos: this.fb.array([])
    });
  }

  async ngOnInit(): Promise<void> {
    await this.initializeForm();
  }

  /**
   * Prepara o formulário, populando-o com dados existentes se estiver em modo de edição.
   */
  async initializeForm(): Promise<void> {
    if (this.isEditMode() && this.data.template.tipoMateriaPrimaId) {
      try {
        const tipoMateriaPrima = await lastValueFrom(this.materialTypeService.findById(this.data.template.tipoMateriaPrimaId));
        this.form.get('materiaPrima')?.setValue(tipoMateriaPrima);
      } catch (error) {
        console.error("Falha ao carregar matéria-prima inicial", error);
        this.entityDialog.showErrorSnackbar("Não foi possível carregar os dados da matéria-prima.");
      }
    }

    if (this.isEditMode() && this.data.template.atributos) {
      this.atributos.clear();
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        // Popula os atributos existentes, marcando-os como "não novos".
        this.addAtributo(key, value as string, false);
      });
    }
    this.cdr.markForCheck();
  }

  // --- Getters para Acesso Fácil ao Template ---
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
   * Adiciona um novo atributo ao formulário.
   * Se for um atributo novo (não um existente carregado na inicialização),
   * a tela rola para baixo para exibi-lo, melhorando a UX.
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
   * Remove um atributo do formulário.
   * - Se o atributo foi recém-adicionado (isNew = true), remove imediatamente.
   * - Se o atributo já existia, pede confirmação ao usuário antes de remover.
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

    // Converte o FormArray de atributos de volta para um mapa [chave]: valor.
    const atributosMap: { [key: string]: string } = {};
    (formValue.atributos || []).forEach((attr: { chave: string; valor: string }) => {
      if (attr.chave) {
        atributosMap[attr.chave] = attr.valor;
      }
    });

    const request: LoteMateriaPrimaRequest = {
      tipoMateriaPrimaId: materiaPrima.id,
      unidadeDeEstoque: materiaPrima.unidadeDeConsumo,
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
