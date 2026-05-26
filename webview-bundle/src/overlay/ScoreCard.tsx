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
      background: color.primary,
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
