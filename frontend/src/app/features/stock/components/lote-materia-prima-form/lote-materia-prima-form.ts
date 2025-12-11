import { Component, OnInit, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
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

export interface LoteMateriaPrimaFormData {
  template: LoteMateriaPrima;
  title: string;
}

/**
 * Formulário para criação e edição de Lotes de Matéria-Prima.
 * Utiliza o componente genérico `app-materia-prima-search` para a seleção do tipo.
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
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LoteMateriaPrimaForm>);
  private readonly loteMateriaPrimaService = inject(LoteMateriaPrimaService);
  private readonly materialTypeService = inject(MaterialTypeService);
  private readonly entityDialog = inject(EntityDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  public readonly data: LoteMateriaPrimaFormData = inject(MAT_DIALOG_DATA);

  form: FormGroup;
  isEditMode = signal(false);

  constructor() {
    this.isEditMode.set(!!this.data.template.id);

    this.form = this.fb.group({
      // Este control recebe o objeto TipoMateriaPrima completo do componente de busca.
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

  async initializeForm(): Promise<void> {
    // Se estiver editando, busca o objeto TipoMateriaPrima completo para popular o form.
    // O componente de busca lidará com este valor assíncrono para mostrar a "última matéria-prima".
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
      Object.entries(this.data.template.atributos).forEach(([key, value]) => {
        this.addAtributo(key, value as string);
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

  // Getter para facilitar o acesso ao FormControl no template.
  get materiaPrimaControl(): FormControl {
    return this.form.get('materiaPrima') as FormControl;
  }

  addAtributo(chave: string = '', valor: string = ''): void {
    this.atributos.push(this.fb.group({
      chave: [chave, Validators.required],
      valor: [valor, Validators.required]
    }));
  }

  removeAtributo(index: number): void {
    this.atributos.removeAt(index);
  }

  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = this.form.getRawValue();
    const materiaPrima: TipoMateriaPrima = formValue.materiaPrima;

    const atributosMap: { [key: string]: string } = {};
    (formValue.atributos || []).forEach((attr: { chave: string; valor: string }) => {
      if (attr.chave) {
        atributosMap[attr.chave] = attr.valor;
      }
    });

    // Monta o payload da requisição com os dados corretos.
    const request: LoteMateriaPrimaRequest = {
      tipoMateriaPrimaId: materiaPrima.id,
      unidadeDeEstoque: materiaPrima.unidadeDeConsumo, // A unidade de estoque é a unidade de consumo da matéria-prima.
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
