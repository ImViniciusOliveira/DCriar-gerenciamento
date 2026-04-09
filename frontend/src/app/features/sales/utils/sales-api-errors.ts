import { ApiErrorHandlingOptions } from '../../../shared/utils/api-errors';

export const salesApiErrorOptions: ApiErrorHandlingOptions = {
  simplifyFieldMessage: ({ backendField, message }) => {
    switch (backendField) {
      case 'cpf':
        return 'CPF inválido.';
      case 'cep':
        return 'CEP inválido.';
      case 'estado':
        return 'Estado inválido.';
      default:
        return message;
    }
  }
};
