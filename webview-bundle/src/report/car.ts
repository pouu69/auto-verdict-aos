import type { MobileOrchestratorInput } from '../orchestrate-mobile';
import type { ReportCar } from './types';
import { groupDigits } from './format';
import { isObj, num, str } from './raw';

/** Reads `cars.base` the same way the orchestrator does (plain or wrapped state). */
const baseOf = (preloadedState: unknown): Record<string, unknown> | null => {
  if (!isObj(preloadedState)) return null;
  const root = isObj(preloadedState.__PRELOADED_STATE__) ? preloadedState.__PRELOADED_STATE__ : preloadedState;
  const cars = isObj(root.cars) ? root.cars : null;
  return cars && isObj(cars.base) ? cars.base : null;
};

/** 9500 -> "9,500km", 98000 -> "9.8만km", 120000 -> "12만km". */
const formatMileage = (km: number): string => {
  if (km < 10_000) return `${groupDigits(km)}km`;
  const man = Math.round(km / 1_000) / 10;
  return `${Number.isInteger(man) ? man : man.toFixed(1)}만km`;
};

export function buildCar(raw: MobileOrchestratorInput): ReportCar {
  const base = baseOf(raw.preloadedState);
  const category = base && isObj(base.category) ? base.category : {};
  const spec = base && isObj(base.spec) ? base.spec : {};
  const ad = base && isObj(base.advertisement) ? base.advertisement : {};

  const yearMonth = str(category, 'yearMonth');
  const yearNum = yearMonth ? Number.parseInt(yearMonth.slice(0, 4), 10) : NaN;
  const year = Number.isFinite(yearNum) ? yearNum : null;
  const mileageKm = num(spec, 'mileage');
  const priceMan = num(ad, 'price');
  const priceWon = priceMan !== null && priceMan > 0 ? priceMan * 10_000 : null;
  const fuel = str(spec, 'fuelName');

  // Same format as the saved-car title written by the old screen.
  const title =
    (base
      ? `${yearMonth?.slice(0, 4) ?? ''} ${str(category, 'manufacturerName') ?? ''} ${str(category, 'modelName') ?? ''}`.trim()
      : '') || '차량 정보 없음';

  const summary = [
    year !== null ? `${year}년` : null,
    mileageKm !== null ? formatMileage(mileageKm) : null,
    fuel,
    priceMan !== null && priceMan > 0 ? `${groupDigits(priceMan)}만원` : null,
  ]
    .filter((s): s is string => s !== null)
    .join(' · ');

  return { title, year, mileageKm, priceWon, fuel, summary };
}
