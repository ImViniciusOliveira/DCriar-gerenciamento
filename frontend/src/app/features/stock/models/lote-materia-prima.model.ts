import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

export interface LoteMateriaPrima {
  id: string | number;
  tipoMateriaPrima?: { id?: string | number; nome?: string };
  saldoEstoque?: number;
  unidadeDeEstoque?: string;
  dataCriacao?: string | Date;
}

export interface ApiResponseLotes extends Hateoas {
  _embedded: {
    loteMateriaPrimaList: LoteMateriaPrima[];
  };
  page: PageInfo;
}
