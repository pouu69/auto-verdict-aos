/**
 * 이력 타임라인, read straight from the insurance history (recordJson).
 * Plate numbers (`carNo`) are never read into the output.
 */
import type { MobileOrchestratorInput } from '../orchestrate-mobile';
import type { TimelineEntry } from './types';
import { formatMan } from './format';
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

export function buildTimeline(raw: MobileOrchestratorInput): TimelineEntry[] {
  const rec = raw.recordJson;
  if (!isObj(rec)) return [];
  const rows: Row[] = [];

  const first = str(rec, 'firstDate');
  const firstYm = toYearMonth(first);
  if (first && firstYm) rows.push(row(Kind.Registration, firstYm, first, '신차 등록', null, 'neutral'));

  // Accidents grouped by (day, side).
  const groups = new Map<string, { date: string; ym: string; side: string; count: number; won: number }>();
  for (const a of arr(rec.accidents)) {
    if (!isObj(a)) continue;
    const date = str(a, 'date');
    const ym = toYearMonth(date);
    const side = accidentSide(a.type);
    if (!date || !ym || !side) continue;
    const key = `${date}|${side}`;
    const g = groups.get(key) ?? { date, ym, side, count: 0, won: 0 };
    g.count += 1;
    g.won += num(a, 'insuranceBenefit') ?? 0;
    groups.set(key, g);
  }
  for (const g of groups.values()) {
    const label = g.count > 1 ? `사고 ${g.count}건 · ${g.side}` : `사고 · ${g.side}`;
    const man = Math.round(g.won / 10_000);
    const detail = man > 0 ? `보험 처리 합계 ${formatMan(g.won)}` : null;
    rows.push(row(Kind.Accident, g.ym, g.date, label, detail, 'warning'));
  }

  for (const d of arr(rec.ownerChanges)) {
    const ym = typeof d === 'string' ? toYearMonth(d) : null;
    if (ym) rows.push(row(Kind.Owner, ym, d as string, '소유자 변경', null, 'neutral'));
  }

  for (let i = 1; i <= 5; i++) {
    const s = str(rec, `notJoinDate${i}`);
    const m = s?.match(/(\d{6})\s*~\s*(\d{6})/);
    const from = toYearMonth(m?.[1]);
    const to = toYearMonth(m?.[2]);
    if (from && to) rows.push(row(Kind.Gap, from, from, '보험 미가입', `${from}~${to}`, 'warning'));
  }

  // The earliest entry is the original registration; later ones are plate changes.
  const plateDates = arr(rec.carInfoChanges)
    .map((c) => (isObj(c) ? str(c, 'date') : null))
    .filter((d): d is string => d !== null && toYearMonth(d) !== null)
    .sort();
  for (const date of plateDates.slice(1)) {
    rows.push(row(Kind.Plate, toYearMonth(date)!, date, '번호 변경', null, 'neutral'));
  }

  for (const [field, label] of LOSS_FIELDS) {
    const date = str(rec, field);
    const ym = toYearMonth(date);
    if (date && ym) rows.push(row(Kind.Loss, ym, date, label, null, 'danger'));
  }

  return rows
    .map((r, i) => ({ r, i }))
    .sort(
      (a, b) =>
        a.r.entry.date.localeCompare(b.r.entry.date) ||
        a.r.kind - b.r.kind ||
        a.r.sortKey.localeCompare(b.r.sortKey) ||
        a.i - b.i,
    )
    .map(({ r }) => r.entry);
}
