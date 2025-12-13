import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

/**
 * Representa a entidade TipoMateriaPrima como recebida da API.
 * O nome do arquivo é material-type.model.ts para padronização, mas a interface
 * mantém o nome em português para consistência com o backend.
 */
export interface MaterialType {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links'];
}

/**
 * Representa o payload para criar ou atualizar um TipoMateriaPrima.
 */
export interface MaterialTypeRequest {
  nome: string;
  unidadeDeConsumo: string;
}

/**
 * Representa a estrutura aninhada `_embedded` para listas de TipoMateriaPrima.
 */
export interface EmbeddedMaterialTypes {
  'tipos-materia-prima': MaterialType[];
}

/**
 * Representa a resposta completa da API para uma busca paginada de TipoMateriaPrima.
 */
export interface ApiResponseMaterialTypes extends Hateoas {
  _embedded: EmbeddedMaterialTypes;
  page: PageInfo;
}
