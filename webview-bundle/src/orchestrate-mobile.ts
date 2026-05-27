import type { EncarParsedData } from '@core/types/ParsedData.js';
import type { ChecklistFacts } from '@core/types/ChecklistFacts.js';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { FieldStatus } from '@core/types/FieldStatus.js';
import { failed, isValue } from '@core/types/FieldStatus.js';
import { extractBase, extractDetailFlags } from '@core/parsers/encar/state.js';
import { parseRecordApi } from '@core/parsers/encar/api-record.js';
import { parseDiagnosisApi } from '@core/parsers/encar/api-diagnosis.js';
import { parseInspectionApi } from '@core/parsers/encar/api-inspection.js';
import { encarToFacts } from '@core/bridge/encar-to-facts.js';
import { evaluate } from '@core/rules/index.js';

type FetchStatus = 'ok' | 'not_found' | 'unauthorized' | 'error' | 'skipped';

export interface MobileOrchestratorInput {
  url: string;
  carId: string;
  preloadedState: unknown;
  recordJson?: unknown;
  diagnosisJson?: unknown;
  inspectionJson?: unknown;
  httpStatus?: {
    recordJson?: FetchStatus;
    diagnosisJson?: FetchStatus;
    inspectionJson?: FetchStatus;
  };
}

export interface MobileOrchestratorResult {
  parsed: EncarParsedData;
  facts: ChecklistFacts;
  report: RuleReport;
}

const reasonForStatus = (status: FetchStatus): string => {
  switch (status) {
    case 'not_found': return 'no_report_for_personal';
    case 'unauthorized': return 'login_required';
    case 'error': return 'api_fetch_error';
    case 'skipped': return 'not_fetched';
    default: return 'not_fetched';
  }
};

const resolveApi = <T>(
  json: unknown,
  status: FetchStatus | undefined,
  parse: (j: unknown) => FieldStatus<T>,
): FieldStatus<T> => {
  if (status && status !== 'ok' && status !== 'skipped') {
    return failed<T>(reasonForStatus(status));
  }
  if (json === undefined || json === null) {
    return failed<T>(status ? reasonForStatus(status) : 'not_fetched');
  }
  return parse(json);
};

const isPlainObject = (v: unknown): v is Record<string, unknown> =>
  typeof v === 'object' && v !== null && !Array.isArray(v);

const toStateRoot = (preloadedState: unknown): Parameters<typeof extractBase>[0] => {
  if (!isPlainObject(preloadedState)) return { __PRELOADED_STATE__: {} } as Parameters<typeof extractBase>[0];

  if (isPlainObject(preloadedState.__PRELOADED_STATE__)) return preloadedState as Parameters<typeof extractBase>[0];

  if (isPlainObject(preloadedState.cars)) return { __PRELOADED_STATE__: { cars: preloadedState.cars } } as Parameters<typeof extractBase>[0];

  return { __PRELOADED_STATE__: preloadedState } as Parameters<typeof extractBase>[0];
};

export const orchestrateMobile = (
  input: MobileOrchestratorInput,
): MobileOrchestratorResult => {
  const stateRoot = toStateRoot(input.preloadedState);

  const base = extractBase(stateRoot);
  const detailFlags = extractDetailFlags(stateRoot);

  const recordApi = resolveApi(
    input.recordJson,
    input.httpStatus?.recordJson,
    parseRecordApi,
  );
  const diagnosisApi = resolveApi(
    input.diagnosisJson,
    input.httpStatus?.diagnosisJson,
    parseDiagnosisApi,
  );
  const inspectionApi = resolveApi(
    input.inspectionJson,
    input.httpStatus?.inspectionJson,
    parseInspectionApi,
  );

  const baseValue = isValue(base) ? base.value : undefined;
  const rawState = isPlainObject(input.preloadedState) ? input.preloadedState : {};
  const rawCars = isPlainObject(rawState.cars) ? rawState.cars : {};
  const rawBase = isPlainObject(rawCars.base) ? rawCars.base : {};
  const vehicleId = typeof rawBase.vehicleId === 'number' ? rawBase.vehicleId : undefined;
  const vehicleNo = baseValue?.vehicleNo;

  const parsed: EncarParsedData = {
    schemaVersion: 1,
    source: 'encar',
    url: input.url,
    carId: input.carId,
    ...(vehicleId !== undefined ? { vehicleId } : {}),
    ...(vehicleNo !== undefined ? { vehicleNo } : {}),
    fetchedAt: Date.now(),
    loginState: 'unknown',
    raw: { base, detailFlags, recordApi, diagnosisApi, inspectionApi },
  };

  const facts = encarToFacts(parsed);
  const report = evaluate(facts);

  return { parsed, facts, report };
};
