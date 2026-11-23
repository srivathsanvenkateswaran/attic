# Comfort `Tinted`: the rules

Source: `app/src/main/java/app/attic/player/ui/comfort/Tinted.kt` (`object ComfortTint`, `fun apply`,
`@Composable fun Tinted(seed, wash)`, `TintedCover`) and `Oklch.kt`. Per-cover results are in
`comfort-tinted-<index>.json`; the untinted palette is `comfort-base.json`.

## Inputs

- **seed**: the cover's colour, not deepened. `TintedCover` and Now Playing call
  `rememberArtColor(mediaId, Color.Unspecified, deepen = false)`, which is `coverSwatch` in `ui/Art.kt`:
  androidx Palette on a 96px thumbnail, `vibrantSwatch ?: mutedSwatch ?: dominantSwatch`.
- **base**: the Comfort palette around it, `ComfortDark` or `ComfortLight` (or an already-tinted
  palette when `Tinted` is nested).
- **wash**: `false` = kit.css `.tinted`; `true` = `.tinted.wash`.

If the seed is unspecified (a song with no art), nothing changes: `if (seed.isSpecified) ComfortTint.apply(base, seed.toArgb(), wash) else base`.

## The formula

The class comment: "Each token keeps the tint's OKLCH hue, scales its chroma by a fixed factor and sets
its own lightness, exactly as the CSS writes `oklch(from var(--tint) L calc(c*k) h)`. So contrast is the
same for a pale cover and a dark one. Only the tint's hue and chroma are read; its lightness is ignored."

```kotlin
val lch = Oklch.fromArgb(tint)
Color(Oklch.toArgb(it.lightness, lch.c * it.chroma, lch.h))
```

In words: convert the seed to OKLCH (L, C, h). For each token named in the table, the result is
`oklch(L_token, C_seed * k_token, h_seed)`. The seed's own lightness is thrown away.

**Out of gamut**: `Oklch.toArgb` keeps L and h and binary-searches chroma down (24 steps) until the
colour fits sRGB, then rounds to 8-bit. So a vivid seed with a large k can come out less saturated than
`C*k`, never lighter or darker.

## Which tokens move

Without wash (`.tinted`), the accent family and the mini player's fills. With wash (`.tinted.wash`) the
wash table is added on top: surfaces and the two quieter inks. Tokens not in the tables keep the base
value: `on` (never changes), `danger`, `onArt`, `knob`, `toast`, `onToast`, `scrim`, `shadow`,
`shadowSmall`, `edge`.

### Dark (`darkTinted`, then `darkWash` when wash)

| token | L | k (× seed chroma) |
|---|---|---|
| accent | 0.76 | 1.0 |
| onAccent | 0.20 | 0.55 |
| accentInk | 0.82 | 0.85 |
| accentSoft | 0.31 | 0.6 |
| onAccentSoft | 0.89 | 0.5 |
| miniBase | 0.25 | 0.5 |
| miniFill | 0.40 | 0.9 |
| miniInk2 | 0.82 | 0.3 |
| *wash:* ground | 0.115 | 0.28 |
| card | 0.215 | 0.34 |
| raised | 0.26 | 0.34 |
| fill | 0.30 | 0.4 |
| fill2 | 0.38 | 0.45 |
| line | 0.28 | 0.36 |
| on2 | 0.77 | 0.3 |
| on3 | 0.57 | 0.26 |

### Light (`lightTinted`, then `lightWash` when wash)

| token | L | k |
|---|---|---|
| accent | 0.47 | 1.0 |
| onAccent | — | plain white `#FFFFFF` (code: `onAccent = if (base.dark) token("onAccent", …) else Color.White`) |
| accentInk | 0.43 | 1.0 |
| accentSoft | 0.90 | 0.4 |
| onAccentSoft | 0.36 | 1.0 |
| miniBase | 0.955 | 0.16 |
| miniFill | 0.87 | 0.42 |
| miniInk2 | 0.45 | 0.3 |
| *wash:* ground | 0.93 | 0.2 |
| card | 0.978 | 0.08 |
| raised | 0.985 | 0.06 |
| fill | 0.905 | 0.26 |
| fill2 | 0.84 | 0.34 |
| line | 0.915 | 0.14 |
| on2 | 0.45 | 0.3 |
| on3 | 0.62 | 0.26 |

Note the light theme has no `onAccent` row: on a tinted accent the ink is white, whereas untinted light
Comfort uses Soot `#2A1800` on Lamplight.

## Material scheme inside `Tinted`

`ProvideComfortColors` rebuilds `MaterialTheme.colorScheme` from the tinted tokens with
`ComfortColors.toColorScheme()` (ComfortColors.kt), so these are plain aliases of the tokens above:

primary = accent, onPrimary = onAccent, primaryContainer = accentSoft, onPrimaryContainer = onAccentSoft,
inversePrimary = accentInk, secondary = accentInk, onSecondary = ground, secondaryContainer = accentSoft,
onSecondaryContainer = onAccentSoft, tertiary = accentInk, onTertiary = ground, tertiaryContainer = accentSoft,
onTertiaryContainer = onAccentSoft, background = ground, onBackground = on, surface = ground, onSurface = on,
surfaceVariant = fill, onSurfaceVariant = on2, surfaceTint = transparent, inverseSurface = toast,
inverseOnSurface = onToast, error = danger, onError = #2A0A06 (dark) / white (light),
errorContainer = #3D1A16 / #FBE0DC, onErrorContainer = #FFB4A9 / #8C1D12, outline = on3,
outlineVariant = line, scrim = scrim, surfaceBright = raised, surfaceDim = ground,
surfaceContainerLowest = ground, surfaceContainerLow = card, surfaceContainer = card,
surfaceContainerHigh = raised, surfaceContainerHighest = fill.

## Where the app applies it

- `Tinted(seed, wash = true)`: Now Playing (ComfortNowPlaying.kt), Queue, full-screen Lyrics (via
  `TintedCover`), the track menu and pickers opened over the player (Sheets.kt), recap cards drawn on
  a cover's wash (ComfortRecapCards.kt `RecapCardColors`), and Search's Browse cards (Tiles.kt
  `BrowseTile`).
- `Tinted(seed)` without wash: the mini player in the dock (Dock.kt) and the ranked rows on recap
  cards (ComfortRecapCards.kt `RankRow`).
- Everything else (Home, Library, Search, collections, Settings) is the untinted base palette with the
  amber Lamplight accent.
- Animation: every token tweens over 450 ms (`tween(durationMillis = 450)`) when the seed changes.

## Expressing this as FigCraft variables

FigCraft colour variables can alias, take alpha-of, or `mix(from, to, t)`, but cannot do an OKLCH
"keep hue, set lightness, scale chroma" operation, so the tinted tokens cannot be derived from a seed
variable exactly. Two ways to model it:

1. **Exact**: one variable mode per cover (or per "tint" collection), with each moved token's value
   taken from `comfort-tinted-<index>.json` → `light|dark` → `tinted|tintedWash` → `tokens`. All
   unmoved tokens alias the base collection. The Material roles alias the tokens per the list above.
2. **Structure only**: keep a `seed` variable for documentation, and treat the moved tokens as
   per-mode literals. Do not substitute `mix(seed, ground, t)`: a mix changes lightness with the
   seed, which is exactly what the rule avoids (contrast is fixed by L_token, independent of the cover).

`changed` in each JSON lists only the tokens that differ from the base, with their L, k, the resulting
OKLCH chroma before gamut mapping (`oklchC`) and the hex.
