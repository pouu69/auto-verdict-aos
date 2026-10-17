import type { Category } from './types';

/** Which collected data a rule reads; picks the API status used for 미확인 reasons. */
export type RuleSource = 'page' | 'record' | 'inspection' | 'diagnosisOrInspection';

export interface RuleMeta {
  category: Category;
  /** Short row title shown in the report. */
  title: string;
  /** "왜 중요한가요" paragraph (해요체), independent of the result. */
  why: string;
  source: RuleSource;
}

export const RULE_IDS: readonly string[] = [
  'R01', 'R02', 'R03', 'R04', 'R05', 'R06', 'R07', 'R08', 'R09', 'R10', 'R11', 'R12',
];

export const CATEGORY_ORDER: readonly Category[] = ['차량 상태', '이력', '사고', '가격', '투명성'];

export const RULE_META: Record<string, RuleMeta> = {
  R01: {
    category: '투명성',
    title: '보험이력 공개',
    why: '보험이력은 이 차가 언제 어떤 사고로 보험 처리를 했는지 보여주는 가장 기본 자료예요. 정상 매물은 공개가 기본이라, 딜러가 굳이 가렸다면 숨기고 싶은 사고가 있을 수 있어요.',
    source: 'page',
  },
  R02: {
    category: '투명성',
    title: '성능점검 공개',
    why: '성능점검은 차량 상태를 제3자가 검사한 공식 문서예요. 공개하지 않으면 엔진·미션·사고 같은 핵심 상태를 확인할 방법이 없어 사실상 깜깜이 구매가 돼요.',
    source: 'page',
  },
  R03: {
    category: '차량 상태',
    title: '엔카진단',
    why: '엔카가 자체 기준으로 통과시킨 차에만 붙는 인증이에요. 있으면 믿을 만하다는 가산점이고, 없다고 감점하지는 않아요.',
    source: 'page',
  },
  R04: {
    category: '차량 상태',
    title: '프레임/외판',
    why: '프레임은 차의 뼈대예요. 여기가 손상됐다는 건 충격이 뼈대까지 갔다는 뜻이라 주행 안정성과 되팔 때 가격에 큰 영향을 줘요. 그래서 위험으로 분류해요. 문·펜더 같은 바깥 패널 교환은 상대적으로 가벼운 편이에요.',
    source: 'diagnosisOrInspection',
  },
  R05: {
    category: '이력',
    title: '렌트·택시 이력',
    why: '렌트·영업용 차는 개인 차보다 하루 주행량과 부하가 몇 배예요. 주행거리가 같아도 실제 마모는 훨씬 커서 위험으로 봐요. 관용차는 관리가 잘된 편이라 주의 정도로 봐요.',
    source: 'record',
  },
  R06: {
    category: '이력',
    title: '전손·침수·도난',
    why: '전손은 보험사가 고쳐 쓰느니 폐차가 낫다고 판정했던 차예요. 침수는 전기장치 부식이 나중에 문제를 일으키고, 도난 이력은 부품이 손상됐을 수 있어 모두 위험으로 봐요.',
    source: 'record',
  },
  R07: {
    category: '이력',
    title: '소유자 변경',
    why: '소유자가 자주 바뀐 차는 문제가 있어 자주 되팔렸거나, 정비 이력이 끊겨 관리 상태를 알기 어려운 경우가 많아요. 3회 이상부터 주의 신호로 봐요.',
    source: 'record',
  },
  R08: {
    category: '이력',
    title: '자차보험 공백',
    why: '보험에 가입하지 않은 기간에 난 사고는 이력에 남지 않아요. 이력이 깨끗해 보여도 숨은 사고가 있을 수 있는 공백 구간이에요.',
    source: 'record',
  },
  R09: {
    category: '사고',
    title: '수리비 확정',
    why: '사고 기록은 있는데 수리비 금액이 확정되지 않은 상태예요. 손상 규모를 숫자로 가늠할 수 없어 실제로 얼마나 크게 다쳤는지 알기 어려워요.',
    source: 'record',
  },
  R10: {
    category: '사고',
    title: '보험처리 규모',
    why: '사고가 있어도 금액이 작으면 가벼운 접촉 수준이에요. 하지만 국산 200만원, 수입 400만원을 넘으면 단순 접촉을 넘어선 사고일 수 있어 주의로 봐요.',
    source: 'record',
  },
  R11: {
    category: '가격',
    title: '가격 적정성',
    why: '신차 가격 대비 지나치게 싸거나 비싸면 한 번 더 따져봐야 해요. 신차 가격의 45%보다 싸면 안 보이는 하자가 숨어 있을 수 있고, 115%보다 비싸면 값을 더 주고 사는 셈이에요.',
    source: 'page',
  },
  R12: {
    category: '차량 상태',
    title: '누유 여부',
    why: '엔진·미션에서 기름이 새면 그대로 두었을 때 큰 수리비로 이어지는 경우가 많아요. 지금 당장 괜찮아 보여도 정비 비용을 미리 생각해 두는 게 좋아요.',
    source: 'inspection',
  },
};
