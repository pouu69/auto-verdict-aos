/**
 * Detail sheets behind 이력 타임라인 entries and the per-accident blocks the
 * R10 sheet reuses. Exact won amounts and YYYY-MM-DD dates (month-only gaps
 * use "YYYY년 M월"). Plate numbers are never read.
 */
import type { DetailBlock, DetailRow, DetailSheet } from '../types';
import { koreanMonth } from '../format';
import { block, dateText, duration, row, won } from './common';

export type AccidentSide = '내차 피해' | '타차 가해';

/** One insurance claim from `recordJson.accidents`. Costs are null when the field is missing. */
export interface AccidentRecord {
  date: string;
  side: AccidentSide;
  insuranceBenefit: number | null;
  partCost: number | null;
  laborCost: number | null;
  paintingCost: number | null;
}

/** partCost + laborCost + paintingCost: the basis of myAccidentCost/otherAccidentCost and R10. */
export const repairCost = (a: AccidentRecord): number => (a.partCost ?? 0) + (a.laborCost ?? 0) + (a.paintingCost ?? 0);

const amount = (n: number | null): string => (n === null ? '기록 없음' : won(n));

const REPAIR_NOTE = '수리비 합계는 부품비·공임·도장비를 더한 금액이에요. 보험사가 지급한 보험금과 다를 수 있어요';

/** One block per claim, newest first: 보험금, 부품비, 공임, 도장비, 수리비 합계. */
export function accidentBlocks(records: AccidentRecord[]): DetailBlock[] {
  return [...records]
    .map((a, i) => ({ a, i }))
    .sort((x, y) => (x.a.date < y.a.date ? 1 : x.a.date > y.a.date ? -1 : x.i - y.i))
    .map(({ a }) =>
      block(`${dateText(a.date)} · ${a.side}`, [
        row('보험금', amount(a.insuranceBenefit)),
        row('부품비', amount(a.partCost)),
        row('공임', amount(a.laborCost)),
        row('도장비', amount(a.paintingCost)),
        row('수리비 합계', won(repairCost(a))),
      ]),
    );
}

/** Sheet for a timeline accident row (claims grouped by month and side). */
export function accidentSheet(title: string, records: AccidentRecord[]): DetailSheet {
  const blocks = accidentBlocks(records);
  const last = blocks[blocks.length - 1];
  if (last) last.note = REPAIR_NOTE;
  const total = records.reduce((s, a) => s + repairCost(a), 0);
  return { title, subtitle: `수리비 합계 ${won(total)}`, blocks };
}

const orderRow = (index: number, total: number): DetailRow =>
  row('순서', total > 1 ? `전체 ${total}회 중 ${index}번째` : '1회');

/** [index] is 1-based in date order. */
export function ownerSheet(date: string, index: number, total: number): DetailSheet {
  return {
    title: '소유자 변경',
    subtitle: null,
    blocks: [block(null, [row('변경일', dateText(date)), orderRow(index, total)])],
  };
}

export function gapSheet(p: { from: string; to: string; months: number }): DetailSheet {
  return {
    title: '보험 미가입',
    subtitle: `${koreanMonth(p.from)}~${koreanMonth(p.to)}`,
    blocks: [
      block(
        null,
        [row('시작', koreanMonth(p.from)), row('끝', koreanMonth(p.to)), row('기간', duration(p.months), 'warning')],
        '이 기간에 난 사고는 보험이력에 남지 않아요',
      ),
    ],
  };
}

/** Number changes carry only the date here; the plate itself is never shown. */
export function plateSheet(date: string, index: number, total: number): DetailSheet {
  return {
    title: '번호 변경',
    subtitle: null,
    blocks: [block(null, [row('변경일', dateText(date)), orderRow(index, total)])],
  };
}

/** 침수 피해 / 전손 처리 / 도난, with the matching counts from the record. */
export function lossSheet(title: string, date: string, counts: Array<[string, number | null]>): DetailSheet {
  const rows = [row('날짜', dateText(date))];
  for (const [label, n] of counts) if (n !== null) rows.push(row(label, n > 0 ? `${n}회` : '없음', n > 0 ? 'danger' : null));
  return { title, subtitle: null, blocks: [block(null, rows)] };
}
