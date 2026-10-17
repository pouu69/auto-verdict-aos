/**
 * Defensive clean-up of the collector payload before it reaches the vendored core.
 *
 * The core parsers trust Encar's shapes and throw on a few malformed ones, which
 * turns a whole evaluation into "결과를 계산하지 못했어요". This only repairs those
 * shapes; well-formed input comes out with equal values, so its results are
 * unchanged (the fixtures' contract JSON guards that):
 *
 * - `cars.base` without an object `advertisement` / `spec` → `{}`, without an
 *   object `category` → `{ domestic: true }` (the core's own default when base is
 *   missing: domestic thresholds). The core then reads the absent values as
 *   missing — e.g. price/new price 0, so the price ratio has nothing to compare —
 *   and the car summary leaves year/price empty, instead of throwing.
 * - record `notJoinDate1..5` that is not a string → `null`
 * - inspection `outers` / `inners` / nested `children` that are not arrays → `[]`,
 *   and their entries that are not objects are dropped
 * - diagnosis `items` entries that are not objects are dropped
 *
 * Never mutates its argument.
 */
import type { MobileOrchestratorInput } from './orchestrate-mobile';

type Obj = Record<string, unknown>;

const isObj = (v: unknown): v is Obj => typeof v === 'object' && v !== null && !Array.isArray(v);

const NOT_JOIN_DATE_KEYS = ['notJoinDate1', 'notJoinDate2', 'notJoinDate3', 'notJoinDate4', 'notJoinDate5'] as const;

const normalizeBase = (base: Obj): Obj => {
  const out: Obj = { ...base };
  // Matches encar-to-facts' `domestic` default for a missing base.
  if (!isObj(out.category)) out.category = { domestic: true };
  if (!isObj(out.advertisement)) out.advertisement = {};
  if (!isObj(out.spec)) out.spec = {};
  return out;
};

/** Repairs `base` in either state shape the orchestrator accepts: `{cars}` or `{__PRELOADED_STATE__: {cars}}`. */
const normalizeState = (state: unknown): unknown => {
  if (!isObj(state)) return state;
  if (isObj(state.__PRELOADED_STATE__)) {
    return { ...state, __PRELOADED_STATE__: normalizeState(state.__PRELOADED_STATE__) };
  }
  const cars = state.cars;
  if (!isObj(cars) || !isObj(cars.base)) return state;
  return { ...state, cars: { ...cars, base: normalizeBase(cars.base) } };
};

const normalizeRecord = (record: unknown): unknown => {
  if (!isObj(record)) return record;
  const out: Obj = { ...record };
  for (const k of NOT_JOIN_DATE_KEYS) {
    if (k in out && out[k] !== null && typeof out[k] !== 'string') out[k] = null;
  }
  return out;
};

/** Object entries only; nested `children`, when present, cleaned the same way. */
const normalizeNodes = (nodes: unknown): Obj[] =>
  (Array.isArray(nodes) ? nodes : [])
    .filter(isObj)
    .map((n) => (n.children === undefined || n.children === null ? n : { ...n, children: normalizeNodes(n.children) }));

const normalizeInspection = (inspection: unknown): unknown => {
  if (!isObj(inspection)) return inspection;
  const out: Obj = { ...inspection };
  for (const key of ['outers', 'inners'] as const) {
    if (out[key] !== undefined && out[key] !== null) out[key] = normalizeNodes(out[key]);
  }
  return out;
};

const normalizeDiagnosis = (diagnosis: unknown): unknown =>
  isObj(diagnosis) && Array.isArray(diagnosis.items)
    ? { ...diagnosis, items: diagnosis.items.filter(isObj) }
    : diagnosis;

export const normalizeInput = (input: MobileOrchestratorInput): MobileOrchestratorInput => {
  const out: MobileOrchestratorInput = { ...input, preloadedState: normalizeState(input.preloadedState) };
  if (input.recordJson !== undefined) out.recordJson = normalizeRecord(input.recordJson);
  if (input.inspectionJson !== undefined) out.inspectionJson = normalizeInspection(input.inspectionJson);
  if (input.diagnosisJson !== undefined) out.diagnosisJson = normalizeDiagnosis(input.diagnosisJson);
  return out;
};
