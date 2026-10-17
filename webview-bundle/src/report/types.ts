/**
 * EvaluationReport — the JSON contract between the headless rule engine and the
 * Compose result screen (spec 4). Kotlin mirror: report/EvaluationReport.kt.
 * Change both sides together and bump SCHEMA_VERSION on breaking changes.
 */
import type { Verdict } from '@core/types/RuleTypes.js';

export const SCHEMA_VERSION = 1;

export type Tone = 'danger' | 'warning' | 'success' | 'neutral';

/** Display label per engine severity: killer 위험, fail 비공개, warn 주의, pass 통과, unknown 미확인. */
export type RuleLabel = '위험' | '비공개' | '주의' | '통과' | '미확인';

export type Category = '차량 상태' | '이력' | '사고' | '가격' | '투명성';

/** One label/value line in a detail sheet. `tone` colours the value; null = plain text. */
export interface DetailRow {
  label: string;
  value: string;
  tone: Tone | null;
}

export interface DetailBlock {
  title: string | null;
  rows: DetailRow[];
  note: string | null;
}

/**
 * Bottom sheet with the full source data behind a list row (spec 4.1).
 * Amounts are exact won ("1,005,260원"); lists stay in 만원.
 */
export interface DetailSheet {
  title: string;
  subtitle: string | null;
  blocks: DetailBlock[];
}

export interface ReportCar {
  /** Saved-list format: "{year} {manufacturer} {model}", or "차량 정보 없음". */
  title: string;
  year: number | null;
  mileageKm: number | null;
  priceWon: number | null;
  fuel: string | null;
  /** One header line, e.g. "2015년 · 9.8만km · 디젤 · 1,290만원" (missing parts omitted). */
  summary: string;
}

export interface ReportCounts {
  danger: number; // killer
  hidden: number; // fail
  warning: number;
  pass: number;
  unknown: number;
  excluded: number;
}

export type GapSource = 'page' | 'record' | 'diagnosis' | 'inspection';
export type GapReason = 'failed' | 'loginRequired';

export interface DataGap {
  source: GapSource;
  reason: GapReason;
}

export type FactId = 'accidents' | 'owners' | 'usage' | 'floodLoss';

export interface Fact {
  id: FactId;
  label: string;
  value: string;
  tone: Tone;
}

export interface RepairPanel {
  name: string;
  /** "교환", "판금·용접", … (multiple joined with " · "). */
  status: string;
  /** "1랭크" | "2랭크" | "A랭크" | "B랭크" | "C랭크" | null */
  rank: string | null;
  tone: Tone;
}

export interface SystemItem {
  /** Immediate parent node title in the inners tree, e.g. "오일누유". */
  group: string;
  name: string;
  status: string;
  tone: Tone;
}

export interface Repairs {
  panels: RepairPanel[];
  systems: {
    /** Non-success leaves, danger first. */
    flagged: SystemItem[];
    okCount: number;
  };
  /** The whole 성능점검 ("성능점검 전체 보기"); null if it could not be built. */
  sheet: DetailSheet | null;
}

export interface RuleItem {
  ruleId: string;
  category: Category;
  title: string;
  tone: Tone;
  label: RuleLabel;
  message: string;
  /** One-line noun phrase for the "전체 항목" rows, shown under the title, e.g. "5회" (spec 4.0). */
  summary: string;
  /** Filled for issues (non-pass); null for pass rows. */
  why: string | null;
  sellerQuestion: string | null;
  /** Evidence, criteria and sources behind the result; filled for every rule, null only on failure. */
  detail: DetailSheet | null;
}

export interface ExcludedItem {
  ruleId: string;
  title: string;
  reason: string;
}

export interface TimelineEntry {
  /** YYYY-MM */
  date: string;
  label: string;
  detail: string | null;
  tone: Tone;
  /** Per-record sheet (accidents, owner changes, gaps, …); null for plain entries such as 신차 등록. */
  sheet: DetailSheet | null;
}

export interface ShareText {
  summary: string;
  sellerQuestions: string | null;
}

export interface EvaluationReport {
  schemaVersion: typeof SCHEMA_VERSION;
  carId: string;
  url: string;
  car: ReportCar;
  verdict: Verdict;
  score: number;
  headline: string;
  counts: ReportCounts;
  dataGaps: DataGap[];
  facts: Fact[];
  /** null when the 성능점검 could not be read. */
  repairs: Repairs | null;
  issues: RuleItem[];
  rules: RuleItem[];
  excluded: ExcludedItem[];
  timeline: TimelineEntry[];
  shareText: ShareText;
}
