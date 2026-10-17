/**
 * Regressions found by the robustness suite (robustness.test.ts): odd listing
 * values the adapter used to format into nonsense or crash on.
 */
import { describe, it, expect } from 'vitest';
import type { MobileOrchestratorInput } from '../../src/orchestrate-mobile';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildReport } from '../../src/report/build-report';
import type { DetailSheet, EvaluationReport } from '../../src/report/types';
import { rawOf, type FixtureId } from './fixtures';

type Obj = Record<string, unknown>;

const reportWith = (id: FixtureId, edit: (raw: MobileOrchestratorInput) => void): EvaluationReport => {
  const raw = rawOf(id);
  edit(raw);
  return buildReport(raw, orchestrateMobile(raw));
};

const rec = (raw: MobileOrchestratorInput): Obj => raw.recordJson as Obj;
const rule = (r: EvaluationReport, id: string) => r.rules.find((x) => x.ruleId === id)!;
const rowValue = (s: DetailSheet | null, label: string): string | undefined =>
  s?.blocks.flatMap((b) => b.rows).find((x) => x.label === label)?.value;

describe('unreadable numbers', () => {
  it('names a huge owner count instead of printing 1e+21', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).ownerChangeCnt = 1e21;
    });
    expect(rule(r, 'R07').message).toBe('주인이 자주 바뀌었어요');
    expect(rule(r, 'R07').summary).toBe('소유자 변경 잦음');
    expect(r.facts.find((f) => f.id === 'owners')!.value).toBe('정보 없음');
    expect(rowValue(rule(r, 'R07').detail, '변경 횟수')).toBe('정보 없음');
  });

  it('drops the R10 breakdown when a repair total is not a number', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).myAccidentCost = 'abc';
    });
    expect(rule(r, 'R10').message).toBe('경미한 수준이에요');
    expect(rule(r, 'R10').summary).toBe('경미');
    expect(rowValue(rule(r, 'R10').detail, '내차 피해')).toBe('정보 없음');
  });

  it('shows a negative claim cost as missing', () => {
    const r = reportWith('39339524', (raw) => {
      for (const a of rec(raw).accidents as Obj[]) a.partCost = -1;
    });
    const sheet = r.timeline.find((t) => t.label.startsWith('사고'))!.sheet;
    expect(rowValue(sheet, '부품비')).toBe('기록 없음');
    expect(JSON.stringify(r)).not.toMatch(/\D-\d/);
  });

  it('keeps a huge loss count out of the R06 copy and sheet', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).totalLossCnt = 1e21;
    });
    expect(rule(r, 'R06').message).toBe('전손·침수·도난 이력이 있어요');
    expect(rowValue(rule(r, 'R06').detail, '전손')).toBe('정보 없음');
  });

  it('leaves a huge price and a negative mileage out of the header', () => {
    const r = reportWith('41623744', (raw) => {
      const base = (raw.preloadedState as { cars: { base: { advertisement: Obj; spec: Obj } } }).cars.base;
      base.advertisement.price = 1e21;
      base.spec.mileage = -1;
    });
    expect(r.car.priceWon).toBeNull();
    expect(r.car.mileageKm).toBeNull();
    expect(r.car.summary).toBe('2010년 · 가솔린');
  });
});

describe('malformed dates', () => {
  it('shows the month, not the raw string, when the day cannot be read', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).ownerChanges = ['202503abc'];
      (rec(raw).accidents as Obj[])[0]!.date = '2021-12-99';
    });
    const owner = r.timeline.find((t) => t.label === '소유자 변경')!;
    expect(rowValue(owner.sheet, '변경일')).toBe('2025년 3월');
    const accident = r.timeline.find((t) => t.date === '2021-12' && t.label.endsWith('내차 피해'))!;
    expect(accident.sheet!.blocks[0]!.title).toBe('2021년 12월 · 내차 피해');
    expect(JSON.stringify(r)).not.toMatch(/202503abc|2021-12-99/);
  });

  it('drops insurance gaps whose months cannot be read', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).notJoinDate1 = '202313~202499';
      rec(raw).notJoinDate2 = '202301~202303';
    });
    const r08 = rule(r, 'R08');
    expect(r08.message).toBe('자차보험 없이 운행한 기간이 3개월 있어요');
    expect(r08.sellerQuestion).toBe('2023년 1월~2023년 3월에 보험 없이 운행했나요?');
    expect(r08.summary).toBe('자차보험 공백 3개월');
    expect(r.timeline.filter((t) => t.label === '보험 미가입').map((t) => t.date)).toEqual(['2023-01']);
    expect(JSON.stringify(r)).not.toMatch(/13월|99월/);
  });

  it('keeps R08 consistent when no gap period can be read', () => {
    const r = reportWith('39339524', (raw) => {
      rec(raw).notJoinDate1 = '209912~200001';
    });
    const r08 = rule(r, 'R08');
    expect(r08.tone).toBe('warning');
    expect(r08.message).toBe('자차보험 없이 운행한 기간이 있어요');
    expect(r08.summary).toBe('자차보험 공백 있음');
    expect(rowValue(r08.detail, '미가입 기간')).toBe('있음');
    expect(r.timeline.some((t) => t.label === '보험 미가입')).toBe(false);
  });
});

describe('성능점검 items without titles', () => {
  it('names untitled flagged items instead of leaving them blank', () => {
    const r = reportWith('39339524', (raw) => {
      (raw.inspectionJson as Obj).inners = [
        { statusType: { code: '5', title: '누수' } },
        { type: { code: 'b', title: '워터펌프' }, statusType: { code: '10' } },
      ];
    });
    expect(r.repairs!.systems.flagged).toEqual([
      { group: '', name: '기타', status: '누수', tone: 'danger' },
      { group: '', name: '워터펌프', status: '기록 없음', tone: 'danger' },
    ]);
  });
});

describe('oil-leak items without names', () => {
  it('keeps the report when a leak item has no status title', () => {
    const r = reportWith('39339524', (raw) => {
      (raw.inspectionJson as Obj).inners = [
        { type: { code: 'a', title: '오일팬' }, statusType: { code: '7' } },
        { type: { code: 'b', title: '실린더 헤드' }, statusType: { code: '6', title: '미세누유' } },
      ];
    });
    expect(rule(r, 'R12').message).toBe('실린더 헤드에 미세누유가 있어요');
    expect(rule(r, 'R12').summary).toBe('실린더 헤드 미세누유');
  });

  it('falls back to the plain sentence when no leak item can be named', () => {
    const r = reportWith('39339524', (raw) => {
      (raw.inspectionJson as Obj).inners = [{ type: { code: 'a', title: '오일팬' }, statusType: { code: '7' } }];
    });
    expect(rule(r, 'R12').message).toBe('누유가 있어요');
    expect(rule(r, 'R12').summary).toBe('누유 있음');
  });
});
