/**
 * Display sections read loosely-typed listing data; one that throws must not
 * cost the user the verdict, so it falls back to its empty form instead.
 */
export const safe = <T>(build: () => T, fallback: T): T => {
  try {
    return build();
  } catch {
    return fallback;
  }
};
