import { Hateoas, PageInfo } from "../../../core/models/hateoas.model";

export interface LoteMateriaPrima {
  id: string | number;
  tipoMateriaPrimaId: number;
  nomeTipoMateriaPrima: string;
  saldoEstoque?: number;
  unidadeDeEstoque?: string;
  atributos?: { [key: string]: any };
  _links?: Hateoas['_links'];
}

export interface ApiResponseLotes extends Hateoas {
  _embedded: {
    ['lotes-materia-prima']: LoteMateriaPrima[];
  };
  page: PageInfo;
}
