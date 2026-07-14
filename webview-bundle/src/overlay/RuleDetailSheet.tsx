import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import { RULE_META } from '../rule-meta';
import { BottomSheet } from './BottomSheet';
import { color, radius } from '../tokens';

interface SeverityStyle { bg: string; text: string; label: string }

const severityStyle: Record<Severity, SeverityStyle> = {
  pass: { bg: color.successBg, text: color.success, label: '통과' },
  warn: { bg: color.warningBg, text: color.warning, label: '주의' },
  fail: { bg: color.dangerBg, text: color.danger, label: '위험' },
  killer: { bg: color.dangerBg, text: color.danger, label: '위험' },
  unknown: { bg: color.unknownBg, text: color.textSecondary, label: '미확인' },
};

interface RuleDetailSheetProps {
  result: RuleResult | null;
  onClose: () => void;
}

/**
 * 항목을 탭하면 아래에서 올라오는 상세 시트.
 * "왜 중요한가요?"(고정 설명) + 이 차의 실제 판정(result.message)을 함께 보여준다.
 */
export function RuleDetailSheet({ result, onClose }: RuleDetailSheetProps) {
  const meta = result ? RULE_META[result.ruleId] : undefined;
  const style = result ? severityStyle[result.severity] ?? severityStyle.unknown : undefined;

  return (
    <BottomSheet open={result !== null} onClose={onClose}>
      {result && style && (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <span style={{ fontSize: '20px' }}>{meta?.icon}</span>
            <span style={{ fontSize: '16px', fontWeight: 600, color: color.textPrimary, letterSpacing: '-0.3px' }}>
              {result.title}
            </span>
            <span style={{
              marginLeft: 'auto',
              padding: '4px 10px',
              borderRadius: radius.badge,
              background: style.bg,
              color: style.text,
              fontSize: '12px',
              fontWeight: 700,
              flexShrink: 0,
            }}>
              {style.label}
            </span>
          </div>

          <div style={{
            padding: '12px 14px',
            background: color.background,
            borderRadius: radius.button,
            marginBottom: '16px',
          }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: color.textSecondary, marginBottom: '4px' }}>
              이 차의 결과
            </div>
            <div style={{ fontSize: '14px', color: color.textPrimary, lineHeight: 1.5, wordBreak: 'keep-all' }}>
              {result.message}
            </div>
          </div>

          {meta?.why && (
            <div style={{ marginBottom: '20px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: color.primary, marginBottom: '6px', letterSpacing: '0.2px' }}>
                왜 중요한가요?
              </div>
              <div style={{ fontSize: '14px', color: color.textSecondary, lineHeight: 1.65, wordBreak: 'keep-all' }}>
                {meta.why}
              </div>
            </div>
          )}

          <button
            onClick={onClose}
            style={{
              width: '100%',
              padding: '14px',
              background: color.background,
              color: color.textPrimary,
              border: 'none',
              borderRadius: radius.pill,
              fontSize: '15px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            닫기
          </button>
        </>
      )}
    </BottomSheet>
  );
}
