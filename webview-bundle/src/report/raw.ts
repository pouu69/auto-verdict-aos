/** Tolerant readers for the raw collector payload (shapes vary per listing). */

export type Obj = Record<string, unknown>;

export const isObj = (v: unknown): v is Obj =>
  typeof v === 'object' && v !== null && !Array.isArray(v);

/**
 * A count or won amount: a finite, non-negative number in the safe-integer
 * range. Anything else (NaN, -1, 1e21, "3") is unreadable: shown as such, never
 * formatted into "NaN", "-1회" or "1e+21원".
 */
export const count = (v: unknown): number | null =>
  typeof v === 'number' && Number.isFinite(v) && v >= 0 && v <= Number.MAX_SAFE_INTEGER ? v : null;

/** [count] read from a raw field. */
export const num = (o: Obj, key: string): number | null => count(o[key]);

export const str = (o: Obj, key: string): string | null => {
  const v = o[key];
  return typeof v === 'string' && v.trim().length > 0 ? v : null;
};

export const arr = (v: unknown): unknown[] => (Array.isArray(v) ? v : []);

/** "2015-04-16" / "20150416" / "201504" -> "2015-04"; null if unrecognised. */
export const toYearMonth = (s: string | null | undefined): string | null => {
  if (!s) return null;
  const m = s.trim().match(/^(\d{4})-?(\d{2})/);
  if (!m) return null;
  const month = Number(m[2]);
  return month >= 1 && month <= 12 ? `${m[1]}-${m[2]}` : null;
};
