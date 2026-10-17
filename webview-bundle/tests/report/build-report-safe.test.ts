import { describe, it, expect, vi } from 'vitest';
import { buildReport } from '../../src/report/build-report';
import { load } from './fixtures';

// A section builder that blows up must not take the whole report down.
vi.mock('../../src/report/timeline', () => ({
  buildTimeline: () => {
    throw new Error('boom');
  },
}));
vi.mock('../../src/report/repairs', () => ({
  buildRepairs: () => {
    throw new Error('boom');
  },
}));
// So does the copy of a single rule row.
vi.mock('../../src/report/rule-copy', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../src/report/rule-copy')>()),
  ruleCopy: () => {
    throw new Error('boom');
  },
  unknownMessage: () => {
    throw new Error('boom');
  },
}));

describe('buildReport section isolation', () => {
  it('falls back per section and keeps verdict, score and rules', () => {
    const { raw, out } = load('39339524');
    const report = buildReport(raw, out);
    expect(report.timeline).toEqual([]);
    expect(report.repairs).toBeNull();
    expect(report.verdict).toBe(out.report.verdict);
    expect(report.score).toBe(out.report.score);
    expect(report.rules).toHaveLength(out.report.results.length);
    expect(report.facts).toHaveLength(4);
    expect(report.car.title).toBe('2012 BMW 3시리즈 (F30)');
    // 39339524 has no 미확인 rule: every row is a decided one on the fallback copy.
    expect(report.rules.map((r) => r.message)).toEqual(report.rules.map(() => '자세한 내용을 표시하지 못했어요'));
    expect(report.issues.length).toBeGreaterThan(0);
  });

  it('falls back for a 미확인 row whose reason cannot be read', () => {
    const { raw, out } = load('41623744');
    const report = buildReport(raw, out);
    const unknown = report.rules.filter((r) => r.label === '미확인');
    expect(unknown.length).toBeGreaterThan(0);
    for (const r of unknown) expect(r.message).toBe('정보를 확인하지 못했어요');
  });
});
