/**
 * Per-rule detail sheet (spec 4.1): ① the source data the rule looked at,
 * ② the thresholds in plain words, ③ which documents were read and how.
 * 미확인 rules replace ① with the reason they could not be checked.
 */
import type { RuleResult, Severity } from '@core/types/RuleTypes.js';
import type { MobileOrchestratorResult } from '../../orchestrate-mobile';
import type { DetailBlock, DetailRow, DetailSheet } from '../types';
import { RULE_META } from '../rule-meta';
import type { FrameSource } from '../frame-source';
import { formatMan, koreanMonth } from '../format';
import { arr, isObj, num, type Obj } from '../raw';
import { reasonShort } from '../reasons';
import { unknownStatus } from '../rule-copy';
import { walkSystems, codeTone } from '../repairs';
import { accidentRecords } from '../timeline';
import { block, duration, fullDate, inspectionOf, recordOf, row, sourceBlock, sourcesOk, times, won, type SourceKey } from './common';
import { diagnosisBlocks } from './diagnosis';
import { accidentRows, panelBlock } from './inspection';
import { accidentBlocks } from './timeline';

/**
 * Thresholds restated from core rules/index.ts, where they are module-private
 * (R07_OWNER_WARN_THRESHOLD, R10_MILD_CEILING_DOMESTIC/IMPORT,
 * R11_SUSPICIOUSLY_LOW_RATIO, R11_OVERPRICED_RATIO). tests/report/details-rules
 * pins them against the engine so a core change fails loudly.
 */
export const CRITERIA = {
  ownerWarnAbove: 2,
  mildCeilingDomestic: 2_000_000,
  mildCeilingImport: 4_000_000,
  lowRatio: 0.45,
  highRatio: 1.15,
} as const;

/** Core api-inspection.ts `OIL_LEAK_CODES` (module-private): 6 미세누유, 7 누유. */
const OIL_LEAK_CODES = new Set(['6', '7']);

const RULE_SOURCES: Record<string, SourceKey[]> = {
  R01: ['page'],
  R02: ['page'],
  R03: ['page', 'diagnosis'],
  R04: ['diagnosis', 'inspection', 'page'],
  R05: ['record'],
  R06: ['record'],
  R07: ['record'],
  R08: ['record'],
  R09: ['record'],
  R10: ['record', 'page'],
  R11: ['page'],
  R12: ['inspection'],
};

const pct = (ratio: number): string => `${Math.round(ratio * 100)}%`;

const pass = (v: string): DetailRow => row('통과', v, 'success');
const warn = (v: string): DetailRow => row('주의', v, 'warning');
const danger = (v: string): DetailRow => row('위험', v, 'danger');
const hidden = (v: string): DetailRow => row('비공개', v, 'danger');
const skipped = (v: string): DetailRow => row('평가 제외', v);

const criteriaBlock = (ruleId: string): DetailBlock | null => {
  const c = CRITERIA;
  const ceilings = `국산 ${formatMan(c.mildCeilingDomestic)}·수입 ${formatMan(c.mildCeilingImport)}`;
  switch (ruleId) {
    case 'R01':
      return block('판정 기준', [pass('보험이력 공개'), hidden('보험이력 비공개')]);
    case 'R02':
      return block('판정 기준', [pass('성능점검 기록 공개'), hidden('성능점검 기록 비공개')]);
    case 'R03':
      return block('판정 기준', [pass('엔카진단을 받은 차'), skipped('엔카진단을 받지 않은 차(감점 없음)')]);
    case 'R04':
      return block(
        '판정 기준',
        [
          danger('엔카진단에서 프레임 손상 판정 또는 성능점검 사고 이력 있음'),
          pass('둘 다 없음(외판 단순 교환·수리만 있으면 감점 없음)'),
        ],
        '엔카진단 판정을 먼저 보고, 판정이 분명하지 않으면 성능점검의 사고 이력을 봐요',
      );
    case 'R05':
      return block('판정 기준', [danger('렌트·영업용 이력이 1회라도 있음'), warn('관용 이력이 1회라도 있음'), pass('세 가지 모두 없음')]);
    case 'R06':
      return block('판정 기준', [danger('전손·침수전손·침수분손·도난 중 1회라도 있음'), pass('모두 없음')]);
    case 'R07':
      return block('판정 기준', [pass(`${c.ownerWarnAbove}회 이하`), warn(`${c.ownerWarnAbove + 1}회 이상`)]);
    case 'R08':
      return block('판정 기준', [warn('자차보험 미가입 기간이 1개월이라도 있음'), pass('미가입 기간 없음')]);
    case 'R09':
      return block('판정 기준', [warn('수리비가 확정되지 않은 사고가 있음'), pass('없음')]);
    case 'R10':
      return block('판정 기준', [pass(`내차·타차 수리비 중 큰 쪽이 ${ceilings} 이하`), warn(`${ceilings} 초과`)]);
    case 'R11':
      return block('판정 기준', [
        warn(`신차 가격의 ${pct(c.lowRatio)} 미만 · 저가 의심`),
        warn(`신차 가격의 ${pct(c.highRatio)} 초과 · 고가`),
        pass(`신차 가격의 ${pct(c.lowRatio)}~${pct(c.highRatio)}`),
        skipped('신차 가격 정보 없음'),
      ]);
    case 'R12':
      return block('판정 기준', [warn('미세누유·누유 항목이 하나라도 있음'), pass('누유 항목 없음'), skipped('성능점검 기록 없음')]);
    default:
      return null;
  }
};

// ── ① 판단 근거 ───────────────────────────────────────────────

type Ctx = { result: RuleResult; out: MobileOrchestratorResult; frameSource: FrameSource };

const disclosureBlock = (out: MobileOrchestratorResult): DetailBlock[] => {
  const flags = out.parsed.raw.detailFlags;
  if (flags.kind !== 'value') return [];
  const f = flags.value;
  return [
    block(
      '딜러 공개 설정',
      [
        row('보험이력', f.isInsuranceExist ? '공개' : '비공개', f.isInsuranceExist ? 'success' : 'danger'),
        row('성능점검', f.isHistoryView ? '공개' : '비공개', f.isHistoryView ? 'success' : 'danger'),
      ],
      '딜러가 매물을 올릴 때 정하는 설정이에요',
    ),
  ];
};

const r03 = ({ out }: Ctx): DetailBlock[] => {
  const flags = out.parsed.raw.detailFlags;
  const rows = flags.kind === 'value' ? [row('엔카진단', flags.value.isDiagnosisExist ? '있음' : '없음', flags.value.isDiagnosisExist ? 'success' : null)] : [];
  return [block('엔카진단 여부', rows), ...(diagnosisBlocks(out) ?? [])];
};

const FRAME_SOURCE_NAME: Record<FrameSource, string> = {
  diagnosis: '엔카진단',
  inspection: '성능점검',
  unknown: '엔카진단 통과 표시',
};

/** `master.accdient` of the 성능점검, or null when it was not read or carries no flag. */
const inspectionAccident = (ins: Obj | null): boolean | null => {
  const master = ins && isObj(ins.master) ? ins.master : null;
  return master && typeof master.accdient === 'boolean' ? master.accdient : null;
};

const r04 = ({ out, frameSource }: Ctx): DetailBlock[] => {
  const frame = out.facts.frameDamage;
  const ins = inspectionOf(out);
  // The source itself is the sheet subtitle ("근거: 성능점검"). A 성능점검 사고 이력
  // also covers welded outer panels (quarter panels), so it is not called 프레임 손상.
  const rows: DetailRow[] = [];
  if (frame.kind === 'value') {
    const label = frameSource === 'inspection' ? '성능점검 사고 이력' : '프레임 손상';
    rows.push(row(label, frame.value.hasDamage ? '있음' : '없음', frame.value.hasDamage ? 'danger' : 'success'));
  }
  let note: string | null = null;
  const accident = inspectionAccident(ins);
  if (frameSource === 'inspection' && out.parsed.raw.diagnosisApi.kind === 'value') {
    note = '엔카진단 판정이 프레임 손상 여부를 분명히 밝히지 않아 성능점검을 근거로 썼어요';
  } else if (frameSource === 'diagnosis' && frame.kind === 'value' && accident !== null && accident !== frame.value.hasDamage) {
    // The 성능점검 rows below disagree with the verdict; say which one won.
    note = frame.value.hasDamage
      ? '성능점검에는 사고 이력이 없지만, 엔카진단이 프레임 손상으로 판정해 엔카진단을 따랐어요'
      : '성능점검에는 사고 이력이 있지만, 엔카진단이 프레임 손상 없음으로 판정해 엔카진단을 따랐어요';
  } else if (frameSource === 'unknown') {
    note = '엔카진단을 통과한 차는 프레임 손상이 없는 차로 봐요';
  }
  const blocks = [block('판정 근거', rows, note)];
  if (ins) blocks.push(block('성능점검', accidentRows(ins)), panelBlock(out));
  blocks.push(...(diagnosisBlocks(out) ?? []));
  return blocks;
};

const r05 = ({ out }: Ctx): DetailBlock[] => {
  const rec = recordOf(out);
  const u = out.facts.usageHistory;
  if (!rec || u.kind !== 'value') return [];
  const tags: string[] = [];
  if (u.value.rental) tags.push('렌트');
  if (u.value.taxi) tags.push('영업용');
  if (u.value.business) tags.push('관용');
  const usageTone = u.value.rental || u.value.taxi ? 'danger' : u.value.business ? 'warning' : null;
  const count = (label: string, key: string, bad: 'danger' | 'warning'): DetailRow => {
    const n = num(rec, key) ?? 0;
    return row(label, times(n), n > 0 ? bad : 'success');
  };
  return [
    block('용도 이력', [
      // Same wording as the 용도 fact tile.
      row('용도', tags.length > 0 ? tags.join('·') : '자가용', usageTone),
      count('렌트 이력', 'loan', 'danger'),
      count('영업용 이력', 'business', 'danger'),
      count('관용 이력', 'government', 'warning'),
    ]),
  ];
};

const r06 = ({ out }: Ctx): DetailBlock[] => {
  const rec = recordOf(out);
  const l = out.facts.totalLossHistory;
  if (!rec || l.kind !== 'value') return [];
  const count = (label: string, n: number): DetailRow => row(label, times(n), n > 0 ? 'danger' : 'success');
  const rows = [
    count('전손', l.value.totalLoss),
    count('침수전손', l.value.floodTotal),
    count('침수분손', l.value.floodPart),
    count('도난', l.value.robber),
  ];
  for (const [label, key] of [['전손 날짜', 'totalLossDate'], ['침수 날짜', 'floodDate'], ['도난 날짜', 'robberDate']] as const) {
    const d = fullDate(rec[key]);
    if (d) rows.push(row(label, d));
  }
  return [block('전손·침수·도난', rows)];
};

const r07 = ({ result, out }: Ctx): DetailBlock[] => {
  const rec = recordOf(out);
  const n = out.facts.ownerChangeCount;
  if (!rec || n.kind !== 'value') return [];
  const dates = arr(rec.ownerChanges)
    .map(fullDate)
    .filter((d): d is string => d !== null)
    .sort()
    .reverse();
  const tone = result.severity === 'warn' ? 'warning' : 'success';
  const blocks = [block('소유자 변경', [row('변경 횟수', times(n.value), tone)])];
  if (dates.length > 0) {
    blocks.push(
      block(
        '변경 날짜',
        dates.map((d, i) => row(`${dates.length - i}번째 변경`, d)),
        dates.length !== n.value ? '보험이력에 날짜가 남은 변경만 보여요' : null,
      ),
    );
  }
  return blocks;
};

const r08 = ({ out }: Ctx): DetailBlock[] => {
  const g = out.facts.insuranceGap;
  if (g.kind !== 'value') return [];
  const note = '자차보험에 가입하지 않은 기간이에요. 이 기간에 난 사고는 보험이력에 남지 않아요';
  if (g.value.periods.length === 0) return [block('보험 미가입 기간', [row('미가입 기간', '없음', 'success')], note)];
  // Newest first, like the R07 change dates ("YYYY-MM" sorts as text).
  const periods = [...g.value.periods].sort((a, b) => (a.from < b.from ? 1 : a.from > b.from ? -1 : 0));
  const rows = periods.map((p) => row(`${koreanMonth(p.from)}~${koreanMonth(p.to)}`, duration(p.months), 'warning'));
  rows.push(row('합계', duration(g.value.totalMonths), 'warning'));
  return [block('보험 미가입 기간', rows, note)];
};

const r09 = ({ out }: Ctx): DetailBlock[] => {
  const rec = recordOf(out);
  const u = out.facts.unconfirmedAccident;
  if (!rec || u.kind !== 'value') return [];
  // Only what the engine decided plus the claim count; per-claim amounts are in the R10 sheet.
  const claims = accidentRecords(rec).length;
  return [
    block('수리비 확정', [
      row('수리비 미확정 사고', u.value ? '있음' : '없음', u.value ? 'warning' : 'success'),
      row('보험 처리 사고', claims > 0 ? `${claims}건` : '없음'),
    ]),
  ];
};

const r10 = ({ out }: Ctx): DetailBlock[] => {
  const rec = recordOf(out);
  const d = out.facts.minorAccidents;
  if (!rec || d.kind !== 'value') return [];
  const ceiling = d.value.domestic ? CRITERIA.mildCeilingDomestic : CRITERIA.mildCeilingImport;
  const side = (label: string, cntKey: string, amount: number): DetailRow => {
    const cnt = num(rec, cntKey) ?? 0;
    const value = cnt > 0 ? `${cnt}건 · ${won(amount)}` : amount > 0 ? won(amount) : '없음';
    return row(label, value, amount > ceiling ? 'warning' : null);
  };
  return [
    block(
      '수리비 합계',
      [
        side('내차 피해', 'myAccidentCnt', d.value.ownDamageWon),
        side('타차 가해', 'otherAccidentCnt', d.value.otherDamageWon),
        row('기준 금액', `${d.value.domestic ? '국산' : '수입'} ${formatMan(ceiling)}`),
      ],
      '부품비·공임·도장비 합계예요',
    ),
    ...accidentBlocks(accidentRecords(rec)),
  ];
};

const r11 = ({ result, out }: Ctx): DetailBlock[] => {
  const p = out.facts.priceVsMarket;
  if (p.kind !== 'value') return [];
  const tone = result.severity === 'warn' ? 'warning' : 'success';
  const rows = [row('신차 가격', p.value.newPriceWon > 0 ? won(p.value.newPriceWon) : '정보 없음'), row('판매 가격', won(p.value.priceWon))];
  if (p.value.ratio > 0) rows.push(row('신차 가격 대비', pct(p.value.ratio), tone));
  return [block('가격 비교', rows)];
};

/**
 * Where the leak is: the item itself ("실린더 블록·오일팬" under 원동기 ›
 * 오일누유), or its unit when the item is just called "오일누유"
 * (변속기 › 자동변속기(A/T) › 오일누유 -> "자동변속기(A/T)").
 */
const leakPart = (n: { group: string; name: string }): string => (n.name === '오일누유' && n.group ? n.group : n.name);

const r12 = ({ out }: Ctx): DetailBlock[] => {
  const ins = inspectionOf(out);
  if (!ins) return [];
  const leaks = walkSystems(ins.inners).filter((n) => OIL_LEAK_CODES.has(n.code));
  const rows =
    leaks.length > 0
      ? leaks.map((n) => row(leakPart(n), n.status, codeTone(n.code)))
      : [row('미세누유·누유', '없음', 'success')];
  return [block('누유 부위', rows, '미세누유·누유만 판정에 써요. 누수 같은 다른 항목은 성능점검 전체 보기에서 볼 수 있어요')];
};

const EVIDENCE: Record<string, (ctx: Ctx) => DetailBlock[]> = {
  R01: ({ out }) => disclosureBlock(out),
  R02: ({ out }) => disclosureBlock(out),
  R03: r03,
  R04: r04,
  R05: r05,
  R06: r06,
  R07: r07,
  R08: r08,
  R09: r09,
  R10: r10,
  R11: r11,
  R12: r12,
};

/**
 * 미확인: each source's status. When every source was read the gap is in the
 * data itself, so the note names the reason the rows cannot show.
 */
const unknownBlock = (result: RuleResult, out: MobileOrchestratorResult, sources: SourceKey[]): DetailBlock => {
  const status = unknownStatus(result, out);
  const note =
    sourcesOk(sources, out) && status ? `자료는 받았지만 판단할 값을 찾지 못했어요. 사유: ${reasonShort(status)}` : null;
  return { ...sourceBlock(sources, out), title: '확인하지 못한 이유', note };
};

const DECIDED: ReadonlySet<Severity> = new Set(['killer', 'fail', 'warn', 'pass']);

/**
 * Sheet for one rule row. The screen already shows the row's result sentence,
 * label, why and seller question around the blocks, so none of them is
 * repeated here; the subtitle carries only short extra context (R04's source).
 * 미확인 rules swap evidence + 데이터 출처 for one "확인하지 못한 이유" block
 * listing each source's status.
 */
export function buildRuleDetail(result: RuleResult, out: MobileOrchestratorResult, frameSource: FrameSource): DetailSheet {
  const id = result.ruleId;
  const sources = RULE_SOURCES[id] ?? ['page'];
  const criteria = criteriaBlock(id);
  const decided = DECIDED.has(result.severity);
  const blocks = decided
    ? [...(EVIDENCE[id]?.({ result, out, frameSource }) ?? []), ...(criteria ? [criteria] : []), sourceBlock(sources, out)]
    : [unknownBlock(result, out, sources), ...(criteria ? [criteria] : [])];
  return {
    title: RULE_META[id]?.title ?? result.title,
    subtitle: decided && id === 'R04' ? `근거: ${FRAME_SOURCE_NAME[frameSource]}` : null,
    blocks,
  };
}
