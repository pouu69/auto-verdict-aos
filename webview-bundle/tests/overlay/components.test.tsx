import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ScoreCard } from '../../src/overlay/ScoreCard';
import { SummaryRow } from '../../src/overlay/SummaryRow';
import { CategoryAccordion, groupByCategory } from '../../src/overlay/CategoryAccordion';
import { RuleLine } from '../../src/overlay/RuleLine';
import { RuleDetailSheet } from '../../src/overlay/RuleDetailSheet';
import { VerdictDetailSheet } from '../../src/overlay/VerdictDetailSheet';
import type { RuleReport, RuleResult, Verdict } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';

// --- ScoreCard ---

const makeBase = (overrides?: Partial<EncarCarBase>): EncarCarBase => ({
  category: { manufacturerName: '현대', modelName: '투싼', yearMonth: '202201', domestic: true },
  advertisement: { price: 2500, preVerified: false, trust: [] },
  spec: { mileage: 35000 },
  ...overrides,
});

describe('ScoreCard', () => {
  it('renders score number', () => {
    render(<ScoreCard score={85} verdict="OK" carBase={makeBase()} onInfo={() => {}} />);
    expect(screen.getByText('85')).toBeInTheDocument();
  });

  it('renders verdict badge', () => {
    render(<ScoreCard score={30} verdict="NEVER" carBase={makeBase()} onInfo={() => {}} />);
    expect(screen.getByText('NEVER')).toBeInTheDocument();
  });

  it('renders vehicle info from carBase', () => {
    render(<ScoreCard score={80} verdict="OK" carBase={makeBase()} onInfo={() => {}} />);
    expect(screen.getByText(/현대 투싼/)).toBeInTheDocument();
  });

  it('handles null carBase gracefully', () => {
    render(<ScoreCard score={50} verdict="UNKNOWN" carBase={null} onInfo={() => {}} />);
    expect(screen.getByText('차량 정보 없음')).toBeInTheDocument();
    expect(screen.getByText('50')).toBeInTheDocument();
  });

  it('formats price over 10000 as 억', () => {
    const base = makeBase({
      advertisement: { price: 15000, preVerified: false, trust: [] },
    });
    render(<ScoreCard score={70} verdict="CAUTION" carBase={base} onInfo={() => {}} />);
    expect(screen.getByText(/1.5억/)).toBeInTheDocument();
  });

  it('formats price under 10000 as 만원', () => {
    render(<ScoreCard score={70} verdict="CAUTION" carBase={makeBase()} onInfo={() => {}} />);
    expect(screen.getByText(/2500만원/)).toBeInTheDocument();
  });

  it('renders all four verdict types', () => {
    const verdicts: Verdict[] = ['OK', 'CAUTION', 'NEVER', 'UNKNOWN'];
    verdicts.forEach((v) => {
      const { unmount } = render(<ScoreCard score={50} verdict={v} carBase={null} onInfo={() => {}} />);
      expect(screen.getByText(v)).toBeInTheDocument();
      unmount();
    });
  });

  it('calls onInfo when the verdict badge is tapped', () => {
    let opened = false;
    render(<ScoreCard score={30} verdict="NEVER" carBase={null} onInfo={() => { opened = true; }} />);
    fireEvent.click(screen.getByLabelText(/등급 설명 보기/));
    expect(opened).toBe(true);
  });
});

// --- VerdictDetailSheet ---

describe('VerdictDetailSheet', () => {
  const makeReport = (overrides: Partial<RuleReport>): RuleReport => ({
    verdict: 'OK',
    score: 90,
    results: [],
    killers: [],
    warns: [],
    ...overrides,
  });

  it('renders nothing when report is null', () => {
    const { container } = render(<VerdictDetailSheet report={null} onClose={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('lists killer titles as the reason for a NEVER verdict', () => {
    const killer: RuleResult = {
      ruleId: 'R04', title: '프레임/외판', severity: 'killer',
      message: '🚨 프레임 손상', evidence: [], acknowledgeable: true,
    };
    render(<VerdictDetailSheet report={makeReport({ verdict: 'NEVER', killers: [killer], results: [killer] })} onClose={() => {}} />);
    expect(screen.getByText(/프레임\/외판/)).toBeInTheDocument();
    expect(screen.getByText('이 등급은 무슨 뜻인가요?')).toBeInTheDocument();
  });

  it('shows the warn count as the reason for a CAUTION verdict', () => {
    const warn: RuleResult = {
      ruleId: 'R07', title: '소유자 변경', severity: 'warn',
      message: '소유자 변경 3회 (잦은 편)', evidence: [], acknowledgeable: false,
    };
    render(<VerdictDetailSheet report={makeReport({ verdict: 'CAUTION', warns: [warn, warn], results: [warn, warn] })} onClose={() => {}} />);
    expect(screen.getByText(/주의 항목이 2개 있어요/)).toBeInTheDocument();
  });
});

// --- SummaryRow ---

const makeResults = (severities: string[]): RuleResult[] =>
  severities.map((sev, i) => ({
    ruleId: `R${String(i + 1).padStart(2, '0')}`,
    title: `Rule ${i + 1}`,
    severity: sev as RuleResult['severity'],
    message: 'msg',
    evidence: [],
    acknowledgeable: false,
  }));

describe('SummaryRow', () => {
  it('counts danger (killer + fail)', () => {
    const results = makeResults(['killer', 'fail', 'pass', 'warn']);
    render(<SummaryRow results={results} />);
    const dangerCells = screen.getAllByText('2');
    expect(dangerCells.length).toBeGreaterThanOrEqual(1);
  });

  it('counts pass correctly', () => {
    const results = makeResults(['pass', 'pass', 'pass', 'warn']);
    render(<SummaryRow results={results} />);
    const passCells = screen.getAllByText('3');
    expect(passCells.length).toBeGreaterThanOrEqual(1);
  });

  it('all zeros for empty results', () => {
    render(<SummaryRow results={[]} />);
    const zeros = screen.getAllByText('0');
    expect(zeros.length).toBe(4);
  });

  it('renders all four labels', () => {
    render(<SummaryRow results={[]} />);
    expect(screen.getByText('위험')).toBeInTheDocument();
    expect(screen.getByText('주의')).toBeInTheDocument();
    expect(screen.getByText('통과')).toBeInTheDocument();
    expect(screen.getByText('미확인')).toBeInTheDocument();
  });
});

// --- RuleLine ---

describe('RuleLine', () => {
  const makeRule = (severity: RuleResult['severity'], ruleId = 'R01'): RuleResult => ({
    ruleId,
    title: '보험이력 공개',
    severity,
    message: '딜러가 보험이력을 공개했습니다',
    evidence: [],
    acknowledgeable: false,
  });

  it('renders rule title and message', () => {
    render(<RuleLine result={makeRule('pass')} onInfo={() => {}} />);
    expect(screen.getByText(/보험이력 공개/)).toBeInTheDocument();
    expect(screen.getByText(/딜러가 보험이력을 공개했습니다/)).toBeInTheDocument();
  });

  it('shows 통과 badge for pass severity', () => {
    render(<RuleLine result={makeRule('pass')} onInfo={() => {}} />);
    expect(screen.getByText('통과')).toBeInTheDocument();
  });

  it('shows 위험 badge for killer severity', () => {
    render(<RuleLine result={makeRule('killer')} onInfo={() => {}} />);
    expect(screen.getByText('위험')).toBeInTheDocument();
  });

  it('shows 주의 badge for warn severity', () => {
    render(<RuleLine result={makeRule('warn')} onInfo={() => {}} />);
    expect(screen.getByText('주의')).toBeInTheDocument();
  });

  it('shows 미확인 badge for unknown severity', () => {
    render(<RuleLine result={makeRule('unknown')} onInfo={() => {}} />);
    expect(screen.getByText('미확인')).toBeInTheDocument();
  });

  it('extracts rule number from ruleId', () => {
    render(<RuleLine result={makeRule('pass', 'R05')} onInfo={() => {}} />);
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('extracts double-digit rule number', () => {
    render(<RuleLine result={makeRule('pass', 'R12')} onInfo={() => {}} />);
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('calls onInfo with the result when the row is tapped', () => {
    let received: RuleResult | null = null;
    const rule = makeRule('warn', 'R07');
    render(<RuleLine result={rule} onInfo={(r) => { received = r; }} />);
    fireEvent.click(screen.getByText(/보험이력 공개/));
    expect(received).toBe(rule);
  });
});

// --- RuleDetailSheet ---

describe('RuleDetailSheet', () => {
  const rule: RuleResult = {
    ruleId: 'R07',
    title: '소유자 변경',
    severity: 'warn',
    message: '소유자 변경 3회 (잦은 편)',
    evidence: [],
    acknowledgeable: false,
  };

  it('renders nothing when result is null', () => {
    const { container } = render(<RuleDetailSheet result={null} onClose={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows the finding message and the "왜 중요한가요?" explanation', () => {
    render(<RuleDetailSheet result={rule} onClose={() => {}} />);
    expect(screen.getByText('소유자 변경 3회 (잦은 편)')).toBeInTheDocument();
    expect(screen.getByText('왜 중요한가요?')).toBeInTheDocument();
    expect(screen.getByText(/자주 바뀐 차는/)).toBeInTheDocument();
  });

  it('calls onClose when 닫기 is tapped', () => {
    let closed = false;
    render(<RuleDetailSheet result={rule} onClose={() => { closed = true; }} />);
    fireEvent.click(screen.getByText('닫기'));
    expect(closed).toBe(true);
  });
});

// --- groupByCategory ---

describe('groupByCategory', () => {
  it('groups rules by their category from RULE_META', () => {
    const results = makeResults(['pass', 'pass']).map((r, i) => ({
      ...r,
      ruleId: i === 0 ? 'R01' : 'R02',
    }));

    const groups = groupByCategory(results);
    const transparency = groups.find((g) => g.category === '투명성');
    expect(transparency).toBeDefined();
    expect(transparency?.results.length).toBe(2);
  });

  it('separates rules into correct categories', () => {
    const results: RuleResult[] = [
      { ruleId: 'R01', title: 't', severity: 'pass', message: 'm', evidence: [], acknowledgeable: false },
      { ruleId: 'R04', title: 't', severity: 'pass', message: 'm', evidence: [], acknowledgeable: false },
      { ruleId: 'R11', title: 't', severity: 'warn', message: 'm', evidence: [], acknowledgeable: false },
    ];

    const groups = groupByCategory(results);
    expect(groups.find((g) => g.category === '투명성')?.results.length).toBe(1);
    expect(groups.find((g) => g.category === '차량 상태')?.results.length).toBe(1);
    expect(groups.find((g) => g.category === '가격')?.results.length).toBe(1);
  });

  it('drops rules with unknown ruleId', () => {
    const results: RuleResult[] = [
      { ruleId: 'R99', title: 'unknown', severity: 'pass', message: 'm', evidence: [], acknowledgeable: false },
    ];

    const groups = groupByCategory(results);
    const total = groups.reduce((sum, g) => sum + g.results.length, 0);
    expect(total).toBe(0);
  });

  it('returns empty array for empty input', () => {
    expect(groupByCategory([])).toEqual([]);
  });
});

// --- CategoryAccordion ---

describe('CategoryAccordion', () => {
  it('auto-opens when results contain danger', () => {
    const results = makeResults(['killer']).map((r) => ({ ...r, ruleId: 'R05' }));
    render(<CategoryAccordion category="이력" results={results} onInfo={() => {}} />);
    expect(screen.getByText(/딜러가|msg/)).toBeInTheDocument();
  });

  it('auto-opens when results contain warning', () => {
    const results = makeResults(['warn']).map((r) => ({ ...r, ruleId: 'R07' }));
    render(<CategoryAccordion category="이력" results={results} onInfo={() => {}} />);
    expect(screen.getByText('msg')).toBeInTheDocument();
  });

  it('starts collapsed when all pass', () => {
    const results = makeResults(['pass']).map((r) => ({ ...r, ruleId: 'R01' }));
    render(<CategoryAccordion category="투명성" results={results} onInfo={() => {}} />);
    expect(screen.queryByText('msg')).not.toBeInTheDocument();
  });

  it('shows pass count ratio', () => {
    const results = makeResults(['pass', 'pass', 'warn']).map((r, i) => ({
      ...r,
      ruleId: ['R05', 'R06', 'R07'][i] ?? 'R05',
    }));
    render(<CategoryAccordion category="이력" results={results} onInfo={() => {}} />);
    expect(screen.getByText(/2\/3/)).toBeInTheDocument();
  });

  it('toggles on click', () => {
    const results = makeResults(['pass']).map((r) => ({ ...r, ruleId: 'R01' }));
    render(<CategoryAccordion category="투명성" results={results} onInfo={() => {}} />);

    expect(screen.queryByText('msg')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('투명성'));
    expect(screen.getByText('msg')).toBeInTheDocument();

    fireEvent.click(screen.getByText('투명성'));
    expect(screen.queryByText('msg')).not.toBeInTheDocument();
  });
});
