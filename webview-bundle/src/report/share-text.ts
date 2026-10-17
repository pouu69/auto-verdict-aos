import type { Verdict } from '@core/types/RuleTypes.js';
import type { EvaluationReport, ShareText } from './types';

/** Same verdict labels as the app's verdict badge. */
const VERDICT_LABEL: Record<Verdict, string> = {
  NEVER: '피하세요',
  CAUTION: '주의',
  OK: '괜찮아요',
  UNKNOWN: '확인 필요',
};

const MAX_ISSUE_LINES = 5;
const FOOTER = 'AutoVerdict로 분석';

export function buildShareText(r: Omit<EvaluationReport, 'shareText'>): ShareText {
  const summary = [
    r.car.title,
    `판정: ${VERDICT_LABEL[r.verdict]} · ${r.score}점`,
    r.headline,
    ...r.issues.slice(0, MAX_ISSUE_LINES).map((i) => `- ${i.title}: ${i.message}`),
    r.url,
    FOOTER,
  ].join('\n');

  const questions = r.issues
    .map((i) => i.sellerQuestion)
    .filter((q): q is string => !!q);
  const sellerQuestions =
    questions.length > 0
      ? [...questions.map((q, i) => `${i + 1}. ${q}`), r.url, FOOTER].join('\n')
      : null;

  return { summary, sellerQuestions };
}
