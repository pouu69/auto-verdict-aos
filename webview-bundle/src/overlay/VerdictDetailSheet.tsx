import type { RuleReport, Verdict } from '@core/types/RuleTypes.js';
import { VERDICT_META } from '../verdict-meta';
import { BottomSheet } from './BottomSheet';
import { color, radius } from '../tokens';

const verdictStyle: Record<Verdict, { bg: string; text: string }> = {
  OK: { bg: color.successBg, text: color.success },
  CAUTION: { bg: color.warningBg, text: color.warning },
  NEVER: { bg: color.dangerBg, text: color.danger },
  UNKNOWN: { bg: color.unknownBg, text: color.textSecondary },
};

interface VerdictDetailSheetProps {
  report: RuleReport | null;
  onClose: () => void;
}

/** 이 리포트가 왜 이 등급을 받았는지, report 내용으로 한 줄 만든다. */
function reasonFor(report: RuleReport): string {
  switch (report.verdict) {
    case 'NEVER': {
      const titles = report.killers.map((k) => k.title).join(', ');
      return titles
        ? `치명적 항목이 발견됐어요: ${titles}.`
        : '치명적 항목이 발견됐어요.';
    }
    case 'CAUTION':
      return `구매 전 확인이 필요한 주의 항목이 ${report.warns.length}개 있어요.`;
    case 'UNKNOWN': {
      const unknownCount = report.results.filter((r) => r.severity === 'unknown').length;
      return `데이터를 확인하지 못한 미확인 항목이 ${unknownCount}개 있어요.`;
    }
    case 'OK':
    default:
      return '위험·주의 신호가 확인되지 않았어요.';
  }
}

export function VerdictDetailSheet({ report, onClose }: VerdictDetailSheetProps) {
  const meta = report ? VERDICT_META[report.verdict] : undefined;
  const style = report ? verdictStyle[report.verdict] : undefined;

  return (
    <BottomSheet open={report !== null} onClose={onClose}>
      {report && meta && style && (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <span style={{ fontSize: '16px', fontWeight: 600, color: color.textPrimary, letterSpacing: '-0.3px' }}>
              {meta.label}
            </span>
            <span style={{
              marginLeft: 'auto',
              padding: '4px 10px',
              borderRadius: radius.pill,
              background: style.bg,
              color: style.text,
              fontSize: '12px',
              fontWeight: 700,
              letterSpacing: '0.2px',
              flexShrink: 0,
            }}>
              {report.verdict}
            </span>
          </div>

          <div style={{
            padding: '12px 14px',
            background: color.background,
            borderRadius: radius.button,
            marginBottom: '16px',
          }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: color.textSecondary, marginBottom: '4px' }}>
              이 차가 이 등급인 이유
            </div>
            <div style={{ fontSize: '14px', color: color.textPrimary, lineHeight: 1.5, wordBreak: 'keep-all' }}>
              {reasonFor(report)}
            </div>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: color.primary, marginBottom: '6px', letterSpacing: '0.2px' }}>
              이 등급은 무슨 뜻인가요?
            </div>
            <div style={{ fontSize: '14px', color: color.textSecondary, lineHeight: 1.65, wordBreak: 'keep-all' }}>
              {meta.meaning}
            </div>
          </div>

          <div style={{ marginBottom: '20px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: color.primary, marginBottom: '6px', letterSpacing: '0.2px' }}>
              어떻게 하면 좋을까요?
            </div>
            <div style={{ fontSize: '14px', color: color.textSecondary, lineHeight: 1.65, wordBreak: 'keep-all' }}>
              {meta.advice}
            </div>
          </div>

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
