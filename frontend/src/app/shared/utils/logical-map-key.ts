export function normalizeLogicalMapKey(key: unknown): string {
  return String(key ?? '')
    .trim()
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .toLowerCase();
}

export function getLogicalMapValue<T>(
  source: Record<string, T> | null | undefined,
  expectedKey: string
): T | undefined {
  if (!source) {
    return undefined;
  }

  const normalizedExpectedKey = normalizeLogicalMapKey(expectedKey);

  for (const [key, value] of Object.entries(source)) {
    if (normalizeLogicalMapKey(key) === normalizedExpectedKey) {
      return value;
    }
  }

  return undefined;
}

export interface LogicalMapKeyEntry {
  chave: unknown;
}

export interface LogicalMapKeyConflict {
  firstKey: string;
  secondKey: string;
}

export function findLogicalMapKeyConflict(
  entries: LogicalMapKeyEntry[],
  reservedKeys: string[] = []
): LogicalMapKeyConflict | null {
  const seenKeys = new Map<string, string>();

  for (const reservedKey of reservedKeys) {
    const normalizedReservedKey = normalizeLogicalMapKey(reservedKey);
    if (normalizedReservedKey) {
      seenKeys.set(normalizedReservedKey, reservedKey);
    }
  }

  for (const entry of entries) {
    const originalKey = String(entry.chave ?? '').trim();
    const normalizedKey = normalizeLogicalMapKey(entry.chave);

    if (!normalizedKey) {
      continue;
    }

    const firstSeenKey = seenKeys.get(normalizedKey);
    if (firstSeenKey) {
      return {
        firstKey: firstSeenKey,
        secondKey: originalKey || String(entry.chave ?? '')
      };
    }

    seenKeys.set(normalizedKey, originalKey);
  }

  return null;
}
