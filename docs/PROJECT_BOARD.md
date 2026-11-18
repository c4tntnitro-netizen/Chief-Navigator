# Chief Navigator work board

Updated November 17, 2026. This is the starting board for requested work and
development changes that still need player testing.

November 17 release validation: user reports "FULL PLAYtest done" and authorizes release of the 0.6.2 candidate. CN-033 remains an unconfirmed known issue; this report does not establish every historical card's separate acceptance criteria.

| Backlog | In Progress | Needs Testing | Done |
| --- | --- | --- | --- |
| [CN-001 Static Sinni dialogue](#cn-001-static-sinni-dialogue) | No implementation currently active | [CN-004 Natural Gautama spawns](#cn-004-natural-gautama-spawns) | [CN-012 Push Sinni text update](#cn-012-push-sinni-text-update) |
| [CN-002 Menelaus Continue cleanup](#cn-002-menelaus-continue-cleanup) | | [CN-005 Sinni dialogue playthrough](#cn-005-sinni-dialogue-playthrough) | [CN-013 Uncapped Ithaca artwork](#cn-013-uncapped-ithaca-artwork) |
| [CN-003 Budai weapons in bounties](#cn-003-budai-weapons-in-bounties) | | | [CN-006 Troy fleet-limit behavior](#cn-006-troy-fleet-limit-behavior) |
| [CN-014 Odyssey system names for 1.0.0](#cn-014-odyssey-system-names-for-100) | | [CN-007 Ungaikyo Overseer and mirrors](#cn-007-ungaikyo-overseer-and-mirrors) | |
| [CN-015 Evolving Gautama finale](#cn-015-evolving-gautama-finale) | | [CN-008 Avici enemies](#cn-008-avici-enemies) | |
| [CN-016 Reincarnation swarm markers](#cn-016-reincarnation-swarm-markers) | | [CN-009 Seventh station and Abyssal Lens](#cn-009-seventh-station-and-abyssal-lens) | |
| [CN-017 Final survivor and Ungaikyo retreat](#cn-017-final-survivor-and-ungaikyo-retreat) | | [CN-010 Ithaca storage compatibility](#cn-010-ithaca-storage-compatibility) | |
| [CN-018 Remove Combat Guard units for 1.0](#cn-018-remove-combat-guard-units-for-10) | | | |
| [CN-019 Debug console command cleanup](#cn-019-debug-console-command-cleanup) | | | |
| [CN-021 Research station persists when component is left behind](#cn-021-research-station-persists-when-component-is-left-behind) | | | |
| | | | [CN-022 Naval turrets render beneath missile housings](#cn-022-naval-turrets-render-beneath-missile-housings) |
| [CN-024 Domain livery for Wall modules](#cn-024-domain-livery-for-wall-modules) | | | |
| | | | [CN-025 Hidden faction for Hegemony expedition fleet](#cn-025-hidden-faction-for-hegemony-expedition-fleet) |
| | | [CN-023 Final fight stations under player control](#cn-023-final-fight-stations-under-player-control) | |
| | | [CN-011 Ithaca gate travel](#cn-011-ithaca-gate-travel) | |
| | | [CN-020 Contact Derelict bounties in Orion](#cn-020-contact-derelict-bounties-in-orion) | |
| [CN-026 Flesh out the Hegemony Expeditionary Fleet](#cn-026-flesh-out-the-hegemony-expeditionary-fleet) | | | |
| [CN-027 Dedicated hidden Ithaca faction](#cn-027-dedicated-hidden-ithaca-faction) | | | |
| [CN-028 Combat Ithaca short side route](#cn-028-combat-ithaca-short-side-route) | | | |
| [CN-029 Reconcile the storyline with At the Gates](#cn-029-reconcile-the-storyline-with-at-the-gates) | | | |
| [CN-030 Aias Kleon side missions for each Labor](#cn-030-aias-kleon-side-missions-for-each-labor) | | | |
| [CN-031 Sinni as an XO for SiC](#cn-031-sinni-as-an-xo-for-sic) | | | |
| | | [CN-032 Peerless optional dependency verification](#cn-032-peerless-optional-dependency-verification) | |

| [CN-033 Survey ships can be fought twice](#cn-033-survey-ships-can-be-fought-twice) | | | |

## How cards move

- **Backlog:** requested work or an unresolved issue that has not been started.
- **In Progress:** someone is actively investigating or implementing it.
- **Needs Testing:** implementation is ready, with specific acceptance checks
  still outstanding. Passing a build does not replace in-game confirmation.
- **Done:** the card's stated acceptance criteria are satisfied. Record the
  result and whether delivery is local development, pushed to `dev`, or released.

Keep one status per card. When work starts, move its card and record the next
action. After implementation, record the build/check results and the remaining
player test. Move it to Done only after its acceptance criteria are met.

## Backlog

### CN-033 Survey ships can be fought twice

**Status:** Potential bug; reported during the November 17, 2026 release test. Not yet independently reproduced.

**Report:** "You can fight survey ships twice."

**Next action:** identify the affected survey ship and reproduce the sequence from the first battle through the second encounter. Record the first battle's outcome, any salvage or leave choices, and whether a reload occurred. Determine whether this is a duplicate encounter or surviving defenders before changing behavior.

**Acceptance:** establish the cause and expected encounter lifecycle; if unintended, prevent a second fight after the encounter has been completed without breaking legitimate follow-up encounters.

### CN-001 Static Sinni dialogue

**Next action:** investigate the reported static Eventide preparation dialogue.
Sinni's dialogue is **NOT dynamically changing** in the reported local test.

**Acceptance:** reproduce with different fleets and expedition limits. Establish
which narration should adapt, separately from whether the existing DP token and
240-DP conditional response work. Agree on the intended behavior before changing
the authored scene.

**Evidence:** [Sinni dialogue status](SINNI_DIALOGUE_STATUS.md).

### CN-002 Menelaus Continue cleanup

**Next action:** remove the redundant first-contact Continue that only reveals
the response “What do you need from me?” without adding prose.

**Acceptance:** show that response directly after the existing paragraph;
preserve the asked flag, portraits, and continuations that reveal new text.

**Evidence:** October 6 pending dialogue-flow note in [project memory](../AGENTS.md).

### CN-003 Budai weapons in bounties

**Next action:** reproduce the report that boss-only weapons appear in an
ordinary bounty fleet. The actual generation path remains unidentified.

**Acceptance:** capture an affected campaign's fitting-faction weapon knowledge,
member variant, and inflater state. Identify a reproducible path before claiming
a cause or changing saved faction knowledge.

**Evidence:** [Budai weapon isolation audit](BUDAI_WEAPON_ISOLATION.md).

### CN-014 Odyssey system names for 1.0.0

**Requested outcome:** defer the global player-facing system renames to 1.0.0:

- Lacedaemon -> Lacedaemon.
- Anatolia -> Anatolia (supersedes Telepylos).
- Bouvo Olympus -> Helios (previously requested; remains pending).

**Next action:** implement and verify the global renames for new 1.0.0 campaigns.
No global code or data renames are part of the current hotfix.

**Acceptance:** system labels, dialogue, navigation guidance, and Intel use the
requested names throughout new 1.0.0 campaigns. Existing-campaign rename or
migration support is outside this card's scope.

### CN-015 Evolving Gautama finale

**Requested outcome:** during Labor V's final fight, Gautama mutates into a
progressively more developed form each time it reincarnates:
base -> Lv2 -> Lv3 -> Lv4.

**Next action:** deferred for later. Register the concept sprites to the
original hull, map reincarnation stages to the visual progression, and define
what happens after Lv4.

**Acceptance:** retain the original general proportions and engine/collision
geometry as the template. Scale the sprite and all associated geometry uniformly
by 20% per level: 1, 1.2, 1.44, 1.728. Follow the selected hard-shell,
insect/shellfish-like THREAT progression, avoiding the rejected fleshy forms.
Apply this evolution to the final fight.

**Evidence:** [Selected Gautama concepts](../graphics/source/gautama_evolution_v2/generation_manifest.json).

### CN-016 Reincarnation swarm markers

**Requested outcome:** mark THREAT ships that will reincarnate with a black
THREAT swarm so their pending return is visible in combat.

**Next action:** deferred for later. Identify the actual reincarnation state
and bind the swarm marker to that state. The current selection happens on
death; define when a future return is committed and its marker becomes visible.

**Acceptance:** the black swarm identifies ships scheduled to reincarnate;
ships without a pending reincarnation do not receive that marker. Keep its
appearance and cleanup synchronized with the reincarnation lifecycle.

### CN-017 Final survivor and Ungaikyo retreat

**Requested outcome:** add the following ending sequence to Labor V's final fight:

1. The last remaining THREAT ship always reincarnates, unless it is Gautama.
2. After that reincarnated ship dies, Ungaikyo's form reincarnates from Gautama.
3. The Ungaikyo manifestation is effectively invincible, then retreats.

**Next action:** deferred for later. Define which hulls count as the last ship
across fighters, remaining waves, reserves, and pending construction. Resolve
the Gautama-only case, the manifestation's emergence point, and when victory
is committed. The current ordinary-THREAT return path reconstructs Gautama;
confirm how that path maps to the requested terminal sequence.

**Acceptance:** preserve Gautama's exception; the guaranteed last-ship return
leads to the stated second death and Ungaikyo sequence rather than an endless
reincarnation loop. Ungaikyo survives the appearance and withdraws. Resolve how
this scene relates to the separate Ungaikyo encounter before implementation.
Coordinate the rebirth and its marker with CN-015 and CN-016.

### CN-018 Remove Combat Guard units for 1.0

**Target release:** 1.0. **Status:** Backlog; deferred until the 1.0 cleanup.

**Requested outcome:** remove the custom Combat Guard unit family entirely
from the mod. The 1.0 release does not need to load saves from earlier versions.

**Next action:** during 1.0 preparation, remove the retained Guard hull/skin and
variant definitions, sprites and other exclusive assets, registration entries,
Guard-specific AI and obsolete IFF/compatibility code. Replace any remaining
dependencies on the old unit family, including friendly fleet faction setup,
with the stock Derelict implementation and the allegiance needed by the story.
Remove obsolete tests, documentation and packaging references along with them.

**Acceptance:**

- No Combat Guard units, exclusive assets or compatibility definitions remain
  in the 1.0 release package; no spawn, script or data entry references removed
  identifiers.
- New campaigns use small stock Derelict fleets; friendly patrols, mothership
  objectives, station defenses and Labor V support still function.
- Build, asset/reference validation and a new-campaign playthrough pass.
- No old-save migration, compatibility stubs or preservation of removed Guard
  identifiers is required. Release notes state that a new campaign is required.

**Current development policy:** stop spawning Guard units now, but retain their
data for existing saves until this 1.0 ticket is implemented. This ticket does
not authorize deleting that compatibility data from the current build.

### CN-019 Debug console command cleanup

**Status:** Backlog; deferred until the current reproduction work is finished.

**Requested outcome:** reduce console clutter by either moving Chief Navigator's
development/testing commands into a separate optional package or disabling their
registration by default.

**Next action:** inventory the commands in `data/console/commands.csv`, including
the bounty refresh/progression/type-testing tools, and choose the packaging or
default-disable approach. Keep the tools available for the current reproduction.

**Acceptance:** ordinary installs do not list the development/testing commands;
they remain available through an explicit debug package or opt-in setting, with
their activation and usage documented. Verify the normal and debug configurations.

### CN-021 Research station persists when component is left behind

**Status:** Backlog. Player-reported October 30, 2026; not independently reproduced.

**Reported behavior:** salvaging a research station while leaving its quest
component in the loot window leaves the derelict station present afterward.
The player suspects this may permit unlimited repeated station salvage.
Repeat loot generation or station duplication has not yet been confirmed.

**Reproduction steps:**

1. Interact with an unsalvaged Odyssey research station and salvage it.
2. Leave the quest component behind instead of transferring it into player cargo.
3. Close the salvage interaction and observe that the station remains present.
4. Reopen the station and check whether salvage can be repeated, ordinary loot
   is replenished, or another station is created. Repeat after save/reload.

**Next action:** reproduce on the current development build. Distinguish a
station retained to hold its unclaimed component from a repeatable salvage or
respawn exploit; inspect when salvage completion and component recovery are
recorded. Exact station, save, and repeated-loot results are not yet captured.

**Acceptance:** leaving the component behind cannot reset or replenish ordinary
station salvage, duplicate the station, or permit unlimited rewards. Resolve
the station lifecycle while preserving the intended handling of the unclaimed
component and correct Labor-II progress. Verify taking, leaving, and later
retrieving the component, repeated interaction, and save/reload.

**Evidence:** the player's report in this chat. Logging only; no runtime change
or fix has been made.

### CN-022 Naval turrets render beneath missile housings

**Status:** Done. Player confirmed fixed November 5, 2026.

**Result:** naval artwork renders after station launcher housings using
ABOVE_SHIPS_LAYER. Weapon placement and gameplay remain unchanged. Focused
renderer regression passes; player confirmed the live fix.

**Delivery:** scoped 0.5.1 release update, source commit 91e142d.

### CN-024 Domain livery for Wall modules

**Status:** Backlog. Requested November 4, 2026.

**Requested outcome:** kitbash Domain livery onto the Wall modules.

**Next action:** review the existing Wall module sprites and suitable Domain
livery references, then prepare the artwork pass.

**Acceptance:** Wall module artwork carries a consistent Domain livery while
preserving module geometry, weapon mounts, and gameplay behavior.

### CN-025 Hidden faction for Hegemony expedition fleet

**Status:** Done. Player confirmed fixed November 5, 2026.

**Result:** Hegemony Expeditionary Fleet hidden from faction Intel. Occupied
forward base denies entry with placeholder text; abandoned interaction preserved.

**Delivery:** scoped 0.5.1 release update, source commit 91e142d.

### CN-026 Flesh out the Hegemony Expeditionary Fleet

**Status:** Backlog. Requested November 5, 2026.

**Requested outcome:** flesh out the Hegemony Expeditionary Fleet.

**Next action:** review the existing expedition encounters, characters, and
forward base, then define the additional dialogue, interactions, and story
content. Include replacing the base entry-denial placeholder when its content
is ready. Detailed scope remains to be defined.

**Acceptance:** the agreed expedition content is implemented and verified in
game, preserving the hidden-faction setup and existing story progression.

### CN-027 Dedicated hidden Ithaca faction

**Status:** Backlog. Requested November 8, 2026.

**Requested outcome:** make Ithaca its own hidden faction.

**Next action:** map FOB Ithaca and its defenses to a dedicated faction, define
its relations with the player and Task Force Spartan, and identify the existing
ownership and battle-joining paths that need updating. Coordinate with CN-028.

**Acceptance:** Ithaca has its own faction identity, hidden from faction Intel.
Station access, defense support, and main-quest progression work with that
identity. IFF does not block joining Ithaca defense battles, including the live
Breach Defense Station fight; allied stations retain allied combat control.

### CN-028 Combat Ithaca short side route

**Status:** Backlog. Requested November 8, 2026.

**Requested outcome:** add a short side route called "Combat Ithaca".

**Next action:** define the route's entry point, availability, combat encounter,
and conclusion, then write and wire the short route. Opponents, rewards, and
effects on the main quest remain to be defined. Coordinate faction behavior
with CN-027.

**Acceptance:** the agreed short route is accessible, plays through its combat
and conclusion, and handles its intended faction relations and quest outcomes
without breaking the main Ithaca progression.

### CN-029 Reconcile the storyline with At the Gates

**Status:** Backlog. Requested November 9, 2026.

**Requested outcome:** Set the Chief Navigator storyline after the player
completes "At the Gates." The Starving Threat begin consuming the Devoured
Ring only after the player reactivates the gates.

**Next action:** Review the Devoured Ring's history in the Labor IV report,
dialogue, and campaign descriptions. Revise references that place feeding
before reactivation, including the current account of centuries of feeding.

**Acceptance:** The quest sequence and in-game accounts agree that the player
reactivates the gates during "At the Gates," the Starving Threat start
consuming the ring afterward, and the Chief Navigator storyline follows
"At the Gates."

### CN-030 Aias Kleon side missions for each Labor

**Backlog.** Requested November 9, 2026.

Captain Aias Kleon offers optional side missions alongside each Labor. These
missions give the player opportunities to acquire ships and recruit crew.

Define when Kleon offers the missions, what the player does, and which ships
and crew become available.

Verify in game that Kleon offers missions for every Labor, that players can
gain ships and crew through them, and that players can continue the Labors
if they skip the side missions.

### CN-031 Sinni as an XO for SiC

**Status:** Backlog. Requested November 16, 2026.

**Requested outcome:** make Sinni available as an XO for SiC.

**Next action:** define her XO abilities and unlock conditions, then plan the
SiC integration alongside her existing recruitment and story progression.

**Acceptance:** Sinni can be recruited and assigned as an XO through SiC, with
the agreed abilities and unlock conditions working in game.

## Needs Testing

### CN-032 Peerless optional dependency verification

**Status:** Needs Testing; requested November 17, 2026.

**Next action:** verify Sea of Stars / Chief Navigator in game without Peerless
Skills, then update the dependency documentation and release notes to reflect
the tested build and results.

**Verified locally:** the current manifest excludes Peerless Skills. The installed
JAR isolates its optional API call behind an enabled-mod check and failure guard.
Tests with the Peerless JAR absent pass for the final debrief, follow-up hunts,
ending, and retired menus, including an enabled-but-missing API. These are
headless checks; full game startup and campaign save/load remain untested.

**Acceptance:**

- Start the game and a new campaign without Peerless Skills; save, exit, and
  reload successfully, with no missing Peerless classes or assets in the log.
- Verify recruitment and story completion in game, including the final debrief,
  hunts, and ending. An unavailable Peerless reward must not block progression
  or consume its one-time receipt.
- Check existing-save behavior on a disposable copy and record separately
  whether removing Peerless from a save that previously used it is supported.
- Record the exact game/mod versions and test results. Update dependency
  documentation and release notes, removing stale required-dependency claims
  for the verified build without implying older releases were tested.

**Evidence:** [Absent-Peerless test results](../out_peerless_absence_20261117/verification.json)
and [installed JAR audit](../out_peerless_absence_20261117/audit/peerless_constants.json).

### CN-020 Contact Derelict bounties in Orion

**Status:** live failure reproduced October 29, 2026. At the user's request, the
earlier creator-specific fix was replaced with native `THEME_HIDDEN` tagging and
the development jar rebuilt. The user subsequently reported "Seems to work" and
requested a push. The reported bounty behavior has live confirmation; separate
old-save save/reload and authored-navigation checks were not explicitly reported.

**Delivery:** pushed scoped source, tests, reproduction tools and canonical jar to
`dev` as `e18a7de5546536fc84d11db45bbe852fcca37672`; remote tip verified. Unrelated
local development and the active development jar were preserved. No release or
`main` update was requested at that stage. The user then explicitly requested
release publication: v0.5.1 ZIP/checksum/notes are updated and freshly downloaded
copies verified. Release manifest commit is `e940c11a6b990d0a2704cfbeaded28e642e6ef32`;
main merge is `66d3a527555725fabff798ad7b77aab17587b505`. The original tag is unchanged.
Stable includes the focused hidden-tag fix; reproduction commands remain on dev.

**Implementation:** restore normal native bounty creators; tag all six owned
Orion systems during generation and on every load, including legacy saved
ownership. Existing-world reconciliation adds only THEME_HIDDEN and preserves
foreign claims, routes, map settings, accepted bounties and cooldowns. The retired
creator class remains inactive for save compatibility. No CORE classification is
added. This supersedes the historical instruction to remove THEME_HIDDEN.

**Latest diagnosis:** the saved Horus contact already has the accepted Derelict
bounty and 57.7 days of native `mcb` cooldown remaining. Logged refresh attempts
stop before generation because there is no unaccepted offer to reroll. This
explains that contact's state; it does not validate the fixed live generator or
establish a cause for any other contact. That read-only diagnosis preceded the
user-authorized hidden-tag implementation; the actual save files remain untouched.

**Evidence:** the user's live screenshot shows Horus Yaribay offering a Derelict
fleet bounty in The Odyssey Expanse, 92 light-years away, for 380,000 credits with
a 120-day deadline. The user reported hundreds of attempts and then closed the
game. The accepted difficulty-10 bounty is confirmed in
`../../saves/save_A_8666867206689798321/campaign.xml`.

**Next action:** launch the rebuilt game, load the campaign, and run
`RefreshContactBounties status`. Expect generator `CBDerelict` and
`Orion Knot systems tagged THEME_HIDDEN: 6/6`. Then inspect a newly generated
`RefreshContactBounties derelict 10` offer at another contact with an available
unaccepted native military bounty, or in a save from before acceptance. An already
accepted pre-fix bounty retains its target. Check map/travel and save/reload as
well. Build, native selection, diagnostic, type, refresh, world initialization,
campaign lifecycle and jump-point lifecycle checks pass. The unchanged native
picker selected zero Orion systems in 4,096 tagged fixture trials. The original
stash `85f0e58075f9f39e876bcf48d90e2517a9a0770f` remains a historical backup of the
superseded creator-specific approach.

**Acceptance:** new regular military-contact Derelict bounties exclude all six
owned Orion systems through native THEME_HIDDEN exclusion while ordinary targets
remain eligible. Confirm the six tags survive old-save load/save/reload and
authored navigation remains usable. Existing accepted bounties retain their
targets. Keep the separate console cleanup under CN-019.

**Record:** [Live reproduction and screenshot](CONTACT_DERELICT_BOUNTY_REPRO.md).

### CN-004 Natural Gautama spawns

**Requested outcome:** make Gautama's spawns look more natural.

**Ready:** fragment-cloud assembly mechanics are implemented and headless
regressions passed.

**Player test:** watch ordinary carrier launches and staged reincarnation escorts
assemble from surrounding fragments. Check movement, appearance, and cleanup;
confirm whether this satisfies the requested natural-spawn behavior.

**Evidence:** [Original TODO](../TODO.md) and
[Gautama swarm assembly](GAUTAMA_SWARM_ASSEMBLY.md).

### CN-005 Sinni dialogue playthrough

**Ready:** the revised bar introduction, Eventide briefing, Troy advice, and
arrival sequence are published in the October 23 revision of 0.5.1.
CSV validation, compilation, dialogue routing, prose parity, pagination, and
font/color regressions passed.

**Player test:** follow the mandatory drinks sequence and both creature/machine
branches. Confirm the chart reveal has no intermediate Continue, the warning and
denial have the intended archive colors, the banner's first line stays together,
and Victor 16 is readable. Verify recruitment and the final Troy marker happen
at their authored choices. Static adaptation is tracked separately in CN-001.

### CN-007 Ungaikyo Overseer and mirrors

**Ready:** the obsidian Overseer, Cautious boss behavior, and mirror CR repair
passed targeted regressions.

**Player test:** inspect the sprite and material rendering in combat. Check
opening and rebuilt mirrors, including modules, enter at their native maximum
CR; confirm mirror behavior and collapse cleanup.

**Evidence:** [Ungaikyo verification and limitations](UNGAIKYO_OBSIDIAN_OVERSEER.md).

### CN-008 Avici enemies

**Ready:** the enemy-only port passed content and headless combat/lifecycle checks.

**Player test:** inspect new enemy bodies, localized shader effects, lightning,
AI behavior, and balance in newly created encounters. Check coexistence with SEP.

**Evidence:** [Avici development notes](DEV_AVICI_DWELLERS.md).

### CN-009 Seventh station and Abyssal Lens

**Ready:** development code adds the seventh station and its consumable upgrade.
Live progression confirmation is outstanding.

**Player test:** recover all six original components and confirm the new station
appears once. Recover and install the Lens, inspect the upgraded Ithaca beams,
and verify Labor II still requires only four installed upgrades.

**Evidence:** October 21 Avici research expansion entry in
[project memory](../AGENTS.md) and [development notes](DEV_AVICI_DWELLERS.md).

### CN-010 Ithaca storage compatibility

**Ready:** the narrow storage-close fix passed the user's BMO/Nexerelin A/B retest.

**Player test:** repeat cargo and ship transfers, close and reopen the services,
then save/reload with the affected mod combination. Confirm stored contents
persist and storage exit stays stable. The original reporter's exact versions
remain unknown.

**Evidence:** [Live A/B results](../../test-patches/ithaca-storage-repro-2026-10-18/RESULTS.md).

### CN-011 Ithaca gate travel

**Ready:** the post-Labor-V scanned Ithaca destination is implemented on `dev`.

**Player test:** finish the final debrief and scan the real Ithaca Gate. Check
destination visibility, Janus/fuel requirements, transit, and coexistence with
other mods' gate destinations. Confirm access remains sealed before completion.

**Evidence:** development commit `1c0a847` and
[Ithaca gate regression](../tools/IthacaGateTravelRegression.java).

## Done

### CN-006 Troy fleet-limit behavior

**Result:** the user confirmed on October 23, 2026 that the LunaLib fleet-limit
setting works in-game and the dialogue text responds to it.

**Verification:** focused regressions passed with LunaLib absent and present,
covering DP boundaries, live setting changes, both transit directions, and the
conditional Sinni response.

**Delivery:** published in the October 23 revision of
[0.5.1](https://github.com/c4tntnitro-netizen/Chief-Navigator/releases/tag/v0.5.1),
commit `c549495`; player-confirmed in development.

**Evidence:** user confirmation in this chat,
[development verification](DEV_AVICI_DWELLERS.md), and
[Troy limit regression](../tools/TroyPortalLimitRegression.java).

### CN-012 Push Sinni text update

**Result:** pushed the revised Ink/runtime text, archive presentation, Victor 16
banner, regression coverage, and explicit static-dialogue issue note.

**Delivery:** `dev`, commit
[e4f666f](https://github.com/c4tntnitro-netizen/Chief-Navigator/commit/e4f666fabb584b3519f543491288cc2c550cde62).
Live dialogue acceptance remains CN-005; broader Eventide adaptation remains
CN-001. Fleet-limit behavior is confirmed in CN-006.

### CN-013 Uncapped Ithaca artwork

**Result:** the active hull artwork matches the approved uncapped source, the
normal rebuild stays uncapped, and the armed preview remains a separate image.

**Delivery:** local development. Historical cap designs remain inactive.

**Evidence:** [Current Ithaca artwork state](FOB_ITHACA_MOUNT_CAPS.md).

### CN-023 Final fight stations under player control

**Status:** Needs Testing; local fix installed November 1, 2026.

**Report:** Stations in the final fight are controlled as part of the player fleet. They should be allied-controlled.

**Implementation:** the center-battle plugin now marks both the native ship and its fleet-member record allied for the player-side Breach Defense Station, two combat-only reserves, and their child modules. This retains their combat side and native AI while removing player command ownership. No campaign fleet membership, station variants, deployment positions, damage, upgrades or wave pacing changes.

**Verification:** reproduced the original non-allied classification using actual Starsector Ship/FleetMember/DeployedFleetMember classes. Native ally classification and attached/detached module checks pass after the fix. Station deployment, native axial rotation/anchors, late modules, enemy isolation, normal damage, final-wave composition, battle joining and fire-control regressions pass, as does the full build. Only the nested center-battle controller class changed; 331 other jar entries are preserved. Evidence: `out_station_allies_20261101/verification.json`.

**Next action:** restart the game and enter the final fight. Confirm the three friendly stations appear as allies in combat, cannot be taken over as player ships, and retain ordinary AI weapons/rotation and the authored positions. Actual in-game confirmation remains pending.

**Acceptance:** all friendly final-fight stations fight on the player side under allied AI control, without player fleet command ownership; preserve station placement, modules, and battle progression. Verify in game.

CN-022 implementation update: custom naval artwork now renders on ABOVE_SHIPS_LAYER after station weapon housings. Focused compilation and renderer regression pass. Installed JAR changes only ColossalNavalTurretRenderer.class. Restart and verify overlap during rotation/firing, glow and damage flashes. Live appearance remains unconfirmed.

November 5, 2026: user confirmed CN-022 and CN-025 fixed in game and authorized release publication; this supersedes their earlier pending-test notes.
