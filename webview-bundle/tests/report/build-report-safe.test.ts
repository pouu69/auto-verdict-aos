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
  });
});
