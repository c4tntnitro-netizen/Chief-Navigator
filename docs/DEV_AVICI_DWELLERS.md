# Avici enemy development

This work lives on `dev`, based on the published 0.5.1 hotfixes and the preserved
local testing work. The installed `mods/Chief Navigator` directory is the dev
worktree. No 0.5.1 tag, release asset or public release note is changed.

## Scope and credit

The port adapts the Shrouded Banshee, Shrouded Brain and Insatiable Hunger enemy
implementations from Shroud Expansion Pack 0.9.6, revision
`e2bafc5a0bfceb86be46ab48a40463d284ca6f55`, by TIDAL and RoaringWriter.
TIDAL gave the project author written permission on October 9, 2026 to include
his custom Dwellers in Chief Navigator's endgame area. Preserve the attribution
and [third-party notice](../THIRD_PARTY_NOTICES.md).

Only the required enemy hulls, AI, systems and body artwork are included. The
port uses vanilla Dweller weapons and existing native assets. SEP's questline,
Entity, officers/XO, trials, rewards, new weapons and recordings are excluded.

## Coexistence

The enemies are available without SEP. Their hulls, variants, systems, classes,
sprite keys and campaign ownership markers belong to Chief Navigator's own
namespace. A separately installed SEP continues to run normally. The port does
not edit its files, replace its command names, suppress its encounters, or count
its fleets against Chief Navigator's population limits. Its original Hunger is
not treated as Budai for music, quests or rewards.

## Budai and manifestations

Budai again uses his enlarged, blue-shifted Shrouded Maw hull and the existing
Maw-specific weapons, distortion, enrage and Convulsive Lunge mechanics. His
escort follows SEP's ordinary `largeFleet` / `HIGH` Transcendent Manifestation
composition, with all Maw entries omitted. The enemy-only port uses its own
Banshee/Brain variants and vanilla Dweller fits; it does not import SEP weapons.
Budai is the only Maw in his fleet. Escort losses never replenish.

An existing undefeated, idle, owned Budai fleet receives this change once. A
saved Hunter boss changes variant on the same native fleet member, preserving
its identity, captain and damage state. Existing escorts are replaced once with
the requested composition; later upkeep leaves the roster alone. Player,
foreign, busy and defeated fleets are excluded. His campaign identity, intro
gate, 100,000-unit initial spawn, pursuit, return, soundtrack and salvage remain.

The Insatiable Hunter remains a separate Avici enemy in the Transcendent patrol
slot, using the ordinary `_Standard` variant rather than Budai's legacy variant.
It retains Forced Reality and its localized domain; it does not receive Budai's
combat music, rewards or quest identity. The old `_Budai` variant remains for
serialized compatibility while the one-time conversion is deferred.

Ambient manifestations unlock on the player's first entry into Avici, using the
system's native visited state. Budai's spawn, Sinni's arrival-scene completion
and elapsed campaign days do not gate them. There are three Lesser slots, two
Greater slots and one Transcendent slot, with their own ownership markers.
They use ordinary local patrol AI and sensor detection, without Budai's forced
player pursuit or position tracking.

Only a verified battle destruction permits a replacement, after another
30 campaign days. Living rosters, foreign/player objects, busy fleets and
existing claims are preserved. Patrols
remain in Avici and avoid the habitat shelter and jump entrances. New Transcendent
manifestations include one ordinary Insatiable Hunter alongside the existing
Banshee and Brain enemies; other tiers retain their existing compositions.

Recovering all six original Ithaca components reveals a seventh research
station in Sanghata orbit, Avici. Installed components and components observed
in actual player cargo count toward this discovery; opening salvage or using
the combat-upgrade test override does not. The station is introduced once and
never recreated after salvage or a missing saved token.

Its Abyssal Focusing Lens is installed through Menelaus's existing upgrade
ledger. Upgraded Ithaca naval beams keep their white cores and gain a teal
fringe at the muzzle that transitions to red by the midpoint. Native Abyssal
Glare rift explosions still come from the game's own per-ray beam effect. Normal-mode
explosions retain 100 maximum / 50 minimum energy damage, 75 radius, 50 core
radius, native timing, collision rules, sound and visuals. The original beam
damage and all six earlier upgrades remain intact. Labor II still requires
four installed components.

## Verification

The October 21 entry/research update passes the development build and native
CSV validation for 899 rules. Focused headless regressions cover the 3/2/1
manifestation cap and local patrols, seventh-station reveal and one-time
salvage, actual component recovery and installation, and the native Glare
effect's explosions, hull scope, color, pause and per-ray state.

Enemy-port checkpoint `979a7bb` was verified October 15, 2026 against its development JAR, SHA-256
`2ee6badc34e55ba2a6d8fd5c602c0cb08d622f09930a062e3ce0ca18a4c7d74c`.
The Java 17-target build and native loader pass for all 891 dialogue rules and
the four changed enemy registries. Content checks pass for all 20 vanilla
weapon fits and all required enemy assets/classes. Combat regressions pass
both without SEP and with installed SEP 0.9.6 initialized. The population,
Budai encounter/roster, campaign lifecycle, music, interaction and salvage
regressions pass; the existing Heavenly Strike spawn and Avici cleanup checks
also pass.

Headless regressions check content references, standalone class loading, coexistence
with SEP, the shared domain boundary, modifier cleanup, lightning ownership,
first-entry unlock, the 30-day replacement boundary and persistent fleet slots.
These checks do not replace in-game testing of the new bodies, shader rendering,
AI or encounter balance.

The content validator accepts `--baseline 159b526` to verify that this port leaves
the existing weapon package unchanged, and `--sep "path/to/SEP"` to check
definition collisions. The combat regression also initializes the installed
SEP's real creator registry and checks that neither registry is overwritten.
The population regression covers first-entry unlock, exact fleet-tier counts,
local patrol orders, slot ownership, native-style battle despawn and delayed
replacements. Existing Budai encounter, roster, salvage and music regressions
retain legacy coverage.

Optional LunaLib settings now expose the Troy fleet-DP limit (1â€“1,000; default 50), applied live to route opening and both directions. At 240 DP or above, the Eventide route briefing offers the additional comment and Sinni response authored by the user. Focused settings, portal, dialogue, guidance and Intel checks pass with LunaLib absent and with installed LunaLib 2.0.4. The broader MenelausFlowRegression still hits its existing Gautama-explanation assertion with both the prior committed rules and current rules; the affected report pagination, final report timing and authorization highlights pass separately.

The public `v0.5.1` ZIP remains SHA-256
`9fde6b8e174464ad737387ee0f348692ef200953d2c1a0b49d8004b264b52941`;
GitHub main remains `058747fc483f83b005ab42c9d6363502bee6a947`.
These were checked read-only after the development build. No dev push or
release update has been made.
