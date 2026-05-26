import { useState } from 'react';
import type { RuleResult } from '@core/types/RuleTypes.js';
import type { Category } from '../rule-meta';
import { RULE_META } from '../rule-meta';
import { RuleLine } from './RuleLine';
import { color } from '../tokens';

interface CategoryAccordionProps {
  category: Category;
  results: RuleResult[];
}

export function CategoryAccordion({ category, results }: CategoryAccordionProps) {
  const hasDanger = results.some((r) => r.severity === 'killer' || r.severity === 'fail');
  const hasWarning = results.some((r) => r.severity === 'warn');
  const [open, setOpen] = useState(hasDanger || hasWarning);

  const passCount = results.filter((r) => r.severity === 'pass').length;
  const borderColor = hasDanger ? color.danger : hasWarning ? color.warning : 'transparent';

  return (
    <div style={{
      background: color.surface,
      borderRadius: '12px',
      margin: '0 16px 8px',
      borderLeft: `3px solid ${borderColor}`,
      boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
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
        <span>{category}</span>
        <span style={{ fontSize: '13px', color: color.textSecondary }}>
          {passCount}/{results.length} {open ? '▲' : '▼'}
        </span>
      </button>
      {open && (
        <div style={{ padding: '0 16px 8px' }}>
          {results.map((r) => (
            <RuleLine key={r.ruleId} result={r} />
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
