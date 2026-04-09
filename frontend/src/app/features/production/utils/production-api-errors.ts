import { ApiErrorHandlingOptions } from '../../../shared/utils/api-errors';

export const productionApiErrorOptions: ApiErrorHandlingOptions = {
  resolveMessageByCode: ({ details }) => {
    if (details['codigo'] !== 'INCOMPATIBILIDADE_MATERIAL') {
      return null;
    }

    const productMaterial = typeof details['nomeMateriaPrimaProduto'] === 'string'
      ? details['nomeMateriaPrimaProduto']
      : null;
    const batchMaterial = typeof details['nomeMateriaPrimaLote'] === 'string'
      ? details['nomeMateriaPrimaLote']
      : null;

    if (productMaterial && batchMaterial) {
      return `O produto usa "${productMaterial}", mas o lote usa "${batchMaterial}". Selecione um lote compatível.`;
    }

    return 'O produto e o lote usam matérias-primas diferentes. Selecione um lote compatível.';
  }
};
