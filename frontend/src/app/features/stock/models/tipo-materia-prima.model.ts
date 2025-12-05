import { UnidadeDeMedida } from "../../../../shared/models/unidade-de-medida.model";
import { Auditable } from "../../../../shared/models/auditable.model";
import { Hateoas } from "../../../../core/models/hateoas.model";
import { PageInfo } from "../../../products/models/product.model"; // Reutilizando PageInfo

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
  page?: PageInfo; // Reutilizando PageInfo
}
