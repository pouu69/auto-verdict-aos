import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { ReportCar } from './types';
import { groupDigits } from './format';
import { count } from './raw';

export const NO_CAR_TITLE = '차량 정보 없음';

/** 9500 -> "9,500km", 98000 -> "9.8만km", 120000 -> "12만km". */
const formatMileage = (km: number): string => {
  if (km < 10_000) return `${groupDigits(km)}km`;
  const man = Math.round(km / 1_000) / 10;
  return `${Number.isInteger(man) ? man : man.toFixed(1)}만km`;
};

/** A price in 만원 whose won value stays exact. */
const priceInMan = (n: unknown): number | null => {
  const man = count(n);
  return man !== null && man > 0 && man * 10_000 <= Number.MAX_SAFE_INTEGER ? man : null;
};
const text = (s: unknown): string | null => (typeof s === 'string' && s.trim().length > 0 ? s : null);

/** Longest display name that still takes the grade detail ("일반형"). */
const MAX_NAME_LENGTH = 30;

/** Joins the known parts with single spaces; inner runs of spaces are collapsed. */
const joinParts = (parts: unknown[]): string =>
  parts
    .map((p) => (typeof p === 'string' ? p.replace(/\s+/g, ' ').trim() : ''))
    .filter((p) => p.length > 0)
    .join(' ');

export const EMPTY_CAR: ReportCar = {
  title: NO_CAR_TITLE,
  name: NO_CAR_TITLE,
  year: null,
  mileageKm: null,
  priceWon: null,
  fuel: null,
  summary: '',
};

export function buildCar(out: MobileOrchestratorResult): ReportCar {
  const base = out.parsed.raw.base;
  if (base.kind !== 'value') return EMPTY_CAR;
  // Typed as EncarCarBase but read defensively: listings omit fields.
  const category = base.value.category ?? {};
  const spec = base.value.spec ?? {};
  const ad = base.value.advertisement ?? {};

  const yearMonth = text(category.yearMonth);
  const yearNum = yearMonth ? Number.parseInt(yearMonth.slice(0, 4), 10) : NaN;
  const year = Number.isFinite(yearNum) ? yearNum : null;
  const mileageKm = count(spec.mileage);
  const priceMan = priceInMan(ad.price);
  const fuel = text(spec.fuelName);

  // Same format as the saved-car title written by the old screen.
  const title =
    `${yearMonth?.slice(0, 4) ?? ''} ${text(category.manufacturerName) ?? ''} ${text(category.modelName) ?? ''}`.trim() ||
    NO_CAR_TITLE;

  const shortName = joinParts([category.manufacturerName, category.modelName, category.gradeName]);
  // The grade detail alone ("일반형") names nothing, so it only ever follows a name.
  const fullName = shortName ? joinParts([shortName, category.gradeDetailName]) : '';
  const name = (fullName.length <= MAX_NAME_LENGTH ? fullName : shortName) || NO_CAR_TITLE;

  const summary = [
    year !== null ? `${year}년` : null,
    mileageKm !== null ? formatMileage(mileageKm) : null,
    fuel,
    priceMan !== null ? `${groupDigits(priceMan)}만원` : null,
  ]
    .filter((s): s is string => s !== null)
    .join(' · ');

  return { title, name, year, mileageKm, priceWon: priceMan !== null ? priceMan * 10_000 : null, fuel, summary };
}
