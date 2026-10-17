import { describe, it, expect } from 'vitest';
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import { failed, hidden, value } from '@core/types/FieldStatus.js';
import { ALL_RULES } from '@core/rules/index.js';
import { CATEGORY_ORDER, RULE_IDS, RULE_META } from '../../src/report/rule-meta';
import {
  excludedReason,
  labelFor,
  ruleCopy,
  toneFor,
  unknownMessage,
} from '../../src/report/rule-copy';
import { FIXTURE_IDS, FORBIDDEN, load } from './fixtures';

const resultOf = (id: (typeof FIXTURE_IDS)[number], ruleId: string): RuleResult => {
  const { out } = load(id);
  const r = out.report.results.find((x) => x.ruleId === ruleId);
  if (!r) throw new Error(`${ruleId} missing in ${id}`);
  return r;
};

const result = (ruleId: string, severity: Severity, evidence: RuleResult['evidence']): RuleResult => ({
  ruleId,
  title: '',
  severity,
  message: '엔진 문구',
  evidence,
  acknowledgeable: false,
});

describe('rule meta', () => {
  it('covers R01..R12 with a category, title, why and source', () => {
    expect(RULE_IDS.length).toBe(ALL_RULES.length);
    expect(RULE_IDS).toEqual(['R01', 'R02', 'R03', 'R04', 'R05', 'R06', 'R07', 'R08', 'R09', 'R10', 'R11', 'R12']);
    expect(CATEGORY_ORDER).toEqual(['차량 상태', '이력', '사고', '가격', '투명성']);
    for (const id of RULE_IDS) {
      const m = RULE_META[id];
      expect(m, id).toBeDefined();
      expect(CATEGORY_ORDER).toContain(m!.category);
      expect(m!.title.length).toBeGreaterThan(0);
      expect(m!.why).toMatch(/요\.?$/);
      expect(m!.why).not.toMatch(FORBIDDEN);
    }
    expect(RULE_META.R04!.source).toBe('diagnosisOrInspection');
    expect(RULE_META.R07!.source).toBe('record');
    expect(RULE_META.R12!.source).toBe('inspection');
    expect(RULE_META.R11!.source).toBe('page');
    expect(RULE_META.R11!.why.startsWith('신차 가격 대비 지나치게 싸거나 비싸면')).toBe(true);
  });
});

describe('ruleCopy on fixtures', () => {
  it('41878112 R07 / R08 warn copy uses the real numbers and periods', () => {
    const { out } = load('41878112');
    const r07 = ruleCopy(resultOf('41878112', 'R07'), out.facts);
    expect(r07.message).toBe('주인이 5번 바뀌었어요');
    expect(r07.sellerQuestion).toContain('5번');

    const r08 = ruleCopy(resultOf('41878112', 'R08'), out.facts);
    expect(r08.message).toBe('자차보험 없이 운행한 기간이 2개 구간, 총 7개월 있어요');
    expect(r08.sellerQuestion).toBe('2023년 11월~2024년 1월, 2026년 4월~2026년 7월에 보험 없이 운행했나요?');
  });

  it('39339524 R04 killer and R10 warn', () => {
    const { out } = load('39339524');
    expect(ruleCopy(resultOf('39339524', 'R04'), out.facts, 'inspection')).toEqual({
      message: '성능점검에 사고 이력이 있어요',
      sellerQuestion: '성능점검 사고 부위의 수리 내역과 사진을 볼 수 있을까요?',
    });
    expect(ruleCopy(resultOf('39339524', 'R10'), out.facts).message).toBe(
      '수리비가 많이 나온 편이에요 · 내차 1,126만원 · 타차 589만원',
    );
  });

  it('42264483 R12 warn names the leaking part', () => {
    const { out } = load('42264483');
    expect(ruleCopy(resultOf('42264483', 'R12'), out.facts)).toEqual({
      message: '실린더 블록·오일팬에 누유가 있어요',
      sellerQuestion: '누유 부위는 수리했나요?',
    });
  });

  it('pass rows have no seller question', () => {
    const { out } = load('41878112');
    for (const r of out.report.results.filter((x) => x.severity === 'pass')) {
      expect(ruleCopy(r, out.facts).sellerQuestion, r.ruleId).toBeNull();
    }
    expect(ruleCopy(resultOf('41878112', 'R04'), out.facts).message).toBe('프레임 손상은 없어요');
    expect(ruleCopy(resultOf('41878112', 'R10'), out.facts).message).toBe('경미한 수준이에요 · 내차 209만원 · 타차 0원');
  });

  it('never emits emoji or 합니다체 for any fixture result', () => {
    for (const id of FIXTURE_IDS) {
      const { out } = load(id);
      for (const r of out.report.results) {
        const text =
          r.severity === 'unknown' ? unknownMessage(r, out) : ruleCopy(r, out.facts).message;
        const q = r.severity === 'unknown' ? null : ruleCopy(r, out.facts).sellerQuestion;
        expect(text, `${id} ${r.ruleId}`).not.toMatch(FORBIDDEN);
        expect(text).not.toBe('엔진 문구');
        if (q) expect(q, `${id} ${r.ruleId}`).not.toMatch(FORBIDDEN);
      }
    }
  });
});

describe('ruleCopy on synthetic results', () => {
  const facts = load('41878112').out.facts;
  const withFacts = (patch: Partial<ChecklistFacts>): ChecklistFacts => ({ ...facts, ...patch });

  it('R01/R02 fail', () => {
    expect(ruleCopy(result('R01', 'fail', []), facts)).toEqual({
      message: '딜러가 보험이력을 공개하지 않았어요',
      sellerQuestion: '보험이력을 공개해 주실 수 있나요?',
    });
    expect(ruleCopy(result('R02', 'fail', []), facts)).toEqual({
      message: '딜러가 성능점검 기록을 공개하지 않았어요',
      sellerQuestion: '성능점검 기록을 보여주실 수 있나요?',
    });
  });

  it('R04 pass lists outer panels', () => {
    const r = result('R04', 'pass', [
      { field: 'frameDamage', value: { hasDamage: false, parts: ['트렁크 리드(교환(교체))', '쿼터 패널(우)(판금/용접)'] } },
    ]);
    expect(ruleCopy(r, facts).message).toBe('프레임 손상은 없어요 · 외판 트렁크 리드, 쿼터 패널(우) 교환·수리');
  });

  it('R04 killer names the evidence source', () => {
    const r = result('R04', 'killer', [{ field: 'frameDamage', value: { hasDamage: true } }]);
    expect(ruleCopy(r, facts, 'diagnosis')).toEqual({
      message: '엔카진단에서 프레임 손상이 확인됐어요',
      sellerQuestion: '프레임 수리 내역과 사진을 볼 수 있을까요?',
    });
    expect(ruleCopy(r, facts, 'inspection')).toEqual({
      message: '성능점검에 사고 이력이 있어요',
      sellerQuestion: '성능점검 사고 부위의 수리 내역과 사진을 볼 수 있을까요?',
    });
    // Source unknown: no claim about where the evidence came from.
    expect(ruleCopy(r, facts)).toEqual({
      message: '사고 이력(프레임 손상)이 있어요',
      sellerQuestion: '프레임 수리 내역과 사진을 볼 수 있을까요?',
    });
  });

  it('R05 killer / warn', () => {
    const killer = result('R05', 'killer', [{ field: 'usageHistory', value: { rental: true, taxi: true, business: false } }]);
    expect(ruleCopy(killer, facts)).toEqual({
      message: '렌트·영업용 이력이 있어요',
      sellerQuestion: '렌트·영업용으로 얼마나 쓰였나요?',
    });
    const rental = result('R05', 'killer', [{ field: 'usageHistory', value: { rental: true, taxi: false, business: false } }]);
    expect(ruleCopy(rental, facts).sellerQuestion).toBe('렌트로 얼마나 쓰였나요?');
    const gov = result('R05', 'warn', [{ field: 'usageHistory', value: { rental: false, taxi: false, business: true } }]);
    expect(ruleCopy(gov, facts).message).toBe('관용 이력이 있어요');
  });

  it('R06 killer lists only actual hits', () => {
    const r = result('R06', 'killer', [{ field: 'totalLoss', value: { totalLoss: 1, floodTotal: 0, floodPart: 1, robber: 0 } }]);
    expect(ruleCopy(r, facts)).toEqual({
      message: '전손 1회, 침수분손 1회 이력이 있어요',
      sellerQuestion: '전손·침수 처리 후 어디서 수리했나요?',
    });
  });

  it('R07 pass variants', () => {
    expect(ruleCopy(result('R07', 'pass', [{ field: 'ownerChangeCount', value: 2 }]), facts).message).toBe(
      '소유자 변경 2회로 정상 범위예요',
    );
    expect(ruleCopy(result('R07', 'pass', [{ field: 'ownerChangeCount', value: 0 }]), facts).message).toBe(
      '소유자 변경이 없어요',
    );
  });

  it('R08 single period has no segment suffix', () => {
    const f = withFacts({
      insuranceGap: value({ hasGap: true, totalMonths: 14, periods: [{ from: '2020-01', to: '2021-02', months: 14 }] }),
    });
    const r = result('R08', 'warn', [{ field: 'insuranceGap', value: { hasGap: true, totalMonths: 14, periods: [{ from: '2020-01', to: '2021-02', months: 14 }] } }]);
    expect(ruleCopy(r, f)).toEqual({
      message: '자차보험 없이 운행한 기간이 1년 2개월 있어요',
      sellerQuestion: '2020년 1월~2021년 2월에 보험 없이 운행했나요?',
    });
  });

  it('R12 joins several leaks as "부위에 상태" with the right particle', () => {
    const leaks = (items: Array<{ part: string; status: string }>) =>
      ruleCopy(result('R12', 'warn', [{ field: 'oilLeak', value: { hasLeak: true, items } }]), facts).message;
    expect(
      leaks([
        { part: '실린더 블록 / 오일팬', status: '누유' },
        { part: '실린더 헤드', status: '미세누유' },
      ]),
    ).toBe('실린더 블록·오일팬에 누유, 실린더 헤드에 미세누유가 있어요');
    expect(leaks([{ part: '변속기', status: '누수' }])).toBe('변속기에 누수가 있어요');
    expect(leaks([{ part: '로커암 커버', status: '미세 누유 흔적' }])).toBe('로커암 커버에 미세 누유 흔적이 있어요');
  });

  it('R09, R11 warn', () => {
    expect(ruleCopy(result('R09', 'warn', [{ field: 'unconfirmed', value: true }]), facts)).toEqual({
      message: '수리비가 확정되지 않은 사고가 있어요',
      sellerQuestion: '미확정 수리비 건은 어떻게 처리됐나요?',
    });
    expect(ruleCopy(result('R11', 'warn', [{ field: 'ratio', value: 0.3 }]), facts)).toEqual({
      message: '신차 가격의 30%로 지나치게 싸요',
      sellerQuestion: '가격이 시세와 다른 이유가 있나요?',
    });
    expect(ruleCopy(result('R11', 'warn', [{ field: 'ratio', value: 1.2 }]), facts).message).toBe(
      '신차 가격의 120%로 비싼 편이에요',
    );
  });
});

describe('ruleCopy without numbers', () => {
  const blank = load('41623744').out.facts; // R05..R10 facts are not values here
  const noRatio = { ...blank, priceVsMarket: failed<{ priceWon: number; newPriceWon: number; ratio: number }>('x') };

  it('falls back to general sentences instead of 0번 / 0%', () => {
    expect(ruleCopy(result('R07', 'warn', []), blank)).toEqual({
      message: '주인이 자주 바뀌었어요',
      sellerQuestion: '주인이 자주 바뀐 이유를 알 수 있을까요?',
    });
    expect(ruleCopy(result('R07', 'pass', []), blank).message).toBe('소유자 변경이 정상 범위예요');
    expect(ruleCopy(result('R08', 'warn', []), blank)).toEqual({
      message: '자차보험 없이 운행한 기간이 있어요',
      sellerQuestion: '보험 없이 운행한 기간이 있었나요?',
    });
    expect(ruleCopy(result('R10', 'warn', []), blank).message).toBe('수리비가 많이 나온 편이에요');
    expect(ruleCopy(result('R10', 'pass', []), blank).message).toBe('경미한 수준이에요');
    expect(ruleCopy(result('R11', 'warn', []), noRatio)).toEqual({
      message: '신차 가격과 차이가 커요',
      sellerQuestion: '가격이 시세와 다른 이유가 있나요?',
    });
    expect(ruleCopy(result('R11', 'pass', []), noRatio).message).toBe('가격이 적정한 편이에요');
  });
});

describe('unknownMessage', () => {
  it('41623744: record 404 -> no insurance history, R04 -> no frame source', () => {
    const { out } = load('41623744');
    const r07 = out.report.results.find((r) => r.ruleId === 'R07')!;
    expect(unknownMessage(r07, out)).toBe('엔카에 이 차의 보험 이력이 없어요');
    const r04 = out.report.results.find((r) => r.ruleId === 'R04')!;
    expect(unknownMessage(r04, out)).toBe('프레임 상태를 확인할 자료가 없어요');
  });

  it('maps API failure reasons by rule source', () => {
    const { out } = load('41623744');
    const r07 = out.report.results.find((r) => r.ruleId === 'R07')!;
    const withRecord = (reason: string) => ({
      ...out,
      parsed: { ...out.parsed, raw: { ...out.parsed.raw, recordApi: failed<never>(reason) } },
    });
    expect(unknownMessage(r07, withRecord('login_required'))).toBe('엔카에 로그인하면 확인할 수 있어요');
    expect(unknownMessage(r07, withRecord('api_fetch_error'))).toBe('엔카에서 정보를 받지 못했어요');
    expect(unknownMessage(r07, withRecord('not_fetched'))).toBe('엔카에서 정보를 받지 못했어요');
    expect(unknownMessage(r07, withRecord('something_else'))).toBe('정보를 확인하지 못했어요');
  });

  it('falls back to facts for page rules', () => {
    const { out } = load('41878112');
    const r01 = result('R01', 'unknown', []);
    const hiddenOut = { ...out, facts: { ...out.facts, insuranceHistoryDisclosed: hidden<boolean>() } };
    expect(unknownMessage(r01, hiddenOut)).toBe('딜러가 공개하지 않았어요');
    const failedOut = {
      ...out,
      facts: { ...out.facts, insuranceHistoryDisclosed: failed<boolean>('detail_flags_unavailable') },
    };
    expect(unknownMessage(r01, failedOut)).toBe('정보를 확인하지 못했어요');
  });
});

describe('excludedReason / labelFor / toneFor', () => {
  it('excluded reasons', () => {
    expect(excludedReason('R03')).toBe('엔카진단을 받지 않은 차예요(감점 없음)');
    expect(excludedReason('R11')).toBe('신차 가격 정보가 없어 평가하지 않았어요');
    expect(excludedReason('R12')).toBe('성능점검 기록이 없어 누유를 평가하지 않았어요');
  });

  it('label and tone table', () => {
    const table: Array<[Severity, string, string]> = [
      ['killer', '위험', 'danger'],
      ['fail', '비공개', 'danger'],
      ['warn', '주의', 'warning'],
      ['pass', '통과', 'success'],
      ['unknown', '미확인', 'neutral'],
    ];
    for (const [s, label, tone] of table) {
      expect(labelFor(s)).toBe(label);
      expect(toneFor(s)).toBe(tone);
    }
  });
});
