import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import { RULE_META } from '../rule-meta';
import { color } from '../tokens';

interface SeverityStyle { bg: string; text: string; label: string }

const severityStyle: Record<Severity, SeverityStyle> = {
  pass: { bg: color.successBg, text: color.success, label: '통과' },
  warn: { bg: color.warningBg, text: color.warning, label: '주의' },
  fail: { bg: color.dangerBg, text: color.danger, label: '위험' },
  killer: { bg: color.dangerBg, text: color.danger, label: '위험' },
  unknown: { bg: color.unknownBg, text: color.textSecondary, label: '미확인' },
};

const unknownStyle: SeverityStyle = { bg: color.unknownBg, text: color.textSecondary, label: '미확인' };

export function RuleLine({ result }: { result: RuleResult }) {
  const meta = RULE_META[result.ruleId];
  const style: SeverityStyle = severityStyle[result.severity] ?? unknownStyle;

  return (
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      padding: '10px 0',
      borderBottom: `1px solid ${color.border}`,
      gap: '10px',
    }}>
      <span style={{
        width: '28px',
        height: '28px',
        borderRadius: '50%',
        background: color.primary,
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '11px',
        fontWeight: 700,
        flexShrink: 0,
      }}>
        {result.ruleId.replace('R0', '').replace('R', '')}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: '14px', fontWeight: 500, color: color.textPrimary }}>
          {meta?.icon} {result.title}
        </div>
        <div style={{
          fontSize: '12px',
          color: color.textSecondary,
          marginTop: '2px',
          wordBreak: 'keep-all',
          lineHeight: '1.4',
        }}>
          {result.message}
        </div>
      </div>
      <span style={{
        padding: '3px 8px',
        borderRadius: '4px',
        background: style.bg,
        color: style.text,
        fontSize: '11px',
        fontWeight: 600,
        flexShrink: 0,
      }}>
        {style.label}
      </span>
    </div>
  );
}
