import type { EncarParsedData } from '@core/types/ParsedData.js';
import { isValue } from '@core/types/FieldStatus.js';
import { getFrameIntact } from '@core/parsers/encar/api-diagnosis.js';
import { getFrameFromInspection } from '@core/parsers/encar/api-inspection.js';

/** Which collected document decided R04's `frameDamage`. */
export type FrameSource = 'diagnosis' | 'inspection' | 'unknown';

/**
 * Mirrors the R04 layering in core `encarToFacts` (bridge/encar-to-facts.ts):
 * the diagnosis checker verdict wins when conclusive, then the 성능점검
 * `master.accdient` flag. The ribbon fallback only ever yields "intact", so it
 * never backs a killer and is reported as 'unknown'.
 */
export function frameDamageSource(raw: EncarParsedData['raw']): FrameSource {
  const dia = raw.diagnosisApi;
  if (isValue(dia) && getFrameIntact(dia.value) !== undefined) return 'diagnosis';
  const ins = raw.inspectionApi;
  if (isValue(ins) && getFrameFromInspection(ins.value)) return 'inspection';
  return 'unknown';
}
