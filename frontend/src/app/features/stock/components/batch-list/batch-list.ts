import { Component, inject, ViewChild, TemplateRef, AfterViewInit, ChangeDetectorRef, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { BaseTable, TableColumn } from '../../../../shared/components/base-table/base-table';
import { TipoMateriaPrima } from '../../models/tipo-materia-prima.model';
import { TipoMateriaPrimaService } from '../../services/tipo-materia-prima.service';
import { LoteMateriaPrima } from '../../models/lote-materia-prima.model';
import { LoteMateriaPrimaService } from '../../services/lote-materia-prima.service';
import { PaginationHandler } from '../../../../shared/services/pagination-handler';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, BaseTable, MatTableModule],
  templateUrl: './batch-list.html',
  styleUrl: './batch-list.scss'
})
export class BatchList implements AfterViewInit {
  private readonly tipoService = inject(TipoMateriaPrimaService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly loteService = inject(LoteMateriaPrimaService);
  readonly pagination = inject(PaginationHandler);

  showMateriasPrima: WritableSignal<boolean> = signal(false);
  materiaPrimaItems: WritableSignal<TipoMateriaPrima[]> = signal([]);
  materiaPrimaColumns: TableColumn<TipoMateriaPrima>[] = [];

  // Lotes state (merged from lote-materia-prima-list)
  lotes: WritableSignal<LoteMateriaPrima[]> = signal([]);
  loteColumns: TableColumn<LoteMateriaPrima>[] = [];

  @ViewChild('materiaNomeTemplate') materiaNomeTemplate!: TemplateRef<any>;
  @ViewChild('materiaUnidadeTemplate') materiaUnidadeTemplate!: TemplateRef<any>;

  @ViewChild('idTemplate') idTemplate!: TemplateRef<any>;
  @ViewChild('tipoTemplate') tipoTemplate!: TemplateRef<any>;
  @ViewChild('saldoTemplate') saldoTemplate!: TemplateRef<any>;
  @ViewChild('unidadeTemplate') unidadeTemplate!: TemplateRef<any>;
  @ViewChild('dataCriacaoTemplate') dataCriacaoTemplate!: TemplateRef<any>;
  @ViewChild('acoesTemplate') acoesTemplate!: TemplateRef<any>;

  ngAfterViewInit(): void {
    // inicializa templates de colunas dos lotes para que a tabela principal possa renderizar
    this.loteColumns = [
      { key: 'id', header: 'ID Lote', sortable: true, cellTemplate: this.idTemplate },
      { key: 'tipoMateriaPrima', header: 'Matéria-Prima', sortable: false, cellTemplate: this.tipoTemplate },
      { key: 'saldoEstoque', header: 'Saldo', sortable: true, cellTemplate: this.saldoTemplate },
      { key: 'unidadeDeEstoque', header: 'Unidade', sortable: true, cellTemplate: this.unidadeTemplate },
      { key: 'dataCriacao', header: 'Data de Entrada', sortable: true, cellTemplate: this.dataCriacaoTemplate },
      { key: 'actions', header: 'Ações', cellTemplate: this.acoesTemplate }
    ];
    // força detecção de mudanças para registrar os ViewChild
    this.cdr.detectChanges();
  }

  toggleMateriasPrima(): void {
    const next = !this.showMateriasPrima();
    this.showMateriasPrima.set(next);
    this.cdr.detectChanges();
    if (next) {
      this.loadMateriasPrima();
    }
  }

  loadMateriasPrima(): void {
    // carregamento simples (primeira página) para mostrar itens; paginação pode ser adicionada depois
    this.tipoService.findAll(0, 50, 'nome', 'asc').subscribe(response => {
      this.materiaPrimaItems.set(response._embedded?.tiposMateriaPrima || []);
      if (this.materiaPrimaColumns.length === 0) {
        this.materiaPrimaColumns = [
          { key: 'nome', header: 'Nome', sortable: true, cellTemplate: this.materiaNomeTemplate },
          { key: 'unidadeDeConsumo', header: 'Unidade', sortable: false, cellTemplate: this.materiaUnidadeTemplate }
        ];
      }
      this.cdr.detectChanges();
    });
  }

  // Lotes: carregamento e handlers de paginação
  loadLotes(): void {
    const page = this.pagination.pageIndex();
    const size = this.pagination.pageSize();
    const sort = this.pagination.sortActive();
    const order = this.pagination.sortDirection();

    this.loteService.findAll(page, size, sort, order).subscribe((response: any) => {
      this.lotes.set(response._embedded?.loteMateriaPrimaList || []);
      this.pagination.updateTotalElements(response.page?.totalElements || 0);
      this.cdr.detectChanges();
    });
  }

  onPageChange(event: any): void {
    // delega o evento ao PaginationHandler
    this.pagination.handlePageEvent(event);
    this.loadLotes();
  }

  onSortChange(sort: any): void {
    this.pagination.handleSortChange(sort);
    this.loadLotes();
  }

  // Ações para as linhas de lote
  editLote(lote: LoteMateriaPrima): void {
    console.log('editar', lote);
    // placeholder: open edit dialog when implemented
  }

  deleteLote(lote: LoteMateriaPrima): void {
    console.log('deletar', lote);
    // placeholder: confirm & delete when implemented
  }
}
