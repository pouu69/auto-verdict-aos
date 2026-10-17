/**
 * Contract JSON shared with the Kotlin parser test (app/src/test/resources/report).
 * `npm run gen:contract` rewrites the files; a plain `vitest run` only compares,
 * so adapter changes without regenerated contract files fail here.
 */
import { describe, it, expect } from 'vitest';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { buildReport } from '../../src/report/build-report';
import { FIXTURE_IDS, load } from '../report/fixtures';

// Kept in a variable: Vite rewrites the literal `new URL(…, import.meta.url)` form as an asset import.
const here = import.meta.url;
const contractPath = (id: string): string =>
  decodeURIComponent(new URL(`../../../app/src/test/resources/report/encar-${id}.json`, here).pathname);

const generate = process.env.GEN_CONTRACT === '1';

describe('contract JSON', () => {
  for (const id of FIXTURE_IDS) {
    it(`encar-${id}.json is up to date`, () => {
      const { raw, out } = load(id);
      const text = JSON.stringify(buildReport(raw, out), null, 2) + '\n';
      const path = contractPath(id);
      if (generate) {
        writeFileSync(path, text);
        return;
      }
      const hint = 'npm run gen:contract로 다시 생성하세요';
      expect(existsSync(path), `${path} 없음 — ${hint}`).toBe(true);
      expect(readFileSync(path, 'utf8') === text, `encar-${id}.json이 어댑터 결과와 달라요 — ${hint}`).toBe(true);
    });
  }
});
