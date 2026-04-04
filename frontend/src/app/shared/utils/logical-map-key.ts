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
