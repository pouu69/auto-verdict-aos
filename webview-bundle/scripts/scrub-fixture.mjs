#!/usr/bin/env node
// Turns a raw collector payload (CacheEntity.rawInputJson) into a test fixture with
// identifying data removed. Usage: node scripts/scrub-fixture.mjs <in.json> <out.json>
//
// preloadedState is reduced to what the core reads (cars.base + cars.detailFlags);
// everything else (dealer contact, address, photos, login state…) is dropped.
import { readFileSync, writeFileSync } from 'node:fs';

const DROP_KEYS = new Set([
  // vehicle / document identifiers
  'vehicleNo', 'vin', 'carNo', 'recordNo', 'supplyNum', 'ordNo', 'diagnosisNo', 'motorType',
  // people, places, accounts
  'contact', 'address', 'profile', 'inspName', 'noticeName', 'registrantId', 'updaterId',
  'reservationId', 'reservationCenterName', 'centerCode', 'tradeCompanyName', 'performTester',
  // media
  'photos', 'photo', 'images', 'innerPanorama', 'underBodyPhotos',
  // irrelevant page plumbing
  'requestUrl', 'view', 'partnership', 'options', 'contents',
]);

const BASE_KEEP = ['manage', 'category', 'advertisement', 'spec', 'condition', 'vehicleType', 'vehicleId'];

const scrub = (v) => {
  if (Array.isArray(v)) return v.map(scrub);
  if (v && typeof v === 'object') {
    const out = {};
    for (const [k, child] of Object.entries(v)) {
      if (DROP_KEYS.has(k)) continue;
      out[k] = scrub(child);
    }
    return out;
  }
  if (typeof v === 'string' && /^https?:\/\//.test(v)) return '';
  return v;
};

const [, , inPath, outPath] = process.argv;
if (!inPath || !outPath) {
  console.error('usage: scrub-fixture.mjs <in.json> <out.json>');
  process.exit(1);
}
const raw = JSON.parse(readFileSync(inPath, 'utf8'));
const cars = raw.preloadedState?.cars ?? {};
const base = Object.fromEntries(BASE_KEEP.filter((k) => cars.base && k in cars.base).map((k) => [k, cars.base[k]]));
const out = {
  url: raw.url,
  carId: raw.carId,
  preloadedState: scrub({ cars: { base, detailFlags: cars.detailFlags } }),
  ...(raw.recordJson !== undefined && { recordJson: scrub(raw.recordJson) }),
  ...(raw.diagnosisJson !== undefined && { diagnosisJson: scrub(raw.diagnosisJson) }),
  ...(raw.inspectionJson !== undefined && { inspectionJson: scrub(raw.inspectionJson) }),
  httpStatus: raw.httpStatus,
};
writeFileSync(outPath, JSON.stringify(out, null, 2) + '\n');
