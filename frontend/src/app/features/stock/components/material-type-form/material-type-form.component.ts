import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Observable, startWith, map } from 'rxjs';

// Definição dos dados que o diálogo espera receber
export interface MaterialTypeFormData {
  template: any; // O "esqueleto" recebido da API
  title: string;
}

@Component({
  selector: 'app-material-type-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatAutocompleteModule
  ],
  templateUrl: './material-type-form.component.html',
  styleUrls: ['./material-type-form.component.scss']
})
export class MaterialTypeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<MaterialTypeFormComponent>);
  public readonly data: MaterialTypeFormData = inject(MAT_DIALOG_DATA);

  form!: FormGroup;
  unidades$: Observable<string[]>;
  unidades: string[] = []; // Array para armazenar as unidades de medida

  constructor() {
    this.unidades$ = new Observable<string[]>();
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      nome: ['', Validators.required],
      unidadeDeConsumo: ['', Validators.required]
    });

    // Aqui virá a lógica para buscar as unidades de medida do link HATEOAS
  }

  onSave(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
