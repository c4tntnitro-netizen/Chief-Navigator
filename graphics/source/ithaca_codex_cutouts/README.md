# Ithaca Codex icon cutouts

These transparent masters derive from Fractal Softworks' vanilla artwork in
`starsector-core/graphics/icons/codex/`. The earlier opaque 80x80 item copies
were the edit targets. Built-in ImageGen removed their exterior backgrounds;
the final item textures are rebuilt at 96x96 by
`tools/build_ithaca_codex_icons.ps1`, with a two-pixel transparent margin.

| Master | Original Codex image |
| --- | --- |
| `macroservos.png` | `custom.png` |
| `naval_cycler.png` | `manual_venting.png` |
| `arc_projector.png` | `manual_energy_weapons.png` |
| `intercept_matrix.png` | `game_mechanics.png` |
| `munitions_compiler.png` | `manual_missiles.png` |
| `autoloader_core.png` | `manual_ballistic_weapons.png` |
| `abyssal_lens.png` | `weapons.png` |

The shared built-in edit prompt requested background extraction with actual
transparent alpha, preserving the native low-resolution pixel-art silhouette,
orientation, colors, highlights, interior dark machinery and firing effects.
It prohibited redesign, new parts, smoothing into another style, labels,
borders, painted checkerboards and exterior shadows. Each edit named the
specific foreground and asked for a centered cutout with roughly 5% transparent
padding. The Lens edit named its silver emitter and pink-red rails; the
Autoloader edit named its silver gun mechanism and orange firing flare.

The game-derived artwork remains outside the mod's MIT grant. Earlier opaque
icons and the untouched installed game files are not background-removal inputs
when rebuilding: these saved transparent masters are authoritative.
