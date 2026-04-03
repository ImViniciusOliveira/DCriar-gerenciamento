export interface FieldLockMetadata {
  camposBloqueados?: Iterable<string> | null;
  motivosBloqueio?: Record<string, string> | null;
}

export function hasLockedField(entity: FieldLockMetadata | null | undefined, field: string): boolean {
  const camposBloqueados = entity?.camposBloqueados;
  if (!camposBloqueados) {
    return false;
  }

  return Array.from(camposBloqueados).includes(field);
}

export function getLockedFieldReason(
  entity: FieldLockMetadata | null | undefined,
  ...fields: string[]
): string | null {
  const motivos = entity?.motivosBloqueio;
  if (!motivos) {
    return null;
  }

  for (const field of fields) {
    const motivo = motivos[field];
    if (motivo) {
      return motivo;
    }
  }

  return null;
}
