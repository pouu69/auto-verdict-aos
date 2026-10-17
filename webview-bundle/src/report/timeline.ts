/**
 * 이력 타임라인, read from the insurance history (record API).
 * Plate numbers (`carNo`) are never read into the output.
 */
import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { TimelineEntry } from './types';
import { formatMan, koreanMonth } from './format';
import { arr, isObj, num, str, toYearMonth } from './raw';

/** Same-month order: 신차 등록 → 사고 → 소유자 변경 → 보험 미가입 → 번호 변경 → 침수·전손·도난. */
const Kind = { Registration: 0, Accident: 1, Owner: 2, Gap: 3, Plate: 4, Loss: 5 } as const;
type Kind = (typeof Kind)[keyof typeof Kind];

interface Row {
  entry: TimelineEntry;
  kind: Kind;
  /** Full date for a stable order inside the same month and kind. */
  sortKey: string;
}

const row = (kind: Kind, date: string, sortKey: string, label: string, detail: string | null, tone: TimelineEntry['tone']): Row => ({
  entry: { date, label, detail, tone },
  kind,
  sortKey,
});

/** Accident `type`: 1·2 내차 피해, 3 타차 가해. */
const accidentSide = (type: unknown): '내차 피해' | '타차 가해' | null => {
  const t = String(type ?? '');
  if (t === '1' || t === '2') return '내차 피해';
  if (t === '3') return '타차 가해';
  return null;
};

const LOSS_FIELDS: Array<[string, string]> = [
  ['floodDate', '침수 피해'],
  ['totalLossDate', '전손 처리'],
  ['robberDate', '도난'],
];

const cmp = (a: string, b: string): number => (a < b ? -1 : a > b ? 1 : 0);

export interface AccidentGroup {
  ym: string;
  firstDate: string;
  side: '내차 피해' | '타차 가해';
  count: number;
  /** Repair cost (partCost + laborCost + paintingCost), the basis of myAccidentCost/otherAccidentCost and R10. */
  won: number;
}

/** Accidents grouped by (month, side): the timeline shows months only. */
export function accidentGroups(rec: Record<string, unknown>): AccidentGroup[] {
  const groups = new Map<string, AccidentGroup>();
  for (const a of arr(rec.accidents)) {
    if (!isObj(a)) continue;
    const date = str(a, 'date');
    const ym = toYearMonth(date);
    const side = accidentSide(a.type);
    if (!date || !ym || !side) continue;
    const key = `${ym}|${side}`;
    const g = groups.get(key) ?? { ym, firstDate: date, side, count: 0, won: 0 };
    g.count += 1;
    g.won += (num(a, 'partCost') ?? 0) + (num(a, 'laborCost') ?? 0) + (num(a, 'paintingCost') ?? 0);
    if (date < g.firstDate) g.firstDate = date;
    groups.set(key, g);
  }
  return [...groups.values()];
}

export function buildTimeline(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): TimelineEntry[] {
  // Gate on the parsed API; the raw body carries the extra fields RecordApi does not type.
  if (out.parsed.raw.recordApi.kind !== 'value') return [];
  const rec = raw.recordJson;
  if (!isObj(rec)) return [];
  const rows: Row[] = [];

  const first = str(rec, 'firstDate');
  const firstYm = toYearMonth(first);
  if (first && firstYm) rows.push(row(Kind.Registration, firstYm, first, '신차 등록', null, 'neutral'));

  for (const g of accidentGroups(rec)) {
    const label = g.count > 1 ? `사고 ${g.count}건 · ${g.side}` : `사고 · ${g.side}`;
    const detail = Math.round(g.won / 10_000) > 0 ? `수리비 합계 ${formatMan(g.won)}` : null;
    rows.push(row(Kind.Accident, g.ym, g.firstDate, label, detail, 'warning'));
  }

  for (const d of arr(rec.ownerChanges)) {
    if (typeof d !== 'string') continue;
    const ym = toYearMonth(d);
    if (ym) rows.push(row(Kind.Owner, ym, d, '소유자 변경', null, 'neutral'));
  }

  // Same periods the R08 copy uses.
  const gap = out.facts.insuranceGap;
  for (const p of gap.kind === 'value' ? gap.value.periods : []) {
    rows.push(row(Kind.Gap, p.from, p.from, '보험 미가입', `${koreanMonth(p.from)}~${koreanMonth(p.to)}`, 'warning'));
  }

  // The earliest entry is the original registration; later ones are plate changes.
  const plates: Array<{ date: string; ym: string }> = [];
  for (const c of arr(rec.carInfoChanges)) {
    const date = isObj(c) ? str(c, 'date') : null;
    const ym = toYearMonth(date);
    if (date && ym) plates.push({ date, ym });
  }
  plates.sort((a, b) => cmp(a.date, b.date));
  for (const p of plates.slice(1)) rows.push(row(Kind.Plate, p.ym, p.date, '번호 변경', null, 'neutral'));

  for (const [field, label] of LOSS_FIELDS) {
    const date = str(rec, field);
    const ym = toYearMonth(date);
    if (date && ym) rows.push(row(Kind.Loss, ym, date, label, null, 'danger'));
  }

  return rows
    .map((r, i) => ({ r, i }))
    .sort(
      (a, b) =>
        cmp(a.r.entry.date, b.r.entry.date) || a.r.kind - b.r.kind || cmp(a.r.sortKey, b.r.sortKey) || a.i - b.i,
    )
    .map(({ r }) => r.entry);
}
