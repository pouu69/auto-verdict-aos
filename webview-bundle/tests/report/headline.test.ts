import { describe, it, expect } from 'vitest';
import type { RuleReport, RuleResult, Severity, Verdict } from '@core/types/RuleTypes.js';
import { buildHeadline } from '../../src/report/headline';
import { load } from './fixtures';

const facts = load('41878112').out.facts;

const r = (ruleId: string, severity: Severity, evidence: RuleResult['evidence'] = []): RuleResult => ({
  ruleId,
  title: '',
  severity,
  message: '',
  evidence,
  acknowledgeable: false,
});

const report = (verdict: Verdict, results: RuleResult[]): RuleReport => ({
  verdict,
  score: 50,
  results,
  killers: results.filter((x) => x.severity === 'killer'),
  warns: results.filter((x) => x.severity === 'warn'),
});

describe('buildHeadline', () => {
  it('NEVER uses the lowest-numbered killer', () => {
    expect(
      buildHeadline(
        report('NEVER', [
          r('R04', 'killer'),
          r('R05', 'killer', [{ field: 'usageHistory', value: { rental: true, taxi: false, business: false } }]),
        ]),
        facts,
      ),
    ).toBe('사고(프레임 손상) 이력이 있어 사지 않는 게 좋아요');
  });

  it('NEVER R05 names only the real usage', () => {
    const usage = (rental: boolean, taxi: boolean) =>
      buildHeadline(
        report('NEVER', [r('R05', 'killer', [{ field: 'usageHistory', value: { rental, taxi, business: false } }])]),
        facts,
      );
    expect(usage(true, false)).toBe('렌트 이력이 있어 사지 않는 게 좋아요');
    expect(usage(false, true)).toBe('영업용 이력이 있어 사지 않는 게 좋아요');
    expect(usage(true, true)).toBe('렌트·영업용 이력이 있어 사지 않는 게 좋아요');
  });

  it('NEVER R06 names only the real losses', () => {
    const loss = (v: { totalLoss: number; floodTotal: number; floodPart: number; robber: number }) =>
      buildHeadline(report('NEVER', [r('R06', 'killer', [{ field: 'totalLoss', value: v }])]), facts);
    expect(loss({ totalLoss: 0, floodTotal: 0, floodPart: 1, robber: 0 })).toBe('침수 이력이 있어 사지 않는 게 좋아요');
    expect(loss({ totalLoss: 1, floodTotal: 0, floodPart: 0, robber: 1 })).toBe('전손·도난 이력이 있어 사지 않는 게 좋아요');
  });

  it('CAUTION counts warnings', () => {
    expect(buildHeadline(report('CAUTION', [r('R07', 'warn'), r('R08', 'warn'), r('R10', 'warn'), r('R01', 'pass')]), facts)).toBe(
      '사기 전에 3가지는 꼭 확인하세요',
    );
  });

  it('UNKNOWN counts unknowns', () => {
    expect(buildHeadline(report('UNKNOWN', [r('R04', 'unknown'), r('R05', 'unknown'), r('R07', 'warn')]), facts)).toBe(
      '확인하지 못한 항목이 2개 있어요',
    );
  });

  it('OK variants', () => {
    expect(buildHeadline(report('OK', [r('R07', 'warn'), r('R01', 'pass')]), facts)).toBe(
      '큰 문제는 없지만 1가지는 확인해 보세요',
    );
    expect(buildHeadline(report('OK', [r('R01', 'fail'), r('R07', 'pass')]), facts)).toBe(
      '큰 문제는 없지만 비공개 항목이 있어요',
    );
    expect(buildHeadline(report('OK', [r('R01', 'pass')]), facts)).toBe('큰 문제는 발견되지 않았어요');
  });
});
