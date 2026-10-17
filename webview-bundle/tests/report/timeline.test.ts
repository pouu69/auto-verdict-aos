import { describe, it, expect } from 'vitest';
import type { MobileOrchestratorInput } from '../../src/orchestrate-mobile';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { accidentGroups, buildTimeline } from '../../src/report/timeline';
import { formatMan } from '../../src/report/format';
import { FIXTURE_IDS, load, rawOf } from './fixtures';

const PLATE = /\d{2,3}[가-힣]\s?\d{4}|XXXX/;

const timelineOf = (raw: MobileOrchestratorInput) => buildTimeline(raw, orchestrateMobile(raw));

describe('buildTimeline', () => {
  it('41878112: registration, owner changes, gaps, accidents in date order', () => {
    const { raw, out } = load('41878112');
    const t = buildTimeline(raw, out);
    expect(t[0]).toEqual({ date: '2015-04', label: '신차 등록', detail: null, tone: 'neutral' });
    expect(t.filter((e) => e.label === '소유자 변경')).toHaveLength(5);
    expect(t.filter((e) => e.label === '보험 미가입')).toEqual([
      { date: '2023-11', label: '보험 미가입', detail: '2023년 11월~2024년 1월', tone: 'warning' },
      { date: '2026-04', label: '보험 미가입', detail: '2026년 4월~2026년 7월', tone: 'warning' },
    ]);
    expect(t.slice(0, 3)).toEqual([
      { date: '2015-04', label: '신차 등록', detail: null, tone: 'neutral' },
      { date: '2015-05', label: '사고 · 내차 피해', detail: '수리비 합계 50만원', tone: 'warning' },
      { date: '2015-09', label: '사고 · 내차 피해', detail: '수리비 합계 159만원', tone: 'warning' },
    ]);
    const dates = t.map((e) => e.date);
    expect([...dates].sort()).toEqual(dates);
    // Same month: owner change (2026-04-01) before the gap that starts in 2026-04.
    const april = t.filter((e) => e.date === '2026-04').map((e) => e.label);
    expect(april).toEqual(['소유자 변경', '보험 미가입']);
  });

  it('39339524: same-month accidents grouped per side', () => {
    const { raw, out } = load('39339524');
    const t = buildTimeline(raw, out);
    const dec = t.filter((e) => e.date === '2021-12');
    expect(dec).toEqual([
      { date: '2021-12', label: '사고 · 내차 피해', detail: '수리비 합계 114만원', tone: 'warning' },
      { date: '2021-12', label: '사고 2건 · 타차 가해', detail: '수리비 합계 589만원', tone: 'warning' },
    ]);
    const accidents = t.filter((e) => e.label.startsWith('사고'));
    for (const a of accidents) expect(a.detail).toMatch(/만원$/);
    // 9 records on 8 distinct (month, side) keys.
    expect(accidents).toHaveLength(8);
  });

  it('42264483: accidents on different days of the same month become one row', () => {
    const { raw, out } = load('42264483');
    const jan = buildTimeline(raw, out).filter((e) => e.date === '2018-01');
    expect(jan).toEqual([
      { date: '2018-01', label: '사고 2건 · 내차 피해', detail: '수리비 합계 221만원', tone: 'warning' },
      { date: '2018-01', label: '사고 · 타차 가해', detail: '수리비 합계 13만원', tone: 'warning' },
    ]);
  });

  it('per-side repair cost totals equal myAccidentCost / otherAccidentCost (the R10 amounts)', () => {
    let checked = 0;
    for (const id of FIXTURE_IDS) {
      const { raw, out } = load(id);
      const rec = raw.recordJson as Record<string, unknown> | null;
      const damages = out.facts.minorAccidents;
      if (!rec || out.parsed.raw.recordApi.kind !== 'value' || damages.kind !== 'value') continue;
      const groups = accidentGroups(rec);
      const total = (side: string) => groups.filter((g) => g.side === side).reduce((s, g) => s + g.won, 0);
      expect(total('내차 피해'), `${id} 내차`).toBe(rec.myAccidentCost);
      expect(total('타차 가해'), `${id} 타차`).toBe(rec.otherAccidentCost);
      expect(total('내차 피해'), `${id} 내차 = R10`).toBe(damages.value.ownDamageWon);
      expect(total('타차 가해'), `${id} 타차 = R10`).toBe(damages.value.otherDamageWon);
      // Every grouped accident row shows its group's repair cost (order-independent).
      const key = (date: string, label: string, detail: string | null) => `${date}|${label}|${detail}`;
      const rows = buildTimeline(raw, out)
        .filter((e) => e.label.startsWith('사고'))
        .map((e) => key(e.date, e.label, e.detail));
      const expected = groups.map((g) =>
        key(
          g.ym,
          g.count > 1 ? `사고 ${g.count}건 · ${g.side}` : `사고 · ${g.side}`,
          Math.round(g.won / 10_000) > 0 ? `수리비 합계 ${formatMan(g.won)}` : null,
        ),
      );
      expect([...rows].sort(), id).toEqual([...expected].sort());
      checked += 1;
    }
    expect(checked).toBe(3);
  });

  it('41623744: no record -> empty', () => {
    const { raw, out } = load('41623744');
    expect(buildTimeline(raw, out)).toEqual([]);
  });

  it('record answered 200 with an unreadable body -> empty', () => {
    const raw = rawOf('41878112');
    raw.recordJson = {};
    expect(timelineOf(raw)).toEqual([]);
  });

  it('number changes, losses and grouped counts; never prints the plate', () => {
    const raw = rawOf('41878112');
    const rec = raw.recordJson as Record<string, unknown>;
    rec.carInfoChanges = [
      { date: '2015-04-16', carNo: '12가3456' },
      { date: '2019-03-02', carNo: '34나5678' },
    ];
    rec.floodDate = '2020-08-10';
    rec.totalLossDate = '2020-08-10';
    rec.robberDate = null;
    rec.accidents = [
      { type: '1', date: '2020-08-10', insuranceBenefit: 0, partCost: 0, laborCost: 0, paintingCost: 0 },
      { type: '2', date: '2020-08-21', insuranceBenefit: 0, partCost: 0, laborCost: 0, paintingCost: 0 },
    ];
    const t = timelineOf(raw);
    expect(t).toContainEqual({ date: '2019-03', label: '번호 변경', detail: null, tone: 'neutral' });
    expect(t.filter((e) => e.label === '번호 변경')).toHaveLength(1);
    expect(t.filter((e) => e.date === '2020-08')).toEqual([
      { date: '2020-08', label: '사고 2건 · 내차 피해', detail: null, tone: 'warning' },
      { date: '2020-08', label: '침수 피해', detail: null, tone: 'danger' },
      { date: '2020-08', label: '전손 처리', detail: null, tone: 'danger' },
    ]);
    expect(JSON.stringify(t)).not.toMatch(PLATE);
  });

  it('no fixture leaks a plate number', () => {
    for (const id of FIXTURE_IDS) {
      const { raw, out } = load(id);
      expect(JSON.stringify(buildTimeline(raw, out)), id).not.toMatch(PLATE);
    }
  });
});
