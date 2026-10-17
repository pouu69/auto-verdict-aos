import { describe, it, expect } from 'vitest';
import type { MobileOrchestratorInput } from '../../src/orchestrate-mobile';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildTimeline } from '../../src/report/timeline';
import { load, rawOf } from './fixtures';

const timelineOf = (raw: MobileOrchestratorInput) => buildTimeline(raw, orchestrateMobile(raw));
const wonOf = (s: string): number => Number(s.replace(/[^\d]/g, ''));

describe('timeline sheets', () => {
  it('39339524: accident sheets cover all 9 claims; per-side totals equal the record totals', () => {
    const { raw, out } = load('39339524');
    const rec = raw.recordJson as { myAccidentCost: number; otherAccidentCost: number };
    const accidents = buildTimeline(raw, out).filter((e) => e.label.startsWith('사고'));
    const claims = accidents.flatMap((e) => e.sheet!.blocks);
    expect(claims).toHaveLength(9);
    const total = (side: string) =>
      claims
        .filter((b) => b.title!.endsWith(side))
        .reduce((s, b) => s + wonOf(b.rows.find((r) => r.label === '수리비 합계')!.value), 0);
    expect(total('내차 피해')).toBe(rec.myAccidentCost);
    expect(total('타차 가해')).toBe(rec.otherAccidentCost);

    const dec = accidents.find((e) => e.date === '2021-12' && e.label === '사고 2건 · 타차 가해')!.sheet!;
    expect(dec.title).toBe('사고 2건 · 타차 가해');
    expect(dec.subtitle).toBe('수리비 합계 5,886,670원');
    expect(dec.blocks.map((b) => b.title)).toEqual(['2021-12-01 · 타차 가해', '2021-12-01 · 타차 가해']);
    expect(dec.blocks[0]!.rows.map((r) => r.label)).toEqual(['보험금', '부품비', '공임', '도장비', '수리비 합계']);
    expect(dec.blocks[0]!.rows[0]).toEqual({ label: '보험금', value: '6,353,300원', tone: null });
    expect(dec.blocks[1]!.note).toMatch(/부품비·공임·도장비/);
  });

  it('41878112: registration has no sheet; owner changes and gaps do', () => {
    const { raw, out } = load('41878112');
    const t = buildTimeline(raw, out);
    expect(t.find((e) => e.label === '신차 등록')!.sheet).toBeNull();

    const owners = t.filter((e) => e.label === '소유자 변경').map((e) => e.sheet!);
    expect(owners.map((s) => s.blocks[0]!.rows)).toEqual(
      ['2018-04-25', '2023-11-14', '2024-02-14', '2024-09-30', '2026-04-01'].map((d, i) => [
        { label: '변경일', value: d, tone: null },
        { label: '순서', value: `전체 5회 중 ${i + 1}번째`, tone: null },
      ]),
    );

    const gap = t.find((e) => e.label === '보험 미가입')!.sheet!;
    expect(gap.subtitle).toBe('2023년 11월~2024년 1월');
    expect(gap.blocks[0]!.rows).toEqual([
      { label: '시작', value: '2023년 11월', tone: null },
      { label: '끝', value: '2024년 1월', tone: null },
      { label: '기간', value: '3개월', tone: 'warning' },
    ]);
  });

  it('number changes and losses get small sheets without the plate', () => {
    const raw = rawOf('41878112');
    Object.assign(raw.recordJson as object, {
      carInfoChanges: [
        { date: '2015-04-16', carNo: '12가3456' },
        { date: '2019-03-02', carNo: '34나5678' },
      ],
      floodDate: '2020-08-10',
      floodTotalLossCnt: 0,
      floodPartLossCnt: 1,
    });
    const t = timelineOf(raw);
    const plate = t.find((e) => e.label === '번호 변경')!.sheet!;
    expect(plate.blocks[0]!.rows).toEqual([
      { label: '변경일', value: '2019-03-02', tone: null },
      { label: '순서', value: '1회', tone: null },
    ]);
    const flood = t.find((e) => e.label === '침수 피해')!.sheet!;
    expect(flood.blocks[0]!.rows).toEqual([
      { label: '날짜', value: '2020-08-10', tone: null },
      { label: '침수전손', value: '없음', tone: null },
      { label: '침수분손', value: '1회', tone: 'danger' },
    ]);
    expect(JSON.stringify(t)).not.toMatch(/12가3456|34나5678/);
  });
});
