# Restored Threat color distribution

User requested algorithmic color-distribution transfer from the attached earlier
wall sprite onto the latest seam-cleaned v2. Source geometry is v2; the attached
512x224 image supplies only the color distribution. Both inputs are retained.

Final: `threaxus_wall_module_clean_v2_palette_restored.png`,512x224 RGBA.
64deterministic orthogonal-projection histogram matches in CIELAB, with
alpha-weighted foreground CDFs and a final luminance-CDF match. Identical
source colors map identically. No pixel displacement, interpolation, spatial
filtering or ImageGen occurs in this step. Source alpha and transparent RGB
remain byte-exact; the cleaned seams and silhouette stay in place.

`distribution_transfer.py` reproduces the result. `distribution_audit.json`
records source/reference/output hashes and color statistics. Mean absolute
Lab marginal quantile errors fell from2.239/1.230/.757 to.012/.028/.029.
Output SHA256ddd29298c1d94e207e8308c9ba8d41a7e6a874e3d4b10ecf873b973f9341225c.

All earlier art is retained. No runtime sprite, ship data or gameplay JAR changes.
