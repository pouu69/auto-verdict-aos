import type { RuleReport, RuleResult } from '@core/types/RuleTypes.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import { lossKinds, usageKinds } from './rule-copy';

const ruleNo = (r: RuleResult): number => Number(r.ruleId.slice(1));

const evidence = <T>(r: RuleResult, field: string): T | undefined =>
  r.evidence.find((e) => e.field === field)?.value as T | undefined;

const avoid = (what: string): string => `${what} 이력이 있어 사지 않는 게 좋아요`;

const killerHeadline = (killer: RuleResult, facts: ChecklistFacts): string => {
  switch (killer.ruleId) {
    case 'R04':
      return avoid('사고(프레임 손상)');
    case 'R05': {
      const u = evidence<{ rental: boolean; taxi: boolean }>(killer, 'usageHistory') ??
        (facts.usageHistory.kind === 'value' ? facts.usageHistory.value : undefined);
      return avoid((u ? usageKinds(u) : []).join('·') || '렌트·영업용');
    }
    case 'R06': {
      const l = evidence<{ totalLoss: number; floodTotal: number; floodPart: number; robber: number }>(killer, 'totalLoss') ??
        (facts.totalLossHistory.kind === 'value' ? facts.totalLossHistory.value : undefined);
      return avoid((l ? lossKinds(l) : []).join('·') || '전손·침수·도난');
    }
    default:
      return '위험한 이력이 있어 사지 않는 게 좋아요';
  }
};

/** 결론 문장 (spec 4 headline table). The verdict itself is the engine's. */
export function buildHeadline(report: RuleReport, facts: ChecklistFacts): string {
  const count = (s: RuleResult['severity']) => report.results.filter((r) => r.severity === s).length;
  switch (report.verdict) {
    case 'NEVER': {
      const killer = [...report.results].filter((r) => r.severity === 'killer').sort((a, b) => ruleNo(a) - ruleNo(b))[0];
      return killer ? killerHeadline(killer, facts) : '위험한 이력이 있어 사지 않는 게 좋아요';
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
