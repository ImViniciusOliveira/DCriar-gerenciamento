import { ApiErrorHandlingOptions } from '../../../shared/utils/api-errors';

function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  const units = ['KB', 'MB', 'GB'];
  let value = bytes / 1024;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex++;
  }

  const rounded = value >= 10 ? Math.round(value) : Math.round(value * 10) / 10;
  return `${rounded} ${units[unitIndex]}`;
}

export const productApiErrorOptions: ApiErrorHandlingOptions = {
  simplifyFieldMessage: ({ backendField, message }) => {
    if (backendField === 'sku' && !message.includes('SKU') && !message.includes('sku')) {
      return 'Esse SKU já está em uso.';
    }

    if (backendField === 'nome' && !message.includes('nome') && !message.includes('Nome')) {
      return 'Já existe um produto com esse nome.';
    }

    return message;
  },
  resolveMessageByCode: ({ details }) => {
    if (details['codigo'] !== 'MAX_UPLOAD_SIZE_EXCEEDED') {
      return null;
    }

    const limitBytes = typeof details['limiteBytes'] === 'number'
      ? details['limiteBytes']
      : null;

    if (limitBytes && limitBytes > 0) {
      return `O arquivo excede o limite permitido de ${formatBytes(limitBytes)}.`;
    }

    return 'O arquivo excede o limite permitido para upload.';
  }
};
