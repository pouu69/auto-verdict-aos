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

export function handleEvaluate(requestId: string, rawJson: string): void {
  let json: string;
  try {
    const raw = JSON.parse(rawJson) as MobileOrchestratorInput;
    json = JSON.stringify(buildReport(raw, orchestrateMobile(raw)));
  } catch (e) {
    bridge.error(requestId, String(e));
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
