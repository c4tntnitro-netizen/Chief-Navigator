# Bouvo Olympus irregular web v3

Built-in ImageGen generated and refined these transparent raster assets on November 14, 2026 (America/Los_Angeles). The user's screenshots supplied visual feedback and were not edited. Older runtime/source artwork remains preserved.

The first drafts are retained as `star_draft.png`, `planets_atlas_draft.png`, and `tethers_atlas_draft.png`. Final sources are `star_raw.png`, `planets_atlas_raw.png`, and `tethers_atlas_raw.png`.

`tools/build_bouvo_web_v3.ps1` extracts neutral dark pigment, bakes RGB7/7/8, crops atlas cells without resampling their features, clears shell borders, and creates rear-only body openings at1.04 body radii. All front-layer alpha mass is normalized to the approved original sun front layer during baking, using a histogram to preserve antialiased strokes under clamping. Final front layers have1.47% integrated pigment coverage. Distinct planet artwork uses physically comparable stroke widths (mean boundary-based estimate1.66–2.91 world units against2.23 for the new sun); the sun image is never scaled down to make a planet's web. Three open-ended painted connection variants attach at three separate positions on both facing hemispheres. The lighter planet atlas is preserved as `planets_atlas_thin.png`; the final atlas uses the additional strand-weight prompt below.

Runtime assets are under `graphics/campaign/ungaikyo`: `chief_navigator_bouvo_star_{front,rear}_v3.png`, `chief_navigator_bouvo_planet_{0..4}_{front,rear}_v3.png`, and `chief_navigator_bouvo_tether_{0..2}_v3.png`. All are registered in the `chief_navigator_bouvo_wrapping` settings category.

## star initial generation prompt

Use case: stylized-concept. Asset type: transparent game sprite for an irregular black filament cocoon over a white sun.
Reference image: style/material reference only. Replace its too-regular arrangement completely.
A sparse, tangled, three-dimensional veil of near-black filaments occupying a roughly round but very ragged footprint. Several coarse uneven branching bundles, knotted intersections, torn wisps, fine side branches and wide completely unequal voids. Branches cross the center as well as the outer fringe. Disordered directional tangles: locally clumped, broken, asymmetric; NOTHING organized as a ring, polygon, mesh grid, radial spokes, regular cells, latitudes or repeated loops. The silhouette has deep uneven bites and long sparse protrusions, so it does not trace a smooth circle. Substantial irregular black ink branches with thin tendrils like the reference, not gray fog. Approximate total filament coverage 4–6% of canvas.
Square canvas. Center of mass at canvas center; all visible pixels inside 92% of the canvas, transparent padding at all edges. Actual transparent background, antialiased near-black pigment only. No sun, planet, circle, disk, nebula, stars, white or colored pixels, glow, shadows, checkerboard, text or border.

## star final refinement prompt

undefined

## planet initial generation prompt

Use case: stylized-concept. Asset type: six distinct transparent black filament planet-web sprites in an atlas, THREE columns by TWO rows, six equal square cells, landscape 3:2 canvas.
Reference: material and tangled irregular branching from the attached new sun web. These will render about one third the diameter of that sun web, so EACH cell needs FEWER branches but PROPORTIONALLY THICKER branches: the same visible strand thickness and percentage of dark coverage after scaling. In each cell, main bundles about 2–3% of the CELL width with finer tapered secondary wisps; roughly 10–15% of each cell covered by black pigment. Keep wide transparent gaps through which a planet will be seen.
Six completely DIFFERENT asymmetric cocoons, each loosely occupying 88% of its square cell. Uneven tangled branching, stout knotted clusters and fine loose ends, irregularly sized empty spaces. Some diagonal, some bent, some crescent-like, some torn and angular. A few branches cross each center. No smooth outer circle, repeated enclosed rings, lattice, radial spokes, balanced symmetry or evenly spaced cells inside a web. Deep gaps in the ragged silhouette. Do not copy a repeated cell design. Each separate web centered within its own cell, with at least 6% transparent padding from every CELL edge, no strands joining cells.
Actual transparent background, antialiased neutral black pigment only. No planets, disks, stars, nebula, glow, colored or white material, backdrop, text, grid lines, labels, border or checkerboard pixels.

## planet final refinement prompt

undefined

## planet final strand-weight prompt

Edit target: preserve this exact transparent THREE-column TWO-row planet-web atlas, all six individual geometries and cell positions. Make EVERY existing filament precisely about TWO TIMES AS WIDE, with opaque neutral-black interiors and smooth antialiased transparent edges. Keep the same number of strands and branching topology, same wide empty gaps, same ragged silhouette, same transparent padding. The current lines are much too faint and hair-thin; the final main strands should read as 3–5px black strokes at this 1536x1024 atlas size. Small knots can be wider, but NEVER create broad membranes or solid web sheets. No gray haze, no low-opacity pigment, no new strands or objects, no text, no backdrops, no circles. The background stays truly transparent.

## tether initial generation prompt

Use case: stylized-concept. Asset type: transparent atlas of THREE DIFFERENT long horizontal black filament webs, stacked in THREE equal-height rows on a square canvas. Each row is an independent wide 3:1 connection sprite.
Reference image is material/style only: the dark irregular branching filaments of the new sun web.
CRITICAL endpoint shape: each horizontal web is BROAD and OPEN at BOTH left and right edges. At each edge, 4–7 separate strands continue out at UNEVENLY SPACED heights across most of the web's breadth. The strands must NOT meet at a point, knot, tip, stalk, trunk, arrowhead, hourglass pinch, funnel or central root at EITHER end. Edge cross-sections should be as wide as the middle. These edges will overlap the wide surface of another web.
Within each strip: winding, irregularly spaced branching near-black strands split and join at scattered off-center knots; sudden unequal bends, little side tangles, long loose wisps, large open negative spaces. Irregular directional flow; no parallel cables, regular diamond cells or repeated mirrored shapes. Three distinctly different arrangements and knot locations. A few thicker branches and thinner antialiased fibers, sparse 5–9% coverage of each row. Keep a transparent gap of 8% of ROW height above and below each strip. Filaments reach the actual left and right canvas edges at several distinct heights, never converging.
Only near-black filament pigment and genuine transparency. No sun or planet, backdrop, haze, glow, fog, gray rectangles, white outlines, text, labels, grid lines, frames, checkerboard or other objects.

## tether final refinement prompt

undefined
