import type { Verdict } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { color, radius } from '../tokens';

interface ScoreCardProps {
  score: number;
  verdict: Verdict;
  carBase: EncarCarBase | null;
}

const verdictDisplay: Record<Verdict, { label: string; bg: string; text: string }> = {
  OK: { label: 'OK', bg: color.successBg, text: color.success },
  CAUTION: { label: 'CAUTION', bg: color.warningBg, text: color.warning },
  NEVER: { label: 'NEVER', bg: color.dangerBg, text: color.danger },
  UNKNOWN: { label: 'UNKNOWN', bg: color.unknownBg, text: color.textSecondary },
};

const formatPrice = (price: number): string => {
  if (price >= 10000) return `${(price / 10000).toFixed(1)}억`;
  return `${price}만원`;
};

const COLOR_STOPS: [number, [number, number, number]][] = [
  [0, [198, 40, 40]],
  [30, [230, 81, 0]],
  [50, [245, 127, 23]],
  [65, [158, 157, 36]],
  [78, [46, 125, 50]],
  [88, [0, 137, 123]],
  [100, [21, 101, 192]],
];

const scoreColor = (score: number): string => {
  const s = Math.max(0, Math.min(100, score));
  let lo = COLOR_STOPS[0]!;
  let hi = COLOR_STOPS[COLOR_STOPS.length - 1]!;
  for (let i = 0; i < COLOR_STOPS.length - 1; i++) {
    if (s >= COLOR_STOPS[i]![0] && s <= COLOR_STOPS[i + 1]![0]) {
      lo = COLOR_STOPS[i]!;
      hi = COLOR_STOPS[i + 1]!;
      break;
    }
  }
  const t = hi[0] === lo[0] ? 0 : (s - lo[0]) / (hi[0] - lo[0]);
  const r = Math.round(lo[1][0] + (hi[1][0] - lo[1][0]) * t);
  const g = Math.round(lo[1][1] + (hi[1][1] - lo[1][1]) * t);
  const b = Math.round(lo[1][2] + (hi[1][2] - lo[1][2]) * t);
  return `rgb(${r},${g},${b})`;
};

export function ScoreCard({ score, verdict, carBase }: ScoreCardProps) {
  const v = verdictDisplay[verdict];
  const bar = scoreColor(score);
  const pct = Math.max(0, Math.min(100, score));
  const title = carBase
    ? `${carBase.category.yearMonth?.slice(0, 4) ?? ''} ${carBase.category.manufacturerName} ${carBase.category.modelName}`
    : '차량 정보 없음';
  const specs = carBase
    ? [
        carBase.spec.mileage ? `${(carBase.spec.mileage / 10000).toFixed(1)}만km` : null,
        carBase.category.yearMonth ? `${carBase.category.yearMonth.slice(0, 4)}년` : null,
        carBase.advertisement.price ? formatPrice(carBase.advertisement.price) : null,
      ].filter(Boolean).join(' · ')
    : '';

  return (
    <div style={{
      background: color.surface,
      border: `1px solid ${color.hairline}`,
      borderRadius: radius.card,
      padding: '18px 16px',
      margin: '0 16px',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
        <div style={{ minWidth: 0 }}>
          <div style={{
            fontSize: '16px',
            fontWeight: 600,
            color: color.textPrimary,
            letterSpacing: '-0.3px',
          }}>
            {title}
          </div>
          {specs && (
            <div style={{ fontSize: '13px', color: color.textSecondary, marginTop: '4px' }}>{specs}</div>
          )}
        </div>
        <span style={{
          flexShrink: 0,
          padding: '5px 12px',
          borderRadius: radius.pill,
          background: v.bg,
          color: v.text,
          fontSize: '12px',
          fontWeight: 700,
          letterSpacing: '0.2px',
        }}>
          {v.label}
        </span>
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px', marginTop: '18px' }}>
        <span style={{ fontSize: '40px', fontWeight: 800, lineHeight: 1, color: bar, letterSpacing: '-1px' }}>
          {score}
        </span>
        <span style={{ fontSize: '13px', fontWeight: 600, color: color.textSecondary }}>/ 100</span>
      </div>

      <div style={{
        marginTop: '12px',
        height: '8px',
        borderRadius: radius.pill,
        background: '#eef0f3',
        overflow: 'hidden',
      }}>
        <div style={{
          width: `${pct}%`,
          height: '100%',
          borderRadius: radius.pill,
          background: bar,
        }} />
      </div>
    </div>
  );
}
