# Stronger seam cleanup

User requested more integration after the first ImageGen cleanup. This second
built-in edit reconstructs the remaining horizontal/vertical crop joins into
continuous armor ribs, pipe connections and overlapping plates. The broader
generative edit is retained; no conservative Poisson mask is reapplied.

`threaxus_wall_module_clean_v2.png` is512x224, nearest-sampled from the raw
1897x829 result and registered to the original native sprite bounds. Its entire
alpha channel exactly matches the original512x224 input in the parent folder.
The raw generated image, latest attached edit target, exact prompt,
normalization script and audit are retained here. Previous versions remain.

This is source art only; runtime hulls, sprites and selected JAR are untouched.

`palette_restored_20261117/` contains the later deterministic color-distribution
transfer: the attached earlier palette restored onto this cleaned geometry,
with512x224 dimensions, exact alpha and no spatial filtering.
