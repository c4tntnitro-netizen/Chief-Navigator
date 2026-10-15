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

Newly created Budai encounters use the Insatiable Hunter-class hull and keep
Budai's historical campaign identity, intro gate, 100,000-unit initial spawn,
Avici pursuit, Kshaya return, one-time defeat, soundtrack and salvage flow.
Existing living saved Budai members remain intact; this build does not replace
their ships or create a second boss. New encounters are needed to test the new
hull; the older Maw implementation remains available for save compatibility.

The hunter's impulse-shader domain has a 1,000-unit resting radius. Activating
Forced Reality expands its boundary toward 4,000 over five unpaused seconds;
after the system deactivates it contracts toward the resting radius. The same radius bounds
the domain buffs and lightning. Lightning retains the old player-side targets
and damage/EMP behavior. The new hull has no outside-shot deflection, DEFLECTED
popup, Maw scaling, leap sequence or movement slowdown. Base system timing is
12 seconds active, a 24-second cooldown and a six-second overload on collapse.

Ambient Lesser, Greater and Transcendent Manifestations unlock after 30 campaign
days from Budai's successful first creation. The timestamp survives saves and
continues while the player is elsewhere or after Budai is defeated. An older
save with positive prior-spawn evidence and no recorded date receives a separate
one-time 30-day grace anchor; that anchor is not claimed to be the original birth
date. A missing or failed Budai spawn never starts the clock.

Patrols have two Lesser slots, one Greater slot and one Transcendent slot, with
their own ownership markers. Only a verified battle destruction permits a
replacement, after another 30 campaign days. Living rosters,
foreign/player objects, busy fleets and existing claims are preserved. Patrols
remain in Avici and avoid the habitat shelter and jump entrances. New manifested
fleets never contain another Insatiable Hunter.

## Verification

Verified October 15, 2026 against the canonical development JAR, SHA-256
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
the 30-day boundary and persistent fleet slots. These checks do not replace
in-game testing of the new bodies, shader rendering, AI or encounter balance.

The content validator accepts `--baseline 159b526` to verify that this port leaves
the existing weapon package unchanged, and `--sep "path/to/SEP"` to check
definition collisions. The combat regression also initializes the installed
SEP's real creator registry and checks that neither registry is overwritten.
The population regression covers actual spawn timing, saved timestamps, slot
ownership, native-style battle despawn and delayed replacements. Existing
Budai encounter, roster, salvage and music regressions retain legacy coverage.

The earlier LunaLib portal-DP slider remains a separate pending development item.

The public `v0.5.1` ZIP remains SHA-256
`9fde6b8e174464ad737387ee0f348692ef200953d2c1a0b49d8004b264b52941`;
GitHub main remains `058747fc483f83b005ab42c9d6363502bee6a947`.
These were checked read-only after the development build. No dev push or
release update has been made.
