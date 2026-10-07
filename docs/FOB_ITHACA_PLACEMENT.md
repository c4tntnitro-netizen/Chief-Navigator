# FOB Ithaca campaign-sprite placement guide

FOB Ithaca uses one persistent visible controller plus three persistent,
non-clickable station-mode fleets beneath the north, east, and south bays.
One additional visible station-mode fleet, **Breach Defense Station**, occupies
the center of the western breach.
Each fleet is paired with an invisible fixed interaction token whose radius
matches the Drifting Wall.
The controller draws the central station and larger orbital structure, each
with a crisp additive equipment-light mask. The old eight-piece procedural
ring is retained only as unused source art; it is no longer assembled at
runtime.

## Relevant files

- `graphics/campaign/fob_ithaca/ithaca_station.png` is the transparent sprite.
- `graphics/campaign/fob_ithaca/ithaca_station_glow.png` isolates the central
  station's lamps and illuminated machinery. Its emitted RGB is sampled from
  the orbital glow asset when the mask is rebuilt.
- `graphics/campaign/fob_ithaca/ithaca_orbital_ring_v4.png` is the live outer
  structure. Three intact cardinal bays use the same wide, stepped fortress
  silhouette. The entire left bay is missing, leaving a broad open arc with
  two severed, jagged rail stumps. There are no antennae, dishes, masts, or
  booms anywhere on the ring.
- `graphics/campaign/fob_ithaca/ithaca_orbital_ring_glow_v4.png` is its
  aligned additive glow layer.
- `data/config/settings.json` registers all four textures under the matching
  `chief_navigator_fob_ithaca_*` keys.
- `src/chiefnavigator/campaign/FobIthacaCampaignEntityPlugin.java` controls
  its rendered size, rotation, and visual offset.
- `src/chiefnavigator/quest/OdysseyExpanseSystem.java` controls the campaign
  location of the complete entity and repairs its persistent backing fleets
  and interaction tokens. It also creates and anchors Breach Defense Station at
  `(-1620, 0)` relative to Ithaca.
- `src/chiefnavigator/quest/IthacaSectionEncounter.java` owns the stable
  north/east/south station IDs, their offsets, combat fleets, their `450`-unit
  interaction tokens, and their campaign-sprite suppression.

## Change the sprite's rendered size

At the top of `FobIthacaCampaignEntityPlugin`:

```java
private static final float STATION_SIZE = 1500f;
```

The sprite is square, so one value controls both width and height.

- Increase it to make the station larger.
- Decrease it to make the station smaller.

The PNG includes a narrow transparent margin. Therefore its visible metal is
slightly smaller than `STATION_SIZE`.

## Tune the orbital ring and glow

```java
private static final float ORBITAL_RING_SIZE = 3800f;
private static final float ALWAYS_VISIBLE_RENDER_RANGE = 100000f;
private static final float ORBITAL_RING_ANGLE = 0f;
private static final float ORBITAL_GLOW_ALPHA = 0f;
private static final float STATION_GLOW_ALPHA = 0f;
private static final float TWINKLE_SPEED = 0.45f;
private static final float TWINKLE_DEPTH = 0.07f;
private static final float STATION_TWINKLE_OFFSET = 1.9f;
```

Each body and its glow always use the same size and angle, so neither mask
extends beyond its body's logical footprint. Both glow alpha controls are
currently zero, fully disabling the yellow additive layers while retaining
their source masks for possible later use.

`TWINKLE_SPEED` advances a slow continuous three-harmonic opacity wave and
`TWINKLE_DEPTH` controls its strength. At `0.07`, opacity varies by no more
than roughly seven percent. `STATION_TWINKLE_OFFSET` keeps the central lights
from pulsing in lockstep with the orbital ring. Twinkle affects opacity only;
it never changes sprite size, position, or angle.

The visible ring is much larger than the transparent entity controller. Keep
the matching entry in `data/config/custom_entities.json` at:

```json
"spriteWidth": 3800,
"spriteHeight": 3800
```

These values inform the campaign engine of the composite's real visual bounds;
they do not change the central controller's `750`-unit interaction radius or
the three bastion tokens' `450`-unit radii. `ensureFobIthaca()` also sets both
the sensor profile and discoverable override to `null`, matching the visibility
model used by planets: the installation is known scenery, not a sensor contact
that fades in when detected. Existing detected-range overrides are removed
during the same repair. Together, the real visual bounds prevent early
viewport culling, the render range keeps the custom plugin active, and
planet-style visibility prevents the ring, modules, and center from popping
into view together.

The 29 invisible collision beads approximating the surviving ring must use a
`null` sensor profile and `null` discoverable override. A numeric profile of
`0` is still acquired as a close-range contact by the campaign engine and
causes repeated detection pings while a fleet passes the ring. The transient
ring-collision controller migrates serialized beads to the profile-less state
once per game load without deleting or recreating them.

The central custom entity interaction radius is `750`, matching the 1,500-unit
central FOB rather than the 3,800-unit orbital ring. Each north, east, and
south bastion has a separate invisible `450`-radius custom entity, matching the
Drifting Wall's interaction size and routing interaction to its persistent
station fleet. The ring and its breached interior remain traversable campaign
space: fleets fly through the opening and approach the center or a bastion to
interact. `OdysseyExpanseSystem.ensureFobIthaca()` replaces an old-radius
central token under the same persistent entity ID and restores the three
completed-bastion flags, so existing saves migrate without using forbidden
reflection. This does not clip the artwork or affect its `100000`-unit
rendering range.

The three player-facing interaction tokens remain fixed relative to the
controller at:

```text
north   (   0, +1650)
east    (+1650,    0)
south   (   0, -1650)
```

The corresponding invisible, fightable station fleets sit slightly farther
out at a radius of `2150` units (`north 0,+2150`, `east +2150,0`, and
`south 0,-2150`). The campaign ring collision resolves outside attackers to
approximately `1930` units from the controller, so this separation keeps the
station battle target reachable without moving the rendered module or its
large player click target. Each backing fleet uses a fixed-location orbit state,
and `FobIthacaRingCollisionScript` re-pins all three every campaign frame;
station mode and disabled AI alone do not prevent hostile fleet collision from
translating a campaign station.

Breach Defense Station sits directly on the broken ring centerline at
`(-1620, 0)`, between the two Spartan mouth formations at `(-2150, +500)` and
`(-2150, -500)`. It is intentionally visible and fightable on the campaign
map, uses `chief_navigator_spartan_battlestation_Teal`, and is re-pinned every
frame with the three hidden bastions. Labor V's final Heavenly Strike attacks
this real fleet through an invisible 100-unit assault coordinate, then closes
with an `INTERCEPT` assignment; the station itself remains the fleet AI's
tactical priority target.

Ambient NPC fleets ignore this defender and receive native do-not-attack
orders. The canonical Heavenly Strike is exempt, and its final assault
explicitly makes the station eligible for combat again. Before that assault,
an absent station or its idle empty core can be restored under the same
historical ID; a committed center assault or final-Labor failure forbids
restoration. No surrounding stations, gates, or campaign topology are rebuilt.

The perimeter invasions retain their existing targets: **Scylla Strike**
attacks north with a Hive-majority mixed roster, **Siren Strike** attacks
south with Skirmish scouts and Line units, and **Cyclops Strike** attacks
east with a Fabricator-heavy mixed roster. All six Starving roles remain
represented, and their deployment costs stay approximately equal to the
previous Third Strike. These compositions apply when fleets are created;
upkeep never replenishes casualties or rewrites a surviving saved roster.

Their flagship campaign sprites are overridden with `graphics/fx/empty.png`
at `1 x 1` while idle because the matching modules are already baked into the
outer-ring art. A colocated invisible custom token gives each bastion a
`450`-unit interaction radius, exactly matching the Drifting Wall. The backing
fleets remain complete `CampaignFleetAPI` stations, so they can join battles
as stations without drawing duplicate 8,192-pixel combat walls on the campaign
layer. The override is replaced with the wall's real combat sprite before a
section engagement, and the three stations are rebuilt under the same stable
IDs when the fleet dialog closes. Do not use reflection for this migration or
representation.

The glow is multiplied by `ORBITAL_GLOW_TINT`. It is currently pale warm gold
(`255, 255, 220`), preserving the mask's yellow equipment-light color while
letting it stand out against the darkened metal.

## Tune structural darkness

Both the inner station and outer ring are multiplied by one opaque tint:

```java
private static final Color STRUCTURE_TINT =
        new Color(115, 110, 115);
```

This keeps the source textures nondestructive while reducing them to roughly
45% of their original RGB brightness. Raise all three values to brighten the
installation, or lower them to make it recede further into Ashen Verge. Keep
the three values close together unless a deliberate color cast is wanted.

## Rotate the whole station

```java
private static final float STATION_ANGLE = 0f;
```

Angles use degrees:

```text
0       original orientation
90      quarter-turn counter-clockwise
180     upside down
-90     quarter-turn clockwise
```

This angle is fixed. The renderer also forces the entity's facing to zero, so
the station does not turn to face the player's fleet.

## Nudge the artwork without moving the entity

```java
private static final float OFFSET_X = 0f;
private static final float OFFSET_Y = 0f;
```

- Positive X moves the artwork right/east.
- Negative X moves it left/west.
- Positive Y moves it up/north.
- Negative Y moves it down/south.

Use these only to correct centering inside the PNG. They do not move the map
label, detection marker, or interaction radius.

## Move the complete entity in Ashen Verge

`OdysseyExpanseSystem.ensureFobIthaca()` contains:

```java
ithaca.setFixedLocation(4500f, 0f);
```

- The first value is east/west.
- The second value is north/south.

Changing this moves the sprite, map icon, label, and entity radius together.

## Replace the artwork later

Keep every replacement PNG square, top-down, centered, and transparent. Either
overwrite the existing file or register a new sprite key and update its paired
renderer constants together:

- station: `SPRITE_KEY` and `TEXTURE_PATH`
- orbital structure: `ORBITAL_RING_SPRITE_KEY` and
  `ORBITAL_RING_TEXTURE_PATH`
- central sharp glow: `STATION_GLOW_SPRITE_KEY` and
  `STATION_GLOW_TEXTURE_PATH`
- orbital sharp glow: `ORBITAL_GLOW_SPRITE_KEY` and
  `ORBITAL_GLOW_TEXTURE_PATH`

The renderer explicitly uploads and checks all four textures, so a bad path
produces an error naming the missing file.

The v4 ring was created as a built-in image-generation palette edit using the
v3 ring as the exact shape target and the Drifting Wall as the material/color
reference. Its attached modules now use oxidized pale-aqua/teal Domain armor,
while the connecting ring uses cold gray, gunmetal, charcoal structure, aged
copper seams, and restrained amber equipment lights. The v3 ring was created
using the v2 ring as the edit target, the requested fortress-module silhouette as a shape
reference, and `ithaca_station.png` as its material/style reference. Its flat
magenta source matte was converted to alpha by
`tools/remove_fob_ring_chroma.ps1`; `tools/build_fob_ithaca_glows.ps1` then
extracts the aligned warm equipment-light mask. Keep the old ring and glow as
non-runtime reference assets so the color-matching step remains reproducible.

## Build and test

From PowerShell in the `Chief Navigator` directory:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Then fully exit and restart Starsector. Reloading a save without restarting
does not reload Java classes or the registered campaign texture. Existing
saves reuse the same FOB Ithaca entity and automatically receive the new
renderer after restart.
