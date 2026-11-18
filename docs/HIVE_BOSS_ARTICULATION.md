# Hive boss articulation reference

The third Threat boss is a concept based on the native Hive Unit: a broad
geoduck-like armored shell, with a single segmented extension ending in a
Bobbit worm head and two hooked mandibles. Its intended system is the retained
Exodus Plague. Gautama has native Extraction Protocol again.

The current images are concept masters. No new boss, encounter, animated
renderer or sprite rig is installed in the game yet. Art and exact ImageGen
prompts are under `graphics/source/hive_boss_exodus_20261116`.

## Diable Avionics evidence

Diable Avionics is absent from this installed mod set. Read the public
scardwell15 repository at pinned tree/commit
`d724d7e0499d578fdf5147e923cb4da345068541`. Private research copies and the tree
listing are retained under `out_hive_boss_animation_20261116/reference`.

- [Versant animation](https://github.com/scardwell15/diable-avionics/blob/d724d7e0499d578fdf5147e923cb4da345068541/src/data/scripts/weapons/Diableavionics_versantEffect.java): `EveryFrameWeaponEffectPlugin` discovers individual weapon parts by slot ID (lines 92–151). It drives their world angle through `WeaponAPI.setCurrAngle` and translates their sprites through `getSprite().setCenter` (lines 260–326). `MagicAnim.smoothNormalizeRange` gives different pieces overlapping eased phases.
- [Versant panel definition](https://github.com/scardwell15/diable-avionics/blob/d724d7e0499d578fdf5147e923cb4da345068541/data/weapons/diableavionics_versant_panel_LEFT.wpn): part is `type: DECORATIVE`, has its own sprite and renders below weapons. Its `.ship` defines decorative slots and built-in part fittings.
- [Virtuous shoulder animation](https://github.com/scardwell15/diable-avionics/blob/d724d7e0499d578fdf5147e923cb4da345068541/src/data/scripts/weapons/Diableavionics_virtuous_pauldronsEffect.java): lines 90–153 ease sway from acceleration, strafing and angular velocity, then turn the left/right shoulder sprites independently. Pause and death gates stop the motion update.
- [License](https://github.com/scardwell15/diable-avionics/blob/d724d7e0499d578fdf5147e923cb4da345068541/license.txt) permits Starsector mod reuse with attribution to **Tartiflette and FlashFrozen**. This is reference research; no Diable code or artwork has been copied into the runtime package and no Diable dependency was added.

Confirmed the corresponding methods in this installation's official API
sources (`WeaponAPI`, `SpriteAPI`), and inspected installed
`MagicLib/src/org/magiclib/util/MagicAnim.java`. A weapon sprite should rotate
through `setCurrAngle`, as `WeaponAPI.getSprite` documents, rather than by
setting that weapon-owned sprite's angle directly.

## Planned rig

Use the Diable decorative-part pattern with an original chain pose controller:

1. Keep the main shell rigid. Place a root joint inside its lower opening so
   the first neck segment draws under the shell and cannot reveal a seam.
2. Make a chain of overlapping neck-segment sprites, a separate
   terminal head, and independent left/right jaw sprites. Author full hidden
   collars under every overlap; cutting a flat picture at visible seams would
   leave gaps during bending. The generated concept is a guide for rebuilding
   those complete parts, not a finished atlas.
3. Compute the chain in ship-local coordinates, from root to tip, with fixed
   joint lengths and bounded joint angles. Add delayed, damped sway so the
   head and distal joints lag the shell's turns. Then transform each joint to
   the ship's current world position/facing.
4. Attach the decorative parts to dedicated built-in slots. Compute each
   part's desired angle and sprite-center offset from its joint pose. Apply
   offsets relative to captured rest pivots every frame, never cumulatively.
   This extends Versant's two-part translation/rotation pattern to a linked
   chain; Diable's inspected code is not a ready-made worm solver.
5. Drive the two mandibles from separate hinges on the head. Let them ease
   open during Exodus windup, hold through emission and settle closed during
   recovery. Preserve the system's packet budget and taper.
6. Derive both Exodus emitter origins and aim directions from the terminal
   head pose. Do not keep the old Gautama-local `[35, +/-85]` firing positions
   after assigning the system to the Hive.
7. Freeze animation on pause; respect ship time multipliers. Keep state per
   ship/part. Define a stable death pose, phased rendering, render order and
   sprite culling radius covering the entire extended chain.

This decorative approach moves the artwork; it does not by itself produce
separate moving ship collision shapes or independently damageable neck
sections. That gameplay decision remains open. No dynamic collision behavior
is implied by the concept image or Diable's visual animation technique.

Before integration, verify joint continuity, all ship facings, abrupt turns,
pause/time scaling, jaw limits, overlapping seams, multiple simultaneous
bosses and projectiles emerging from the actual animated mouth position.
