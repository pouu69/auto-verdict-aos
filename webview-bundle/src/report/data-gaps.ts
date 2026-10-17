import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../orchestrate-mobile';
import type { DataGap, GapReason, GapSource } from './types';

const API_SOURCES: Array<[keyof NonNullable<MobileOrchestratorInput['httpStatus']>, GapSource]> = [
  ['recordJson', 'record'],
  ['diagnosisJson', 'diagnosis'],
  ['inspectionJson', 'inspection'],
];

/**
 * Data we tried to collect but could not. `not_found` (no report exists for
 * this car), `skipped` and an absent status are not gaps.
 */
export function buildDataGaps(raw: MobileOrchestratorInput, out: MobileOrchestratorResult): DataGap[] {
  const gaps: DataGap[] = [];
  if (out.parsed.raw.base.kind !== 'value') gaps.push({ source: 'page', reason: 'failed' });
  for (const [key, source] of API_SOURCES) {
    const status = raw.httpStatus?.[key];
    const reason: GapReason | null =
      status === 'error' ? 'failed' : status === 'unauthorized' ? 'loginRequired' : null;
    if (reason) gaps.push({ source, reason });
  }
  return gaps;
}
