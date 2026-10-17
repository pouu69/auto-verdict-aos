/**
 * One-line noun-phrase result for the "전체 항목" rows (spec 4.0), e.g.
 * "소유자 변경 5회". No 해요체: the full sentence stays in `message` for
 * cards, sheets and share text. Numbers come from evidence, then facts,
 * the same way [ruleCopy] reads them.
 */
import type { RuleResult } from '@core/types/RuleTypes.js';
import type { FieldStatus } from '@core/types/FieldStatus.js';
import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { FrameSource } from './frame-source';
import { formatMan } from './format';
import { evidenceOf, gapDuration, leakPart, lossHits, unknownStatus, usageKinds } from './rule-copy';

const FRAME_KILLER: Record<FrameSource, string> = {
  diagnosis: '엔카진단 프레임 손상',
  inspection: '성능점검 사고 이력 있음',
  unknown: '프레임 손상 있음',
};

/** Summary for a decided rule (not 미확인; use [unknownSummary]). */
export function ruleSummary(result: RuleResult, out: MobileOrchestratorResult, frameSource: FrameSource = 'unknown'): string {
  const facts = out.facts;
  const s = result.severity;
  const problem = s === 'killer' || s === 'fail' || s === 'warn';
  switch (result.ruleId) {
    case 'R01':
      return problem ? '보험이력 비공개' : '보험이력 공개';
    case 'R02':
      return problem ? '성능점검 비공개' : '성능점검 공개';
    case 'R03':
      return '엔카진단 통과 차량';
    case 'R04': {
      if (problem) return FRAME_KILLER[frameSource];
      const parts = evidenceOf(result, 'frameDamage', facts.frameDamage)?.parts ?? [];
      return parts.length > 0 ? `프레임 무사고 · 외판 수리 ${parts.length}곳` : '프레임 무사고';
    }
    case 'R05': {
      if (s === 'killer') {
        const u = evidenceOf(result, 'usageHistory', facts.usageHistory);
        return `${(u ? usageKinds(u) : []).join('·') || '렌트·영업용'} 이력 있음`;
      }
      return s === 'warn' ? '관용 이력 있음' : '렌트·택시·영업용 이력 없음';
    }
    case 'R06': {
      if (!problem) return '전손·침수·도난 없음';
      const l = evidenceOf(result, 'totalLoss', facts.totalLossHistory);
      return (l ? lossHits(l) : []).join(' · ') || '전손·침수·도난 이력 있음';
    }
    case 'R07': {
      const n = evidenceOf(result, 'ownerChangeCount', facts.ownerChangeCount);
      if (n === undefined) return problem ? '소유자 변경 잦음' : '소유자 변경 정상 범위';
      return n === 0 ? '소유자 변경 없음' : `소유자 변경 ${n}회`;
    }
    case 'R08': {
      if (!problem) return '자차보험 공백 없음';
      const g = evidenceOf(result, 'insuranceGap', facts.insuranceGap);
      const months = g?.totalMonths ?? 0;
      const periods = g?.periods.length ?? 0;
      if (months <= 0) return '자차보험 공백 있음';
      return periods > 1 ? `자차보험 공백 ${gapDuration(months)} · ${periods}구간` : `자차보험 공백 ${gapDuration(months)}`;
    }
    case 'R09':
      return problem ? '수리비 미확정 있음' : '수리비 미확정 없음';
    case 'R10': {
      const d = evidenceOf(result, 'damages', facts.minorAccidents);
      if (!d) return problem ? '수리비 큼' : '경미';
      if (!problem && d.ownDamageWon === 0 && d.otherDamageWon === 0) return '보험 처리 없음';
      return `${problem ? '수리비 큼' : '경미'} · 내차 ${formatMan(d.ownDamageWon)} · 타차 ${formatMan(d.otherDamageWon)}`;
    }
    case 'R11': {
      const ratio =
        evidenceOf<number>(result, 'ratio') ??
        (facts.priceVsMarket.kind === 'value' ? facts.priceVsMarket.value.ratio : undefined);
      if (!ratio) return problem ? '신차가 대비 차이 큼' : '신차가 대비 정상';
      const tag = !problem ? '정상' : ratio < 1 ? '저가 의심' : '고가';
      return `신차가 대비 ${Math.round(ratio * 100)}% · ${tag}`;
    }
    case 'R12': {
      if (!problem) return '누유 없음';
      const items = evidenceOf(result, 'oilLeak', facts.oilLeak)?.items ?? [];
      const first = items[0];
      if (!first) return '누유 있음';
      const head = `${leakPart(first.part)} ${first.status}`;
      // Keep one line: the sheet lists every item.
      return items.length > 1 ? `${head} 외 ${items.length - 1}곳` : head;
    }
    default:
      return '확인 불가';
  }
}

const SHORT_REASON: Record<string, string> = {
  login_required: '로그인 필요',
  api_fetch_error: '받지 못함',
  not_fetched: '받지 못함',
  no_report_for_personal: '자료 없음',
  not_applicable_personal: '개인 매물',
};

const shortReason = (s: FieldStatus<unknown> | null): string => {
  if (!s) return '정보 없음';
  if (s.kind === 'hidden_by_dealer') return '딜러 비공개';
  if (s.kind === 'loading' || s.kind === 'timeout') return '받지 못함';
  if (s.kind === 'parse_failed') return SHORT_REASON[s.reason] ?? '정보 없음';
  return '정보 없음';
};

/** "확인 불가 · 로그인 필요" — same reason [unknownMessage] explains in full. */
export function unknownSummary(result: RuleResult, out: MobileOrchestratorResult): string {
  return `확인 불가 · ${shortReason(unknownStatus(result, out))}`;
}
