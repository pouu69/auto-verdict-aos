/**
 * Engine output -> EvaluationReport (spec 4). Pure; the verdict and score are
 * passed through untouched, everything else is display copy and grouping.
 */
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { EvaluationReport, ExcludedItem, ReportCounts, RuleItem } from './types';
import { SCHEMA_VERSION } from './types';
import { CATEGORY_ORDER, RULE_IDS, RULE_META, ruleNo } from './rule-meta';
import { excludedReason, labelFor, ruleCopy, toneFor, unknownMessage } from './rule-copy';
import { buildFacts } from './facts';
import { buildRepairs } from './repairs';
import { buildTimeline } from './timeline';
import { buildDataGaps } from './data-gaps';
import { buildHeadline } from './headline';
import { EMPTY_CAR, buildCar } from './car';
import { buildShareText } from './share-text';
import { frameDamageSource, type FrameSource } from './frame-source';
import { safe } from './safe';
import { buildRuleDetail } from './details/rules';
import { buildInspectionSheet } from './details/inspection';

/** Issue order: 위험 → 비공개 → 주의 → 미확인. */
const ISSUE_RANK: Record<Severity, number> = { killer: 0, fail: 1, warn: 2, unknown: 3, pass: 4 };

const toItem = (r: RuleResult, out: MobileOrchestratorResult, frameSource: FrameSource): RuleItem => {
  const meta = RULE_META[r.ruleId];
  const tone = toneFor(r.severity);
  const copy = r.severity === 'unknown' ? null : ruleCopy(r, out.facts, frameSource);
  const issue = tone !== 'success';
  return {
    ruleId: r.ruleId,
    category: meta?.category ?? '투명성',
    title: meta?.title ?? r.title,
    tone,
    label: labelFor(r.severity),
    message: copy ? copy.message : unknownMessage(r, out),
    why: issue ? (meta?.why ?? null) : null,
    sellerQuestion: issue && (tone === 'danger' || tone === 'warning') ? (copy?.sellerQuestion ?? null) : null,
    detail: safe(() => buildRuleDetail(r, out, frameSource), null),
  };
};

export function buildReport(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): EvaluationReport {
  const results = out.report.results;
  const catIndex = (r: RuleResult) => CATEGORY_ORDER.indexOf(RULE_META[r.ruleId]?.category ?? '투명성');
  const sorted = [...results].sort((a, b) => catIndex(a) - catIndex(b) || ruleNo(a.ruleId) - ruleNo(b.ruleId));
  const frameSource = safe(() => frameDamageSource(out.parsed.raw), 'unknown' as FrameSource);
  // One item per result: issues reuse the rows (and their detail sheets) built for rules.
  const items = new Map(sorted.map((r) => [r, toItem(r, out, frameSource)] as const));
  const itemOf = (r: RuleResult): RuleItem => items.get(r) as RuleItem;
  const rules = sorted.map(itemOf);

  const issues = sorted
    .filter((r) => toneFor(r.severity) !== 'success')
    .sort((a, b) => ISSUE_RANK[a.severity] - ISSUE_RANK[b.severity] || ruleNo(a.ruleId) - ruleNo(b.ruleId))
    .map(itemOf);

  const present = new Set(results.map((r) => r.ruleId));
  const excluded: ExcludedItem[] = RULE_IDS.filter((id) => !present.has(id)).map((id) => ({
    ruleId: id,
    title: RULE_META[id]?.title ?? id,
    reason: excludedReason(id),
  }));

  const count = (s: Severity) => results.filter((r) => r.severity === s).length;
  const counts: ReportCounts = {
    danger: count('killer'),
    hidden: count('fail'),
    warning: count('warn'),
    pass: count('pass'),
    unknown: count('unknown'),
    excluded: excluded.length,
  };

  const report: Omit<EvaluationReport, 'shareText'> = {
    schemaVersion: SCHEMA_VERSION,
    carId: raw.carId,
    url: raw.url,
    car: safe(() => buildCar(out), EMPTY_CAR),
    verdict: out.report.verdict,
    score: out.report.score,
    headline: buildHeadline(out.report, out.facts, frameSource),
    counts,
    dataGaps: safe(() => buildDataGaps(raw, out), []),
    facts: safe(() => buildFacts(out), []),
    repairs: safe(() => {
      const repairs = buildRepairs(out);
      return repairs && { ...repairs, sheet: safe(() => buildInspectionSheet(out), null) };
    }, null),
    issues,
    rules,
    excluded,
    timeline: safe(() => buildTimeline(raw, out), []),
  };
  const shareText = safe(() => buildShareText(report), {
    summary: [report.car.title, report.headline, report.url].join('\n'),
    sellerQuestions: null,
  });
  return { ...report, shareText };
}
