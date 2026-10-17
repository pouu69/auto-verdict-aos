/**
 * Headless rule engine entry (spec 3.1, 3.3), built as an IIFE and loaded by
 * `engine.html`. Native calls `window.avEvaluate(requestId, rawJson)` and gets
 * exactly one `onReport` or `onEngineError` back for that requestId.
 *
 * Evaluation is fully synchronous: an off-screen WebView throttles timers,
 * so nothing here may defer through setTimeout, Promises or rAF.
 */
import type { MobileOrchestratorInput } from './orchestrate-mobile';
import { orchestrateMobile } from './orchestrate-mobile';
import { buildReport } from './report/build-report';
import type { EngineWindow } from './engine-bridge';
import { bridge } from './engine-bridge';

/** Native logs this; the stack is what makes an on-device failure traceable. */
const MAX_ERROR_LENGTH = 2000;

const errorMessage = (e: unknown): string =>
  (e instanceof Error ? (e.stack ?? e.message) : String(e)).slice(0, MAX_ERROR_LENGTH);

/** Rejects payloads the collector could never send, before they reach the parsers. */
const parseInput = (rawJson: string): MobileOrchestratorInput => {
  const raw: unknown = JSON.parse(rawJson);
  if (
    typeof raw !== 'object' ||
    raw === null ||
    Array.isArray(raw) ||
    typeof (raw as { carId?: unknown }).carId !== 'string'
  ) {
    throw new TypeError('invalid engine input');
  }
  return raw as MobileOrchestratorInput;
};

export function handleEvaluate(requestId: string, rawJson: string): void {
  let json: string;
  try {
    const raw = parseInput(rawJson);
    json = JSON.stringify(buildReport(raw, orchestrateMobile(raw)));
  } catch (e) {
    bridge.error(requestId, errorMessage(e));
    return;
  }
  // Outside the try so a throwing bridge cannot also send an error for this id.
  bridge.report(requestId, json);
}

// Guarded so Node tests can import this module without a window.
if (typeof window !== 'undefined') {
  (window as unknown as EngineWindow).avEvaluate = handleEvaluate;
  bridge.ready();
}
