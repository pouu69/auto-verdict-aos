/**
 * Engine output -> EvaluationReport (spec 4). Pure; the verdict and score are
 * passed through untouched, everything else is display copy and grouping.
 */
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { EvaluationReport, ExcludedItem, ReportCounts, RuleItem } from './types';
import { SCHEMA_VERSION } from './types';
import { CATEGORY_ORDER, RULE_IDS, RULE_META } from './rule-meta';
import { excludedReason, labelFor, ruleCopy, toneFor, unknownMessage } from './rule-copy';
import { buildFacts } from './facts';
import { buildRepairs } from './repairs';
import { buildTimeline } from './timeline';
import { buildDataGaps } from './data-gaps';
import { buildHeadline } from './headline';
import { buildCar } from './car';
import { buildShareText } from './share-text';

const ruleNo = (id: string): number => Number(id.slice(1));

/** Issue order: 위험 → 비공개 → 주의 → 미확인. */
const ISSUE_RANK: Record<Severity, number> = { killer: 0, fail: 1, warn: 2, unknown: 3, pass: 4 };

const toItem = (r: RuleResult, out: MobileOrchestratorResult): RuleItem => {
  const meta = RULE_META[r.ruleId];
  const tone = toneFor(r.severity);
  const copy = r.severity === 'unknown' ? null : ruleCopy(r, out.facts);
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
  };
};

export function buildReport(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): EvaluationReport {
  const results = out.report.results;
  const severityOf = new Map(results.map((r) => [r.ruleId, r.severity]));

  const catIndex = (r: RuleResult) => CATEGORY_ORDER.indexOf(RULE_META[r.ruleId]?.category ?? '투명성');
  const sorted = [...results].sort((a, b) => catIndex(a) - catIndex(b) || ruleNo(a.ruleId) - ruleNo(b.ruleId));
  const rules = sorted.map((r) => toItem(r, out));

  const issues = rules
    .filter((i) => i.tone !== 'success')
    .sort(
      (a, b) =>
        ISSUE_RANK[severityOf.get(a.ruleId)!] - ISSUE_RANK[severityOf.get(b.ruleId)!] ||
        ruleNo(a.ruleId) - ruleNo(b.ruleId),
    );

  const excluded: ExcludedItem[] = RULE_IDS.filter((id) => !severityOf.has(id)).map((id) => ({
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
    car: buildCar(raw),
    verdict: out.report.verdict,
    score: out.report.score,
    headline: buildHeadline(out.report, out.facts),
    counts,
    dataGaps: buildDataGaps(raw, out),
    facts: buildFacts(raw, out),
    repairs: buildRepairs(raw),
    issues,
    rules,
    excluded,
    timeline: buildTimeline(raw),
  };
  return { ...report, shareText: buildShareText(report) };
}
