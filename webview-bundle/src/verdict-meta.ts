import type { Verdict } from '@core/types/RuleTypes.js';

export interface VerdictMeta {
  /** 한글 라벨 (영문 배지 옆 병기 / 시트 제목) */
  label: string;
  /** 이 등급이 무슨 뜻인지 */
  meaning: string;
  /** 사용자가 취할 행동 */
  advice: string;
}

export const VERDICT_META: Record<Verdict, VerdictMeta> = {
  NEVER: {
    label: '절대 피하세요',
    meaning:
      '되돌릴 수 없는 치명적 이력이 하나라도 발견된 등급이에요. 프레임 손상, 렌트·택시 이력, 전손·침수·도난처럼 안전이나 재산 가치에 직접 타격을 주는 항목이 여기 해당합니다.',
    advice: '아무리 조건이 좋아 보여도 이런 차는 피하는 걸 권합니다.',
  },
  CAUTION: {
    label: '주의',
    meaning:
      '치명적 문제는 없지만, 구매 전 확인이 필요한 주의 항목이 2개 이상 쌓인 등급이에요. 잦은 소유자 변경, 보험 공백, 큰 보험처리 규모 등이 여기 해당합니다.',
    advice: '아래 주의 항목을 하나씩 확인하고, 실차 점검이나 가격 협상에 활용하세요.',
  },
  OK: {
    label: '양호',
    meaning:
      '눈에 띄는 위험·주의 신호가 없는 깨끗한 매물이에요. 데이터상으로는 큰 문제가 확인되지 않았습니다.',
    advice: '그래도 실차 확인과 시운전은 항상 권장합니다.',
  },
  UNKNOWN: {
    label: '미확인',
    meaning:
      '판단에 필요한 데이터를 일부 확인하지 못한 상태예요. 엔카 로그인이 필요하거나 딜러가 정보를 비공개해서, 아직 등급을 확정할 수 없습니다.',
    advice: '엔카 로그인 후 다시 분석하거나, 딜러에게 미확인 항목 정보를 요청하세요.',
  },
};
