# Starving Threat Weapon Reference

Starving Threat fleets and all three Gautama variants use the **unmodified
vanilla Starsector THREAT arsenal**, not Chief Navigator's bone-themed weapon
copies or weapons from **Emergent Threats**. Their authored weapon slots,
groups, autofire settings, and counts are unchanged. Gautama retains two
Neoferric Quadcoils and four Heavy Mass Drivers in his thirteen-gun loadout.

Ordinary Starving variants retain their existing exclusion of the six
fragment-dependent weapons: `swarm_launcher`, `seeker_fragment`,
`kinetic_fragments`, `unstable_fragment`, `devouring_swarm`, and
`voltaic_discharge`. Gautama remains an authored exception, using stock
`devouring_swarm`, `unstable_fragment`, and `voltaic_discharge`.

The twelve `chief_navigator_bone_*` definitions remain **inactive prototypes**.
No active Starving or Gautama variant equips them, and the hull generator no
longer substitutes them for stock IDs. The tables below archive their former
specifications, not the stats of weapons currently fielded. These prototypes
have no faction loadout-pool tag and remain blocked from dealers, selling,
ordinary drops, and post-battle salvage.

Damage abbreviations used below: **K** = kinetic, **HE** = high explosive,
**E** = energy, and **F** = fragmentation. Damage and flux figures are per
projectile unless the table says otherwise. Ammunition regeneration is shown
in rounds per second; reload size is the batch size from `weapon_data.csv`.

## Archived self-contained prototypes

| Weapon | Mount and role | Combat data | Firing and ammunition | Special effects | Former loadouts |
|---|---|---|---|---|---|
| **Femur Torpedo**<br>`chief_navigator_bone_neutron_torpedo`<br>4,000 credits | Small missile<br>Anti-shield torpedo | 2 OP; 1,200 range; 2,500 K; 75 impact; 0 flux; speed 900; turn rate 10 | One round; 10-second recovery; no regeneration | A single-use torpedo travelling at three times the stock projectile speed. | Skirmish Type 101; Line Type 301 |
| **Rib Driver**<br>`chief_navigator_bone_light_mass_driver`<br>3,000 credits | Small ballistic<br>Anti-shield | 5 OP; 700 range; 90 K; 10 impact; 60 flux; speed 900; turn rate 30 | One shot per second; unlimited ammunition | Every shot reaches 350 range, but only 50% survive to the full 700 range. | Assault Types 200/201; Fabricator Type 450; Line Types 300/302 |
| **Spine Driver**<br>`chief_navigator_bone_heavy_mass_driver`<br>6,000 credits | Medium ballistic<br>Anti-shield | 8 OP; 800 range; 200 K; 40 impact; 140 flux; speed 900; turn rate 12 | One shot per second; unlimited ammunition | Its underpowered traverse tracks slowly. Every shot reaches 400 range, but only 50% survive to the full 800 range. | Skirmish Types 100/101; Overseer Type 250; Fabricator Type 450; Line Types 300/301/302; Gautama (four) |
| **Vertebral Quadcoil**<br>`chief_navigator_bone_neoferric_quadcoil`<br>10,000 credits | Large ballistic<br>Anti-armor | 10 OP; 900 range; 200 HE and 10 impact per shot; 200 flux per shot; speed 900; turn rate 30 | Eight-shot burst at 0.1-second intervals; 8-second total cycle; 160 rounds with no regeneration | Delivers 1,600 HE per complete burst. Its finite magazine supports twenty complete bursts. | Line Types 300/301/302; Gautama (two) |
| **Osteon Cannon**<br>`chief_navigator_bone_voltaic_cannon`<br>5,000 credits | Small energy<br>Suppression | 2 OP; 800 range; 300 E plus 500 EMP; 500 flux; speed 300; turn rate 50 | 0.5-second charge; starts with two charges; regenerates one charge every 90 seconds | Hull or armor hits create 10–14 additional 500-EMP arcs. On a shield hit, each arc can pierce based on the target's hard-flux level above 10%, modified by its shield-piercing multiplier. | Skirmish Type 101; Overseer Type 250; Line Type 301 |
| **Skullblaster**<br>`chief_navigator_bone_voidblaster`<br>7,000 credits | Medium energy<br>Anti-armor | 8 OP; 700 range; 300 HE; 200 flux; speed 5,000; turn rate 15; fixed spread 10 | One shot per 0.25 seconds; 12 rounds; regenerates 0.5 rounds per second in batches of four | Deals soft flux to shields. Every two shots—approximately every 0.5 seconds at its base firing rate—detonates a 1,500-HE explosion on its own mount, damaging the firing ship. | Assault Types 200/201 |

## Archived fragment-dependent prototypes

| Weapon | Mount and role | Combat data | Firing and ammunition | Special effects |
|---|---|---|---|---|
| **Ossuary Swarm Launcher**<br>`chief_navigator_bone_swarm_launcher`<br>30,000 credits | Medium missile<br>Special | 10 OP; dummy 1-range projectile data; 0 flux | 0.25-second charge plus 1.25-second recovery; 30 rounds with no regeneration | Consumes 50 Fragment Swarm members per launch and can control up to four attack swarms. It cannot be fired manually and launches whenever possible. |
| **Marrow Seeker**<br>`chief_navigator_bone_seeker_fragment`<br>3,000 credits | Small missile<br>General | 2 OP; 2,000 range; 600 HE; speed 300 | One-second cycle; one round; regenerates 0.2 rounds per second | Converts ten Fragment Swarm members into an excellent-tracking guided missile. |
| **Bone Shard Battery**<br>`chief_navigator_bone_kinetic_fragments`<br>3,000 credits | Small missile<br>Anti-shield | 2 OP; 1,500 range; 400 K; 0 flux; speed 400 | 0.5-second cycle; one round; regenerates 0.2 rounds per second | Consumes five fragments. The primary fragment deals 400 K, while four secondary fragments can each inflict up to 100 K. |
| **Splintered Skull**<br>`chief_navigator_bone_unstable_fragment`<br>3,000 credits | Small missile<br>Area anti-fighter and point defense | 1 OP; 500 range; 750 F; speed 100 | One-second cycle; one round; regenerates 0.5 rounds per second | Consumes one fragment. It uses a brief initial aiming stage, then becomes unguided and proximity-fused. Gautama formerly mounted two. |
| **Carrion Swarm**<br>`chief_navigator_bone_devouring_swarm`<br>3,000 credits | Small missile<br>Anti-armor | 2 OP; 2,500 range; 200 HE; speed 250 | One-second cycle; nine rounds; regenerates 0.2 rounds per second in batches of three | Commits six fragments and deals no damage to shields. On armor or hull impact, it applies its damage as an eleven-tick disintegration effect and can create replacement fragments while feeding. Gautama formerly mounted two. |
| **Grave Spark**<br>`chief_navigator_bone_voltaic_discharge`<br>2,000 credits | Small energy<br>Point defense | 3 OP; 500 range; 25 E plus 50 EMP; 25 flux | 0.2-second cycle; twenty charges; regenerates two charges per second in a batch of twenty | Requires at least ten active fragments but does not consume them. It fires automatically targeted, guaranteed-hit EMP arcs through a 360-degree envelope. Gautama formerly mounted one. |

## Archived prototype implementation details

- Each weapon costs half the OP of its vanilla THREAT counterpart, rounded up.
- All twelve use the `Starving Threat` manufacturer and the
  `chief_navigator_starving_weapon` tag. They deliberately omit the broad
  vanilla `threat` tag so they cannot enter faction-generated loadouts.
- They are restricted, have no blueprint, cannot be sold or stocked by a
  dealer, and carry both ordinary-drop and salvage-drop exclusions.
- Their lightly cropped, ivory weapon sprites are generated from the vanilla
  THREAT artwork.
- The fragment-dependent group is excluded because its shared swarm economy is
  substantially harder to balance with the reduced-OP weapon package.

## Authoritative files

- `data/variants/chief_navigator_starving_*.variant` and the three
  `chief_navigator_scylla_*.variant` files define the active stock weapon fits.
- Vanilla `starsector-core/data/weapons` defines their weapon specifications,
  projectiles, sprites, and effects. Chief Navigator does not override them.
- `tools/generate_starving_threat_hulls.ps1` retains stock weapon IDs in
  ordinary Starving variants while removing the six fragment-dependent ones.
  Gautama's three variants are maintained separately with their identical
  fixed thirteen-gun loadout.

The remaining files describe or generate the inactive prototypes:

- `data/weapons/weapon_data.csv` contains names, prices, range, damage, flux,
  ammunition, timing, tags, tooltip text, and manufacturer data.
- `data/weapons/chief_navigator_bone_*.wpn` contains weapon class, mount size,
  projectile linkage, sprites, and effect-plugin linkage.
- `data/weapons/proj/chief_navigator_bone_*_shot.proj` implements the Rib and
  Spine Drivers' range failure.
- `src/chiefnavigator/weapons/StarvingSkullblasterEffect.java` implements the
  Skullblaster's self-damaging feedback explosion while delegating its ordinary
  hit behavior to vanilla `VoidblasterEffect`.
- `tools/generate_starving_threat_weapons.ps1` generates the weapon family from
  vanilla Starsector THREAT data.
