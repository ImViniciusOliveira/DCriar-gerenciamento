import { Auditable } from '../../../shared/models/auditable.model';
import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';
import { UnidadeDeMedida } from '../../../shared/models/unidade-de-medida.model';

export interface TipoMateriaPrima extends Auditable {
  id: number;
  nome: string;
  unidadeDeConsumo: UnidadeDeMedida;
  _links?: Hateoas['_links'];
}

export interface TipoMateriaPrimaRequest {
  nome: string;
  unidadeDeConsumo: UnidadeDeMedida;
}

export interface EmbeddedTipoMateriaPrima {
  tiposMateriaPrima: TipoMateriaPrima[];
}

export interface ApiResponseTipoMateriaPrima extends Hateoas {
  _embedded: EmbeddedTipoMateriaPrima;
  page?: PageInfo;
}
