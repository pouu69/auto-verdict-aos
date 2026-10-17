/**
 * The one table from collection status to words: a short noun form for
 * summaries and source rows ("로그인 필요") and the 해요체 sentence for the
 * 미확인 message (spec 6). Reason codes come from orchestrate-mobile / core.
 */
import type { FieldStatus } from '@core/types/FieldStatus.js';
import type { RuleSource } from './rule-meta';

interface Reason {
  short: string;
  message: (source: RuleSource) => string;
}

const fixed = (short: string, message: string): Reason => ({ short, message: () => message });

const NOT_FOUND_BY_SOURCE: Record<RuleSource, string> = {
  record: '엔카에 이 차의 보험 이력이 없어요',
  inspection: '엔카에 성능점검 기록이 없어요',
  diagnosisOrInspection: '프레임 상태를 확인할 자료가 없어요',
  page: '정보를 확인하지 못했어요',
};

const FALLBACK = fixed('정보 없음', '정보를 확인하지 못했어요');
const OK = fixed('확인 완료', '정보를 확인하지 못했어요');
const NOT_RECEIVED = fixed('받지 못함', '엔카에서 정보를 받지 못했어요');

const BY_REASON: Record<string, Reason> = {
  login_required: fixed('로그인 필요', '엔카에 로그인하면 확인할 수 있어요'),
  api_fetch_error: NOT_RECEIVED,
  not_fetched: NOT_RECEIVED,
  no_report_for_personal: { short: '자료 없음', message: (source) => NOT_FOUND_BY_SOURCE[source] },
  not_applicable_personal: fixed('개인 매물 해당 없음', '개인 매물은 엔카진단을 받을 수 없어요'),
};

const reasonOf = (s: FieldStatus<unknown>): Reason => {
  switch (s.kind) {
    case 'value':
      return OK;
    case 'hidden_by_dealer':
      return fixed('딜러 비공개', '딜러가 공개하지 않았어요');
    case 'loading':
    case 'timeout':
      return NOT_RECEIVED;
    case 'parse_failed':
      return BY_REASON[s.reason] ?? FALLBACK;
  }
};

/** "로그인 필요", "자료 없음", "확인 완료", … */
export const reasonShort = (s: FieldStatus<unknown>): string => reasonOf(s).short;

/** "엔카에 로그인하면 확인할 수 있어요", … [source] picks the 자료 없음 wording. */
export const reasonMessage = (s: FieldStatus<unknown>, source: RuleSource): string => reasonOf(s).message(source);
