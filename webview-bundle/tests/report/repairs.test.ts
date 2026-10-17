import { describe, it, expect } from 'vitest';
import { buildRepairs, walkSystems } from '../../src/report/repairs';
import { FIXTURE_IDS, load, rawOf } from './fixtures';

describe('buildRepairs', () => {
  it('39339524: two panels, 2랭크 before 1랭크, statuses cleaned', () => {
    const repairs = buildRepairs(rawOf('39339524'));
    expect(repairs).not.toBeNull();
    expect(repairs!.panels).toEqual([
      { name: '쿼터 패널(우)', status: '판금·용접 · 용접·절단', rank: '2랭크', tone: 'warning' },
      { name: '트렁크 리드', status: '교환', rank: '1랭크', tone: 'warning' },
    ]);
    expect(repairs!.systems.flagged).toEqual([]);
    expect(repairs!.systems.okCount).toBeGreaterThan(20);
  });

  it('42264483: frame rank first, leak and failure flagged danger-first', () => {
    const repairs = buildRepairs(rawOf('42264483'))!;
    expect(repairs.panels.map((p) => [p.name, p.rank, p.tone])).toEqual([
      ['리어 패널', 'A랭크', 'danger'],
      ['쿼터 패널(우)', '2랭크', 'warning'],
      ['프론트 휀더(우)', '1랭크', 'warning'],
      ['트렁크 리드', '1랭크', 'warning'],
      ['프론트 휀더(좌)', '1랭크', 'warning'],
    ]);
    expect(repairs.systems.flagged).toEqual([
      { group: '오일누유', name: '실린더 블록 / 오일팬', status: '누유', tone: 'danger' },
      { group: '동력전달', name: '등속조인트', status: '불량', tone: 'danger' },
    ]);
  });

  it('41878112: inspection without panels', () => {
    const repairs = buildRepairs(rawOf('41878112'))!;
    expect(repairs.panels).toEqual([]);
    expect(repairs.systems.flagged).toEqual([]);
  });

  it('41623744: no inspection -> null', () => {
    expect(buildRepairs(rawOf('41623744'))).toBeNull();
    const raw = rawOf('41878112');
    raw.inspectionJson = 'oops';
    expect(buildRepairs(raw)).toBeNull();
  });

  it('unknown rank and odd tone codes', () => {
    const raw = rawOf('41878112');
    raw.inspectionJson = {
      outers: [
        { type: { code: 'P1', title: '후드' }, statusTypes: [{ code: 'X', title: '교환(교체)' }], attributes: ['RANK_Z'] },
        { type: { code: 'P2', title: '도어' }, statusTypes: [{ code: 'A', title: '판금' }, { code: 'A', title: '판금' }], attributes: [] },
        { type: { code: 'P3', title: '루프' }, statusTypes: [], attributes: ['RANK_A'] },
      ],
      inners: [
        {
          type: { code: 'S01', title: '원동기' },
          statusType: null,
          children: [
            { type: { code: 'a', title: '오일 유량' }, statusType: { code: '8', title: '부족' } },
            { type: { code: 'b', title: '소음' }, statusType: { code: '11', title: '있음' } },
            { type: { code: 'c', title: '누수' }, statusType: { code: '5', title: '누수' } },
          ],
        },
      ],
    };
    const repairs = buildRepairs(raw)!;
    expect(repairs.panels).toEqual([
      { name: '후드', status: '교환', rank: null, tone: 'neutral' },
      { name: '도어', status: '판금', rank: null, tone: 'neutral' },
    ]);
    expect(repairs.systems.flagged.map((f) => [f.name, f.tone])).toEqual([
      ['누수', 'danger'],
      ['오일 유량', 'warning'],
      ['소음', 'neutral'],
    ]);
    expect(repairs.systems.okCount).toBe(0);
  });

  it('agrees with R12 on every fixture: an oil-leak node (6/7) exists iff R12 warns', () => {
    for (const id of FIXTURE_IDS) {
      const { raw, out } = load(id);
      const ins = raw.inspectionJson as { inners?: unknown } | undefined;
      const nodes = walkSystems(ins?.inners);
      const hasLeak = nodes.some((n) => n.code === '6' || n.code === '7');
      const r12 = out.report.results.find((r) => r.ruleId === 'R12');
      expect(r12?.severity === 'warn', id).toBe(hasLeak);
    }
  });
});
