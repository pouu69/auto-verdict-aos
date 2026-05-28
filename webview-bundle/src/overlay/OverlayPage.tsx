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
  hideSave?: boolean;
}

export function OverlayPage({ report, carBase, onClose, onSave, hideSave }: OverlayPageProps) {
  const groups = groupByCategory(report.results);
  const sortedGroups = [...groups].sort(
    (a, b) => CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category),
  );

  return (
    <div style={{
      minHeight: '100vh',
      background: color.background,
      paddingBottom: hideSave
        ? 'calc(16px + env(safe-area-inset-bottom, 0px))'
        : 'calc(96px + env(safe-area-inset-bottom, 16px))',
    }}>
      <div style={{ paddingTop: '12px' }}>
        <ScoreCard score={report.score} verdict={report.verdict} carBase={carBase} />
      </div>

      <SummaryRow results={report.results} />

      <div style={{ paddingTop: '4px' }}>
        {sortedGroups.map(({ category, results }) => (
          <CategoryAccordion key={category} category={category} results={results} />
        ))}
      </div>

      {!hideSave && (
        <div style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          paddingTop: '12px',
          paddingLeft: '16px',
          paddingRight: '16px',
          paddingBottom: 'calc(12px + env(safe-area-inset-bottom, 16px))',
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
      )}
    </div>
  );
}
