/**
 * 엔카진단 rows for the R03/R04 sheets. Part codes ("FRONT_DOOR_LEFT") become
 * the same Korean names the 성능점검 uses; the checker comment is reduced to its
 * quoted verdict because the full text is 합니다체.
 */
import { getFrameIntact } from '@core/parsers/encar/api-diagnosis.js';
import type { MobileOrchestratorResult } from '../../orchestrate-mobile';
import type { DetailBlock, DetailRow, Tone } from '../types';
import { arr, isObj, str, type Obj } from '../raw';
import { block, fullDate, row } from './common';

const PART_NAME: Record<string, string> = {
  HOOD: '후드',
  BONNET: '후드',
  FRONT_FENDER: '프론트 휀더',
  REAR_FENDER: '리어 휀더',
  FRONT_DOOR: '프론트 도어',
  BACK_DOOR: '리어 도어',
  REAR_DOOR: '리어 도어',
  TRUNK_LID: '트렁크 리드',
  TAILGATE: '테일게이트',
  TAIL_GATE: '테일게이트',
  RADIATOR_SUPPORT: '라디에이터 서포트',
  QUARTER_PANEL: '쿼터 패널',
  ROOF_PANEL: '루프 패널',
  SIDE_SILL_PANEL: '사이드실 패널',
  SIDE_SILL: '사이드실 패널',
  FRONT_PANEL: '프론트 패널',
  REAR_PANEL: '리어 패널',
  CROSS_MEMBER: '크로스 멤버',
  INSIDE_PANEL: '인사이드 패널',
  SIDE_MEMBER: '사이드 멤버',
  FRONT_SIDE_MEMBER: '프론트 사이드 멤버',
  REAR_SIDE_MEMBER: '리어 사이드 멤버',
  WHEEL_HOUSE: '휠하우스',
  FRONT_WHEEL_HOUSE: '프론트 휠하우스',
  REAR_WHEEL_HOUSE: '리어 휠하우스',
  PILLAR_A: 'A 필러',
  PILLAR_B: 'B 필러',
  PILLAR_C: 'C 필러',
  A_PILLAR: 'A 필러',
  B_PILLAR: 'B 필러',
  C_PILLAR: 'C 필러',
  DASH_PANEL: '대쉬 패널',
  FLOOR_PANEL: '플로어 패널',
  TRUNK_FLOOR: '트렁크 플로어',
  PACKAGE_TRAY: '패키지 트레이',
};

/** "FRONT_DOOR_LEFT" -> "프론트 도어(좌)"; null when the part is not in the table. */
export const diagnosisPartName = (code: string): string | null => {
  const m = code.match(/^(.+?)_(LEFT|RIGHT)$/);
  const name = PART_NAME[m?.[1] ?? code];
  if (!name) return null;
  if (!m) return name;
  return `${name}(${m[2] === 'LEFT' ? '좌' : '우'})`;
};

const isComment = (name: string): boolean => name.endsWith('_COMMENT');

const partTone = (result: string, code: string | null): Tone =>
  code === 'NORMAL' || result === '정상' ? 'success' : 'warning';

/** The quoted verdict inside the checker comment, e.g. "외부패널 단순교환 차량". */
const checkerVerdict = (comment: string | null): string | null => {
  const m = comment?.match(/['‘"“]([^'’"”]{1,40})['’"”]/);
  return m?.[1]?.trim() || null;
};

/**
 * 엔카진단 blocks: overall verdict and every inspected part. null when the
 * diagnosis was not read (the source block says why).
 */
export function diagnosisBlocks(out: MobileOrchestratorResult): DetailBlock[] | null {
  const api = out.parsed.raw.diagnosisApi;
  if (api.kind !== 'value') return null;
  const items = arr(api.value.items).filter(isObj);

  const comment = items.find((i) => str(i, 'name') === 'CHECKER_COMMENT');
  const intact = getFrameIntact(api.value);
  const verdict =
    checkerVerdict(comment ? str(comment, 'result') : null) ??
    (intact === true ? '무사고' : intact === false ? '사고' : '판정 없음');
  const summary: DetailRow[] = [];
  const date = fullDate((api.value as unknown as Obj).diagnosisDate);
  if (date) summary.push(row('진단일', date));
  summary.push(row('종합 판정', verdict, intact === true ? 'success' : intact === false ? 'danger' : null));

  const parts: DetailRow[] = [];
  for (const i of items) {
    const name = str(i, 'name') ?? '';
    // Comment rows carry a null resultCode (core getFrameIntact): skip them even without a name.
    if (isComment(name) || i.resultCode === null) continue;
    const result = (str(i, 'result') ?? '').trim() || '기록 없음';
    const code = typeof i.resultCode === 'string' ? i.resultCode : null;
    // Unmapped parts keep their code in readable form so rows stay distinct.
    const label = diagnosisPartName(name) ?? (name.replace(/_/g, ' ').trim() || '기타 부위');
    parts.push(row(label, result, partTone(result, code)));
  }

  return [
    block('엔카진단 결과', summary),
    block('진단 부위', parts, parts.length === 0 ? '부위별 진단 결과가 없어요' : null),
  ];
}
