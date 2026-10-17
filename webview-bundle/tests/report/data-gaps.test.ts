import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../../src/orchestrate-mobile';
import { buildDataGaps } from '../../src/report/data-gaps';
import { FIXTURE_IDS, load, rawOf } from './fixtures';

describe('buildDataGaps', () => {
  it('fixtures have no gaps (404 means the report does not exist, not a gap)', () => {
    for (const id of FIXTURE_IDS) {
      const { raw, out } = load(id);
      expect(buildDataGaps(raw, out), id).toEqual([]);
    }
  });

  it('maps unauthorized and error per API, in source order', () => {
    const raw = rawOf('41878112');
    raw.httpStatus = { ...raw.httpStatus, recordJson: 'unauthorized', inspectionJson: 'error' };
    expect(buildDataGaps(raw, orchestrateMobile(raw))).toEqual([
      { source: 'record', reason: 'loginRequired' },
      { source: 'inspection', reason: 'failed' },
    ]);
  });

  it('skipped or missing status is not a gap', () => {
    const raw = rawOf('41878112');
    raw.httpStatus = { recordJson: 'skipped' };
    expect(buildDataGaps(raw, orchestrateMobile(raw))).toEqual([]);
    delete raw.httpStatus;
    expect(buildDataGaps(raw, orchestrateMobile(raw))).toEqual([]);
  });

  it('page base missing -> page failed first', () => {
    const raw = rawOf('41878112');
    raw.preloadedState = {};
    raw.httpStatus = { diagnosisJson: 'error' };
    expect(buildDataGaps(raw, orchestrateMobile(raw))).toEqual([
      { source: 'page', reason: 'failed' },
      { source: 'diagnosis', reason: 'failed' },
    ]);
  });
});
