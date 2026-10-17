import { describe, it, expect, vi } from 'vitest';
import { buildReport } from '../../src/report/build-report';
import { load } from './fixtures';

// A detail builder that blows up costs only its sheet, never the report.
const { boom } = vi.hoisted(() => ({
  boom: (): never => {
    throw new Error('boom');
  },
}));
vi.mock('../../src/report/details/rules', () => ({ buildRuleDetail: boom }));
vi.mock('../../src/report/rule-summary', () => ({ ruleSummary: boom, unknownSummary: boom }));
vi.mock('../../src/report/details/inspection', () => ({ buildInspectionSheet: boom }));
vi.mock('../../src/report/details/timeline', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../src/report/details/timeline')>()),
  accidentSheet: boom,
  ownerSheet: boom,
  gapSheet: boom,
}));

describe('detail sheet isolation', () => {
  it('falls back to null sheets and keeps everything else', () => {
    const { raw, out } = load('41878112');
    const report = buildReport(raw, out);
    expect(report.verdict).toBe(out.report.verdict);
    expect(report.score).toBe(out.report.score);
    expect(report.rules).toHaveLength(out.report.results.length);
    expect(report.rules.every((r) => r.detail === null)).toBe(true);
    // A broken summary falls back to the sentence.
    expect(report.rules.every((r) => r.summary === r.message)).toBe(true);
    expect(report.repairs).not.toBeNull();
    expect(report.repairs!.sheet).toBeNull();
    expect(report.timeline.length).toBeGreaterThan(5);
    expect(report.timeline.every((e) => e.sheet === null)).toBe(true);
  });
});
