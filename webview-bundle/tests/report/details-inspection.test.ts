import { describe, it, expect } from 'vitest';
import { buildReport } from '../../src/report/build-report';
import { diagnosisPartName } from '../../src/report/details/diagnosis';
import type { DetailSheet } from '../../src/report/types';
import { load } from './fixtures';

const sheetOf = (id: '39339524' | '41878112' | '42264483' | '41623744') => {
  const { raw, out } = load(id);
  return buildReport(raw, out).repairs;
};

/** Rows of the 주요 장치 blocks (everything after the panel block). */
const systemRows = (s: DetailSheet) => s.blocks.slice(2).flatMap((b) => b.rows);

describe('Repairs.sheet (성능점검 전체)', () => {
  it('42264483: summary, every panel, every inners item grouped by top-level group', () => {
    const repairs = sheetOf('42264483')!;
    const s = repairs.sheet!;
    expect(s.title).toBe('성능점검 전체');
    expect(s.blocks.map((b) => b.title)).toEqual([
      '사고·단순수리',
      '외판·골격 부위',
      '자기진단',
      '원동기',
      '변속기',
      '동력전달',
      '조향',
      '제동',
      '전기',
      '연료',
    ]);
    expect(s.blocks[0]!.rows.slice(0, 2)).toEqual([
      { label: '사고 이력', value: '있음', tone: 'danger' },
      { label: '단순수리', value: '있음', tone: 'warning' },
    ]);
    expect(s.blocks[0]!.rows).toContainEqual({ label: '점검일', value: '2026-05-08', tone: null });
    expect(s.blocks[1]!.rows).toEqual(
      repairs.panels.map((p) => ({ label: p.name, value: `${p.rank} ${p.status}`, tone: p.tone })),
    );
    expect(s.blocks[1]!.rows[0]).toEqual({ label: '리어 패널', value: 'A랭크 교환', tone: 'danger' });

    const rows = systemRows(s);
    expect(rows).toContainEqual({ label: '오일누유 · 실린더 블록 / 오일팬', value: '누유', tone: 'danger' });
    expect(rows).toContainEqual({ label: '등속조인트', value: '불량', tone: 'danger' });
    // Same-named parts under different groups stay apart.
    expect(rows).toContainEqual({ label: '냉각수누수 · 실린더 헤드 / 개스킷', value: '없음', tone: 'success' });
    // Every 양호-type item is listed, matching the summary count.
    expect(rows.filter((r) => r.tone === 'success')).toHaveLength(repairs.systems.okCount);
    for (const f of repairs.systems.flagged) {
      expect(rows.some((r) => r.label.endsWith(f.name) && r.value === f.status && r.tone === f.tone), f.name).toBe(true);
    }
    // Items the inspector left blank are shown, not dropped.
    expect(rows).toContainEqual({ label: '클러치 어셈블리', value: '기록 없음', tone: null });
  });

  it('41878112 / 39339524: success rows equal okCount; empty panel list explained', () => {
    for (const id of ['41878112', '39339524'] as const) {
      const repairs = sheetOf(id)!;
      expect(systemRows(repairs.sheet!).filter((r) => r.tone === 'success'), id).toHaveLength(repairs.systems.okCount);
    }
    const panels = sheetOf('41878112')!.sheet!.blocks[1]!;
    expect(panels).toEqual({ title: '외판·골격 부위', rows: [], note: '교환하거나 수리한 부위가 없어요' });
  });

  it('41623744: no inspection -> no repairs, no sheet', () => {
    expect(sheetOf('41623744')).toBeNull();
  });
});

describe('diagnosisPartName', () => {
  it('maps part codes to Korean names with side', () => {
    expect(diagnosisPartName('FRONT_DOOR_LEFT')).toBe('프론트 도어(좌)');
    expect(diagnosisPartName('BACK_DOOR_RIGHT')).toBe('리어 도어(우)');
    expect(diagnosisPartName('FRONT_FENDER_RIGHT')).toBe('프론트 휀더(우)');
    expect(diagnosisPartName('TRUNK_LID')).toBe('트렁크 리드');
    expect(diagnosisPartName('HOOD')).toBe('후드');
    expect(diagnosisPartName('SOMETHING_NEW')).toBeNull();
  });
});
