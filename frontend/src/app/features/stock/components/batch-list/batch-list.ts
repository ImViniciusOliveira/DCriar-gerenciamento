import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { BaseList } from '../../../../shared/components/base-list/base-list';
import { LoteMateriaPrima } from '../../models/lote-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { MaterialTypeList } from '../material-type-list/material-type-list';
import { DetailsPopoverComponent } from '../../../../shared/components/details-popover/details-popover.component';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    BaseTable,
    DetailsPopoverComponent
  ],
  templateUrl: './batch-list.html',
  styleUrl: './batch-list.scss'
})
export class BatchList extends BaseList<LoteMateriaPrima> implements AfterViewInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly loteService = inject(LoteMateriaPrimaService);
  private readonly dialog = inject(MatDialog);

  tableColumns: TableColumn<LoteMateriaPrima>[] = [];

  @ViewChild('tipoTemplate') tipoTemplate!: TemplateRef<any>;
  @ViewChild('saldoTemplate') saldoTemplate!: TemplateRef<any>;
  @ViewChild('unidadeTemplate') unidadeTemplate!: TemplateRef<any>;
  @ViewChild('atributosTemplate') atributosTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  ngAfterViewInit(): void {
    this.tableColumns = [
      { key: 'nomeTipoMateriaPrima', header: 'Matéria-Prima', sortable: true, cellTemplate: this.tipoTemplate },
      { key: 'saldoEstoque', header: 'Saldo', sortable: true, cellTemplate: this.saldoTemplate },
      { key: 'unidadeDeEstoque', header: 'Unidade', sortable: true, cellTemplate: this.unidadeTemplate },
      { key: 'atributos', header: 'Atributos', sortable: false, cellTemplate: this.atributosTemplate },
      { key: 'acoes', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    this.cdr.detectChanges();
  }

  override loadItems(): void {
    const page = this.pagination.pageIndex();
    const size = this.pagination.pageSize();
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.loteService.findAll(page, size, sort, order).subscribe((response: any) => {
      this.items.set(response._embedded?.['lotes-materia-prima'] || []);
      this.pagination.updateTotalElements(response.page?.totalElements || 0);
    });
  }

  openMaterialTypeDialog(): void {
    this.dialog.open(MaterialTypeList, {
      width: '80vw',
      maxWidth: '900px',
      height: '80vh'
    });
  }

  onCreate(): void {
    console.log('Adicionar novo lote');
  }

  editLote(lote: LoteMateriaPrima): void {
    console.log('editar', lote);
  }

  deleteLote(lote: LoteMateriaPrima): void {
    console.log('deletar', lote);
  }

  getAtributosAsArray(atributos: { [key: string]: any }): { key: string, value: any }[] {
    if (!atributos) {
      return [];
    }
    return Object.entries(atributos).map(([key, value]) => ({ key, value }));
  }
}
