import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { ScoreCard } from './ScoreCard';
import { SummaryRow } from './SummaryRow';
import { CategoryAccordion, groupByCategory } from './CategoryAccordion';
import { CATEGORY_ORDER } from '../rule-meta';
import { color } from '../tokens';

interface OverlayPageProps {
  report: RuleReport;
  carBase: EncarCarBase | null;
  onClose: () => void;
  onSave: () => void;
}

export function OverlayPage({ report, carBase, onClose, onSave }: OverlayPageProps) {
  const groups = groupByCategory(report.results);
  const sortedGroups = [...groups].sort(
    (a, b) => CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category),
  );

  return (
    <div style={{
      minHeight: '100vh',
      background: color.background,
      paddingBottom: '80px',
    }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '16px',
        background: color.surface,
      }}>
        <div style={{ fontSize: '17px', fontWeight: 700, color: color.textPrimary }}>
          점검 리포트
        </div>
        <button
          onClick={onClose}
          style={{
            border: 'none',
            background: 'transparent',
            fontSize: '24px',
            color: color.textSecondary,
            cursor: 'pointer',
            padding: '4px',
          }}
        >
          ✕
        </button>
      </div>

      <div style={{ paddingTop: '12px' }}>
        <ScoreCard score={report.score} verdict={report.verdict} carBase={carBase} />
      </div>

      <SummaryRow results={report.results} />

      <div style={{ paddingTop: '4px' }}>
        {sortedGroups.map(({ category, results }) => (
          <CategoryAccordion key={category} category={category} results={results} />
        ))}
      </div>

      <div style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        padding: '12px 16px',
        background: color.surface,
        borderTop: `1px solid ${color.border}`,
      }}>
        <button
          onClick={onSave}
          style={{
            width: '100%',
            padding: '14px',
            background: color.primary,
            color: '#fff',
            border: 'none',
            borderRadius: '12px',
            fontSize: '16px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          저장하기
        </button>
      </div>
    </div>
  );
}
