import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ALL_RULES } from '@core/rules/index.js';
import { RULE_IDS, RULE_META } from '../src/report/rule-meta';
import { rawOf } from './report/fixtures';

interface FakeAndroid {
  onReady: ReturnType<typeof vi.fn>;
  onReport: ReturnType<typeof vi.fn>;
  onEngineError: ReturnType<typeof vi.fn>;
}

const fakeAndroid = (): FakeAndroid => ({
  onReady: vi.fn(),
  onReport: vi.fn(),
  onEngineError: vi.fn(),
});

type EngineModule = typeof import('../src/engine');

/** The entry registers itself at import time, so globals must be in place first. */
const loadEngine = async (): Promise<EngineModule> => {
  vi.resetModules();
  return import('../src/engine');
};

describe('engine entry', () => {
  let android: FakeAndroid;
  let win: { Android?: FakeAndroid; avEvaluate?: (requestId: string, rawJson: string) => void };

  beforeEach(() => {
    android = fakeAndroid();
    win = { Android: android };
    vi.stubGlobal('window', win);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('registers window.avEvaluate and signals ready once', async () => {
    const engine = await loadEngine();
    expect(win.avEvaluate).toBe(engine.handleEvaluate);
    expect(android.onReady).toHaveBeenCalledTimes(1);
  });

  it('reports a schemaVersion 1 report synchronously with the same requestId', async () => {
    await loadEngine();
    win.avEvaluate!('req-7', JSON.stringify(rawOf('39339524')));

    // No await between the call and the assertion: evaluation must not defer.
    expect(android.onEngineError).not.toHaveBeenCalled();
    expect(android.onReport).toHaveBeenCalledTimes(1);
    const [id, json] = android.onReport.mock.calls[0] as [string, string];
    expect(id).toBe('req-7');
    const report = JSON.parse(json) as { schemaVersion: number; carId: string; verdict: string; score: number };
    expect(report.schemaVersion).toBe(1);
    expect(report.carId).toBe('39339524');
    expect(report.verdict).toBe('NEVER');
    expect(report.score).toBe(85);
  });

  it('reports broken JSON as an engine error for that request', async () => {
    const engine = await loadEngine();
    engine.handleEvaluate('req-8', '{not json');

    expect(android.onReport).not.toHaveBeenCalled();
    expect(android.onEngineError).toHaveBeenCalledTimes(1);
    const [id, message] = android.onEngineError.mock.calls[0] as [string, string];
    expect(id).toBe('req-8');
    expect(message).toMatch(/SyntaxError/);
  });

  it.each([
    ['null', 'null'],
    ['an array', '[]'],
    ['a number', '1'],
    ['a string', '"x"'],
    ['an object without carId', '{}'],
    ['an object with a non-string carId', '{"carId":39339524}'],
  ])('rejects %s as invalid engine input', async (_label, rawJson) => {
    const engine = await loadEngine();
    engine.handleEvaluate('req-9', rawJson);

    expect(android.onReport).not.toHaveBeenCalled();
    expect(android.onEngineError).toHaveBeenCalledTimes(1);
    const [id, message] = android.onEngineError.mock.calls[0] as [string, string];
    expect(id).toBe('req-9');
    expect(message).toMatch(/^TypeError: invalid engine input/);
  });

  it('sends the stack with the error, capped at 2000 chars', async () => {
    const engine = await loadEngine();
    engine.handleEvaluate('req-10', '[]');

    const [, message] = android.onEngineError.mock.calls[0] as [string, string];
    expect(message).toMatch(/\n\s+at /);
    expect(message.length).toBeLessThanOrEqual(2000);
  });

  describe('when the engine itself throws', () => {
    afterEach(() => {
      vi.doUnmock('../src/orchestrate-mobile');
    });

    const engineThrowing = async (thrown: unknown): Promise<EngineModule> => {
      vi.resetModules();
      vi.doMock('../src/orchestrate-mobile', () => ({
        orchestrateMobile: () => {
          throw thrown;
        },
      }));
      return import('../src/engine');
    };

    it('caps a long error message at 2000 chars', async () => {
      const engine = await engineThrowing(new Error('x'.repeat(5000)));
      engine.handleEvaluate('req-11', JSON.stringify(rawOf('39339524')));

      const [id, message] = android.onEngineError.mock.calls[0] as [string, string];
      expect(id).toBe('req-11');
      expect(message).toBe(`Error: ${'x'.repeat(5000)}`.slice(0, 2000));
    });

    it('stringifies a non-Error throw', async () => {
      const engine = await engineThrowing('boom');
      engine.handleEvaluate('req-12', JSON.stringify(rawOf('39339524')));

      expect(android.onEngineError).toHaveBeenCalledWith('req-12', 'boom');
      expect(android.onReport).not.toHaveBeenCalled();
    });
  });
});

describe('engine entry without a bridge', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('imports and evaluates safely when window is absent', async () => {
    vi.stubGlobal('window', undefined);
    const engine = await loadEngine();
    expect(() => engine.handleEvaluate('x', JSON.stringify(rawOf('41623744')))).not.toThrow();
    expect(() => engine.handleEvaluate('y', '{')).not.toThrow();
  });

  it('registers but stays silent when window.Android is missing', async () => {
    const win: { avEvaluate?: unknown } = {};
    vi.stubGlobal('window', win);
    const engine = await loadEngine();
    expect(win.avEvaluate).toBe(engine.handleEvaluate);
    expect(() => engine.handleEvaluate('z', '{')).not.toThrow();
  });
});

describe('rule registry', () => {
  // Rules are bare functions without ids, so the count is the cross-check:
  // a rule added to core must also get display meta (and an excluded reason).
  it('report rule meta covers as many rules as the engine registry', () => {
    expect(RULE_IDS.length).toBe(ALL_RULES.length);
    for (const id of RULE_IDS) expect(RULE_META[id], id).toBeDefined();
  });
});
