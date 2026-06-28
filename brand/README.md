# Brand assets (debene.dev)

Drop generated assets here, then ping me to wire them into the app.

## Icon (generate as 1024×1024 PNG)
- `icon-foreground.png` — logo/mark on **transparent** background; keep artwork within the
  centre **~66%** (adaptive-icon safe zone, so the round/squircle mask never clips it).
- `icon-background.png` — full-bleed 1024×1024 (solid / gradient / pattern).
  *Or* skip this and just specify a background hex below.
- `icon-monochrome.png` *(optional)* — single-colour silhouette on transparent, for
  Android-13+ themed icons.

## Theme colors (hex) — app is pinned to these (dynamic color is OFF)
Fill these in (light + dark):

| Role       | Light hex | Dark hex |
|------------|-----------|----------|
| primary    |           |          |
| secondary  |           |          |
| tertiary   |           |          |
| background  |          |          |
| (icon bg)  |           | —        |

Minimum required: a primary hex. I'll derive the rest if you don't specify them.

## What I'll generate from the master
- `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` + `ic_launcher_round.png`
  (48 → 192 px) — needed for Android 7.0–7.1 (minSdk 24), which ignores adaptive XML.
- `res/mipmap-anydpi-v26/ic_launcher.xml` (foreground + background + monochrome).
- Updated `app/src/main/java/dev/debene/gopher/ui/theme/Theme.kt` palette.
