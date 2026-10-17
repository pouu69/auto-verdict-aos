/** Small Korean formatting helpers shared by the report builders. */

/** 1234567 -> "1,234,567". Locale-independent (the WebView and Node ICU may differ). */
export const groupDigits = (n: number): string =>
  String(Math.trunc(n)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');

/** Won -> "1,126만원" (rounded), 0 -> "0원". */
export const formatMan = (won: number): string =>
  won === 0 ? '0원' : `${groupDigits(Math.round(won / 10_000))}만원`;

/** Joined part names read with "·": "실린더 블록 / 오일팬" -> "실린더 블록·오일팬"; "자동변속기(A/T)" stays. */
export const partName = (title: string): string => title.trim().replace(/\s+\/\s+/g, '·');

/** "2023-11" -> "2023년 11월"; anything else is returned as is. */
export const koreanMonth = (ym: string): string => {
  const m = ym.match(/^(\d{4})-(\d{2})$/);
  return m ? `${m[1]}년 ${Number(m[2])}월` : ym;
};

/**
 * Picks a particle by the final syllable: [withBatchim] after a closed syllable,
 * otherwise [without]. For 으로/로 a final ㄹ takes 로.
 */
export const josa = (word: string, withBatchim: string, without: string): string => {
  const code = word.charCodeAt(word.length - 1) - 0xac00;
  if (code < 0 || code > 11171) return without;
  const jong = code % 28;
  if (jong === 0) return without;
  if (withBatchim === '으로' && jong === 8) return without;
  return withBatchim;
};
