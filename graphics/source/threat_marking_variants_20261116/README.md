# Slight marking variants

Final deliverables are **80x92 RGBA**, the exact size of the user's original.
They preserve its alpha, silhouette, geometry and all pixels outside the small
painted marks. No source resizing or geometric edit occurs.

- [Variant 1](variant_01.png): subtle offset traces, 50 changed pixels.
- [Variant 2](variant_02.png): broken bands and paired dashes, 42 changed pixels.
- [Variant 3](variant_03.png): small block accents, 23 changed pixels.
- [Original](original.png).
- [Native-size comparison](comparison_native.png): original plus three variants,
  placed side by side without resizing.

Final artwork uses deterministic markings-only edits on protected armor faces,
retaining local shading and the existing muted palette. `make_marking_variants.py`
reproduces the result; `audit.json` records exact alpha preservation, restricted
pixel changes and file hashes. The user explicitly requested no upscaling;
the current script neither enlarges nor resizes the source or final sprites.

The earlier ImageGen attempt expanded the cropped part and was rejected for
changing its shape. It and its prompts remain historical studies, separate from
the final native-pixel variants. Earlier enlarged inspection previews are also
historical, not final deliverables. No runtime sprite or game data was replaced.
