/** Tolerant readers for the raw collector payload (shapes vary per listing). */

export type Obj = Record<string, unknown>;

export const isObj = (v: unknown): v is Obj =>
  typeof v === 'object' && v !== null && !Array.isArray(v);

export const num = (o: Obj, key: string): number | null => {
  const v = o[key];
  return typeof v === 'number' && Number.isFinite(v) ? v : null;
};

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
