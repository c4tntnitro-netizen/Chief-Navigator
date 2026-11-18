# Tetsu Kannon prototype

Restart Starsector and open **Missions → Tetsu Kannon Test**. You command an
Onslaught, an Eagle and two Hammerheads against a three-wall shell and its
living core. There is no arbitrary loss condition tied to your
initial flagship; the normal fleet battle decides the result. Console
alternative for piloting the fabricator: `AddShip chief_navigator_threatxus_Standard`.

The mobile capital core uses the user's supplied
`C:/Users/myauc/Documents/threatxus.png`, now with three custom Threat wall modules.
It uses `SHIP_WITH_MODULES` without the stationary `STATION` hint, `vastbulk`
or shared station flux mechanics. Legacy `threatxus` IDs remain compatible;
the runtime sprite filename is `graphics/ships/starving_threat/threaxus.png`.
The sprite is copied exactly, at **555x502**, without resizing, repainting or
resampling. The archived master is under `graphics/source/threatxus_20261116`.

## Initial test configuration

| Setting | Prototype value |
| --- | --- |
| Hull / variant | `chief_navigator_threatxus` / `chief_navigator_threatxus_Standard` |
| Hull / armor | 40,000 / 1,600 |
| Base flux capacity / dissipation | 30,000 / 1,200 |
| Dormant movement | Zero speed/acceleration/turning |
| Awake speed / acceleration / turn rate | 70 / 20 / 15 degrees per second |
| Shield | 360-degree omni, 0.9 base flux/damage |
| System | Fabricate Threat Units; automatically cycles after awakening |
| Main weapons | Two Plasma Cannons, two Tachyon Lances |
| Secondary weapons | Two Heavy Blasters, one Ion Pulser |
| Point defense | Five Burst PD Lasers |
| Loadout | 50 vents, 40 capacitors, Integrated Targeting Unit, Hardened Shields |
| OP / deployment points | 315 fitted out of 400 available / 100 DP |
| Engines | None; no engine flares or contrails |
| Surrounding swarm | 1,500 native fragments; original broad skirt plus a denser core |

Group 1 controls Plasma Cannons, group 2 controls Tachyon Lances, and groups
3/4 start the medium battery/PD on autofire when piloting the fabricator.
After awakening the hull uses native forward acceleration, blocks reverse/strafe/braking
commands and removes the zero-flux speed boost. It retains native turning and
collision physics; no position or momentum is forcibly overwritten.
These remain provisional test stats. Gautama retains Extraction Protocol.

## Breakaway shell and awakening

The core begins inert and protected, with its own weapons, shield and factory
dormant. Three copies of `chief_navigator_threaxus_wall_Standard` occupy the
triangle's sides on `WALL_LEFT`, `WALL_RIGHT` and `WALL_REAR`, 80 units outward
from the source triangle's edge midpoints. Each uses the user's exact 513x225
supplied sprite, with its image bottom facing outward. The core's twelve
historical weapon slots remain intact.

Each separate frigate-class wall has 15,000 hull, 1,000 armor, 14,000 flux,
1,000 dissipation, no shield or engines, and `always_detaches`. Its 179-vertex
concave collision outline covers 99.64% of the connected opaque silhouette.
All fourteen mounts face outward; every weapon group starts on autofire:

| Mounts per wall | Stock Threat weapons |
| --- | --- |
| Two large ballistic turrets | Neoferric Quadcoils |
| Two medium ballistic turrets | Heavy Mass Drivers |
| Two medium missile hardpoints | Swarm Launchers |
| Four small ballistic turrets | Light Mass Drivers |
| Four small energy turrets | Voltaic Discharges |

Native Fragment Swarm and Threat hullmods retain ordinary ammunition spending,
regrowth and launcher wing limits. A wall-specific hidden hullmod increases its
local reserve to 200 fragments and native regeneration to ten per second.
The core's private visual cloud remains separate and supplies no ammunition.
Mount and assembly previews are in `graphics/source/threaxus_wall_shell_20261117`.

Destroy every attached module, including armor sections. The last module wakes
the core once: protection ends, its own guns/shield/factory become available,
and it steers toward the nearest visible hostile nonfighter. Native motion uses
70 speed,20 acceleration,15 braking,15 turn rate and25 turn acceleration. It
still has no engine flares. No health refill, teleport or momentum overwrite
occurs. Empty deployment warmup and pause cannot prematurely trigger the change.

Dormant protection uses a ship-scoped `DamageTakenModifier`; the native hull
damage multiplier remains positive so the engine cannot kill the parent when
its last active module dies. Only individual core weapons are suppressed:
native parent hold-fire would also silence the attached module guns.

## Fabrication and assault units

The awakened parent system automatically activates when ready and below its cap, for
NPCs and player-controlled copies alike. Base timing is2s windup,1s active
and25s cooldown: one unit per28-second cycle. Each unit separately assembles
over20 seconds. Separate caps are **three Assault Units and three Fabricators
per Threatxus**, counting active and building units. Destroyed, removed or
captured units free their owned slots. No releases queue behind either cap.

When no Assault Unit is alive or assembling, the next build is an Assault Unit.
Otherwise, Threatxus builds stock `fabricator_unit_Type450` Fabricators until
three exist, then fills its remaining Assault Unit slots. Losing every Assault
Unit immediately makes its replacement the next priority. Each parent tracks
only its own children; other ships never consume its slots.

Fabricators retain their native hull, weapons, AI, Fragment Swarm/Coordinator
and Construction Swarm system. Their assembly receives160 native fragments;
the larger build site leaves80 units beyond both collision radii. Fabricators
can produce ordinary Threat ships through their existing native system.

Construction uses vanilla `ThreatShipConstructionScript` and a separate
hidden construction-swarm carrier at alternating sides of the parent. The
carrier receives native construction protection; **Threatxus stays vulnerable**
and keeps drifting/firing. Native fragment transfer, assembly fading, armor
progression and completion cleanup handle the new body. Deployment warmup
advances full-size ships too, so the assault hullmod applies its construction
gate immediately at ship creation, before any early AI/system tick.

`chief_navigator_exodus_assault` uses unchanged native Assault Unit geometry,
sprite and basic destroyer stats. It has **no weapon slots or fitted guns**,
no engine flares, and **Exodus Plague as its only attack**. Its custom AI
approaches visible enemies, steers, manages spacing/shields and triggers the
existing plague system. The two emitters use the old forward mount positions
`[50,+21]` and `[50,-21]`. The attack retains its1s/5s/20s timing, tapered400-packet
burst, PD interception, kinetic hits and Defabricate caps/lifetime.

Each assault unit receives400 native fragments, ten times a normal destroyer's40.
Threatxus's1,500-fragment visual is private and supplies no ammunition. Assault
clouds register for the native construction handoff but have no exchange class
or ordinary fragment weapons. Construction pauses their initial fill/respawn;
the assembly transfer produces the full400-member cloud on completion. Clouds
use native sprite sizes. Threatxus replaces the native hull-following band with
a filled, diffuse oval: maximum local extents approximately1,005 forward/back
and904 laterally. The original1,000-member broad component uses area density
proportional to `(1-r*r)^2`, with normalized elliptical radius `r`, reaching zero
smoothly at the outskirts. Another500 members use the same distribution at
half radius (one-quarter area), adding twice the original peak density. This
triples the existing density at the center while preserving the broad skirt's
width and expected density. Most fragments stay closer to
the hull while a sparse skirt extends beyond600 units. There is no inner
exclusion ring or clipped, densely populated rim. Assault units retain their
existing native spread and400-member count; all Exodus attack behavior is unchanged.
They follow hull alpha, freeze on pause and disperse on death/removal. Private
expiry cannot erase another registered cloud.

## Collision segmentation

The builder thresholds source alpha at 96, selects the connected hull,
traces its external contour and simplifies it with a 2.5-pixel tolerance.
The resulting **146-segment closed concave outline** covers **99.32%** of the
thresholded solid silhouette. It keeps the exterior cutouts between spines
open instead of wrapping the hull in a convex polygon or rectangle. The
collision radius encloses every vertex, and the polygon has no repeated
vertices or nonadjacent edge intersections.

An ordinary Starsector `.ship` outline has one closed boundary, so fully
enclosed transparent holes remain solid for collision. No custom collision
plugin or independently damageable segmentation is implied.

The `.ship` center is `[277,226]` measured from the sprite's bottom left,
equivalent to image pixel `[277,276]`. Conversion uses local forward
`276 - pixelY` and local left `277 - pixelX`. The usual upright art faces north
at combat facing90. Native slot transforms were checked at all four cardinal
facings. The supplied image and collision coordinates use the same scale.

## Nexus-derived weapon layout

Installed `starsector-core/data/hulls/remnant_station2.ship` is the Nexus core;
it uses `remnant_station1.png`, with a native center of `[256,228]`. Its slots
are station-module anchors, not weapon mounts. This prototype retains twelve
of their IDs and outward angles, converts them to normal energy turrets and
aligns their positions to the supplied art's visible mounting pads.

| Native anchor | Ship mount | Fitted weapon |
| --- | --- | --- |
| WS001 | Forward tip, small | Burst PD Laser |
| WS007 / WS016 | Rear outer wings, small | Burst PD Lasers |
| WS032 | Rear center, medium | Ion Pulser |
| WS033 / WS034 | Midsection pair, medium | Heavy Blasters |
| WS035 / WS038 | Outer shoulders, large | Tachyon Lances |
| WS036 / WS037 | Forward pair, large | Plasma Cannons |
| WS039 / WS040 | Rear inner pair, small | Burst PD Lasers |

[Collision and mount overlay](../graphics/source/threatxus_20261116/collision_and_mounts.png)
shows the green outline, orange large mounts, purple medium mounts, blue small
mounts and white origin. `geometry_audit.json` beside it
retains every native source anchor, projected coordinate and adjusted mount.

## Verification and reproduction

Run `python tools/build_threatxus_prototype.py` to reproduce owned art/data
outputs, and `out_threaxus_launcher_smoke_20261117/verify.ps1 -Mode installed` for the
current scoped native/runtime checks.

The diffusion check samples the actual1,500 native members and verifies
decreasing density per annular area, a substantial outer skirt, sparse edges
and unchanged assault-unit spread. Latest scoped packaging adds only
`ThreaxusSwarmLauncherEffect.class`; all 355 previous archive entries retain
their exact bytes. The user's live screenshot confirms the three-wall assembly is present.
The brown launcher haze came from GraphicsLib's `WeaponDamageSmoke`: it treats
native scripted waiting at the four-swarm cap as damage. The wall-only launcher
registration inherits every native stat, sprite, tag, wing/fragment gate and
on-fire callback. `ThreaxusSwarmLauncherEffect.advanceAfter` clears only the
scripted disable flag after native firing operations and preserves waiting with
one-frame no-fire. Actual weapon damage still produces smoke. Its init hook
reuses the stock launcher's GraphicsLib material/normal/surface maps. Stock
launchers on every other hull remain unchanged. Restart to load this fix.
The builder is byte-idempotent and preserves unrelated CSV records.

Passed against this machine's installed RC7 runtime:

- Actual native `.ship` loading, all146 collision segments and zero engine slots.
- Three custom walls, separate attachment slots, outward poses at all four
  cardinal facings, 42 native weapon fits, breakaway loadouts and mobile-parent hint.
- Actual GraphicsLib smoke reproduction on a fully healthy, capped launcher;
  post-advance fix removes false smoke while preserving true damage smoke, native
  cap/depletion/startup gates, hull scope and stock texture mapping.
- Actual 200-fragment wall reserve, ten-per-second native regrowth, launcher
  readiness, ammo refill and depleted-reserve firing gates.
- Actual native weapon loading and fitting for all12 mounts.
- Native gun-position transforms at0/90/180/270 degrees.
- Native CSV parsing, positive movement stats and absence of station/module hints.
- The game's Janino compiler compiles the loose mission; its actual callback
  creates the four-ship player strike force against enemy Threatxus on the
  open16,000-square map.
- Actual native1,500/400-member clouds, private-map isolation, drift commands,
  native20s construction/completion transfer, parent vulnerability, one-per-cycle
  assault/Fabricator fabrication, priority, independent three-live caps/reuse,
  ownership and per-parent isolation. Native stock Type450 hull/loadout resolves.
- Pre-construction deployment warmup cannot fire Exodus; the finished unarmed
  unit navigates and emits the full400-packet burst. Existing Exodus/PD/cap tests
  and Gautama carrier-launch checks pass.
- Dormant core damage guard, live module guns, all-module/pause/warmup gate,
  vulnerable awakened core, one-time pursuit boost and fabrication unlock.
- Source/runtime sprite hashes match, collision outline is simple, fitted OP
  is legal, and a repeated build produces identical owned files.

The selected runtime is `ChiefNavigator.threaxus-launcher-smoke-20261117.jar`.
All prior JARs remain;355 previous entries are preserved byte-for-byte. The
ship has no faction imports or campaign encounter placement.
Live flight, collision feel, weapon rendering and balance still need review in
the new mission after restart; headless checks are not an in-game playtest.

The supplied composition uses Starsector-derived artwork; retain its source
and Fractal Softworks' applicable asset ownership/terms.

## Pixel-only Threat wall study

`graphics/source/threaxus_threat_wall_module_20261117/threaxus_wall_module.png`
is a separate512x224 art study, about one central triangle side in length.
It uses only literal RGBA pixels from the seven supplied native Threat sprites;
cutouts, quarter-turns, mirrors and a binary silhouette mask introduce no new
visible pixel values. `pixel_provenance.npz` records each output pixel's source
image and coordinate; `audit.json` verifies exact equality. Reproduce with
`python tools/build_threaxus_threat_wall_module.py`. This piece is an art asset;
the installed test shell uses the later ImageGen-cleaned and palette-transferred
513x225 sprite explicitly supplied by the user. That later artwork is not
claimed to consist exclusively of unchanged native source pixels.
