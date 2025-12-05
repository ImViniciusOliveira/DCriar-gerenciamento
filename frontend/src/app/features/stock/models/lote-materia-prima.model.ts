export interface LoteMateriaPrima {
  id: string | number;
  tipoMateriaPrima?: { id?: string | number; nome?: string };
  saldoEstoque?: number;
  unidadeDeEstoque?: string;
  dataCriacao?: string | Date;
}
