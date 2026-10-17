import type { Severity } from '@core/types/RuleTypes.js';
import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { Fact, Tone } from './types';
import { lossHits } from './rule-copy';

/** Tile tone follows the matching rule result so the thresholds live in the engine only. */
const toneOf = (out: MobileOrchestratorResult, ruleId: string): Tone => {
  const severity: Severity | undefined = out.report.results.find((r) => r.ruleId === ruleId)?.severity;
  if (severity === 'killer' || severity === 'fail') return 'danger';
  if (severity === 'warn') return 'warning';
  return 'neutral';
};

/**
 * The four headline facts, in fixed order. Empty unless the insurance history
 * (record API) was read.
 */
export function buildFacts(out: MobileOrchestratorResult): Fact[] {
  const record = out.parsed.raw.recordApi;
  if (record.kind !== 'value') return [];
  const rec = record.value;

  const accidentParts: string[] = [];
  if (rec.myAccidentCnt > 0) accidentParts.push(`내차 ${rec.myAccidentCnt}건`);
  if (rec.otherAccidentCnt > 0) accidentParts.push(`타차 ${rec.otherAccidentCnt}건`);

  const ownerFact = out.facts.ownerChangeCount;
  const owners = ownerFact.kind === 'value' ? ownerFact.value : rec.ownerChangeCnt;

  const usage = out.facts.usageHistory;
  const usageTags: string[] = [];
  if (usage.kind === 'value') {
    if (usage.value.rental) usageTags.push('렌트');
    if (usage.value.taxi) usageTags.push('영업용');
    if (usage.value.business) usageTags.push('관용');
  }

  const loss = out.facts.totalLossHistory;
  const hits = loss.kind === 'value' ? lossHits(loss.value) : [];

  return [
    {
      id: 'accidents',
      label: '사고',
      value: accidentParts.length > 0 ? accidentParts.join(' · ') : '없음',
      tone: rec.myAccidentCnt > 0 ? 'warning' : 'neutral',
    },
    { id: 'owners', label: '소유자 변경', value: owners === 0 ? '없음' : `${owners}회`, tone: toneOf(out, 'R07') },
    { id: 'usage', label: '용도', value: usageTags.length > 0 ? usageTags.join('·') : '자가용', tone: toneOf(out, 'R05') },
    { id: 'floodLoss', label: '침수·전손', value: hits.length > 0 ? hits.join(' · ') : '없음', tone: toneOf(out, 'R06') },
  ];
}
