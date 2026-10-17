import { describe, it, expect } from 'vitest';
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildReport } from '../../src/report/build-report';
import { ruleSummary } from '../../src/report/rule-summary';
import type { EvaluationReport } from '../../src/report/types';
import { FIXTURE_IDS, FORBIDDEN, load, rawOf, type FixtureId } from './fixtures';

/** Upper bound for one list line; the longest spec example (R10) is 29. */
const MAX_LENGTH = 30;

const reportOf = (id: FixtureId): EvaluationReport => {
  const { raw, out } = load(id);
  return buildReport(raw, out);
};

const summaries = (r: EvaluationReport): Record<string, string> =>
  Object.fromEntries(r.rules.map((x) => [x.ruleId, x.summary]));

const result = (ruleId: string, severity: Severity, evidence: RuleResult['evidence']): RuleResult => ({
  ruleId,
  title: '',
  severity,
  message: '엔진 문구',
  evidence,
  acknowledgeable: false,
});

describe('rule summary (전체 항목 rows)', () => {
  it('every rule of every fixture: short noun phrase, no 해요체', () => {
    for (const id of FIXTURE_IDS) {
      for (const item of reportOf(id).rules) {
        const s = item.summary;
        expect(s.length, `${id} ${item.ruleId}`).toBeGreaterThan(0);
        expect(s.length, `${id} ${item.ruleId} "${s}"`).toBeLessThanOrEqual(MAX_LENGTH);
        expect(s, `${id} ${item.ruleId}`).not.toMatch(/요$|해요|있어요|없어요/);
        expect(s, `${id} ${item.ruleId}`).not.toMatch(FORBIDDEN);
        expect(s).not.toContain('\n');
      }
    }
  });

  it('fixture summaries match the spec examples', () => {
    expect(summaries(reportOf('39339524'))).toMatchObject({
      R03: '엔카진단 통과 차량',
      R04: '성능점검 사고 이력 있음',
      R05: '렌트·택시·영업용 이력 없음',
      R06: '전손·침수·도난 없음',
      R07: '소유자 변경 1회',
      R10: '수리비 큼 · 내차 1,126만원 · 타차 589만원',
      R01: '공개',
      R02: '공개',
    });
    expect(summaries(reportOf('41878112'))).toMatchObject({
      R04: '프레임 무사고',
      R07: '소유자 변경 5회',
      R08: '자차보험 공백 7개월 · 2구간',
      R09: '수리비 미확정 없음',
      R10: '경미 · 내차 209만원 · 타차 0원',
      R12: '누유 없음',
    });
    expect(summaries(reportOf('42264483'))).toMatchObject({
      R08: '자차보험 공백 9개월',
      R12: '실린더 블록·오일팬 누유',
    });
    expect(summaries(reportOf('41623744')).R07).toBe('확인 불가 · 자료 없음');
  });

  it('is never the row title word for word (the row shows it above)', () => {
    for (const id of FIXTURE_IDS) {
      for (const item of reportOf(id).rules) {
        expect(item.summary, `${id} ${item.ruleId}`).not.toBe(item.title);
      }
    }
  });

  it('message keeps the 해요체 sentence', () => {
    const r07 = reportOf('41878112').rules.find((x) => x.ruleId === 'R07')!;
    expect(r07.message).toBe('주인이 5번 바뀌었어요');
    expect(r07.summary).toBe('소유자 변경 5회');
  });

  it('미확인 reasons', () => {
    const raw = rawOf('41878112');
    raw.recordJson = undefined;
    raw.httpStatus = { ...raw.httpStatus, recordJson: 'unauthorized' };
    expect(summaries(buildReport(raw, orchestrateMobile(raw))).R07).toBe('확인 불가 · 로그인 필요');

    const hidden = rawOf('41878112');
    const out = orchestrateMobile(hidden);
    out.report.results = out.report.results.map((r) =>
      r.ruleId === 'R01' ? { ...r, severity: 'unknown' as const } : r,
    );
    out.facts.insuranceHistoryDisclosed = { kind: 'hidden_by_dealer' };
    expect(summaries(buildReport(hidden, out)).R01).toBe('확인 불가 · 딜러 비공개');
  });

  it('R11 by price ratio', () => {
    const priced = (newPriceMan: number) => {
      const raw = rawOf('41878112');
      const cars = (raw.preloadedState as { cars: { base: { category: Record<string, unknown> } } }).cars;
      cars.base.category.newPrice = newPriceMan;
      return summaries(buildReport(raw, orchestrateMobile(raw))).R11;
    };
    expect(priced(1500)).toBe('신차가 대비 61% · 정상');
    expect(priced(5000)).toBe('신차가 대비 18% · 저가 의심');
    expect(priced(700)).toBe('신차가 대비 131% · 고가');
  });

  it('killer / warn variants from evidence', () => {
    const { out } = load('41878112');
    expect(ruleSummary(result('R04', 'killer', []), out, 'diagnosis')).toBe('엔카진단 프레임 손상');
    expect(ruleSummary(result('R04', 'killer', []), out, 'inspection')).toBe('성능점검 사고 이력 있음');
    expect(
      ruleSummary(result('R04', 'pass', [{ field: 'frameDamage', value: { hasDamage: false, parts: ['a', 'b'] } }]), out),
    ).toBe('프레임 무사고 · 외판 수리 2곳');
    expect(
      ruleSummary(result('R05', 'killer', [{ field: 'usageHistory', value: { rental: true, taxi: false, business: false } }]), out),
    ).toBe('렌트 이력 있음');
    expect(ruleSummary(result('R05', 'warn', []), out)).toBe('관용 이력 있음');
    expect(
      ruleSummary(
        result('R06', 'killer', [{ field: 'totalLoss', value: { totalLoss: 0, floodTotal: 1, floodPart: 0, robber: 0 } }]),
        out,
      ),
    ).toBe('침수전손 1회');
    expect(ruleSummary(result('R07', 'pass', [{ field: 'ownerChangeCount', value: 0 }]), out)).toBe('소유자 변경 없음');
    expect(ruleSummary(result('R09', 'warn', []), out)).toBe('수리비 미확정 있음');
    expect(ruleSummary(result('R01', 'fail', []), out)).toBe('비공개');
    expect(ruleSummary(result('R02', 'fail', []), out)).toBe('비공개');
    expect(
      ruleSummary(result('R10', 'pass', [{ field: 'damages', value: { ownDamageWon: 0, otherDamageWon: 0, domestic: true } }]), out),
    ).toBe('보험 처리 없음');
    expect(ruleSummary(result('R12', 'pass', []), out)).toBe('누유 없음');
    expect(
      ruleSummary(
        result('R12', 'warn', [
          {
            field: 'oilLeak',
            value: {
              hasLeak: true,
              items: [
                { part: '실린더 커버(로커암 커버)', status: '미세누유' },
                { part: '실린더 블록 / 오일팬', status: '누유' },
              ],
            },
          },
        ]),
        out,
      ),
    ).toBe('실린더 커버(로커암 커버) 미세누유 외 1곳');
  });
});
