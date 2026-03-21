import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';

/**
 * Modos de cálculo para ordens de corte.
 */
export type CalculationMode = 'AUTOMATICO' | 'MANUAL';

/**
 * Representa um corte realizado (produto ou retalho).
 */
export interface PerformedCut {
  larguraCm: number;
  comprimentoCm: number;
  quantidade: number;
  tipo: 'PRODUTO' | 'RETALHO';
  retalhoCategoria?: string;
}

/**
 * Representa as margens de segurança aplicadas em um corte.
 */
export interface Margins {
  superior: number;
  inferior: number;
  esquerda: number;
  direita: number;
}

/**
 * Representa uma Ordem de Produção.
 */
export interface ProductionOrder {
  id: number;
  produtoId: number;
  nomeProduto: string;
  tipoProduto?: 'CORTE' | 'CONSUMO';
  lotesConsumidosIds: number[];
  quantidadeProduzida: number;
  modoCalculo?: CalculationMode;
  dataCriacao: string; // ISO Date
  dataAtualizacao: string; // ISO Date
  motivo?: string;

  // Campos específicos de Corte
  larguraFinalCm?: number;
  comprimentoFinalCm?: number;
  rotacionado?: boolean;
  cortesRealizados?: PerformedCut[];
  margens?: Margins;

  _links?: Hateoas['_links'];
}

/**
 * Resposta paginada da API para listagem de ordens de produção.
 */
export interface ApiResponseProduction extends Hateoas {
  _embedded: {
    ordensDeProducao: ProductionOrder[];
  };
  page: PageInfo;
}
