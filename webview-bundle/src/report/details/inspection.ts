/**
 * 성능점검 detail: the "성능점검 전체 보기" sheet (Repairs.sheet) and the
 * pieces the R04/R12 sheets share with it. Codes (RANK_*, status codes) are
 * mapped to words; nothing identifying (plate, VIN) is read.
 */
import type { MobileOrchestratorResult } from '../../orchestrate-mobile';
import type { DetailBlock, DetailRow, DetailSheet, Tone } from '../types';
import { buildRepairs, codeTone } from '../repairs';
import { groupDigits } from '../format';
import { arr, isObj, num, str, type Obj } from '../raw';
import { block, fullDate, inspectionOf, row } from './common';

const yesNo = (v: boolean, yesTone: Tone): DetailRow['tone'] => (v ? yesTone : 'success');

const titles = (v: unknown): string[] =>
  arr(v)
    .map((t) => (isObj(t) ? str(t, 'title') : null))
    .filter((t): t is string => t !== null);

/** 사고 이력 / 단순수리 rows from `master` (the R04 fallback signal). */
export function accidentRows(ins: Obj): DetailRow[] {
  const master = isObj(ins.master) ? ins.master : {};
  const rows: DetailRow[] = [];
  if (typeof master.accdient === 'boolean') {
    rows.push(row('사고 이력', master.accdient ? '있음' : '없음', yesNo(master.accdient, 'danger')));
  }
  if (typeof master.simpleRepair === 'boolean') {
    rows.push(row('단순수리', master.simpleRepair ? '있음' : '없음', yesNo(master.simpleRepair, 'warning')));
  }
  return rows;
}

/** Everything in `master.detail` that describes the car's condition. */
const conditionRows = (ins: Obj): DetailRow[] => {
  const master = isObj(ins.master) ? ins.master : {};
  const d = isObj(master.detail) ? master.detail : {};
  const rows: DetailRow[] = [];
  if (typeof d.waterlog === 'boolean') rows.push(row('침수', d.waterlog ? '있음' : '없음', yesNo(d.waterlog, 'danger')));
  if (typeof d.tuning === 'boolean') {
    rows.push(row('구조 변경(튜닝)', d.tuning ? '있음' : '없음', yesNo(d.tuning, 'warning')));
  }
  const serious = titles(d.seriousTypes);
  rows.push(row('특별 이력', serious.length > 0 ? serious.join('·') : '없음', serious.length > 0 ? 'danger' : 'success'));
  const usage = titles(d.usageChangeTypes);
  rows.push(row('용도 변경', usage.length > 0 ? usage.join('·') : '없음', usage.length > 0 ? 'warning' : 'success'));
  const mileage = num(d, 'mileage');
  if (mileage !== null) rows.push(row('점검 당시 주행거리', `${groupDigits(mileage)}km`));
  const issued = fullDate(d.issueDate);
  if (issued) rows.push(row('점검일', issued));
  return rows;
};

const RANK_NOTE = '1랭크·2랭크는 외판, A·B·C랭크는 차의 뼈대인 주요 골격이에요';

/** Every outer panel with recorded work, frame ranks first (same order as the list). */
export function panelBlock(out: MobileOrchestratorResult): DetailBlock {
  const panels = buildRepairs(out)?.panels ?? [];
  const rows = panels.map((p) => row(p.name, p.rank ? `${p.rank} ${p.status}` : p.status, p.tone));
  return block('외판·골격 부위', rows, rows.length > 0 ? RANK_NOTE : '교환하거나 수리한 부위가 없어요');
}

/**
 * One block per top-level `inners` group (원동기, 변속기, …) with every item,
 * 양호 included. Rows are labelled with the path below the group so repeated
 * names ("실린더 헤드 / 개스킷" under 오일누유 and 냉각수누수) stay apart.
 * Items with a status use the same code table as `repairs.systems`; leaves
 * without one are listed as "기록 없음".
 */
export function systemBlocks(inners: unknown): DetailBlock[] {
  const blocks: DetailBlock[] = [];
  for (const top of arr(inners)) {
    if (!isObj(top)) continue;
    const group = (isObj(top.type) ? str(top.type, 'title') : null) ?? '기타';
    const rows: DetailRow[] = [];
    const walk = (n: Obj, path: string[]): void => {
      const children = arr(n.children).filter(isObj);
      if (isObj(n.statusType)) {
        const status = str(n.statusType, 'title') ?? '기록 없음';
        rows.push(row(path.join(' · ') || group, status, codeTone(String(n.statusType.code ?? ''))));
      } else if (children.length === 0 && path.length > 0) {
        rows.push(row(path.join(' · '), '기록 없음'));
      }
      for (const c of children) {
        const name = (isObj(c.type) ? str(c.type, 'title') : null) ?? '기타';
        walk(c, [...path, name]);
      }
    };
    walk(top, []);
    if (rows.length > 0) blocks.push(block(group, rows));
  }
  return blocks;
}

/** Repairs.sheet: the whole 성능점검. null when it was not read. */
export function buildInspectionSheet(out: MobileOrchestratorResult): DetailSheet | null {
  const ins = inspectionOf(out);
  if (!ins) return null;
  return {
    title: '성능점검 전체',
    subtitle: null,
    blocks: [
      block(
        '사고·단순수리',
        [...accidentRows(ins), ...conditionRows(ins)],
        '단순수리는 문·휀더처럼 떼어 낼 수 있는 외판을 바꾼 경우예요. 프레임 사고와는 달라요',
      ),
      panelBlock(out),
      ...systemBlocks(ins.inners),
    ],
  };
}
