# ImageGen seam cleanup

User authorized ImageGen to clean the composite seams and asked about computer
vision methods. The earlier pixel-only master is retained unchanged one folder
above (SHA256fc68f95ff13247a2157d1241a7561613dac80460ba62c1ce3e58f7822ba913cf).
This cleanup intentionally permits new RGB values; it is not a pixel-only edit.

- `input.png`: exact new attached512x224 image, the edit target.
- `imagegen_raw.png`: built-in ImageGen output,1897x829; prompt in `prompt.txt`.
- `imagegen_native.png`: ImageGen crop registered by robust alpha bounds and
  nearest sampling to512x224, then restored to the input's exact alpha channel.
  This is the broader generative cleanup; source textures may be reinterpreted.
- `threaxus_wall_module_clean.png`: conservative ImageGen+OpenCV version.
  Source provenance identifies abrupt patch boundaries, expanded to7pixel
  neighborhoods while guarding the silhouette. NORMAL_CLONE Poisson integration
  matches the generated gradients to the original boundary colors.11074pixels
  change inside11336seam-neighborhood pixels; every other RGBA pixel and the
  entire original alpha channel remain exact. No global smoothing is applied.
- `seam_edit_mask.png`: binary map of permitted conservative edits.
- `finish_cleanup.py`: reproducible native-size registration and local cleanup.
- `audit.json`: exact sizes, masks, invariants, counts and hashes.

References: OpenCV GraphCutSeamFinder chooses seams between overlapping images;
seamlessClone performs gradient-domain Poisson blending. Graph-cut selection
was discussed but not applied here; this study applies local Poisson blending
after ImageGen. NORMAL_CLONE follows the generated gradients; MIXED_CLONE can
retain the original strong cut boundary instead.

https://docs.opencv.org/doc/doxygen/html/db/dda/classcv_1_1detail_1_1GraphCutSeamFinder.html
https://docs.opencv.org/4.12.0/df/da0/group__photo__clone.html

Original/runtime sprites, ship data and the selected gameplay JAR are untouched.

`v2/` contains the later, user-requested stronger ImageGen seam reconstruction,
including its512x224 version with the original native alpha silhouette restored.
