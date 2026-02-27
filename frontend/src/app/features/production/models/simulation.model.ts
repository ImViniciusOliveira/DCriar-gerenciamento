/**
 * Representa o resultado de uma simulação de corte.
 * O campo 'tipoSimulacao' atua como um discriminador para type guards.
 */
export interface SimulationCutResult {
  tipoSimulacao: 'CORTE';
  modoCalculo: string;
  larguraFinalCm: number;
  comprimentoFinalCm: number;
  consumoEstimado: number;
}

/**
 * Representa o resultado de uma simulação de consumo direto.
 * O campo 'tipoSimulacao' atua como um discriminador para type guards.
 */
export interface SimulationConsumptionResult {
  tipoSimulacao: 'CONSUMO_DIRETO';
  consumoTotalEstimado: number;
  unidadeDeConsumo: string;
}

/**
 * Um tipo união que representa qualquer resultado de simulação possível.
 * Permite o uso de type guards para diferenciar os resultados de forma segura.
 */
export type SimulationResult = SimulationCutResult | SimulationConsumptionResult;
