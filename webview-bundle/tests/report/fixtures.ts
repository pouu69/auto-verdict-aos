import type { MobileOrchestratorInput, MobileOrchestratorResult } from '../../src/orchestrate-mobile';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import fx39339524 from '../fixtures/encar-39339524.json';
import fx41878112 from '../fixtures/encar-41878112.json';
import fx42264483 from '../fixtures/encar-42264483.json';
import fx41623744 from '../fixtures/encar-41623744.json';

export const FIXTURE_IDS = ['39339524', '41878112', '42264483', '41623744'] as const;
export type FixtureId = (typeof FIXTURE_IDS)[number];

const RAW: Record<FixtureId, unknown> = {
  '39339524': fx39339524,
  '41878112': fx41878112,
  '42264483': fx42264483,
  '41623744': fx41623744,
};

/** Deep copy so tests can mutate the payload freely. */
export const rawOf = (id: FixtureId): MobileOrchestratorInput =>
  JSON.parse(JSON.stringify(RAW[id])) as MobileOrchestratorInput;

export const load = (
  id: FixtureId,
): { raw: MobileOrchestratorInput; out: MobileOrchestratorResult } => {
  const raw = rawOf(id);
  return { raw, out: orchestrateMobile(raw) };
};

/** Copy that must never reach users: emoji/markers and 합니다체. */
export const FORBIDDEN = /[⚠🚨🔒⏱]|합니다|습니다|\p{Extended_Pictographic}/u;
