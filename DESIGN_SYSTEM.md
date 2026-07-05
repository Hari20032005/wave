# Stillwater Design System (M0)

The canonical reference. Every future screen is reviewed against this file and
the Design Psychology section of BUILD_PROMPT.md. Code lives in
`app/src/main/java/com/stillwater/app/ui/theme/` and `ui/components/`.

## Palette — low-arousal, 60/30/10, dark-first

Dark theme (primary — late night is the peak-risk context):

| Role | Color | Hex |
|---|---|---|
| Background (60%) | Deep Water — near-black spruce | `#0F1714` |
| Surface / card | Dark Surface | `#161F1B` |
| Raised surface | | `#1D2823` |
| Text | Mist (soft off-white) | `#E3E8E4` |
| Secondary text | Mist Faded | `#9DACA2` |
| Primary accent (10%) | Seafoam | `#8FB8A5` |
| Secondary (30%) | Slate Blue (muted) | `#90A7B6` |
| Warm accent (single focal use) | Sand | `#D5C2A1` |
| Celebrate tone | bloom `#22322B` / content `#A9CDBA` |
| Lapse tone | warm sand `#322D22` / content `#DFD2B6` |
| Error (system faults ONLY) | Clay | `#C29488` |

Light theme mirrors it on warm paper (`#F4F1EA`), deep sage primary (`#48705F`).

Hard rules:
- No saturated red/orange anywhere. `error` slot = muted clay, reserved for
  genuine system failures (e.g., export failed) — never user behavior.
- Lapse content uses `CalmTone.Lapse` (warm sand). Styling a lapse with error
  colors is a spec violation, not a taste choice.
- Sand appears at most once per screen.
- Dynamic color (Material You) is permanently off.

## Typography — Figtree (bundled, OFL), one family

| Style | Size/Line | Weight | Use |
|---|---|---|---|
| displayLarge | 34/44 | Light | breathing screens, calm celebration |
| headlineMedium | 24/32 | Medium | screen titles |
| titleMedium | 20/28 | Medium | card/section titles |
| bodyLarge | 17/26 | Regular | default reading text |
| bodyMedium | 15/22 | Regular | supporting text |
| labelLarge | 17/24 | SemiBold | buttons |
| labelMedium | 13/18 | Medium | chips, meta |

Nothing heavier than SemiBold. Body is deliberately large (17sp) with airy
line height — crisis reading happens with impaired executive function.

## Spacing — 8pt grid (`Spacing`)
`xs 4 · sm 8 · md 16 · lg 24 · xl 32 · xxl 48 · huge 64` — screen edge 24.
Touch targets: 48dp minimum everywhere; **72dp on SOS/crisis screens**.

## Motion (`Motion`, `BreathCadence`)
- Durations: GENTLE 450ms · CALM 700ms · DRIFT 1200ms (SOS transitions).
- Easings: CalmEase (0.35, 0, 0.25, 1) and BreathEase (sine-like, for loops).
- **No spring/bounce/overshoot tokens exist** — overshoot is slot-machine
  grammar. Screen transitions are cross-fades at CALM pace.
- Breathing cadences are tokens: BOX 4-4-4-4s, FOUR_SEVEN_EIGHT 4-7-8s.

## Shapes
Rounded 12/16/24/28 — soft, nothing sharp.

## Components (`ui/components/CalmComponents.kt`)
Feature code composes these, not raw Material widgets:
- `CalmCard(tone = Neutral | Celebrate | Lapse)` — the ONLY sanctioned
  celebration and lapse styling.
- `CalmPrimaryButton(isCrisis = true)` — one filled button per screen;
  crisis variant grows to the 72dp SOS target.
- `CalmQuietButton` — low-emphasis escape hatch; leaving is never punished.

## Anti-dopamine invariants (enforced at review, forever)
No confetti · no badges · no streaks that reset · no red notification dots ·
no leaderboards · no variable rewards · no infinite scroll · no autoplay ·
no guilt-nudge copy.
