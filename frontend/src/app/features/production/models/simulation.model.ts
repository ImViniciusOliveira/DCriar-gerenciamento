/**
 * Representa um único link HATEOAS.
 */
export interface HateoasLink {
  href: string;
}

/**
 * Representa o objeto _links em uma resposta HATEOAS.
 */
export interface HateoasLinks {
  [key: string]: HateoasLink;
}

/**
 * Interface base para qualquer resposta de API que siga o padrão HATEOAS,
 * incluindo a propriedade opcional _links.
 */
export interface RepresentationModel {
  _links?: HateoasLinks;
}

/**
 * Representa o resultado de uma simulação de corte.
 * O campo 'tipoSimulacao' atua como um discriminador para type guards.
 */
export interface SimulationCutResult extends RepresentationModel {
  tipoSimulacao: 'CORTE';
  modoCalculo: string;
  larguraFinalCm: number;
  comprimentoFinalCm: number;
  consumoEstimado: number;
  rotacionado: boolean;
  produtosPorLinha: number;
  numeroLinhasCompletas: number;
  produtosNaUltimaLinha: number;
  sobraLateral: string;
  sobraInferior: string;
  dimensaoProduto: string;
  consumoTotal: string;
  larguraBlocoProdutosCm?: number;
  comprimentoBlocoProdutosCm?: number;
}

/**
 * Representa o resultado de uma simulação de consumo.
 * O campo 'tipoSimulacao' atua como um discriminador para type guards.
 */
export interface SimulationConsumptionResult extends RepresentationModel {
  tipoSimulacao: 'CONSUMO';
  consumoTotalEstimado: number;
  unidadeDeConsumo: string;
  planoDeConsumo: Array<{
    loteId: number;
    motivoLote: string;
    quantidadeAConsumir: number;
  }>;
  saldoRestante: Record<number, number>;
}

/**
 * Um tipo união que representa qualquer resultado de simulação possível.
 * Permite o uso de type guards para diferenciar os resultados de forma segura.
 */
export type SimulationResult = SimulationCutResult | SimulationConsumptionResult;
