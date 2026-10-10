# Chief Navigator 0.5.1 compatibility test patch 1

Patch version: **0.5.1-compat-test.1**. For Starsector **0.98a** and the published **Chief Navigator 0.5.1** release.

This is a small patch download to apply over the complete 0.5.1 mod. It contains replacement code and console registration, plus instructions and manifests. The complete mod's art, music, gameplay data, README credits, and third-party notices come from your existing 0.5.1 installation.

## Installation

1. Install the complete Chief Navigator 0.5.1 release first, including its normal GraphicsLib and Combat Chatter dependencies.
2. Close Starsector. Back up your `mods/Chief Navigator` folder and campaign save.
3. Extract `Chief-Navigator-0.5.1-compat-test.1.zip` into `Starsector/mods`. **Merge its `Chief Navigator` folder with the existing folder and overwrite the included files.** Keep the other files from the full 0.5.1 installation.
4. Launch the game with the same Chief Navigator mod entry enabled. Its version should read `0.5.1-compat-test.1`. There should be only one Chief Navigator installation.

This package targets the published 0.5.1 release. It does not include the unreleased Sinni officer-skill changes from the local development build; that build needs its matching jar and skill data. Wide Horizons and Expanded Core Worlds are optional and are not bundled or required dependencies. Console Commands is optional and needed only to use the recovery command.

## Changes

- New Troy placement scales with sector dimensions while retaining the vanilla-map position. Larger maps can increase travel distance and supply use. Existing Troy systems retain their saved positions.
- Outside Troy, quest and contact Intel prefer its existing owned reciprocal hyperspace entrance. Missing, unmarked, or broken entrances fall back to the existing objective without repairing saved topology.
- Orion placement helpers derive their center from saved Alpha Odyssey coordinates rather than later map settings. This aligns those helpers and Charybdis staging; it does not migrate saved terrain or the serialized Abyss wrapper.
- Added `InitializeNavigatorWorlds`, an explicit console recovery for pre-arrival campaigns with no surviving expedition worlds. Normal game-load generation retains the original 0.5.1 behavior, including clean mid-save installation.

## Optional missing-world recovery

If the expedition was accepted but its worlds were never created, make a separate pre-command save and run `InitializeNavigatorWorlds` using Console Commands.

The command preserves existing quest flags and refuses any surviving expedition system/entity, recorded Troy arrival, departure or Labor progress, or previous recovery attempt. It never rebuilds partial worlds. The attempt is permanently recorded before generation; a failed or interrupted attempt requires restoring the pre-command save to retry.

For diagnostic travel, the saved system IDs are `chief_navigator_treadmill` and `chief_navigator_odyssey_expanse`. The forum examples `waypoint_troy` and `alpha_odyssey` are not the saved IDs.

## Validation and scope

The user confirmed that the local compatibility build works with **Wide Horizons 1.4.2** and **Expanded Core Worlds 1.0.4**. The original reported failure has not been reproduced, and its root cause remains unconfirmed.

This package isolates the compatibility changes on the published 0.5.1 source baseline. Its build passed native validation of 890 dialogue rules and seven headless regression suites covering initialization, recovery guards, campaign lifecycle, map placement and Intel fallbacks, actual Wide Horizons marker filtering, jump topology, and arrival behavior. The isolated package has not received a separate live campaign test.

The archive was checked against the original full 0.5.1 ZIP, including unchanged asset and credit hashes. `PATCH_MANIFEST.json` records the baseline, patch inputs, and archive-member hashes. `RELEASE_MANIFEST.json` describes the runtime file hashes after applying the patch; it excludes itself and the supplemental patch manifest.

## Rollback

Close Starsector and restore the backed-up mod folder. If you used the recovery command and need to undo its campaign changes, restore the pre-command save as well.

This is a test patch intended for feedback before integration into a full release. Original project material retains the included MIT license; the complete mod's third-party assets retain their existing terms and notices.
