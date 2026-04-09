import { ApiErrorHandlingOptions } from '../../../shared/utils/api-errors';

function looksLikeRawValue(message: string): boolean {
  return !message.includes('obrigat')
    && !message.includes('deve')
    && !message.includes('não pode')
    && !message.includes('nao pode')
    && !message.includes('já existe')
    && !message.includes('ja existe');
}

export const stockApiErrorOptions: ApiErrorHandlingOptions = {
  simplifyFieldMessage: ({ backendField, message }) => {
    if (backendField === 'nome' && looksLikeRawValue(message)) {
      return 'Esse nome já está em uso.';
    }

    if (backendField === 'unidadeDeConsumo' && looksLikeRawValue(message)) {
      return 'Selecione uma unidade de consumo válida.';
    }

    return message;
  }
};
