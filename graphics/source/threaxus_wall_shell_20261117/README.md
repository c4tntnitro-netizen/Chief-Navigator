# Three-wall Threat shell

`supplied_wall.png` is the user's exact 513x225 attachment. The runtime copy is
`graphics/ships/starving_threat/threaxus_wall.png`; both SHA256 hashes are
`5b110268a1b7b3e4a02e40507a463a037209f1011ffd346fec6187573dd4f62c`.
The image bottom faces outward. No recoloring or resizing occurs on import.

`wall_audit.json` records the 179-point silhouette, fourteen visible-pad gun
mounts and three side attachment poses. `weapon_mounts.png` annotates the plan;
`assembly_preview.png` uses those poses without game-rendered gun housings.
The planning grid is an inspection enlargement, not a runtime asset.

Reproduce with `tools/build_threatxus_prototype.py` and
`tools/render_threaxus_wall_shell.py`. Eight native/runtime checks and packaging
evidence are in `out_threaxus_wall_shell_20261117`. The user has seen the shell
in game; brown haze over its six launcher mounts was traced to false GraphicsLib damage smoke. The wall-only post-advance fix is verified in `out_threaxus_launcher_smoke_20261117` and requires a game restart.
