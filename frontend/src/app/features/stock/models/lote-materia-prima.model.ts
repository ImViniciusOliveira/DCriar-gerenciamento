import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

export interface LoteMateriaPrima {
  id: string | number;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  unidadeDeEstoque?: string;
  saldoEstoque?: number;
  custoTotalLote?: number;
  atributos?: { [key: string]: any };
  motivo?: string;
  _links?: Hateoas['_links'];
}

export interface LoteMateriaPrimaRequest {
  tipoMateriaPrimaId: number;
  unidadeDeEstoque: string;
  quantidadeInicial: number;
  custoTotalLote: number;
  atributos?: { [key: string]: any };
  motivo: string;
}

export interface ApiResponseLotes extends Hateoas {
  _embedded: {
    ['lotes-materia-prima']: LoteMateriaPrima[];
  };
  page: PageInfo;
}
