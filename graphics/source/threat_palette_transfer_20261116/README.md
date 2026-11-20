# Threat palette recolor comparison

Recolored the user's first attached orange industrial ship using the palette
of their second attached Threat Hive image. These are separate asset studies;
no live game sprite, ship data, boss, source class or JAR is replaced.

- [Side-by-side preview](comparison.png): ImageGen left, distribution transfer right.
- [Built-in ImageGen recolor](imagegen_recolor.png): transparent 1041x1511 PNG;
  a generative interpretation of the recolor. [Exact prompt](imagegen_recolor.prompt.txt).
- [Distribution-based color transfer](distribution_transfer.png): transparent
  248x360 PNG with original canvas, per-pixel alpha and geometry unchanged.
- [Distribution transfer enlarged 3x](distribution_transfer_3x.png): nearest-neighbor preview.
- [Original first image](source_original.png) and [palette reference](palette_reference.png).

The numerical method performs 64 deterministic iterative 3-D color-distribution
matches in CIELAB using randomized orthogonal projections and weighted histogram
CDFs, followed by a luminance CDF match. Foreground alpha weights the statistics;
transparent background pixels are excluded. It changes only colors, with no
resampling, drawing, masking edits, sharpening or geometric operations. Equal
original colors receive equal output colors. This matches the palette's full
distributions more closely than a single mean/standard-deviation adjustment.

Reproduce with `python distribution_transfer.py`. The script uses Pillow and
NumPy, records its seed and palette statistics in `distribution_audit.json`, and
asserts exact alpha/size/transparent-RGB preservation. `make_comparison.py` only
resizes copies for the labeled preview; delivered masters are unchanged.

The distribution result follows the reference's darker overall tonal
distribution. ImageGen chooses a lighter ivory interpretation and can redraw
small details. Both are preserved for comparison, with no runtime integration.

Input filenames from the user:
`codex-clipboard-979a2ad1-7ed0-4c08-9598-f00321092aa1.png` and
`codex-clipboard-1d9e7896-e480-47c7-9cb0-d99ca8e8ee86.png`.
These are supplied Starsector sprite references; retain their original asset
ownership and applicable terms.
