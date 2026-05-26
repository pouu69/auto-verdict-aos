import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OverlayPage } from '../../src/overlay/OverlayPage';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';

const mockReport: RuleReport = {
  verdict: 'CAUTION',
  score: 72,
  results: [
    { ruleId: 'R01', title: '보험이력 공개', severity: 'pass', message: '딜러가 보험이력을 공개했습니다', evidence: [], acknowledgeable: false },
    { ruleId: 'R05', title: '렌트/택시 이력', severity: 'warn', message: '⚠ 관용 이력', evidence: [], acknowledgeable: false },
  ],
  killers: [],
  warns: [
    { ruleId: 'R05', title: '렌트/택시 이력', severity: 'warn', message: '⚠ 관용 이력', evidence: [], acknowledgeable: false },
  ],
};

const mockBase: EncarCarBase = {
  category: { manufacturerName: '현대', modelName: '투싼', yearMonth: '202201', domestic: true },
  advertisement: { price: 2500, preVerified: false, trust: [] },
  spec: { mileage: 35000 },
};

describe('OverlayPage', () => {
  it('renders score and verdict', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getByText('72')).toBeInTheDocument();
    expect(screen.getByText('CAUTION')).toBeInTheDocument();
  });

  it('renders vehicle title', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getByText(/현대 투싼/)).toBeInTheDocument();
  });

  it('renders summary counts', () => {
    render(<OverlayPage report={mockReport} carBase={mockBase} onClose={() => {}} onSave={() => {}} />);
    expect(screen.getAllByText('통과').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('주의').length).toBeGreaterThanOrEqual(1);
  });
});
