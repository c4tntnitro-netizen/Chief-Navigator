# Full-resolution palette restoration

User explicitly selected attached image1 as the full-resolution edit target and
image2 as the palette reference. Their exact files are retained here as
`source_original.png` (1897x829) and `palette_reference.png` (512x224).

Final: `threaxus_wall_module_fullres_palette_restored.png`,1897x829 RGBA.
64 deterministic CIELAB projected-distribution/CDF matches, alpha-weighted,
with a final luminance-CDF match. Repeated RGB colors are aggregated with their
exact alpha mass for efficiency, then the color lookup is applied at every
original pixel. This is not sampling, resizing or spatial filtering.

Original alpha and transparent RGB remain byte-exact. Identical source colors
map identically. The source geometry and full resolution stay intact.
Lab marginal quantile mean absolute errors fall from2.113/1.207/.712 to
.011/.025/.023. Script and audit reproduce/document the operation.
Final SHA2564265f9be0a08ba797cdbbe8336da0a21fd0b00fa1db64342c95679979497b069.

All earlier art remains. No runtime sprite, ship data or gameplay JAR changes.
