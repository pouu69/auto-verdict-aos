/**
 * Copy rules over every detail sheet string (spec 4.1 / 6): no emoji or
 * 합니다체, no raw codes, nothing identifying (plate, VIN, dealer, URLs).
 */
import { describe, it, expect } from 'vitest';
import type { MobileOrchestratorInput } from '../../src/orchestrate-mobile';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildReport } from '../../src/report/build-report';
import type { DetailSheet, EvaluationReport } from '../../src/report/types';
import { FIXTURE_IDS, FORBIDDEN, rawOf } from './fixtures';

const PLATE = /\d{2,3}[가-힣]\s?\d{4}/;
const VIN = 'KMHXX81CBXU123456';
const DEALER = ['테스트상사', '홍길동', '010-1234-5678', '서울 테스트구 테스트로 1'];
const CODES = /RANK_|NORMAL|REPLACEMENT|EXCHANGE|_COMMENT|[A-Z]{2,}_[A-Z]{2,}|undefined|null|NaN|\[object/;

/** A fixture with identifying data put back in, to prove the sheets never read it. */
const withIdentity = (raw: MobileOrchestratorInput): MobileOrchestratorInput => {
  const cars = (raw.preloadedState as { cars: { base: Record<string, unknown> } }).cars;
  Object.assign(cars.base, {
    vin: VIN,
    vehicleNo: '12가3456',
    contact: { address: DEALER[3], phone: DEALER[2], userType: 'DEALER' },
    partnership: { dealer: { name: DEALER[1], shop: DEALER[0] } },
  });
  if (raw.recordJson && typeof raw.recordJson === 'object') {
    Object.assign(raw.recordJson, {
      carNo: '12가3456',
      carInfoChanges: [
        { date: '2010-01-01', carNo: '12가3456' },
        { date: '2020-01-01', carNo: '34나5678' },
      ],
    });
  }
  return raw;
};

const sheetsOf = (r: EvaluationReport): DetailSheet[] =>
  [
    ...r.rules.map((x) => x.detail),
    ...r.issues.map((x) => x.detail),
    ...r.timeline.map((x) => x.sheet),
    r.repairs?.sheet ?? null,
  ].filter((s): s is DetailSheet => s !== null);

const stringsOf = (s: DetailSheet): string[] => [
  s.title,
  ...(s.subtitle ? [s.subtitle] : []),
  ...s.blocks.flatMap((b) => [...(b.title ? [b.title] : []), ...(b.note ? [b.note] : []), ...b.rows.flatMap((r) => [r.label, r.value])]),
];

describe('detail sheet copy', () => {
  for (const id of FIXTURE_IDS) {
    it(`encar-${id}: every string follows the copy rules`, () => {
      const raw = withIdentity(rawOf(id));
      const report = buildReport(raw, orchestrateMobile(raw));
      const sheets = sheetsOf(report);
      expect(sheets.length).toBeGreaterThan(0);
      for (const s of sheets) {
        for (const text of stringsOf(s)) {
          expect(text.trim().length, `${s.title}: empty string`).toBeGreaterThan(0);
          expect(text, s.title).not.toMatch(FORBIDDEN);
          expect(text, s.title).not.toMatch(CODES);
          expect(text, s.title).not.toMatch(PLATE);
          expect(text, s.title).not.toMatch(/https?:|\.jpe?g|\.png/i);
          expect(text, s.title).not.toContain(VIN);
          for (const d of DEALER) expect(text, s.title).not.toContain(d);
        }
      }
    });
  }
});
