import type { RuleResult } from '@core/types/RuleTypes.js';
import { color } from '../tokens';

interface SummaryRowProps {
  results: RuleResult[];
}

export function SummaryRow({ results }: SummaryRowProps) {
  const danger = results.filter((r) => r.severity === 'killer' || r.severity === 'fail').length;
  const caution = results.filter((r) => r.severity === 'warn').length;
  const pass = results.filter((r) => r.severity === 'pass').length;
  const unknown = results.filter((r) => r.severity === 'unknown').length;

  const cells: Array<{ label: string; count: number; bg: string; text: string }> = [
    { label: '위험', count: danger, bg: color.dangerBg, text: color.danger },
    { label: '주의', count: caution, bg: color.warningBg, text: color.warning },
    { label: '통과', count: pass, bg: color.successBg, text: color.success },
    { label: '미확인', count: unknown, bg: color.unknownBg, text: color.textSecondary },
  ];

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(4, 1fr)',
      gap: '8px',
      padding: '12px 16px',
    }}>
      {cells.map((cell) => (
        <div key={cell.label} style={{
          background: cell.bg,
          borderRadius: '8px',
          padding: '10px 8px',
          textAlign: 'center',
        }}>
          <div style={{ fontSize: '20px', fontWeight: 700, color: cell.text }}>
            {cell.count}
          </div>
          <div style={{ fontSize: '11px', color: cell.text, marginTop: '2px' }}>
            {cell.label}
          </div>
        </div>
      ))}
    </div>
  );
}
