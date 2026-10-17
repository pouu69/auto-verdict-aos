/**
 * Rule result -> user copy (해요체). The engine's own `message` is never shown:
 * it mixes 합니다체, emoji and internal reason codes (spec 6).
 */
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import type { FieldStatus } from '@core/types/FieldStatus.js';
import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { RuleLabel, Tone } from './types';
import { RULE_META, type RuleSource } from './rule-meta';
import type { FrameSource } from './frame-source';
import { formatMan, josa, koreanMonth } from './format';

export interface RuleCopy {
  message: string;
  sellerQuestion: string | null;
}

const LABELS: Record<Severity, RuleLabel> = {
  killer: '위험',
  fail: '비공개',
  warn: '주의',
  pass: '통과',
  unknown: '미확인',
};

const TONES: Record<Severity, Tone> = {
  killer: 'danger',
  fail: 'danger',
  warn: 'warning',
  pass: 'success',
  unknown: 'neutral',
};

export const labelFor = (severity: Severity): RuleLabel => LABELS[severity];
export const toneFor = (severity: Severity): Tone => TONES[severity];

/** Evidence value by field name, falling back to the matching facts value. */
export const evidenceOf = <T>(result: RuleResult, field: string, fallback?: FieldStatus<T>): T | undefined => {
  const hit = result.evidence.find((e) => e.field === field);
  if (hit) return hit.value as T;
  return fallback?.kind === 'value' ? fallback.value : undefined;
};

export const gapDuration = (months: number): string => {
  if (months < 12) return `${months}개월`;
  const y = Math.floor(months / 12);
  const m = months % 12;
  return m === 0 ? `${y}년` : `${y}년 ${m}개월`;
};

/** "실린더 블록 / 오일팬" -> "실린더 블록·오일팬". */
export const leakPart = (part: string): string => part.trim().replace(/\s*\/\s*/g, '·');

/** Strip the trailing "(상태)" group that the core appends to outer panel names. */
const panelName = (part: string): string =>
  part.replace(/\((?:[^()]|\([^()]*\))*\)$/, '').trim() || part;

export type UsageFlags = { rental: boolean; taxi: boolean };
export type LossCounts = { totalLoss: number; floodTotal: number; floodPart: number; robber: number };

/** Usage tags behind an R05 killer, e.g. ["렌트", "영업용"]. */
export const usageKinds = (u: UsageFlags): string[] => {
  const tags: string[] = [];
  if (u.rental) tags.push('렌트');
  if (u.taxi) tags.push('영업용');
  return tags;
};

/** Count phrases for R06, e.g. ["전손 1회", "침수분손 1회"]. */
export const lossHits = (l: LossCounts): string[] => {
  const hits: string[] = [];
  if (l.totalLoss > 0) hits.push(`전손 ${l.totalLoss}회`);
  if (l.floodTotal > 0) hits.push(`침수전손 ${l.floodTotal}회`);
  if (l.floodPart > 0) hits.push(`침수분손 ${l.floodPart}회`);
  if (l.robber > 0) hits.push(`도난 ${l.robber}회`);
  return hits;
};

/** Kinds behind an R06 killer, e.g. ["전손", "침수"]. */
export const lossKinds = (l: LossCounts): string[] => {
  const kinds: string[] = [];
  if (l.totalLoss > 0) kinds.push('전손');
  if (l.floodTotal > 0 || l.floodPart > 0) kinds.push('침수');
  if (l.robber > 0) kinds.push('도난');
  return kinds;
};

const ok = (message: string): RuleCopy => ({ message, sellerQuestion: null });
const issue = (message: string, sellerQuestion: string): RuleCopy => ({ message, sellerQuestion });

/** R04 killer sentence by evidence source; never claims a source we can't name. */
const FRAME_KILLER: Record<FrameSource, RuleCopy> = {
  diagnosis: issue('엔카진단에서 프레임 손상이 확인됐어요', '프레임 수리 내역과 사진을 볼 수 있을까요?'),
  inspection: issue('성능점검에 사고 이력이 있어요', '성능점검 사고 부위의 수리 내역과 사진을 볼 수 있을까요?'),
  unknown: issue('사고 이력(프레임 손상)이 있어요', '프레임 수리 내역과 사진을 볼 수 있을까요?'),
};

/**
 * Result sentence and seller question for a decided rule (not 미확인; use
 * [unknownMessage] for those). Numbers come from evidence, then facts.
 * [frameSource] says which document backs R04 (see frame-source.ts).
 */
export function ruleCopy(result: RuleResult, facts: ChecklistFacts, frameSource: FrameSource = 'unknown'): RuleCopy {
  const s = result.severity;
  const problem = s === 'killer' || s === 'fail' || s === 'warn';
  switch (result.ruleId) {
    case 'R01':
      return problem
        ? issue('딜러가 보험이력을 공개하지 않았어요', '보험이력을 공개해 주실 수 있나요?')
        : ok('딜러가 보험이력을 공개했어요');
    case 'R02':
      return problem
        ? issue('딜러가 성능점검 기록을 공개하지 않았어요', '성능점검 기록을 보여주실 수 있나요?')
        : ok('성능점검 기록이 공개돼 있어요');
    case 'R03':
      return ok('엔카진단을 통과한 차예요');
    case 'R04': {
      // Killer rows skip the panel list; the repairs section already shows it.
      if (problem) return FRAME_KILLER[frameSource];
      const frame = evidenceOf(result, 'frameDamage', facts.frameDamage);
      const parts = (frame?.parts ?? []).map(panelName);
      return ok(parts.length > 0 ? `프레임 손상은 없어요 · 외판 ${parts.join(', ')} 교환·수리` : '프레임 손상은 없어요');
    }
    case 'R05': {
      if (s === 'killer') {
        const u = evidenceOf(result, 'usageHistory', facts.usageHistory);
        const tags = (u ? usageKinds(u) : []).join('·') || '렌트·영업용';
        return issue(`${tags} 이력이 있어요`, `${tags}${josa(tags, '으로', '로')} 얼마나 쓰였나요?`);
      }
      if (s === 'warn') return issue('관용 이력이 있어요', '관용으로 쓰인 기간과 정비 이력을 알 수 있을까요?');
      return ok('렌트·영업용 이력이 없어요');
    }
    case 'R06': {
      if (!problem) return ok('전손·침수·도난 이력이 없어요');
      const l = evidenceOf(result, 'totalLoss', facts.totalLossHistory);
      const hits = l ? lossHits(l) : [];
      const kinds = l ? lossKinds(l) : [];
      return issue(
        `${hits.join(', ') || '전손·침수·도난'} 이력이 있어요`,
        `${kinds.join('·') || '전손·침수·도난'} 처리 후 어디서 수리했나요?`,
      );
    }
    case 'R07': {
      const n = evidenceOf(result, 'ownerChangeCount', facts.ownerChangeCount);
      if (problem) {
        return n === undefined
          ? issue('주인이 자주 바뀌었어요', '주인이 자주 바뀐 이유를 알 수 있을까요?')
          : issue(`주인이 ${n}번 바뀌었어요`, `주인이 ${n}번 바뀐 이유를 알 수 있을까요?`);
      }
      if (n === undefined) return ok('소유자 변경이 정상 범위예요');
      return ok(n === 0 ? '소유자 변경이 없어요' : `소유자 변경 ${n}회로 정상 범위예요`);
    }
    case 'R08': {
      if (!problem) return ok('자차보험 공백이 없어요');
      const g = evidenceOf(result, 'insuranceGap', facts.insuranceGap);
      const periods = g?.periods ?? [];
      const ranges = periods.map((p) => `${koreanMonth(p.from)}~${koreanMonth(p.to)}`).join(', ');
      const months = g?.totalMonths ?? 0;
      // No parentheses: "(2개 구간)" wrapped mid-phrase at large font sizes.
      const span = periods.length > 1 ? `${periods.length}개 구간, 총 ${gapDuration(months)}` : gapDuration(months);
      return issue(
        months > 0 ? `자차보험 없이 운행한 기간이 ${span} 있어요` : '자차보험 없이 운행한 기간이 있어요',
        ranges ? `${ranges}에 보험 없이 운행했나요?` : '보험 없이 운행한 기간이 있었나요?',
      );
    }
    case 'R09':
      return problem
        ? issue('수리비가 확정되지 않은 사고가 있어요', '미확정 수리비 건은 어떻게 처리됐나요?')
        : ok('수리비가 확정되지 않은 사고는 없어요');
    case 'R10': {
      const d = evidenceOf(result, 'damages', facts.minorAccidents);
      const breakdown = d ? ` · 내차 ${formatMan(d.ownDamageWon)} · 타차 ${formatMan(d.otherDamageWon)}` : '';
      // Amounts are repair costs (myAccidentCost/otherAccidentCost), the same basis as the timeline.
      if (problem) return issue(`수리비가 많이 나온 편이에요${breakdown}`, '보험 처리한 사고는 어느 부위를 어떻게 수리했나요?');
      if (d && d.ownDamageWon === 0 && d.otherDamageWon === 0) return ok('보험 처리 기록이 없어요');
      return ok(`경미한 수준이에요${breakdown}`);
    }
    case 'R11': {
      const ratio =
        evidenceOf<number>(result, 'ratio') ??
        (facts.priceVsMarket.kind === 'value' ? facts.priceVsMarket.value.ratio : undefined);
      const question = '가격이 시세와 다른 이유가 있나요?';
      if (!ratio) return problem ? issue('신차 가격과 차이가 커요', question) : ok('가격이 적정한 편이에요');
      const p = Math.round(ratio * 100);
      if (!problem) return ok(`신차 가격의 ${p}%로 적정한 편이에요`);
      return issue(ratio < 1 ? `신차 가격의 ${p}%로 지나치게 싸요` : `신차 가격의 ${p}%로 비싼 편이에요`, question);
    }
    case 'R12': {
      if (!problem) return ok('누유가 없어요');
      const items = evidenceOf(result, 'oilLeak', facts.oilLeak)?.items ?? [];
      const last = items[items.length - 1];
      if (!last) return issue('누유가 있어요', '누유 부위는 수리했나요?');
      const list = items.map((i) => `${leakPart(i.part)}에 ${i.status}`).join(', ');
      return issue(`${list}${josa(last.status, '이', '가')} 있어요`, '누유 부위는 수리했나요?');
    }
    default:
      return ok('정보를 확인하지 못했어요');
  }
}

// ── 미확인 사유 ───────────────────────────────────────────────

const FACT_FIELD: Record<string, keyof ChecklistFacts> = {
  R01: 'insuranceHistoryDisclosed',
  R02: 'inspectionReportDisclosed',
  R03: 'hasEncarDiagnosis',
  R04: 'frameDamage',
  R05: 'usageHistory',
  R06: 'totalLossHistory',
  R07: 'ownerChangeCount',
  R08: 'insuranceGap',
  R09: 'unconfirmedAccident',
  R10: 'minorAccidents',
  R11: 'priceVsMarket',
  R12: 'oilLeak',
};

const NOT_FOUND_BY_SOURCE: Record<RuleSource, string> = {
  record: '엔카에 이 차의 보험 이력이 없어요',
  inspection: '엔카에 성능점검 기록이 없어요',
  diagnosisOrInspection: '프레임 상태를 확인할 자료가 없어요',
  page: '정보를 확인하지 못했어요',
};

/** Most actionable first when several source APIs failed. */
const REASON_PRIORITY = ['login_required', 'api_fetch_error', 'not_fetched', 'no_report_for_personal'];

const reasonRank = (s: FieldStatus<unknown>): number => {
  const i = s.kind === 'parse_failed' ? REASON_PRIORITY.indexOf(s.reason) : -1;
  return i === -1 ? REASON_PRIORITY.length : i;
};

const statusMessage = (status: FieldStatus<unknown>, source: RuleSource): string => {
  if (status.kind === 'hidden_by_dealer') return '딜러가 공개하지 않았어요';
  if (status.kind === 'loading' || status.kind === 'timeout') return '엔카에서 정보를 받지 못했어요';
  if (status.kind !== 'parse_failed') return '정보를 확인하지 못했어요';
  switch (status.reason) {
    case 'login_required':
      return '엔카에 로그인하면 확인할 수 있어요';
    case 'api_fetch_error':
    case 'not_fetched':
      return '엔카에서 정보를 받지 못했어요';
    case 'no_report_for_personal':
      return NOT_FOUND_BY_SOURCE[source];
    case 'not_applicable_personal':
      return '개인 매물은 엔카진단을 받을 수 없어요';
    default:
      return '정보를 확인하지 못했어요';
  }
};

/**
 * Why a rule is 미확인. API failures only survive in `out.parsed.raw.*Api`
 * (the bridge turns them into `not_derived`), so the rule's source API is read
 * first; facts are consulted only when that API was fine.
 */
export function unknownMessage(result: RuleResult, out: MobileOrchestratorResult): string {
  const meta = RULE_META[result.ruleId];
  const status = unknownStatus(result, out);
  return meta && status ? statusMessage(status, meta.source) : '정보를 확인하지 못했어요';
}

/** The status behind a 미확인 rule (see [unknownMessage]); null if none can be named. */
export function unknownStatus(result: RuleResult, out: MobileOrchestratorResult): FieldStatus<unknown> | null {
  const meta = RULE_META[result.ruleId];
  if (!meta) return null;
  const raw = out.parsed.raw;
  const apis: FieldStatus<unknown>[] =
    meta.source === 'record'
      ? [raw.recordApi]
      : meta.source === 'inspection'
        ? [raw.inspectionApi]
        : meta.source === 'diagnosisOrInspection'
          ? [raw.diagnosisApi, raw.inspectionApi]
          : [];
  const failedApis = apis.filter((a) => a.kind !== 'value').sort((a, b) => reasonRank(a) - reasonRank(b));
  const top = failedApis[0];
  if (top) return top;
  const field = FACT_FIELD[result.ruleId];
  return field ? (out.facts[field] as FieldStatus<unknown>) : null;
}

const EXCLUDED_REASON: Record<string, string> = {
  R03: '엔카진단을 받지 않은 차예요(감점 없음)',
  R11: '신차 가격 정보가 없어 평가하지 않았어요',
  R12: '성능점검 기록이 없어 누유를 평가하지 않았어요',
};

/** Why the engine dropped a rule (returned null). */
export function excludedReason(ruleId: string): string {
  return EXCLUDED_REASON[ruleId] ?? '이 차에는 해당하지 않아 평가하지 않았어요';
}
