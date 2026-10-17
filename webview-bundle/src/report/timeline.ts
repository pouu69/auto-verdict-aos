/**
 * 이력 타임라인, read from the insurance history (record API).
 * Plate numbers (`carNo`) are never read into the output.
 */
import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { DetailSheet, TimelineEntry } from './types';
import { groupDigits, koreanMonth } from './format';
import { arr, isObj, num, str, toYearMonth } from './raw';
import { safe } from './safe';
import { gapPeriods } from './rule-copy';
import {
  type AccidentRecord,
  type AccidentSide,
  accidentSheet,
  gapSheet,
  lossSheet,
  ownerSheet,
  plateSheet,
  repairCost,
} from './details/timeline';

/** Same-month order: 신차 등록 → 사고 → 소유자 변경 → 보험 미가입 → 번호 변경 → 침수·전손·도난. */
const Kind = { Registration: 0, Accident: 1, Owner: 2, Gap: 3, Plate: 4, Loss: 5 } as const;
type Kind = (typeof Kind)[keyof typeof Kind];

interface Row {
  entry: TimelineEntry;
  kind: Kind;
  /** Full date for a stable order inside the same month and kind. */
  sortKey: string;
}

/** [sheet] is built lazily under `safe`: a broken sheet costs only the sheet, never the entry. */
const row = (
  kind: Kind,
  date: string,
  sortKey: string,
  label: string,
  detail: string | null,
  tone: TimelineEntry['tone'],
  sheet: (() => DetailSheet | null) | null = null,
): Row => ({
  entry: { date, label, detail, tone, sheet: sheet ? safe(sheet, null) : null },
  kind,
  sortKey,
});

/** Accident `type`: 1·2 내차 피해, 3 타차 가해. */
const accidentSide = (type: unknown): AccidentSide | null => {
  const t = String(type ?? '');
  if (t === '1' || t === '2') return '내차 피해';
  if (t === '3') return '타차 가해';
  return null;
};

/** Date field, label, and the record counts shown in its sheet. */
const LOSS_FIELDS: Array<[string, string, Array<[string, string]>]> = [
  ['floodDate', '침수 피해', [['침수전손', 'floodTotalLossCnt'], ['침수분손', 'floodPartLossCnt']]],
  ['totalLossDate', '전손 처리', [['전손', 'totalLossCnt']]],
  ['robberDate', '도난', [['도난', 'robberCnt']]],
];

const cmp = (a: string, b: string): number => (a < b ? -1 : a > b ? 1 : 0);

export interface AccidentGroup {
  ym: string;
  firstDate: string;
  side: AccidentSide;
  count: number;
  /** Repair cost (partCost + laborCost + paintingCost), the basis of myAccidentCost/otherAccidentCost and R10. */
  won: number;
  /** The grouped claims, in record order. */
  records: AccidentRecord[];
}

/** Every readable claim in `recordJson.accidents`, in record order. */
export function accidentRecords(rec: Record<string, unknown>): AccidentRecord[] {
  const out: AccidentRecord[] = [];
  for (const a of arr(rec.accidents)) {
    if (!isObj(a)) continue;
    const date = str(a, 'date');
    const side = accidentSide(a.type);
    if (!date || !toYearMonth(date) || !side) continue;
    out.push({
      date,
      side,
      insuranceBenefit: num(a, 'insuranceBenefit'),
      partCost: num(a, 'partCost'),
      laborCost: num(a, 'laborCost'),
      paintingCost: num(a, 'paintingCost'),
    });
  }
  return out;
}

/** Accidents grouped by (month, side): the timeline shows months only. */
export function accidentGroups(rec: Record<string, unknown>): AccidentGroup[] {
  const groups = new Map<string, AccidentGroup>();
  for (const a of accidentRecords(rec)) {
    const ym = toYearMonth(a.date) as string;
    const key = `${ym}|${a.side}`;
    const g = groups.get(key) ?? { ym, firstDate: a.date, side: a.side, count: 0, won: 0, records: [] };
    g.count += 1;
    g.won += repairCost(a);
    g.records.push(a);
    if (a.date < g.firstDate) g.firstDate = a.date;
    groups.set(key, g);
  }
  return [...groups.values()];
}

/**
 * Splits [target] 만원 over [wons] by largest remainder: each share is its
 * floored 만원 plus one for the largest fractions until the target is met.
 * Rounding each row on its own can drift from the rounded total (39339524
 * 내차: rows summed to 1,127만원 against R10's 1,126만원).
 */
export function allocateMan(wons: number[], target: number): number[] {
  const exact = wons.map((w) => w / 10_000);
  const shares = exact.map(Math.floor);
  let left = target - shares.reduce((s, n) => s + n, 0);
  const order = exact
    .map((x, i) => ({ frac: x - Math.floor(x), i }))
    .sort((a, b) => b.frac - a.frac || a.i - b.i);
  for (const { i } of order) {
    if (left <= 0) break;
    shares[i]! += 1;
    left -= 1;
  }
  return shares;
}

/** Accident row 만원 per group; each side sums to the rounded side total that R10 shows. */
const accidentMan = (groups: AccidentGroup[]): Map<AccidentGroup, number> => {
  const out = new Map<AccidentGroup, number>();
  for (const side of ['내차 피해', '타차 가해'] as const) {
    const mine = groups.filter((g) => g.side === side);
    const total = mine.reduce((s, g) => s + g.won, 0);
    allocateMan(mine.map((g) => g.won), Math.round(total / 10_000)).forEach((man, i) => out.set(mine[i]!, man));
  }
  return out;
};

export function buildTimeline(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): TimelineEntry[] {
  // Gate on the parsed API; the raw body carries the extra fields RecordApi does not type.
  if (out.parsed.raw.recordApi.kind !== 'value') return [];
  const rec = raw.recordJson;
  if (!isObj(rec)) return [];
  const rows: Row[] = [];

  const first = str(rec, 'firstDate');
  const firstYm = toYearMonth(first);
  if (first && firstYm) rows.push(row(Kind.Registration, firstYm, first, '신차 등록', null, 'neutral'));

  const groups = accidentGroups(rec);
  const man = accidentMan(groups);
  for (const g of groups) {
    const label = g.count > 1 ? `사고 ${g.count}건 · ${g.side}` : `사고 · ${g.side}`;
    const shown = man.get(g) ?? 0;
    const detail = shown > 0 ? `수리비 합계 ${groupDigits(shown)}만원` : null;
    rows.push(row(Kind.Accident, g.ym, g.firstDate, label, detail, 'warning', () => accidentSheet(label, g.records)));
  }

  const owners = arr(rec.ownerChanges)
    .filter((d): d is string => typeof d === 'string' && toYearMonth(d) !== null)
    .sort(cmp);
  owners.forEach((d, i) => {
    const ym = toYearMonth(d) as string;
    rows.push(row(Kind.Owner, ym, d, '소유자 변경', null, 'neutral', () => ownerSheet(d, i + 1, owners.length)));
  });

  // Same periods the R08 copy uses.
  const gap = out.facts.insuranceGap;
  for (const p of gapPeriods(gap.kind === 'value' ? gap.value : undefined).periods) {
    const detail = `${koreanMonth(p.from)}~${koreanMonth(p.to)}`;
    rows.push(row(Kind.Gap, p.from, p.from, '보험 미가입', detail, 'warning', () => gapSheet(p)));
  }

  // The earliest entry is the original registration; later ones are plate changes.
  const plates: Array<{ date: string; ym: string }> = [];
  for (const c of arr(rec.carInfoChanges)) {
    const date = isObj(c) ? str(c, 'date') : null;
    const ym = toYearMonth(date);
    if (date && ym) plates.push({ date, ym });
  }
  plates.sort((a, b) => cmp(a.date, b.date));
  const changes = plates.slice(1);
  changes.forEach((p, i) => {
    rows.push(row(Kind.Plate, p.ym, p.date, '번호 변경', null, 'neutral', () => plateSheet(p.date, i + 1, changes.length)));
  });

  for (const [field, label, counts] of LOSS_FIELDS) {
    const date = str(rec, field);
    const ym = toYearMonth(date);
    if (!date || !ym) continue;
    const sheet = () => lossSheet(label, date, counts.map(([name, key]) => [name, num(rec, key)]));
    rows.push(row(Kind.Loss, ym, date, label, null, 'danger', sheet));
  }

  return rows
    .map((r, i) => ({ r, i }))
    .sort(
      (a, b) =>
        cmp(a.r.entry.date, b.r.entry.date) || a.r.kind - b.r.kind || cmp(a.r.sortKey, b.r.sortKey) || a.i - b.i,
    )
    .map(({ r }) => r.entry);
}
