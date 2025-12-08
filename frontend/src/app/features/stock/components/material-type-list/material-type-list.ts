import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';
import { TipoMateriaPrimaService } from '../../services/tipo-materia-prima.service';

@Component({
  selector: 'app-material-type-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, BaseTable],
  templateUrl: './material-type-list.html',
  styleUrl: './material-type-list.scss'
})
export class MaterialTypeList extends BaseList<TipoMateriaPrima> implements AfterViewInit {
  private readonly tipoService = inject(TipoMateriaPrimaService);
  private readonly cdr = inject(ChangeDetectorRef);

  tableColumns: TableColumn<TipoMateriaPrima>[] = [];

  @ViewChild('materiaNomeTemplate') materiaNomeTemplate!: TemplateRef<any>;
  @ViewChild('materiaUnidadeTemplate') materiaUnidadeTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;


  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nome', header: 'Nome', sortable: true, cellTemplate: this.materiaNomeTemplate },
      { key: 'unidadeDeConsumo', header: 'Unidade', sortable: false, cellTemplate: this.materiaUnidadeTemplate },
      { key: 'actions', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    const page = this.pagination.pageIndex();
    const size = this.pagination.pageSize();
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.tipoService.findAll(page, size, sort, order).subscribe((response: any) => {
      this.items.set(response._embedded?.['tipos-materia-prima'] || []);
      this.pagination.updateTotalElements(response.page?.totalElements || 0);
    });
  }

  onCreate(): void {
    console.log('Adicionar nova matéria-prima');
    // Placeholder para a lógica de criação
  }

  editItem(item: TipoMateriaPrima): void {
    console.log('editar', item);
    // placeholder
  }

  deleteItem(item: TipoMateriaPrima): void {
    console.log('deletar', item);
    // placeholder
  }
}
