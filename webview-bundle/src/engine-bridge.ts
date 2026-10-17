/**
 * Type-safe wrapper around `window.Android` on the headless engine page
 * (Kotlin `EngineBridge`). Every call is a no-op when the bridge is absent,
 * so the entry can be imported in Node tests.
 */
export interface AndroidEngineBridge {
  onReady(): void;
  onReport(requestId: string, json: string): void;
  onEngineError(requestId: string, message: string): void;
}

/** The engine page's view of `window`. */
export interface EngineWindow {
  Android?: AndroidEngineBridge;
  avEvaluate?: (requestId: string, rawJson: string) => void;
}

// Property access on globalThis (not a bare `window` reference) stays safe in Node.
const android = (): AndroidEngineBridge | undefined =>
  (globalThis as { window?: EngineWindow }).window?.Android;

export const bridge = {
  ready(): void {
    android()?.onReady();
  },
  report(requestId: string, json: string): void {
    android()?.onReport(requestId, json);
  },
  error(requestId: string, message: string): void {
    android()?.onEngineError(requestId, message);
  },
};
