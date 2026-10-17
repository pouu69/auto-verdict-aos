import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildFacts } from '../../src/report/facts';
import { load, rawOf } from './fixtures';

describe('buildFacts', () => {
  it('41878112: owner changes flagged, clean usage and loss', () => {
    expect(buildFacts(load('41878112').out)).toEqual([
      { id: 'accidents', label: '사고', value: '내차 2건', tone: 'warning' },
      { id: 'owners', label: '소유자 변경', value: '5회', tone: 'warning' },
      { id: 'usage', label: '용도', value: '자가용', tone: 'neutral' },
      { id: 'floodLoss', label: '침수·전손', value: '없음', tone: 'neutral' },
    ]);
  });

  it('39339524: both accident sides, one owner change', () => {
    const facts = buildFacts(load('39339524').out);
    expect(facts.map((f) => f.id)).toEqual(['accidents', 'owners', 'usage', 'floodLoss']);
    expect(facts[0]).toEqual({ id: 'accidents', label: '사고', value: '내차 7건 · 타차 2건', tone: 'warning' });
    expect(facts[1]).toEqual({ id: 'owners', label: '소유자 변경', value: '1회', tone: 'neutral' });
  });

  it('41623744: no insurance history -> no facts', () => {
    expect(buildFacts(load('41623744').out)).toEqual([]);
  });

  it('record answered 200 with an unreadable body -> no facts', () => {
    const raw = rawOf('41878112');
    raw.recordJson = {};
    expect(buildFacts(orchestrateMobile(raw))).toEqual([]);
  });

  it('usage and loss tones follow the R05/R06 results', () => {
    const raw = rawOf('41878112');
    Object.assign(raw.recordJson as Record<string, unknown>, {
      loan: 1,
      business: 1,
      government: 1,
      totalLossCnt: 1,
      floodPartLossCnt: 2,
      myAccidentCnt: 0,
      otherAccidentCnt: 3,
      ownerChangeCnt: 0,
    });
    expect(buildFacts(orchestrateMobile(raw))).toEqual([
      { id: 'accidents', label: '사고', value: '타차 3건', tone: 'neutral' },
      { id: 'owners', label: '소유자 변경', value: '없음', tone: 'neutral' },
      { id: 'usage', label: '용도', value: '렌트·영업용·관용', tone: 'danger' },
      { id: 'floodLoss', label: '침수·전손', value: '전손 1회 · 침수분손 2회', tone: 'danger' },
    ]);
  });

  it('government only is warning (R05 warn), three owners is warning (R07 warn)', () => {
    const raw = rawOf('41878112');
    Object.assign(raw.recordJson as Record<string, unknown>, { government: 2, ownerChangeCnt: 3, myAccidentCnt: 0, otherAccidentCnt: 0 });
    const facts = buildFacts(orchestrateMobile(raw));
    expect(facts[0]).toEqual({ id: 'accidents', label: '사고', value: '없음', tone: 'neutral' });
    expect(facts[1]).toEqual({ id: 'owners', label: '소유자 변경', value: '3회', tone: 'warning' });
    expect(facts[2]).toEqual({ id: 'usage', label: '용도', value: '관용', tone: 'warning' });
  });
});
