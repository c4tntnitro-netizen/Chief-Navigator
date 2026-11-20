# Remnant station recolored with Fabricator palette

User requested algorithmic distribution transfer only. No ImageGen was used.

- [Result](distribution_transfer.png): 512x512 RGBA, original dimensions and
  per-pixel alpha exactly preserved.
- [Enlarged preview](distribution_transfer_3x.png): nearest-neighbor 3x view.
- [Original station](source_original.png): exact copy of the supplied installed
  `starsector-core/graphics/ships/remnants/remnant_station1.png`.
- [Palette reference](palette_reference.png): exact copy of the supplied
  `starsector-core/graphics/ships/threat/fabricator.png`.

Uses the same deterministic 64-iteration CIELAB distribution-transfer method
as the preceding sprite recolor. Random orthogonal projections match weighted
foreground color CDFs, followed by a luminance CDF match. Alpha weights the
statistics; transparent pixels are excluded. Original geometry, alpha and
hidden RGB are preserved. No resampling or spatial edits occur in the result.

Run `python distribution_transfer.py` to reproduce. `distribution_audit.json`
records source/output hashes, color statistics and exact preservation checks.
Original installed game files and active mod art are unchanged. These derived
Starsector assets retain their original ownership and applicable terms.
