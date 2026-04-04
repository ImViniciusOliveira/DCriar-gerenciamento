import { Hateoas } from "../../../core/models/hateoas.model";

/**
 * Representa a entidade Canal de Venda pura.
 * Utilizada para listagens de seleção e gerenciamento (CRUD).
 */
export interface Channel {
  id: number;
  nome: string;
  _links?: Hateoas['_links'];
}

export interface ChannelRequest {
  nome: string;
}

/**
 * Estrutura de resposta da API para listagem de canais.
 */
export interface ApiResponseChannels extends Hateoas {
  _embedded: {
    'canais-venda': Channel[];
  };
}
