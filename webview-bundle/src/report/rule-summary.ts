/**
 * One-line noun-phrase result for the "전체 항목" rows (spec 4.0), e.g.
 * "5회" under the "소유자 변경" title. The row shows the rule title right
 * above, so a summary never starts by repeating it (R03 keeps "엔카진단 통과
 * 차량"). No 해요체: the full sentence stays in `message` for cards, sheets
 * and share text. Numbers come from evidence, then facts, the same way
 * [ruleCopy] reads them.
 */
import type { RuleResult } from '@core/types/RuleTypes.js';
import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { FrameSource } from './frame-source';
import { formatMan, partName } from './format';
import { evidenceOf, gapDuration, lossHits, unknownStatus, usageKinds } from './rule-copy';
import { reasonShort } from './reasons';

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
    case 'R02':
      return problem ? '비공개' : '공개';
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
      if (!problem) return '없음';
      const l = evidenceOf(result, 'totalLoss', facts.totalLossHistory);
      return (l ? lossHits(l) : []).join(' · ') || '이력 있음';
    }
    case 'R07': {
      const n = evidenceOf(result, 'ownerChangeCount', facts.ownerChangeCount);
      if (n === undefined) return problem ? '잦음' : '정상 범위';
      return n === 0 ? '없음' : `${n}회`;
    }
    case 'R08': {
      if (!problem) return '없음';
      const g = evidenceOf(result, 'insuranceGap', facts.insuranceGap);
      const months = g?.totalMonths ?? 0;
      const periods = g?.periods.length ?? 0;
      if (months <= 0) return '있음';
      return periods > 1 ? `${gapDuration(months)} · ${periods}구간` : gapDuration(months);
    }
    case 'R09':
      return problem ? '미확정 사고 있음' : '미확정 사고 없음';
    case 'R10': {
      const d = evidenceOf(result, 'damages', facts.minorAccidents);
      if (!d) return problem ? '수리비 큼' : '경미';
      if (!problem && d.ownDamageWon === 0 && d.otherDamageWon === 0) return '없음';
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
      if (!problem) return '없음';
      const items = evidenceOf(result, 'oilLeak', facts.oilLeak)?.items ?? [];
      const first = items[0];
      if (!first) return '있음';
      const head = `${partName(first.part)} ${first.status}`;
      // Keep one line: the sheet lists every item.
      return items.length > 1 ? `${head} 외 ${items.length - 1}곳` : head;
    }
    default:
      return '확인 불가';
  }
}

/** "확인 불가 · 로그인 필요" — same reason [unknownMessage] explains in full. */
export function unknownSummary(result: RuleResult, out: MobileOrchestratorResult): string {
  const status = unknownStatus(result, out);
  return `확인 불가 · ${status ? reasonShort(status) : '정보 없음'}`;
}
