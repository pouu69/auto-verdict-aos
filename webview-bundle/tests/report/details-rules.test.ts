import { describe, it, expect } from 'vitest';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import { value } from '@core/types/FieldStatus.js';
import { r07, r10, r11 } from '@core/rules/index.js';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildReport } from '../../src/report/build-report';
import { CRITERIA } from '../../src/report/details/rules';
import { RULE_META } from '../../src/report/rule-meta';
import type { DetailBlock, DetailSheet, EvaluationReport } from '../../src/report/types';
import { FIXTURE_IDS, load, rawOf, type FixtureId } from './fixtures';

const reportOf = (id: FixtureId): EvaluationReport => {
  const { raw, out } = load(id);
  return buildReport(raw, out);
};

const detailOf = (report: EvaluationReport, ruleId: string): DetailSheet => {
  const d = report.rules.find((r) => r.ruleId === ruleId)?.detail;
  if (!d) throw new Error(`${ruleId} has no detail`);
  return d;
};

const blockOf = (sheet: DetailSheet, title: string): DetailBlock => {
  const b = sheet.blocks.find((x) => x.title === title);
  if (!b) throw new Error(`no block ${title}`);
  return b;
};

/** "1,005,260원" -> 1005260. */
const wonOf = (s: string): number => Number(s.replace(/[^\d]/g, ''));
/** "3개월" / "1년 2개월" -> months. */
const monthsOf = (s: string): number => {
  const m = s.match(/^(?:(\d+)년)?\s*(?:(\d+)개월)?$/);
  return Number(m?.[1] ?? 0) * 12 + Number(m?.[2] ?? 0);
};

describe('rule detail sheets', () => {
  it('every rule of every fixture has a sheet: evidence, 판정 기준, 데이터 출처', () => {
    for (const id of FIXTURE_IDS) {
      const report = reportOf(id);
      for (const item of report.rules) {
        const d = item.detail;
        expect(d, `${id} ${item.ruleId}`).not.toBeNull();
        expect(d!.title).toBe(RULE_META[item.ruleId]!.title);
        const titles = d!.blocks.map((b) => b.title);
        expect(titles).toContain('판정 기준');
        if (item.label === '미확인') {
          expect(titles[0], `${id} ${item.ruleId}`).toBe('확인하지 못한 이유');
          expect(titles).not.toContain('데이터 출처');
        } else {
          expect(titles[titles.length - 1], `${id} ${item.ruleId}`).toBe('데이터 출처');
          expect(titles.indexOf('판정 기준')).toBe(titles.length - 2);
          // ① evidence comes first.
          expect(titles.length, `${id} ${item.ruleId}`).toBeGreaterThan(2);
        }
      }
    }
  });

  it('never repeats the result sentence, why or seller question; subtitle only for R04', () => {
    for (const id of FIXTURE_IDS) {
      const report = reportOf(id);
      for (const item of report.rules) {
        const d = item.detail!;
        const texts = [d.title, d.subtitle ?? '', ...d.blocks.flatMap((b) => [b.title ?? '', b.note ?? '', ...b.rows.flatMap((r) => [r.label, r.value])])];
        for (const repeated of [item.message, item.why, item.sellerQuestion]) {
          if (repeated) expect(texts, `${id} ${item.ruleId}`).not.toContain(repeated);
        }
        if (item.ruleId !== 'R04' || item.label === '미확인') expect(d.subtitle, `${id} ${item.ruleId}`).toBeNull();
      }
    }
    expect(detailOf(reportOf('39339524'), 'R04').subtitle).toBe('근거: 성능점검');
    expect(detailOf(reportOf('41878112'), 'R04').subtitle).toBe('근거: 엔카진단');
  });

  it('issues carry the same sheet as their rule row', () => {
    const report = reportOf('41878112');
    for (const issue of report.issues) {
      expect(issue.detail).toEqual(report.rules.find((r) => r.ruleId === issue.ruleId)!.detail);
    }
  });

  it('39339524 R10: all 9 claims with exact amounts, sides summing to myAccidentCost/otherAccidentCost', () => {
    const { raw } = load('39339524');
    const rec = raw.recordJson as { myAccidentCost: number; otherAccidentCost: number };
    const d = detailOf(reportOf('39339524'), 'R10');
    expect(blockOf(d, '보험 처리 합계').rows).toEqual([
      { label: '내차 피해', value: '7건 · 11,258,480원', tone: 'warning' },
      { label: '타차 가해', value: '2건 · 5,886,670원', tone: 'warning' },
      { label: '기준 금액', value: '수입 400만원', tone: null },
    ]);
    const claims = d.blocks.filter((b) => /^\d{4}-\d{2}-\d{2} · (내차 피해|타차 가해)$/.test(b.title ?? ''));
    expect(claims).toHaveLength(9);
    // Newest first.
    const dates = claims.map((b) => b.title!.slice(0, 10));
    expect(dates).toEqual([...dates].sort().reverse());
    const total = (side: string) =>
      claims
        .filter((b) => b.title!.endsWith(side))
        .reduce((s, b) => s + wonOf(b.rows.find((r) => r.label === '수리비 합계')!.value), 0);
    expect(total('내차 피해')).toBe(rec.myAccidentCost);
    expect(total('타차 가해')).toBe(rec.otherAccidentCost);
    expect(claims[0]!.rows).toEqual([
      { label: '보험금', value: '1,005,260원', tone: null },
      { label: '부품비', value: '195,120원', tone: null },
      { label: '공임', value: '711,370원', tone: null },
      { label: '도장비', value: '235,570원', tone: null },
      { label: '수리비 합계', value: '1,142,060원', tone: null },
    ]);
  });

  it('39339524 R04: 성능점검 basis, every panel, diagnosis parts in Korean', () => {
    const d = detailOf(reportOf('39339524'), 'R04');
    expect(d.blocks.map((b) => b.title)).toEqual([
      '판정 근거',
      '성능점검',
      '외판·골격 부위',
      '엔카진단 결과',
      '진단 부위',
      '판정 기준',
      '데이터 출처',
    ]);
    expect(blockOf(d, '판정 근거').note).toMatch(/성능점검을 근거로/);
    expect(blockOf(d, '성능점검').rows).toEqual([
      { label: '사고 이력', value: '있음', tone: 'danger' },
      { label: '단순수리', value: '있음', tone: 'warning' },
    ]);
    expect(blockOf(d, '외판·골격 부위').rows).toEqual([
      { label: '쿼터 패널(우)', value: '2랭크 판금·용접 · 용접·절단', tone: 'warning' },
      { label: '트렁크 리드', value: '1랭크 교환', tone: 'warning' },
    ]);
    expect(blockOf(d, '엔카진단 결과').rows).toContainEqual({ label: '종합 판정', value: '외부패널 단순교환 차량', tone: null });
    const parts = blockOf(d, '진단 부위').rows;
    expect(parts).toHaveLength(8);
    expect(parts).toContainEqual({ label: '프론트 도어(좌)', value: '정상', tone: 'success' });
    expect(parts).toContainEqual({ label: '트렁크 리드', value: '교환', tone: 'warning' });
    expect(parts.map((p) => p.label)).not.toContain('기타 부위');
  });

  it('41878112 R07: all 5 change dates, newest first', () => {
    const { raw } = load('41878112');
    const d = detailOf(reportOf('41878112'), 'R07');
    expect(blockOf(d, '소유자 변경').rows).toEqual([{ label: '변경 횟수', value: '5회', tone: 'warning' }]);
    const rows = blockOf(d, '변경 날짜').rows;
    const expected = [...(raw.recordJson as { ownerChanges: string[] }).ownerChanges].sort().reverse();
    expect(rows.map((r) => r.value)).toEqual(expected);
    expect(rows.map((r) => r.label)).toEqual(['5번째 변경', '4번째 변경', '3번째 변경', '2번째 변경', '1번째 변경']);
  });

  it('41878112 R08: both periods, months summing to the engine total', () => {
    const { out } = load('41878112');
    const gap = out.facts.insuranceGap;
    if (gap.kind !== 'value') throw new Error('no gap');
    const rows = blockOf(detailOf(reportOf('41878112'), 'R08'), '보험 미가입 기간').rows;
    expect(rows).toEqual([
      { label: '2023년 11월~2024년 1월', value: '3개월', tone: 'warning' },
      { label: '2026년 4월~2026년 7월', value: '4개월', tone: 'warning' },
      { label: '합계', value: '7개월', tone: 'warning' },
    ]);
    const periods = rows.slice(0, -1).reduce((s, r) => s + monthsOf(r.value), 0);
    expect(periods).toBe(gap.value.totalMonths);
    expect(monthsOf(rows[rows.length - 1]!.value)).toBe(gap.value.totalMonths);
  });

  it('42264483 R12: the leaking part with its group, danger tone', () => {
    const rows = blockOf(detailOf(reportOf('42264483'), 'R12'), '누유 항목').rows;
    expect(rows).toEqual([{ label: '오일누유 · 실린더 블록 / 오일팬', value: '누유', tone: 'danger' }]);
  });
});

/** 41878112 with a new-car price, so R11 is evaluated (no fixture carries one). */
const pricedReport = (newPriceMan: number): EvaluationReport => {
  const raw = rawOf('41878112');
  const cars = (raw.preloadedState as { cars: { base: { category: Record<string, unknown> } } }).cars;
  cars.base.category.newPrice = newPriceMan;
  return buildReport(raw, orchestrateMobile(raw));
};

describe('rule detail evidence rows', () => {
  it('R05/R06/R09/R11', () => {
    const r = reportOf('41878112');
    expect(blockOf(detailOf(r, 'R05'), '용도 이력').rows.map((x) => [x.label, x.value])).toEqual([
      ['용도', '자가용'],
      ['렌트 이력', '없음'],
      ['영업용 이력', '없음'],
      ['관용 이력', '없음'],
      ['용도 변경', '없음'],
    ]);
    expect(blockOf(detailOf(r, 'R06'), '전손·침수·도난').rows.map((x) => x.value)).toEqual(['없음', '없음', '없음', '없음']);
    expect(blockOf(detailOf(r, 'R09'), '수리비 확정').note).toBe('보험 처리한 사고 2건 모두 수리비가 기록돼 있어요');
    // 920만원 against a 5,000만원 new car: 18%, below the 45% bound.
    expect(blockOf(detailOf(pricedReport(5000), 'R11'), '가격 비교').rows).toEqual([
      { label: '신차 가격', value: '50,000,000원', tone: null },
      { label: '판매 가격', value: '9,200,000원', tone: null },
      { label: '신차 가격 대비', value: '18%', tone: 'warning' },
    ]);
    expect(blockOf(detailOf(pricedReport(1500), 'R11'), '가격 비교').rows[2]).toEqual({
      label: '신차 가격 대비',
      value: '61%',
      tone: 'success',
    });
  });

});

describe('rule detail edge cases', () => {
  it('R06 with a loss on record lists counts and dates', () => {
    const raw = rawOf('41878112');
    Object.assign(raw.recordJson as object, { floodPartLossCnt: 1, floodDate: '2020-08-10' });
    const report = buildReport(raw, orchestrateMobile(raw));
    const rows = blockOf(detailOf(report, 'R06'), '전손·침수·도난').rows;
    expect(rows).toContainEqual({ label: '침수분손', value: '1회', tone: 'danger' });
    expect(rows).toContainEqual({ label: '침수 날짜', value: '2020-08-10', tone: null });
  });

  it('미확인: the reason block lists each source status', () => {
    const r = reportOf('41623744');
    expect(blockOf(detailOf(r, 'R07'), '확인하지 못한 이유').rows).toEqual([
      { label: '엔카 보험이력', value: '자료 없음', tone: 'neutral' },
    ]);
    const raw = rawOf('41878112');
    raw.recordJson = undefined;
    raw.httpStatus = { ...raw.httpStatus, recordJson: 'unauthorized' };
    const login = buildReport(raw, orchestrateMobile(raw));
    expect(blockOf(detailOf(login, 'R08'), '확인하지 못한 이유').rows).toEqual([
      { label: '엔카 보험이력', value: '로그인 필요', tone: 'neutral' },
    ]);
  });
});

describe('판정 기준 restates the engine thresholds', () => {
  // Rules read a single field each, so a partial facts object is enough.
  const facts = (f: Partial<ChecklistFacts>): ChecklistFacts => f as ChecklistFacts;

  it('R07 owner changes', () => {
    expect(r07(facts({ ownerChangeCount: value(CRITERIA.ownerWarnAbove) }))?.severity).toBe('pass');
    expect(r07(facts({ ownerChangeCount: value(CRITERIA.ownerWarnAbove + 1) }))?.severity).toBe('warn');
  });

  it('R10 mild ceilings', () => {
    const at = (won: number, domestic: boolean) =>
      r10(facts({ minorAccidents: value({ ownDamageWon: won, otherDamageWon: 0, domestic }) }))?.severity;
    expect(at(CRITERIA.mildCeilingDomestic, true)).toBe('pass');
    expect(at(CRITERIA.mildCeilingDomestic + 1, true)).toBe('warn');
    expect(at(CRITERIA.mildCeilingImport, false)).toBe('pass');
    expect(at(CRITERIA.mildCeilingImport + 1, false)).toBe('warn');
  });

  it('R11 price ratio bounds', () => {
    const at = (ratio: number) => r11(facts({ priceVsMarket: value({ priceWon: 1, newPriceWon: 1, ratio }) }))?.severity;
    expect(at(CRITERIA.lowRatio)).toBe('pass');
    expect(at(CRITERIA.lowRatio - 0.001)).toBe('warn');
    expect(at(CRITERIA.highRatio)).toBe('pass');
    expect(at(CRITERIA.highRatio + 0.001)).toBe('warn');
  });

  it('criteria rows read as words', () => {
    const r = pricedReport(1500);
    expect(blockOf(detailOf(r, 'R07'), '판정 기준').rows.map((x) => x.value)).toEqual(['2회 이하', '3회 이상']);
    expect(blockOf(detailOf(r, 'R11'), '판정 기준').rows.map((x) => x.value)).toEqual([
      '신차 가격의 45% 미만 · 저가 의심',
      '신차 가격의 115% 초과 · 고가',
      '신차 가격의 45%~115%',
      '신차 가격 정보 없음',
    ]);
  });
});
