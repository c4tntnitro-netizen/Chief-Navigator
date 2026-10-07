# Drifting Wall fight rebuild

The replacement Drifting Wall fight starts from one inert station base. It is
not derived from the old seven-berth curved frame.

## Artwork pipeline

The authored source is the left half of the structure. Its exposed outer end
is destroyed, broken, and charred while its right edge is kept clean as the
join. `tools/build_drifting_wall_base_v1.ps1` performs four deterministic
steps:

1. remove the generated magenta matte;
2. crop only fully transparent columns beyond the right join;
3. abut the half to an exact horizontal mirror;
4. enlarge the assembled sprite by 2x with nearest-neighbor sampling.

The important files are:

- `graphics/source/chief_navigator_drifting_wall_module_half_destroyed_v1_chroma.png`
  - generated edit source;
- `graphics/ships/chief_navigator_drifting_wall_module_half_destroyed_v1.png`
  - transparent high-resolution half;
- `graphics/ships/chief_navigator_drifting_wall_base_v1.png`
  - mirrored complete base, 5,632 by 2,216 pixels;
- `graphics/ships/chief_navigator_drifting_wall_base_domain_v2.png`
  - current combat/campaign base in the oxidized pale-aqua Domain mothership
    palette, on the same 5,632 by 2,216 hull canvas;
- `graphics/weapons/chief_navigator_single_rail_station_gun_domain_v3.png`
  - matching single-rail station turret artwork;
- `graphics/weapons/chief_navigator_drifting_wall_colossal_inert.png`
  - unsplit combat-scaled, pivot-corrected source for the surviving colossal
    emplacements; the historical weapon ID is retained for compatibility;
- `graphics/source/chief_navigator_drifting_wall_colossal_destroyed_chroma.png`
  - generated flat-magenta source for the inert front-left naval wreck;
- `graphics/weapons/chief_navigator_drifting_wall_colossal_destroyed.png`
  - transparent 420 by 1,160 destroyed emplacement on the exact live-turret
    canvas and pivot;
- `graphics/weapons/chief_navigator_drifting_wall_colossal_base_v2.png`
  - unused recoil-layer experiment retained as source art;
- `graphics/weapons/chief_navigator_drifting_wall_colossal_barrel_v2.png`
  - unused recoil-layer experiment retained as source art;
- `graphics/weapons/chief_navigator_drifting_wall_impulse_slug_v1.png`
  - unused projectile experiment retained as source art;
- `graphics/source/chief_navigator_drifting_wall_annihilator_launcher_chroma.png`
  - generated flat-magenta source for the sealed Wall Annihilator Battery;
- `graphics/weapons/chief_navigator_drifting_wall_annihilator_launcher.png`
  - transparent 210 by 179 combat sprite, shown from directly overhead with
    closed armor and no visible rockets, tubes, bores, or apertures;
- `graphics/source/chief_navigator_drifting_wall_annihilator_destroyed_chroma.png`
  - generated flat-magenta source for the crushed missile-battery wreck;
- `graphics/weapons/chief_navigator_drifting_wall_annihilator_destroyed.png`
  - transparent 210 by 179 inert wreck on the exact live-launcher canvas and
    pivot;
- `tools/prepare_drifting_wall_annihilator_launcher.ps1`
  - deterministic chroma removal, edge despill, crop, and combat downscale for
    the launcher art;
- `tools/split_drifting_wall_colossal_recoil.ps1`
  - deterministic source-art tool for the unused recoil-layer experiment;
- `graphics/source/chief_navigator_drifting_wall_reactor_v1_chroma.png`
  - generated chroma-key source for the exposed rear reactor;
- `graphics/ships/chief_navigator_drifting_wall_reactor_v1.png`
  - transparent oxidized-aqua reactor module with a cyan-white containment
    core;
- `tools/build_drifting_wall_base_v1.ps1`
  - reproducible alpha, mirror, and upscale pipeline.

Both exposed ends are therefore exact mirrors of the same catastrophic damage.
Do not independently repaint one end unless asymmetry becomes an explicit new
direction.

The active foundation and its single-rail turret use the Domain mothership
scheme: oxidized pale-aqua armor, cold gray-white plating, dark charcoal
structure, exposed gunmetal, aged copper seams, and restrained amber details.

## Base hull

The scaffold hull is `chief_navigator_drifting_wall_base`, with variant
`chief_navigator_drifting_wall_base_Standard`. Its sprite center is
`[2816,1108]`, matching the exact center of the PNG. Its built-in hullmods are:

- `vastbulk`, making the foundation function as the non-destroyable body of a
  modular station;
- `reduced_explosion`, preventing the inert foundation from producing a
  disproportionate death effect.

The damaged encounter state begins with three functional colossal naval
emplacements. The front-left emplacement is a permanently inert decorative
wreck: it cannot turn, track, fire, animate, glow, vent, or repair during the
fight. Each surviving gun is a rigid, non-recoiling 10,000-range energy-damage beam turret that
remains mechanically ballistic for socket compatibility. Its three-second
windup ramps an animated pale-cyan timeflow shimmer across the emplacement
without separate glints or spark particles. The shimmer is visual only: it
does not alter turret traverse, ship or target movement, or combat timeflow.
The weapon then fires one straight beam for five seconds at 2,000 DPS,
preserving the previous 10,000 total damage budget, followed by a ten-second
refire delay. Its CSV impact is zero, so the sustained beam cannot accumulate
physics impulses or reduce a target's velocity to zero. The completed firing
cycle then produces a compact bank of dark
smoke centered on the turret pivot, with pale nebular steam and short
high-velocity jets venting laterally from that base. No purge particles are
distributed down the barrel. The three surviving turrets retain
their three-second charge, ten-second refire delay, cannon sound, and linked
autofire group. The fight applies a 20x weapon-health multiplier before ship
creation through the hidden built-in
`chief_navigator_drifting_wall_foundation` hullmod, and the encounter then
restores every live mount to its new maximum health.

The encounter activates engine-default ship AI on the direct-weapon
foundation when the engine has not supplied one, keeps its controls unlocked,
and switches the authored groups into ordinary autofire. Target selection,
traverse, and the decision to shoot remain entirely with vanilla AI; no target
or shot is forced. The intact Ithaca foundation uses the same activation path.
Destroyed turret and launcher fixtures remain in separate autofire-off groups
and are force-disabled during initialization, so they can never become the
representative weapon for a linked live group.

The hull also starts with
two live custom Wall Annihilator Batteries, two inert destroyed batteries on
the left side, and ten standard medium weapons at the
annotated sockets. Each launcher is a closed, pure top-down oxidized-teal
armored box. Nothing in the static sprite exposes a missile, tube, bore,
aperture, or viewer-facing launch mouth; ten Annihilator rockets spawn across
its top/forward edge during the firing animation. It renders beneath all other
weapons so a nearby colossal naval turret always occludes the launcher where
the two sprites overlap. Each battery has 10,000
range, a ten-rocket salvo, a ten-round magazine, and a ten-second full reload.
The symmetric medium pairs mix Heavy Needlers, Heavy Maulers, and Pulse
Lasers. These are built into the foundation, and it has 100,000 flux capacity
plus 5,000 dissipation so all weapons remain operational.
Every hull that directly carries a colossal naval gun also carries the hidden
Ithaca Naval Fire Control package. Its +30,000 combat sight radius lets the
gun acquire targets across a standard battle map without changing weapon
range, damage, or native autofire behavior.
The lowest socket originally marked blue is treated as the fourth large
ballistic slot.

All four colossal naval sockets use full 360-degree traverse, matching the
regular Ithaca wall module's targeting envelope while retaining the authored
nominal angles used for art alignment and initial orientation:

- `COL_NAVAL_01`: 35 degrees, 360-degree arc;
- `COL_NAVAL_02`: 329 degrees, 360-degree arc;
- `COL_NAVAL_03`: 108 degrees, 360-degree arc;
- `COL_NAVAL_04`: 251 degrees, 360-degree arc.

At the base 2.5-degree-per-second traverse, an arbitrary worst-case 180-degree
slew takes 72 seconds; from the Wall's fixed facing toward the ordinary
player-side approach, the largest initial nominal offset is about 109 degrees,
or 44 seconds. This initial slew is intentional and distinct from an arc
exclusion. Preserve the nominal angles rather than normalizing the art; Ithaca's
Macroservos upgrade accelerates friendly emplacements through the existing
weapon-spec clone.

The damaged Wall still replaces `COL_NAVAL_01` with a decorative wreck; its
nominal 35-degree angle remains solely to orient that art, while the intact
Ithaca skin's live replacement inherits the socket's full arc.
`LARGE_BALLISTIC_01` and
`LARGE_BALLISTIC_03` use the decorative destroyed-launcher spec, forming one
coherent left-side damage cluster with that naval wreck. None of these three
destroyed fixtures participates in autofire despite remaining listed in the
variant for explicit, stable placement.

Its collision uses one coarse 43-point concave silhouette around the entire
surviving structure, backed by a 3,100-unit collision radius. The polygon follows
the major stepped shoulders, outer wrecked wings, lower equipment bays, and
central recesses shown in the collision reference, while deliberately ignoring
exposed cables, broken teeth, antennae, sockets, and surface greebles. The
authored PNG is horizontal, but `.ship` bounds and
weapon locations use Starsector's forward/lateral axes: annotated pixel
positions must be converted as `localX = centerY - pixelY` and
`localY = centerX - pixelX`. The exposed reactor is attached as a real
`STATION_MODULE` at local `[-1180,0]`, behind the foundation's authored rear
edge. It has a deliberately coarse collision polygon around its major armored
containment silhouette rather than its individual conduits and repair arms.
The reactor renders at `627 x 627`, exactly half its source sprite's linear
dimensions, with a correspondingly halved collision polygon and 305-unit
collision radius.

The foundation has 20,000 armor and 1,000,000 mass. The encounter uses the
standard `BattleCreationPluginImpl` map dimensions and anchors the Wall at
`(0,3000)`, one and a half 2,000-unit tactical-grid blocks toward the enemy
side, with a fixed `270` degree facing. It zeros all
movement stats plus linear and angular velocity, while deliberately leaving
controls unlocked so the station AI can fire. Its armor grid is restored once
on initialization. These are structural
requirements: the Vast Bulk parent is the immobile center of a much larger
station, not a vessel, and a zero armor grid produces unwanted live damage
decals on its visible sprite.

## Reactor objective and persistence

The exposed rear reactor is the sole victory target. It carries 4,000 hull
and 4,000 armor, emits a subtle cyan combat pulse, and begins repairing at 0.5%
of maximum hull per second after six uninterrupted seconds without taking
damage. Destroying it force-disables the foundation's weapons, systems, and
defenses, then ends combat in the player's favor; the Vast Bulk parent is not
destroyed.

The existing eight-ship Combat Guard screen spawns clear of the foundation
and other ships, then uses native free-moving Fearless combat AI. It has no
fixed reactor posts or pursuit restrictions: normal AI handles targeting,
weapons, maneuvering, venting, and ship systems. No Gargoyles or timed
reinforcement waves are deployed.
The immobile foundation relies entirely on engine-default station AI to select
targets, while its guns retain their full arcs, authored nominal angles,
native autofire, and ordinary collision.

The fixed campaign station remains at the encounter after the battle. Its
autonomous repair systems take 30 campaign days to rebuild the reactor. During
that interval the station interaction reports the remaining time and does not
create a combat proxy. Once the timer expires the same persistent station can
be challenged again with a fully repaired reactor.
