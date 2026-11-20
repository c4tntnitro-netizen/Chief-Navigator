# Pixel-only Threat wall module

Final: `threaxus_wall_module.png`,512x224 transparent RGBA. Visible bounds are
508x208, about the468-512pixel side length of the Threatxus central triangle.
The clipped/stepped armor-section shape references the Ithaca Wall; none of
its pixels are used. All83928visible output pixels are copied literally from
the seven user-specified native Threat sprites, at original source resolution.

Only integer crops/translations, quarter-turn transposes, horizontal mirrors
and a binary silhouette mask are used. No interpolation, blending, recoloring,
new outlines, shading or generated material. Every visible RGBA tuple equals
the source pixel identified in `pixel_provenance.npz` (source index,x,y).
`audit.json` records hashes, visible source contributions and the full layer list.
The7reference files are retained byte-exact under `references/`.

Rebuild: `python tools/build_threaxus_threat_wall_module.py` from the mod root.
The final source art belongs to Fractal Softworks. This is a reviewable art
asset, not yet a replacement for the15stock Nexus pieces in the awakening test.

The later `seam_cleanup_20261117/` study contains the user-authorized ImageGen
cleanup and a localized ImageGen+Poisson variant. Those versions introduce new
join pixels; this original master and its pixel-only provenance remain intact.
