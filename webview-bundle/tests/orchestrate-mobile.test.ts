import { describe, it, expect } from 'vitest';
import { orchestrateMobile } from '../src/orchestrate-mobile';

describe('orchestrateMobile', () => {
  it('produces a valid RuleReport from minimal preloaded state', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/41623743',
      carId: '41623743',
      preloadedState: {
        cars: {
          base: {
            category: {
              manufacturerName: '현대',
              modelName: '투싼',
              yearMonth: '202201',
              domestic: true,
            },
            advertisement: { price: 2500 },
            spec: { mileage: 35000 },
            contact: { userType: 'DEALER' },
          },
          detailFlags: {
            isInsuranceExist: true,
            isHistoryView: true,
            isDiagnosisExist: false,
            isDealer: true,
          },
        },
      },
    });

    expect(result.report.score).toBeGreaterThanOrEqual(0);
    expect(result.report.score).toBeLessThanOrEqual(100);
    expect(result.report.verdict).toBeDefined();
    expect(result.parsed.carId).toBe('41623743');
    expect(result.parsed.source).toBe('encar');
    expect(result.facts.schemaVersion).toBe(1);
    expect(result.facts.insuranceHistoryDisclosed).toEqual({
      kind: 'value',
      value: true,
    });
  });

  it('handles record API data for R05-R10', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/123',
      carId: '123',
      preloadedState: {
        cars: {
          base: {
            category: { manufacturerName: '기아', modelName: '스포티지', yearMonth: '202101', domestic: true },
            advertisement: { price: 2000 },
            spec: { mileage: 50000 },
            contact: { userType: 'DEALER' },
          },
          detailFlags: { isInsuranceExist: true, isHistoryView: true, isDiagnosisExist: false, isDealer: true },
        },
      },
      recordJson: {
        myAccidentCnt: 0,
        otherAccidentCnt: 0,
        ownerChangeCnt: 1,
        robberCnt: 0,
        totalLossCnt: 0,
        floodTotalLossCnt: 0,
        floodPartLossCnt: 0,
        government: 0,
        business: 0,
        loan: 1,
        carNoChangeCnt: 0,
        myAccidentCost: 0,
        otherAccidentCost: 0,
      },
      httpStatus: { recordJson: 'ok' },
    });

    expect(result.facts.usageHistory).toEqual({
      kind: 'value',
      value: { rental: true, taxi: false, business: false },
    });
    const r05 = result.report.results.find((r) => r.ruleId === 'R05');
    expect(r05?.severity).toBe('killer');
  });

  it('handles missing preloaded state gracefully', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/999',
      carId: '999',
      preloadedState: null,
    });

    expect(result.report.verdict).toBe('UNKNOWN');
    expect(result.parsed.raw.base.kind).toBe('parse_failed');
  });

  it('marks personal listing diagnosis as not applicable', () => {
    const result = orchestrateMobile({
      url: 'https://fem.encar.com/cars/detail/456',
      carId: '456',
      preloadedState: {
        cars: {
          base: {
            category: { manufacturerName: 'BMW', modelName: '530i', yearMonth: '201901', domestic: false },
            advertisement: { price: 3500 },
            spec: { mileage: 60000 },
            contact: { userType: 'CLIENT' },
          },
          detailFlags: { isInsuranceExist: true, isHistoryView: false, isDiagnosisExist: false, isDealer: false },
        },
      },
      httpStatus: { diagnosisJson: 'not_found', inspectionJson: 'not_found' },
    });

    expect(result.facts.hasEncarDiagnosis).toEqual({
      kind: 'parse_failed',
      reason: 'not_applicable_personal',
    });
  });
});
