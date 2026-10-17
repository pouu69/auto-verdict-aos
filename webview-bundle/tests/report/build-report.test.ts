import { describe, it, expect } from 'vitest';
import type { EvaluationReport, RuleItem } from '../../src/report/types';
import { SCHEMA_VERSION } from '../../src/report/types';
import { buildReport } from '../../src/report/build-report';
import { buildCar } from '../../src/report/car';
import { buildShareText } from '../../src/report/share-text';
import { CATEGORY_ORDER, RULE_IDS } from '../../src/report/rule-meta';
import { FIXTURE_IDS, FORBIDDEN, load, rawOf } from './fixtures';

const build = (id: (typeof FIXTURE_IDS)[number]) => {
  const { raw, out } = load(id);
  return { raw, out, report: buildReport(raw, out) };
};

const ISSUE_RANK: Record<string, number> = { 위험: 0, 비공개: 1, 주의: 2, 미확인: 3 };
const ruleNo = (i: RuleItem) => Number(i.ruleId.slice(1));

/** Every string anywhere in the report. */
const strings = (v: unknown): string[] => {
  if (typeof v === 'string') return [v];
  if (Array.isArray(v)) return v.flatMap(strings);
  if (v && typeof v === 'object') return Object.values(v).flatMap(strings);
  return [];
};

describe('buildReport on fixtures', () => {
  for (const id of FIXTURE_IDS) {
    it(`${id}: invariants`, () => {
      const { raw, out, report } = build(id);
      expect(report.schemaVersion).toBe(SCHEMA_VERSION);
      expect(report.carId).toBe(raw.carId);
      expect(report.url).toBe(raw.url);
      expect(report.verdict).toBe(out.report.verdict);
      expect(report.score).toBe(out.report.score);

      const c = report.counts;
      expect(c.danger + c.hidden + c.warning + c.pass + c.unknown).toBe(out.report.results.length);
      expect(c.excluded + out.report.results.length).toBe(12);
      expect(report.excluded.map((e) => e.ruleId)).toEqual(
        RULE_IDS.filter((r) => !out.report.results.some((x) => x.ruleId === r)),
      );

      // rules: category order, then rule number.
      const cat = (i: RuleItem) => CATEGORY_ORDER.indexOf(i.category);
      for (let k = 1; k < report.rules.length; k++) {
        const a = report.rules[k - 1]!;
        const b = report.rules[k]!;
        expect(cat(a) < cat(b) || (cat(a) === cat(b) && ruleNo(a) < ruleNo(b)), `${a.ruleId} before ${b.ruleId}`).toBe(true);
      }
      // pass rows carry no why/question.
      for (const rule of report.rules.filter((x) => x.tone === 'success')) {
        expect(rule.why).toBeNull();
        expect(rule.sellerQuestion).toBeNull();
      }

      // issues: non-success subset of rules, sorted by label rank then rule number, why always set.
      expect(report.issues.every((i) => report.rules.some((r) => JSON.stringify(r) === JSON.stringify(i)))).toBe(true);
      expect(report.issues.length).toBe(report.rules.filter((r) => r.tone !== 'success').length);
      for (let k = 1; k < report.issues.length; k++) {
        const a = report.issues[k - 1]!;
        const b = report.issues[k]!;
        const ra = ISSUE_RANK[a.label]!;
        const rb = ISSUE_RANK[b.label]!;
        expect(ra < rb || (ra === rb && ruleNo(a) < ruleNo(b))).toBe(true);
      }
      for (const i of report.issues) {
        expect(i.why).toBeTruthy();
        if (i.tone === 'neutral') expect(i.sellerQuestion).toBeNull();
        else expect(i.sellerQuestion).toBeTruthy();
      }

      for (const s of strings(report)) expect(s, `${id}: ${s}`).not.toMatch(FORBIDDEN);
      expect(JSON.parse(JSON.stringify(report))).toEqual(report);
    });
  }

  it('41878112: CAUTION headline, issues and share text', () => {
    const { report } = build('41878112');
    expect(report.headline).toBe('사기 전에 2가지는 꼭 확인하세요');
    expect(report.issues.map((i) => i.ruleId)).toEqual(['R07', 'R08']);
    expect(report.counts).toEqual({ danger: 0, hidden: 0, warning: 2, pass: 9, unknown: 0, excluded: 1 });
    expect(report.excluded).toEqual([
      { ruleId: 'R11', title: '가격 적정성', reason: '신차 가격 정보가 없어 평가하지 않았어요' },
    ]);
    expect(report.car).toEqual({
      title: '2015 BMW 3시리즈 (F30)',
      year: 2015,
      mileageKm: 119559,
      priceWon: 9_200_000,
      fuel: '디젤',
      summary: '2015년 · 12만km · 디젤 · 920만원',
    });
    expect(report.shareText.summary).toBe(
      [
        '2015 BMW 3시리즈 (F30)',
        '판정: 주의 · 89점',
        '사기 전에 2가지는 꼭 확인하세요',
        '- 소유자 변경: 주인이 5번 바뀌었어요',
        '- 자차보험 공백: 자차보험 없이 운행한 기간이 7개월 있어요 (2개 구간)',
        report.url,
        'AutoVerdict로 분석',
      ].join('\n'),
    );
    expect(report.shareText.sellerQuestions).toBe(
      [
        '1. 주인이 5번 바뀐 이유를 알 수 있을까요?',
        '2. 2023년 11월~2024년 1월, 2026년 4월~2026년 7월에 보험 없이 운행했나요?',
        report.url,
        'AutoVerdict로 분석',
      ].join('\n'),
    );
  });

  it('39339524: NEVER headline from R04', () => {
    const { report } = build('39339524');
    expect(report.headline.startsWith('사고(프레임 손상) 이력이 있어')).toBe(true);
    expect(report.issues.map((i) => [i.ruleId, i.label])).toEqual([
      ['R04', '위험'],
      ['R10', '주의'],
    ]);
    expect(report.rules[0]!.ruleId).toBe('R03');
    expect(report.repairs?.panels).toHaveLength(2);
    expect(report.car.summary).toBe('2012년 · 11.3만km · 디젤 · 599만원');
  });

  it('41623744: UNKNOWN with nothing collected', () => {
    const { report } = build('41623744');
    expect(report.verdict).toBe('UNKNOWN');
    expect(report.headline).toBe('확인하지 못한 항목이 7개 있어요');
    expect(report.repairs).toBeNull();
    expect(report.timeline).toEqual([]);
    expect(report.dataGaps).toEqual([]);
    expect(report.facts).toEqual([]);
    expect(report.issues.every((i) => i.label === '미확인' && i.sellerQuestion === null)).toBe(true);
    expect(report.shareText.sellerQuestions).toBeNull();
    expect(report.issues.find((i) => i.ruleId === 'R07')!.message).toBe('엔카에 이 차의 보험 이력이 없어요');
  });
});

describe('buildCar', () => {
  it('missing base -> placeholder title, nulls', () => {
    const raw = rawOf('41878112');
    raw.preloadedState = {};
    expect(buildCar(raw)).toEqual({
      title: '차량 정보 없음',
      year: null,
      mileageKm: null,
      priceWon: null,
      fuel: null,
      summary: '',
    });
  });

  it('short mileage and partial info', () => {
    const raw = rawOf('41878112');
    const base = (raw.preloadedState as { cars: { base: Record<string, any> } }).cars.base;
    base.spec.mileage = 9500;
    delete base.spec.fuelName;
    base.advertisement.price = 12900;
    expect(buildCar(raw).summary).toBe('2015년 · 9,500km · 12,900만원');
    base.spec.mileage = 98_000;
    expect(buildCar(raw).summary).toBe('2015년 · 9.8만km · 12,900만원');
  });
});

describe('share text without issues', () => {
  it('omits issue lines and seller questions', () => {
    const { report } = build('41878112');
    const clean: Omit<EvaluationReport, 'shareText'> = { ...report, issues: [] };
    delete (clean as Partial<EvaluationReport>).shareText;
    const share = buildShareText(clean);
    expect(share.sellerQuestions).toBeNull();
    expect(share.summary.split('\n')).toHaveLength(5);
  });
});
