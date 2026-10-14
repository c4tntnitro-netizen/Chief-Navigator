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
