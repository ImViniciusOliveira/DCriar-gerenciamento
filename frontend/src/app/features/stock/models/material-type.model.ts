import { Hateoas } from '../../../core/models/hateoas.model';
// Caminho corrigido
import { PageInfo } from '../../products/models/product.model';

export interface MaterialType {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links'];
}

export interface EmbeddedMaterialTypes {
  'tipos-materia-prima': MaterialType[];
}

export interface ApiResponseMaterialTypes extends Hateoas {
  _embedded: EmbeddedMaterialTypes;
  page: PageInfo;
}
