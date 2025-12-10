import { Hateoas } from '../../../core/models/hateoas.model';

/**
 * Representa a entidade TipoMateriaPrima como recebida da API.
 */
export interface TipoMateriaPrima {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  _links?: Hateoas['_links'];
}

/**
 * Representa o payload para criar ou atualizar um TipoMateriaPrima.
 * Geralmente um subconjunto da interface principal, sem campos gerados pelo servidor.
 */
export interface TipoMateriaPrimaRequest {
  nome: string;
  unidadeDeConsumo: string;
}
