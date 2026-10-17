import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { Fact, Tone } from './types';
import { lossHits } from './rule-copy';
import { isObj, num } from './raw';

const UNKNOWN_VALUE = '확인 불가';

const fact = (id: Fact['id'], label: string, value: string | null, tone: Tone): Fact =>
  value === null ? { id, label, value: UNKNOWN_VALUE, tone: 'neutral' } : { id, label, value, tone };

/** R07 warns above 2 changes; keep the tile in step. */
const OWNER_WARN_FROM = 3;

/**
 * The four headline facts, in fixed order. Empty when the insurance history
 * (recordJson) was not collected at all.
 */
export function buildFacts(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): Fact[] {
  const rec = raw.recordJson;
  if (!isObj(rec)) return [];

  const my = num(rec, 'myAccidentCnt');
  const other = num(rec, 'otherAccidentCnt');
  let accidents: string | null = null;
  if (my !== null && other !== null) {
    const parts: string[] = [];
    if (my > 0) parts.push(`내차 ${my}건`);
    if (other > 0) parts.push(`타차 ${other}건`);
    accidents = parts.length > 0 ? parts.join(' · ') : '없음';
  }

  const owners = num(rec, 'ownerChangeCnt');

  const usage = out.facts.usageHistory;
  let usageValue: string | null = null;
  let usageTone: Tone = 'neutral';
  if (usage.kind === 'value') {
    const tags: string[] = [];
    if (usage.value.rental) tags.push('렌트');
    if (usage.value.taxi) tags.push('영업용');
    if (usage.value.business) tags.push('관용');
    usageValue = tags.length > 0 ? tags.join('·') : '자가용';
    usageTone = usage.value.rental || usage.value.taxi ? 'danger' : usage.value.business ? 'warning' : 'neutral';
  }

  const loss = out.facts.totalLossHistory;
  const hits = loss.kind === 'value' ? lossHits(loss.value) : null;

  return [
    fact('accidents', '사고', accidents, my !== null && my > 0 ? 'warning' : 'neutral'),
    fact(
      'owners',
      '소유자 변경',
      owners === null ? null : owners === 0 ? '없음' : `${owners}회`,
      owners !== null && owners >= OWNER_WARN_FROM ? 'warning' : 'neutral',
    ),
    fact('usage', '용도', usageValue, usageTone),
    fact(
      'floodLoss',
      '침수·전손',
      hits === null ? null : hits.length > 0 ? hits.join(' · ') : '없음',
      hits !== null && hits.length > 0 ? 'danger' : 'neutral',
    ),
  ];
}
