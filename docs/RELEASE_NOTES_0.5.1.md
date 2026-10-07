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
