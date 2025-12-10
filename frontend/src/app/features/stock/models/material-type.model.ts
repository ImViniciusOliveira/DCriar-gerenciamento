import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

/**
 * Representa a entidade TipoMateriaPrima como recebida da API.
 * O nome do arquivo é material-type.model.ts para padronização, mas a interface
 * mantém o nome em português para consistência com o backend.
 */
export interface TipoMateriaPrima {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links'];
}

/**
 * Representa o payload para criar ou atualizar um TipoMateriaPrima.
 */
export interface TipoMateriaPrimaRequest {
  nome: string;
  unidadeDeConsumo: string;
}

/**
 * Representa a estrutura aninhada `_embedded` para listas de TipoMateriaPrima.
 */
export interface EmbeddedTiposMateriaPrima {
  'tipos-materia-prima': TipoMateriaPrima[];
}

/**
 * Representa a resposta completa da API para uma busca paginada de TipoMateriaPrima.
 */
export interface ApiResponseTiposMateriaPrima extends Hateoas {
  _embedded: EmbeddedTiposMateriaPrima;
  page: PageInfo;
}
