/**
 * Representa um link individual em uma resposta HATEOAS.
 */
export interface Link {
  /** A URL do recurso, que pode ser um URI Template. */
  href: string;
  /** Indica se a URL é um URI Template (contém variáveis como `{?page,size}`). */
  templated?: boolean;
}

/**
 * Interface base para qualquer recurso que segue o padrão HATEOAS (Hypermedia as an Engine of Application State).
 *
 * Recursos HATEOAS incluem um objeto `_links` que contém URLs para ações e recursos relacionados,
 * permitindo que o cliente descubra dinamicamente as capacidades da API.
 */
export interface Hateoas {
  _links?: {
    [key: string]: Link;
  };
}

/**
 * Representa as informações de paginação retornadas por uma API paginada.
 */
export interface PageInfo {
  /** O número máximo de elementos por página. */
  size: number;
  /** O número total de elementos disponíveis. */
  totalElements: number;
  /** O número total de páginas. */
  totalPages: number;
  /** O índice da página atual (baseado em zero). */
  number: number;
}
