# Bouvo Olympus planet connections

Generated with the built-in ImageGen tool on November 14, 2026. The existing `graphics/campaign/ungaikyo/chief_navigator_bouvo_filaments_front_v2.png` was a style reference only. The user's gameplay screenshot supplied the placement intent; it was not edited.

`tether_raw.png` preserves the generated transparent image. `tools/build_bouvo_filaments.ps1` extracts the same neutral dark pigment as the existing cocoon assets, preserving alpha and baking RGB 7/7/8 into `graphics/campaign/ungaikyo/chief_navigator_bouvo_filaments_tether_v1.png`. The existing front/rear artwork is reused at each planet's real radius. Runtime placement stretches this painted connection between the current stellar limb and each current authored planet, with no procedural line art or runtime recoloring.

## Final generation prompt

Use case: stylized-concept
Asset type: transparent 2D game sprite, a long black filament connection to stretch from a star to a planet.
Input image: reference only for the delicate black web material, irregular forks and knots. Do not reproduce its circular arrangement.
Primary request: one long horizontal bundle of thin, uneven black strands connecting the middle of the LEFT edge to the middle of the RIGHT edge. Several meandering strands split and rejoin, with sparse elongated web cells, delicate loose wisps and occasional slightly thicker knots. Natural asymmetry and tiny branch detail matching the black filament cocoon in the reference.
Composition: very wide landscape 3:1. Entire bundle inside the central 60% of canvas height. Both ends taper to narrow connected junctions at exactly left-center and right-center so it can join other web sprites. Transparent empty areas above and below.
Palette: only neutral near-black filament pigment with antialiased alpha.
Constraints: genuinely transparent background. No planets, sun, stars, nebula, glow, shadow, white outlines, text, labels, frames or checkerboard pixels. Avoid thick ropes, uniform straight lines, concentric loops or a regular lattice.
