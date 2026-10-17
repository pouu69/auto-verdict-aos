/**
 * Shared pieces for the detail sheets (spec 4.1): row/block constructors,
 * exact amounts and dates, and the "데이터 출처" status block.
 */
import type { FieldStatus } from '@core/types/FieldStatus.js';
import type { MobileOrchestratorResult } from '../../orchestrate-mobile';
import type { DetailBlock, DetailRow, Tone } from '../types';
import { groupDigits, koreanMonth } from '../format';
import { isObj, toYearMonth, type Obj } from '../raw';
import { reasonShort } from '../reasons';

export const row = (label: string, value: string, tone: Tone | null = null): DetailRow => ({ label, value, tone });

export const block = (title: string | null, rows: DetailRow[], note: string | null = null): DetailBlock => ({
  title,
  rows,
  note,
});

/** Exact won: 1005260 -> "1,005,260원". */
export const won = (n: number): string => `${groupDigits(n)}원`;

/** "2012-05-02" / "20120502" / "2026-10-09T00:00:00" -> "2012-05-02"; null if unrecognised. */
export const fullDate = (s: unknown): string | null => {
  if (typeof s !== 'string') return null;
  const m = s.trim().match(/^(\d{4})-?(\d{2})-?(\d{2})/);
  if (!m) return null;
  const month = Number(m[2]);
  const day = Number(m[3]);
  return month >= 1 && month <= 12 && day >= 1 && day <= 31 ? `${m[1]}-${m[2]}-${m[3]}` : null;
};

/**
 * A record date for display: [fullDate], else "2021년 5월" when only the month
 * reads ("2021-05", "2021-05-99"). The raw string is never shown.
 */
export const dateText = (s: string): string => {
  const ym = toYearMonth(s);
  return fullDate(s) ?? (ym ? koreanMonth(ym) : '날짜 없음');
};

/** Months as "7개월" / "1년" / "1년 2개월". */
export const duration = (months: number): string => {
  if (months < 12) return `${months}개월`;
  const y = Math.floor(months / 12);
  const m = months % 12;
  return m === 0 ? `${y}년` : `${y}년 ${m}개월`;
};

/** 0 -> "없음", n -> "n회". */
export const times = (n: number): string => (n > 0 ? `${n}회` : '없음');

/** The insurance history body, or null when the record API was not read. */
export const recordOf = (out: MobileOrchestratorResult): Obj | null => {
  const api = out.parsed.raw.recordApi;
  const v: unknown = api.kind === 'value' ? api.value : null;
  return isObj(v) ? v : null;
};

/** The 성능점검 body, or null when the inspection API was not read. */
export const inspectionOf = (out: MobileOrchestratorResult): Obj | null => {
  const api = out.parsed.raw.inspectionApi;
  const v: unknown = api.kind === 'value' ? api.value : null;
  return isObj(v) ? v : null;
};

// ── 데이터 출처 ───────────────────────────────────────────────

export type SourceKey = 'page' | 'record' | 'inspection' | 'diagnosis';

const SOURCE_NAME: Record<SourceKey, string> = {
  page: '매물 정보',
  record: '엔카 보험이력',
  inspection: '성능점검',
  diagnosis: '엔카진단',
};

const sourceStatus = (key: SourceKey, out: MobileOrchestratorResult): FieldStatus<unknown> => {
  const raw = out.parsed.raw;
  switch (key) {
    case 'page':
      // The listing page feeds both the base data and the dealer flags.
      return raw.base.kind !== 'value' ? raw.base : raw.detailFlags;
    case 'record':
      return raw.recordApi;
    case 'inspection':
      return raw.inspectionApi;
    case 'diagnosis':
      return raw.diagnosisApi;
  }
};

export const sourceRow = (key: SourceKey, out: MobileOrchestratorResult): DetailRow => {
  const status = sourceStatus(key, out);
  return row(SOURCE_NAME[key], reasonShort(status), status.kind === 'value' ? null : 'neutral');
};

export const sourceBlock = (keys: SourceKey[], out: MobileOrchestratorResult): DetailBlock =>
  block(
    '데이터 출처',
    keys.map((k) => sourceRow(k, out)),
  );

/** True when every listed source was read. */
export const sourcesOk = (keys: SourceKey[], out: MobileOrchestratorResult): boolean =>
  keys.every((k) => sourceStatus(k, out).kind === 'value');
