# AutoVerdict Design Language

Toss-style, light only. Rationale and the full decision record live in
`docs/superpowers/specs/2026-10-17-ux-revamp-0-design-language-design.md`.
This file is the day-to-day reference.

## Principles

- One message per screen: a large conclusion sentence first, evidence below.
- White page, grey scale for hierarchy, a single accent (Toss blue `#3182F6`).
- List rows and 8dp grey bands instead of bordered cards and shadows.
- Primary action in a fixed bottom CTA (`AvBottomCta`).
- Press feedback is a 96% scale (`Modifier.avPressable`), not a ripple.

## Tokens (`ui/theme`)

Read tokens through `AvTheme.colors / typography / shapes / spacing`. Screens never
use raw hex or `AvPalette`.

| Group | Tokens |
|---|---|
| Surfaces | `background` white · `surfaceMuted` grey100 · `surfaceSubtle` grey50 |
| Text | `textPrimary` · `textSecondary` · `textTertiary` · `textDisabled` (disabled only) |
| Accent | `accent` (fills) · `accentText` (accent-colored text/icons) · `accentWeak` · `accentPressed` · `onAccent` |
| Status | `danger*`, `warning*`, `success*`, `neutral*` — each `Fill` / `Text` / `Bg` |
| Other | `border` · `toastBg` · `toastAction` · `scrim` |

Contrast: all text pairs meet WCAG AA 4.5:1 (locked by `AvColorsContrastTest`) except
white on `accent` (Primary button labels only, SemiBold 15sp+) and placeholders on
`surfaceMuted`. Never put white text on `dangerFill`.

Verdict tones: `AvTone.fromVerdict()` — NEVER→Danger "피하세요", CAUTION→Warning "주의",
OK→Success "괜찮아요", UNKNOWN→Neutral "확인 필요".

Type (Pretendard): `title1` 26 Bold · `title2` 22 Bold · `title3` 19 SemiBold ·
`body1` 17 · `body2` 15 · `buttonL/M/S` 17/15/13 SemiBold · `caption1` 13 ·
`caption2` 11 SemiBold (minimum size). Only Regular, SemiBold and Bold are bundled — don't use Medium.

Shapes: chip pill · tile 12 · button S/M/L 10/14/16 · input 16 · dialog 24 · sheet top 24.
Spacing: 4 · 8 · 12 · 16 · 20 · 24 · 32; screen side padding 20.

## Components (`ui/components`)

`AvTopBar` (+`AvIconButton`) · `AvButton` (Primary/Weak/Secondary/Danger × L/M/S; Primary is
L/M only) · `AvBottomCta` · `AvListRow` · `AvTextField` · `AvBadge` · `AvFactTile` ·
`AvSectionHeader` (optional `expanded`/`onToggle` makes the whole title row a fold button with
a chevron; pair with `AvCollapsibleBody`) · `AvSectionDivider` · `AvEmptyState` · `AvIllustrationIcon` ·
`AvBottomSheet` (optional `maxHeightFraction` caps it, e.g. detail sheets at 0.5; content scrolls) · `AvDialog` · `AvToastHost`/`LocalAvToast` · `AvTabBar` · `AvLoadingSteps`.
Debug builds show all of them under Settings → 컴포넌트 갤러리.

Insets: `AvTopBar` owns the status bar, `AvTabBar`/`AvBottomCta` own the navigation bar
(and IME for `AvBottomCta`). Don't add blanket insets at the screen root.

## Copy

- 해요체 everywhere; conclusion first.
- Say what the user can do, not what the app failed at ("엔카에 로그인하면 확인할 수 있어요",
  not "데이터 추출 실패").
- No English labels or developer terms in UI. Used-car domain terms (전손, 침수, 성능점검,
  프레임, 자차보험 공백) are used as-is; explanations go in detail sheets.
- Errors: what happened + what to do next.
- Buttons: verb, 2–3 words ("저장하기", "비교하기", "다시 시도").
- Numbers: "8.2만km", "2,180만원", "2022년 2월~9월".
- No emoji in UI text; use `AvIllustrationIcon` for pictures.
- New UI strings (components, navigation, new screens) go in `res/values/strings.xml`.
  Existing screen copy moves there when that screen is redesigned (sub-projects 1–4).
