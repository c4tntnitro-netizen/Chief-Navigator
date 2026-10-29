# Chief Navigator 0.5.1

Chief Navigator can now be enabled in an existing Starsector campaign.

## Changes

- On the first load of a campaign that has never used Chief Navigator, install
  Waypoint Troy and the six Orion Knot systems once.
- Record world initialization permanently before generation. Later loads
  preserve the campaign's existing worlds and quest progression.
- Adopt campaigns from older Chief Navigator versions without regenerating
  their worlds, including saves with partial or missing mod content.
- Update the installation instructions and add checks for first installation,
  save/reload, legacy campaign adoption, and interrupted initialization.

All gameplay and salvage fixes from the revised 0.5.0 release are included.

## October 12, 2026 asset revision

The 0.5.1 download now omits three obsolete Ithaca combat textures, removing
73.7 MiB of uncompressed artwork:

- `chief_navigator_ithaca_connected_rear_wall.png`
- `ithaca_wall_curved_core_v1.png`
- `ithaca_wall_mounting_apron_v2.png`

Their legacy hull definitions remain registered with Starsector's transparent
sprite, retaining their IDs, collision geometry and weapon slots. Current
Ithaca encounters use separate intact-base artwork. An older saved defense-wall
hull will lose its visible body. Both Wall fights were tested locally and
appeared normal after the cleanup.

The public 0.5.1 compiled jar and all other runtime gameplay data are unchanged.
The archive and checksum manifest identify this asset revision; the original
`v0.5.1` tag is preserved.

## October 13, 2026 Budai hotfix

- Remove Budai's special Hungering Rift from the substrate weapon assembly menu.
- Fix its tooltip description so hovering it no longer throws a formatting
  error.

Budai retains his special weapons and their combat statistics. The compiled
jar is unchanged. Restart Starsector after installing this hotfix.

## October 13, 2026 Sinni recruitment hotfix

- Restore the prerequisite for Sinni's initial bar proposal: complete the
  vanilla Hyperspace Topography ability tree by reaching Generate Slipsurge
  at 700 points.
- Fresh campaigns and incomplete topography no longer qualify. Previously
  earned completion remains valid after the repeatable 1,000-point data payout.
- Preserve already accepted quests and the normal decline timeout. Sinni's
  later 1,950-point extension is not an initial recruitment requirement.

This hotfix includes an updated compiled jar. Restart Starsector after updating.

## October 14, 2026 Heavenly Strike hotfix

- Heavenly Strike now spawns after all three perimeter Strikes are defeated,
  even when the Breach Defense Station has been destroyed. Recover active
  saves stuck at 3/4 when no final-fleet claim remains; failed creation can retry.
- Without the Breach Defense Station, Heavenly Strike approaches Ithaca Gate
  and performs a saved, interruptible 30-day scan. A lost player battle no
  longer immediately fails Labor V. Completing the scan shows its defeat scene
  and withdraws the fleet through Ithaca Gate; no Sector-wide incursions spawn.
- The two reserve battlestations require a live canonical Breach Defense
  Station at battle start. Preserve its combat behavior, staged waves and
  surviving Spartan defenders, including joining with the transponder off.

The confirmed in-game fix includes an updated compiled jar. The Budai tooltip
and substrate fixes and Sinni recruitment prerequisite remain included. Restart
Starsector after updating.

## October 22, 2026 Budai weapon isolation hotfix

- Reserve Budai's custom Hungering Rift and Pseudoparticle Jet for his authored
  loadout by removing faction and autofit selection tags and adding the native
  `SYSTEM` selection exclusion.
- Use native blueprint, market, dealer and drop exclusions for both weapons.
- Preserve their combat statistics, effects, boss fittings and earlier tooltip
  fix. The Heavy Adjudicator reward remains mountable.

This is a weapon-data hotfix; the compiled jar is unchanged. It does not remove
weapons from fleets already generated in a save. The reported bounty's original
selection path remains unconfirmed. Restart Starsector after updating.

## October 23, 2026 dialogue and fleet-limit update

- Revise Sinni's introduction, Eventide briefing, expedition preparation advice,
  and the first arrival in the Orion Knot, including readable archive messages.
- Add an optional LunaLib fleet deployment-point limit, from 1 to 1,000 DP.
  The default remains 50 DP. The current setting controls route opening,
  both transit directions, dialogue, navigation guidance, and Intel.
- Correct Troy's advice menus: the first page has no Esc shortcut; the final
  page has no Back option and binds Esc to Leave.
- Remove the secondary captions beneath system names on Sinni's local chart.

LunaLib is optional. Without it, the limit remains 50 DP. Lowering the limit
below the current fleet's DP can block transit until the limit is raised or
the fleet is reduced. This update does not rebuild saved worlds, replace
fleets, or reset quest progress. Existing system names are retained; the
planned system renames are not included.

The archive contains a matching compiled jar and retains the earlier 0.5.1
hotfixes. The original `v0.5.1` tag is preserved. Restart Starsector after updating.

## October 24, 2026 Ithaca dialogue update

- Rewrite the first approach to FOB Ithaca, from the ring's reveal through
  Task Force Spartan's interception and the damaged ring's final reveal.
- Restore Victor 14 for the Spartan inscriptions in Sinni's office and
  the Ithaca approach, with ordinary dialogue resuming afterward.
- Shorten Menelaus's upgrade-ledger greeting and show the drone-command
  highlight on the report page containing that advice.

The approach was tested in game. The update preserves existing quest progress
and the current Ashen Verge system name. It retains the earlier 0.5.1 hotfixes
and the original `v0.5.1` tag. Restart Starsector after updating.

## October 26, 2026 Menelaus and Derelict hotfix

- Revise Menelaus's first contact and Labor I briefing. The green skill-reward
  explanation appears once in the main briefing and contains two sentences.
- Keep the Ithaca approach separate from Menelaus's introduction: contact the
  central module after the approach to begin first contact.
- Require an explicit order for Sara to guard Sinni on every shuttle branch,
  with Yvan taking that role when Sara has died.
- Accepting Labor I now grants the vanilla Automated Ships skill outright,
  or one free skill point if already owned. This replaces the custom per-ship
  authorization reward; normal automated fleet limits apply.
- Already accepted Labor I? Select **Discuss the Labors** with Menelaus to
  claim the new reward. Each campaign receives it only once.
- New drone patrols and combat screens use small stock Derelict groups.
  Roaming packs contain three or four frigates/destroyers; Ashen patrols no
  longer expand into nine-ship Guard armadas after the Labors.
- Retain the Combat Guard ship data and existing saved ships, fleet rosters
  and losses. This hotfix does not convert or replenish saved fleets.
- Fix the duplicate hyperspace-exit option at the Odyssey fringe when the
  two duplicate endpoints share the same native orbit provenance.

The download includes a matching compiled jar and all earlier 0.5.1 hotfixes.
The original `v0.5.1` tag remains unchanged. Replace the previous mod folder
and restart Starsector after updating.

## October 29, 2026 contact bounty hotfix

- Prevent new contact Derelict bounties from targeting the distant Orion Knot
  systems. All six systems are excluded from native random mission generation.
- Apply the exclusion when existing campaigns load, as well as to new campaigns.
  Preserve saved worlds, quest progress, map markers and jump routes.
- Bounties generated before this update keep their original destinations.

Replace the previous mod folder and restart Starsector after updating. This
hotfix retains all earlier changes in the 0.5.1 download and its original tag.

## Requirements

- Starsector 0.98a
- GraphicsLib 1.10.2 or newer
- Combat Chatter 1.15.0 or newer

Hall of Triumph is optional for Isa's integration.

## Installation

1. Download **Chief-Navigator-0.5.1.zip** from the release assets.
2. Extract its **Chief Navigator** folder into **Starsector/mods**, replacing
   the previous Chief Navigator folder when updating.
3. Enable Chief Navigator and its dependencies in the launcher, then restart
   Starsector.
4. Start a new campaign or load an existing campaign. First installation adds
   the mod's worlds; campaigns that already used Chief Navigator retain their
   saved state. This update does not rebuild missing content in previously
   modded saves.

The ZIP includes the compiled jar and required runtime assets. Use the named
release ZIP for installation. Its internal manifest records the exact build
commit and individual file checksums; a SHA-256 checksum is supplied alongside
the download.
