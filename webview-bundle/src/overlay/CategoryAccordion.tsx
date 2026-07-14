import { useState } from 'react';
import type { RuleResult } from '@core/types/RuleTypes.js';
import type { Category } from '../rule-meta';
import { RULE_META } from '../rule-meta';
import { RuleLine } from './RuleLine';
import { color, radius } from '../tokens';

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      width="12"
      height="12"
      viewBox="0 0 24 24"
      fill="none"
      stroke={color.textSecondary}
      strokeWidth="2.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{
        transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
        transition: 'transform 0.18s ease',
      }}
      aria-hidden="true"
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

interface CategoryAccordionProps {
  category: Category;
  results: RuleResult[];
  onInfo: (result: RuleResult) => void;
}

export function CategoryAccordion({ category, results, onInfo }: CategoryAccordionProps) {
  const hasDanger = results.some((r) => r.severity === 'killer' || r.severity === 'fail');
  const hasWarning = results.some((r) => r.severity === 'warn');
  const [open, setOpen] = useState(hasDanger || hasWarning);

  const passCount = results.filter((r) => r.severity === 'pass').length;
  const borderColor = hasDanger ? color.danger : hasWarning ? color.warning : 'transparent';

  return (
    <div style={{
      background: color.surface,
      borderRadius: radius.card,
      margin: '0 16px 8px',
      border: `1px solid ${color.hairline}`,
      borderLeft: `3px solid ${borderColor === 'transparent' ? color.hairline : borderColor}`,
      overflow: 'hidden',
    }}>
      <button
        onClick={() => setOpen((prev) => !prev)}
        style={{
          width: '100%',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '14px 16px',
          border: 'none',
          background: 'transparent',
          cursor: 'pointer',
          fontSize: '15px',
          fontWeight: 600,
          color: color.textPrimary,
        }}
      >
        <span style={{ letterSpacing: '-0.3px' }}>{category}</span>
        <span style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: color.textSecondary }}>
          <span>{passCount}/{results.length}</span>
          <Chevron open={open} />
        </span>
      </button>
      {open && (
        <div style={{ padding: '0 16px 8px' }}>
          {results.map((r) => (
            <RuleLine key={r.ruleId} result={r} onInfo={onInfo} />
          ))}
        </div>
      )}
    </div>
  );
}

export function groupByCategory(results: RuleResult[]): Array<{ category: Category; results: RuleResult[] }> {
  const map = new Map<Category, RuleResult[]>();
  for (const r of results) {
    const meta = RULE_META[r.ruleId];
    if (!meta) continue;
    const list = map.get(meta.category) ?? [];
    list.push(r);
    map.set(meta.category, list);
  }
  return Array.from(map.entries()).map(([category, results]) => ({ category, results }));
}
