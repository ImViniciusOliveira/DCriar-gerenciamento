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

export interface LogicalMapKeyAnalysis {
  conflict: LogicalMapKeyConflict | null;
  duplicateIndexes: Set<number>;
  conflictingReservedKeys: Set<string>;
}

export function analyzeLogicalMapKeys(
  entries: LogicalMapKeyEntry[],
  reservedKeys: string[] = []
): LogicalMapKeyAnalysis {
  const duplicateIndexes = new Set<number>();
  const conflictingReservedKeys = new Set<string>();
  const seenIndexesByKey = new Map<string, number[]>();
  const reservedKeysByNormalizedValue = new Map<string, string>();
  let conflict: LogicalMapKeyConflict | null = null;

  for (const reservedKey of reservedKeys) {
    const normalizedReservedKey = normalizeLogicalMapKey(reservedKey);
    if (normalizedReservedKey) {
      reservedKeysByNormalizedValue.set(normalizedReservedKey, reservedKey);
    }
  }

  entries.forEach((entry, index) => {
    const originalKey = String(entry.chave ?? '').trim();
    const normalizedKey = normalizeLogicalMapKey(entry.chave);
    if (!normalizedKey) {
      return;
    }

    const conflictingReservedKey = reservedKeysByNormalizedValue.get(normalizedKey);
    if (conflictingReservedKey) {
      if (!conflict) {
        conflict = {
          firstKey: conflictingReservedKey,
          secondKey: originalKey || String(entry.chave ?? '')
        };
      }
      duplicateIndexes.add(index);
      conflictingReservedKeys.add(conflictingReservedKey);
    }

    const seenIndexes = seenIndexesByKey.get(normalizedKey) ?? [];
    if (!conflict && seenIndexes.length > 0) {
      const firstDuplicateIndex = seenIndexes[0];
      const firstDuplicateKey = String(entries[firstDuplicateIndex]?.chave ?? '').trim();
      conflict = {
        firstKey: firstDuplicateKey || String(entries[firstDuplicateIndex]?.chave ?? ''),
        secondKey: originalKey || String(entry.chave ?? '')
      };
    }
    seenIndexes.push(index);
    seenIndexesByKey.set(normalizedKey, seenIndexes);
  });

  for (const indexes of seenIndexesByKey.values()) {
    if (indexes.length > 1) {
      indexes.forEach(index => duplicateIndexes.add(index));
    }
  }

  return {
    conflict,
    duplicateIndexes,
    conflictingReservedKeys
  };
}
