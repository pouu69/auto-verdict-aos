import type { RuleReport, RuleResult } from '@core/types/RuleTypes.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import { evidenceOf, lossKinds, usageKinds } from './rule-copy';
import { ruleNo } from './rule-meta';
import type { FrameSource } from './frame-source';

const avoid = (what: string): string => `${what} 이력이 있어 사지 않는 게 좋아요`;

/** R04 headline by evidence source; mirrors the R04 killer sentence in rule-copy. */
const FRAME_HEADLINE: Record<FrameSource, string> = {
  diagnosis: '엔카진단에서 프레임 손상이 확인돼 사지 않는 게 좋아요',
  inspection: '성능점검에 사고 이력이 있어 사지 않는 게 좋아요',
  unknown: avoid('사고'),
};

const killerHeadline = (killer: RuleResult, facts: ChecklistFacts, frameSource: FrameSource): string => {
  switch (killer.ruleId) {
    case 'R04':
      return FRAME_HEADLINE[frameSource];
    case 'R05': {
      const u = evidenceOf(killer, 'usageHistory', facts.usageHistory);
      return avoid((u ? usageKinds(u) : []).join('·') || '렌트·영업용');
    }
    case 'R06': {
      const l = evidenceOf(killer, 'totalLoss', facts.totalLossHistory);
      return avoid((l ? lossKinds(l) : []).join('·') || '전손·침수·도난');
    }
    default:
      return '위험한 이력이 있어 사지 않는 게 좋아요';
  }
};

/** 결론 문장 (spec 4 headline table). The verdict itself is the engine's. */
export function buildHeadline(
  report: RuleReport,
  facts: ChecklistFacts,
  frameSource: FrameSource = 'unknown',
): string {
  const count = (s: RuleResult['severity']) => report.results.filter((r) => r.severity === s).length;
  switch (report.verdict) {
    case 'NEVER': {
      const killer = report.results
        .filter((r) => r.severity === 'killer')
        .sort((a, b) => ruleNo(a.ruleId) - ruleNo(b.ruleId))[0];
      return killer ? killerHeadline(killer, facts, frameSource) : '위험한 이력이 있어 사지 않는 게 좋아요';
    }
    case 'CAUTION':
      return `사기 전에 ${count('warn')}가지는 꼭 확인하세요`;
    case 'UNKNOWN':
      return `확인하지 못한 항목이 ${count('unknown')}개 있어요`;
    case 'OK':
    default:
      if (count('warn') === 1) return '큰 문제는 없지만 1가지는 확인해 보세요';
      if (count('fail') > 0) return '큰 문제는 없지만 비공개 항목이 있어요';
      return '큰 문제는 발견되지 않았어요';
  }
}
