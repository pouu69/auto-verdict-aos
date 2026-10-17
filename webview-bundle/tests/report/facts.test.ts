import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildFacts } from '../../src/report/facts';
import { load, rawOf } from './fixtures';

describe('buildFacts', () => {
  it('41878112: owner changes flagged, clean usage and loss', () => {
    const { raw, out } = load('41878112');
    expect(buildFacts(raw, out)).toEqual([
      { id: 'accidents', label: '사고', value: '내차 2건', tone: 'warning' },
      { id: 'owners', label: '소유자 변경', value: '5회', tone: 'warning' },
      { id: 'usage', label: '용도', value: '자가용', tone: 'neutral' },
      { id: 'floodLoss', label: '침수·전손', value: '없음', tone: 'neutral' },
    ]);
  });

  it('39339524: both accident sides, one owner change', () => {
    const { raw, out } = load('39339524');
    const facts = buildFacts(raw, out);
    expect(facts.map((f) => f.id)).toEqual(['accidents', 'owners', 'usage', 'floodLoss']);
    expect(facts[0]).toEqual({ id: 'accidents', label: '사고', value: '내차 7건 · 타차 2건', tone: 'warning' });
    expect(facts[1]).toEqual({ id: 'owners', label: '소유자 변경', value: '1회', tone: 'neutral' });
  });

  it('41623744: no insurance history -> no facts', () => {
    const { raw, out } = load('41623744');
    expect(buildFacts(raw, out)).toEqual([]);
  });

  it('usage and loss danger, zero counts', () => {
    const raw = rawOf('41878112');
    const rec = raw.recordJson as Record<string, unknown>;
    Object.assign(rec, {
      loan: 1,
      business: 1,
      government: 1,
      totalLossCnt: 1,
      floodPartLossCnt: 2,
      myAccidentCnt: 0,
      otherAccidentCnt: 3,
      ownerChangeCnt: 0,
    });
    const facts = buildFacts(raw, orchestrateMobile(raw));
    expect(facts).toEqual([
      { id: 'accidents', label: '사고', value: '타차 3건', tone: 'neutral' },
      { id: 'owners', label: '소유자 변경', value: '없음', tone: 'neutral' },
      { id: 'usage', label: '용도', value: '렌트·영업용·관용', tone: 'danger' },
      { id: 'floodLoss', label: '침수·전손', value: '전손 1회 · 침수분손 2회', tone: 'danger' },
    ]);
  });

  it('government only is warning; unreadable record fields are 확인 불가', () => {
    const raw = rawOf('41878112');
    const rec = raw.recordJson as Record<string, unknown>;
    rec.government = 2;
    expect(buildFacts(raw, orchestrateMobile(raw))[2]).toEqual({
      id: 'usage', label: '용도', value: '관용', tone: 'warning',
    });

    const broken = rawOf('41878112');
    broken.recordJson = { unexpected: true };
    expect(buildFacts(broken, orchestrateMobile(broken))).toEqual([
      { id: 'accidents', label: '사고', value: '확인 불가', tone: 'neutral' },
      { id: 'owners', label: '소유자 변경', value: '확인 불가', tone: 'neutral' },
      { id: 'usage', label: '용도', value: '확인 불가', tone: 'neutral' },
      { id: 'floodLoss', label: '침수·전손', value: '확인 불가', tone: 'neutral' },
    ]);
  });
});
