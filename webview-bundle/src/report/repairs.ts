/**
 * 교환·수리 부위 (성능점검 `outers`) and 주요 장치 상태 (`inners`). Display only:
 * the verdict keeps using R04 (`master.accdient`) and R12.
 */
import type { MobileOrchestratorResult } from '../orchestrate-mobile';
import type { RepairPanel, Repairs, SystemItem, Tone } from './types';
import { arr, isObj, str } from './raw';

const RANKS: Record<string, { rank: string; tone: Tone; order: number }> = {
  RANK_A: { rank: 'A랭크', tone: 'danger', order: 0 },
  RANK_B: { rank: 'B랭크', tone: 'danger', order: 0 },
  RANK_C: { rank: 'C랭크', tone: 'danger', order: 0 },
  RANK_TWO: { rank: '2랭크', tone: 'warning', order: 1 },
  RANK_ONE: { rank: '1랭크', tone: 'warning', order: 2 },
};
const NO_RANK_ORDER = 3;

/** "교환(교체)" -> "교환", "판금/용접" -> "판금·용접", "용접,절단" -> "용접·절단". */
const cleanStatus = (title: string): string =>
  title
    .replace(/\([^)]*\)/g, '')
    .split(/[/,]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .join('·');

const toPanel = (o: unknown): (RepairPanel & { order: number }) | null => {
  if (!isObj(o) || !isObj(o.type)) return null;
  const name = str(o.type, 'title');
  if (!name) return null;
  const statuses: string[] = [];
  for (const s of arr(o.statusTypes)) {
    const t = isObj(s) ? str(s, 'title') : null;
    const cleaned = t ? cleanStatus(t) : '';
    if (cleaned && !statuses.includes(cleaned)) statuses.push(cleaned);
  }
  // Only parts with recorded work are listed.
  if (statuses.length === 0) return null;
  const rankKey = arr(o.attributes).find((a): a is string => typeof a === 'string' && a in RANKS);
  const rank = rankKey ? RANKS[rankKey] : undefined;
  return {
    name,
    status: statuses.join(' · '),
    rank: rank?.rank ?? null,
    tone: rank?.tone ?? 'neutral',
    order: rank?.order ?? NO_RANK_ORDER,
  };
};

/** 성능점검 `statusType.code` -> tone (code table in core api-inspection.ts `InnerNode`). */
const CODE_TONE: Record<string, Tone> = {
  '1': 'success', '2': 'success', '3': 'success',
  '4': 'warning', '6': 'warning', '8': 'warning', '9': 'warning',
  '5': 'danger', '7': 'danger', '10': 'danger',
};
export const codeTone = (code: string): Tone => CODE_TONE[code] ?? 'neutral';

const TONE_ORDER: Record<Tone, number> = { danger: 0, warning: 1, neutral: 2, success: 3 };

/**
 * Every `inners` node that carries a `statusType`, in tree order — the same walk
 * as core R12 (`getOilLeakItems`), so the code table can be checked against it.
 * `group` is the direct parent's title.
 */
export function walkSystems(inners: unknown): Array<SystemItem & { code: string }> {
  const out: Array<SystemItem & { code: string }> = [];
  const walk = (nodes: unknown, parent: string): void => {
    for (const n of arr(nodes)) {
      if (!isObj(n)) continue;
      const name = isObj(n.type) ? (str(n.type, 'title') ?? '') : '';
      if (isObj(n.statusType)) {
        const code = String(n.statusType.code ?? '');
        out.push({
          group: parent,
          name,
          status: str(n.statusType, 'title') ?? '',
          tone: codeTone(code),
          code,
        });
      }
      walk(n.children, name);
    }
  };
  walk(inners, '');
  return out;
}

/**
 * null unless the 성능점검 was read and carries an `outers` or `inners` list.
 * The "성능점검 전체 보기" sheet is attached by build-report (details/inspection.ts).
 */
export function buildRepairs(out: MobileOrchestratorResult): Omit<Repairs, 'sheet'> | null {
  const api = out.parsed.raw.inspectionApi;
  if (api.kind !== 'value') return null;
  const ins: unknown = api.value;
  if (!isObj(ins) || (!Array.isArray(ins.outers) && !Array.isArray(ins.inners))) return null;

  const panels = arr(ins.outers)
    .map(toPanel)
    .filter((p): p is RepairPanel & { order: number } => p !== null)
    .map((p, i) => ({ p, i }))
    .sort((a, b) => a.p.order - b.p.order || a.i - b.i)
    .map(({ p: { order: _order, ...panel } }) => panel);

  const nodes = walkSystems(ins.inners);
  const flagged = nodes
    .map((n, i) => ({ n, i }))
    .filter(({ n }) => n.tone !== 'success')
    .sort((a, b) => TONE_ORDER[a.n.tone] - TONE_ORDER[b.n.tone] || a.i - b.i)
    .map(({ n: { code: _code, ...item } }) => item);

  return {
    panels,
    systems: { flagged, okCount: nodes.filter((n) => n.tone === 'success').length },
  };
}
