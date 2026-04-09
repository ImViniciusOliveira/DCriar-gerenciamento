import { FormGroup } from '@angular/forms';
import { clearControlError, setControlError } from './control-errors';

export interface ApiErrorPayload {
  status?: number;
  message?: string;
  detail?: string;
  details?: Record<string, unknown>;
}

export interface ApiErrorHandlingOptions {
  fieldMap?: Record<string, string>;
  simplifyFieldMessage?: (params: {
    backendField: string;
    controlPath: string;
    message: string;
  }) => string;
  resolveMessageByCode?: (params: {
    payload: ApiErrorPayload;
    details: Record<string, unknown>;
  }) => string | null;
}

const KNOWN_FIELD_KEYS = new Set([
  'cpf',
  'cep',
  'estado',
  'observacao',
  'nome',
  'sku',
  'descricao',
  'precoComercial',
  'unidadesPorProduto',
  'unidadeCadastroConsumo',
  'cor',
  'codigoFabricante',
  'unidadeDeEstoque',
  'quantidadeInicial',
  'custoTotalLote',
  'motivo',
  'larguraMm',
  'unidadeDeConsumo'
]);

function sanitizeText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function asString(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null;
  }

  const sanitized = sanitizeText(value);
  return sanitized.length > 0 ? sanitized : null;
}

function defaultSimplifyFieldMessage(message: string): string {
  return sanitizeText(message);
}

export function extractApiErrorPayload(error: unknown): ApiErrorPayload {
  if (!error || typeof error !== 'object') {
    return {};
  }

  const maybeHttpError = error as { error?: unknown };
  const payload = maybeHttpError.error && typeof maybeHttpError.error === 'object'
    ? maybeHttpError.error as Record<string, unknown>
    : error as Record<string, unknown>;

  const details = payload['details'];

  return {
    status: typeof payload['status'] === 'number' ? payload['status'] : undefined,
    message: asString(payload['message']) ?? undefined,
    detail: asString(payload['detail']) ?? undefined,
    details: details && typeof details === 'object' && !Array.isArray(details)
      ? details as Record<string, unknown>
      : undefined
  };
}

export function collectApiFieldErrors(
  error: unknown,
  options: ApiErrorHandlingOptions = {}
): Record<string, string> {
  const payload = extractApiErrorPayload(error);
  const details = payload.details;

  if (!details) {
    return {};
  }

  const fieldErrors: Record<string, string> = {};
  const fieldMap = options.fieldMap ?? {};
  const hasExplicitFieldMap = Object.keys(fieldMap).length > 0;

  for (const [backendField, rawValue] of Object.entries(details)) {
    const message = asString(rawValue);
    if (!message) {
      continue;
    }

    if (hasExplicitFieldMap) {
      const controlPath = fieldMap[backendField];
      if (!controlPath) {
        continue;
      }

      fieldErrors[controlPath] = options.simplifyFieldMessage?.({
        backendField,
        controlPath,
        message
      }) ?? defaultSimplifyFieldMessage(message);
      continue;
    }

    if (!KNOWN_FIELD_KEYS.has(backendField)) {
      continue;
    }

    fieldErrors[backendField] = options.simplifyFieldMessage?.({
      backendField,
      controlPath: backendField,
      message
    }) ?? defaultSimplifyFieldMessage(message);
  }

  return fieldErrors;
}

export function applyApiFieldErrors(
  form: FormGroup,
  error: unknown,
  options: ApiErrorHandlingOptions = {}
): boolean {
  const fieldErrors = collectApiFieldErrors(error, options);
  let applied = false;

  for (const [controlPath, message] of Object.entries(fieldErrors)) {
    const control = form.get(controlPath);
    if (!control) {
      continue;
    }

    setControlError(control, 'backend', message);
    control.markAsTouched();
    applied = true;
  }

  return applied;
}

export function clearApiFieldErrors(form: FormGroup, controlPaths: string[]): void {
  controlPaths.forEach(controlPath => clearControlError(form.get(controlPath), 'backend'));
}

export function resolveApiErrorMessage(
  error: unknown,
  fallbackMessage: string,
  options: ApiErrorHandlingOptions = {}
): string {
  const payload = extractApiErrorPayload(error);
  const details = payload.details;

  if (details) {
    const codeMessage = options.resolveMessageByCode?.({ payload, details });
    if (codeMessage) {
      return codeMessage;
    }
  }

  const fieldErrors = collectApiFieldErrors(error, options);
  const firstFieldError = Object.values(fieldErrors)[0];
  if (firstFieldError) {
    return firstFieldError;
  }

  if (payload.detail && !payload.detail.startsWith('Erro de validação')) {
    return payload.detail;
  }

  if (payload.message && !payload.message.startsWith('Erro de validação')) {
    return defaultSimplifyFieldMessage(payload.message);
  }

  return fallbackMessage;
}
