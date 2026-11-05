export type DiffEntry = { path: string; before: unknown; after: unknown };

const isObj = (v: unknown): v is Record<string, unknown> =>
  typeof v === "object" && v !== null && !Array.isArray(v);
const KEY_FIELDS = ["module", "type", "eventType", "id", "roleId", "warnCount"] as const;

function keyOf(item: unknown, index: number): string {
  if (isObj(item)) {
    for (const k of KEY_FIELDS)
      if (typeof item[k] === "string" || typeof item[k] === "number") return String(item[k]);
  }
  return String(index);
}

/**
 * Diff dwóch wartości do audit logu. Obiekty porównywane pole po polu, tablice obiektów
 * po kluczu (module/type/id…) — np. `rules[ANTI_CAPS].enabled`. Tablice prymitywów jako całość.
 */
export function computeDiff(before: unknown, after: unknown, prefix = ""): DiffEntry[] {
  if (isObj(before) && isObj(after)) {
    const keys = new Set([...Object.keys(before), ...Object.keys(after)]);
    return [...keys]
      .sort()
      .flatMap((k) => computeDiff(before[k], after[k], prefix ? `${prefix}.${k}` : k));
  }
  if (
    Array.isArray(before) &&
    Array.isArray(after) &&
    [...before, ...after].every(isObj) &&
    before.length + after.length > 0
  ) {
    const b = new Map(before.map((x, i) => [keyOf(x, i), x]));
    const a = new Map(after.map((x, i) => [keyOf(x, i), x]));
    const keys = [...new Set([...b.keys(), ...a.keys()])];
    // brak wspólnych kluczy (np. same indeksy po przebudowie) → porównaj całość
    return keys.flatMap((k) => computeDiff(b.get(k), a.get(k), `${prefix}[${k}]`));
  }
  const same = JSON.stringify(before ?? null) === JSON.stringify(after ?? null);
  return same ? [] : [{ path: prefix || "(root)", before: before ?? null, after: after ?? null }];
}
