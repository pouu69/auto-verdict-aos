import { describe, it, expect } from 'vitest';
import { buildTimeline } from '../../src/report/timeline';
import { FIXTURE_IDS, rawOf } from './fixtures';

const PLATE = /\d{2,3}[가-힣]\s?\d{4}|XXXX/;

describe('buildTimeline', () => {
  it('41878112: registration, owner changes, gaps, accidents in date order', () => {
    const t = buildTimeline(rawOf('41878112'));
    expect(t[0]).toEqual({ date: '2015-04', label: '신차 등록', detail: null, tone: 'neutral' });
    expect(t.filter((e) => e.label === '소유자 변경')).toHaveLength(5);
    expect(t.filter((e) => e.label === '보험 미가입')).toEqual([
      { date: '2023-11', label: '보험 미가입', detail: '2023-11~2024-01', tone: 'warning' },
      { date: '2026-04', label: '보험 미가입', detail: '2026-04~2026-07', tone: 'warning' },
    ]);
    expect(t.slice(0, 3)).toEqual([
      { date: '2015-04', label: '신차 등록', detail: null, tone: 'neutral' },
      { date: '2015-05', label: '사고 · 내차 피해', detail: '보험 처리 합계 95만원', tone: 'warning' },
      { date: '2015-09', label: '사고 · 내차 피해', detail: '보험 처리 합계 139만원', tone: 'warning' },
    ]);
    const dates = t.map((e) => e.date);
    expect([...dates].sort()).toEqual(dates);
    // Same month: owner change (2026-04-01) before the gap that starts in 2026-04.
    const april = t.filter((e) => e.date === '2026-04').map((e) => e.label);
    expect(april).toEqual(['소유자 변경', '보험 미가입']);
  });

  it('39339524: same-day accidents grouped per side', () => {
    const t = buildTimeline(rawOf('39339524'));
    const dec = t.filter((e) => e.date === '2021-12');
    expect(dec).toEqual([
      { date: '2021-12', label: '사고 · 내차 피해', detail: '보험 처리 합계 101만원', tone: 'warning' },
      { date: '2021-12', label: '사고 2건 · 타차 가해', detail: '보험 처리 합계 736만원', tone: 'warning' },
    ]);
    const accidents = t.filter((e) => e.label.startsWith('사고'));
    for (const a of accidents) expect(a.detail).toMatch(/만원$/);
    // 9 records on 8 distinct (date, side) keys.
    expect(accidents).toHaveLength(8);
  });

  it('41623744: no record -> empty', () => {
    expect(buildTimeline(rawOf('41623744'))).toEqual([]);
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
      { type: '2', date: '2020-08-10', insuranceBenefit: 0, partCost: 0, laborCost: 0, paintingCost: 0 },
    ];
    const t = buildTimeline(raw);
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
      expect(JSON.stringify(buildTimeline(rawOf(id))), id).not.toMatch(PLATE);
    }
  });
});
