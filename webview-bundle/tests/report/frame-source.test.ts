import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import type { MobileOrchestratorInput } from '../../src/orchestrate-mobile';
import { buildReport } from '../../src/report/build-report';
import { frameDamageSource } from '../../src/report/frame-source';
import { load, rawOf } from './fixtures';

/** 39339524 with Encar's own diagnosis rewritten to a 사고 verdict. */
const diagnosisKillerRaw = (): MobileOrchestratorInput => {
  const raw = rawOf('39339524');
  const dia = raw.diagnosisJson as { items: { name: string; result: string }[] };
  const checker = dia.items.find((i) => i.name === 'CHECKER_COMMENT')!;
  checker.result = "본 차량은 엔카의 진단 결과 프레임(주요 골격) 부위가 교환되어 '사고' 차량으로 판정합니다.";
  return raw;
};

describe('frameDamageSource', () => {
  it('39339524: diagnosis is inconclusive, so the inspection decides', () => {
    expect(frameDamageSource(load('39339524').out.parsed.raw)).toBe('inspection');
  });

  it('41878112: diagnosis 무사고 decides', () => {
    expect(frameDamageSource(load('41878112').out.parsed.raw)).toBe('diagnosis');
  });

  it('42264483: no diagnosis, inspection decides', () => {
    expect(frameDamageSource(load('42264483').out.parsed.raw)).toBe('inspection');
  });

  it('41623744: nothing collected', () => {
    expect(frameDamageSource(load('41623744').out.parsed.raw)).toBe('unknown');
  });

  it('diagnosis 사고 outranks the inspection', () => {
    expect(frameDamageSource(orchestrateMobile(diagnosisKillerRaw()).parsed.raw)).toBe('diagnosis');
  });
});

describe('R04 killer copy names its evidence source', () => {
  it('39339524: inspection-sourced killer', () => {
    const { raw, out } = load('39339524');
    const report = buildReport(raw, out);
    const r04 = report.issues.find((i) => i.ruleId === 'R04')!;
    expect(report.headline).toBe('성능점검에 사고 이력이 있어 사지 않는 게 좋아요');
    expect(r04.message).toBe('성능점검에 사고 이력이 있어요');
    expect(r04.sellerQuestion).toBe('성능점검 사고 부위의 수리 내역과 사진을 볼 수 있을까요?');
    expect(report.shareText.summary).toContain('성능점검에 사고 이력이 있어 사지 않는 게 좋아요');
  });

  it('diagnosis-sourced killer', () => {
    const raw = diagnosisKillerRaw();
    const out = orchestrateMobile(raw);
    expect(out.report.verdict).toBe('NEVER');
    const report = buildReport(raw, out);
    const r04 = report.issues.find((i) => i.ruleId === 'R04')!;
    expect(r04.label).toBe('위험');
    expect(report.headline).toBe('엔카진단에서 프레임 손상이 확인돼 사지 않는 게 좋아요');
    expect(r04.message).toBe('엔카진단에서 프레임 손상이 확인됐어요');
    expect(r04.sellerQuestion).toBe('프레임 수리 내역과 사진을 볼 수 있을까요?');
  });
});
