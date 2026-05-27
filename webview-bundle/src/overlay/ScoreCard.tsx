import type { Verdict } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { color } from '../tokens';

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

const scoreBackground = (score: number): string => {
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
      background: scoreBackground(score),
      borderRadius: '12px',
      padding: '20px 16px',
      margin: '0 16px',
      color: '#ffffff',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: '16px', fontWeight: 600 }}>{title}</div>
          <div style={{ fontSize: '12px', opacity: 0.8, marginTop: '4px' }}>{specs}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '32px', fontWeight: 800, lineHeight: 1 }}>{score}</div>
          <div style={{ fontSize: '11px', opacity: 0.7 }}>/ 100</div>
        </div>
      </div>
      <div style={{ marginTop: '12px' }}>
        <span style={{
          display: 'inline-block',
          padding: '4px 12px',
          borderRadius: '4px',
          background: v.bg,
          color: v.text,
          fontSize: '13px',
          fontWeight: 700,
        }}>
          {v.label}
        </span>
      </div>
    </div>
  );
}
