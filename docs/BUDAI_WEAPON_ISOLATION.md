# Budai weapon isolation

A player reported a ship in a random bounty battle equipped with Budai Hungering Rifts.
These custom weapons are reserved for Budai's historical Maw encounter.

The two weapon rows previously copied vanilla `monster` and `dweller` tags and
did not use the native `SYSTEM` hint. That is also how normal vanilla Dweller
weapons are registered; absence of this hint is not itself a fleet-generation
bug. The `restricted`, `no_bp` and drop exclusions do not remove weapons from
the general API catalog, which is distinct from a faction's permitted loadout
pool. `DefaultFleetInflater` builds that pool from faction `knownWeapons`.
Ordinary human factions do not import the `dweller` tag. Assuming a vanilla
bounty, the original Budai rows should already have been absent from its normal
weapon pool. The reported loadout has not been reproduced and its cause remains
unidentified.

The October 22 comparison checked all 13 vanilla Omega, six Dweller and twelve
THREAT weapon rows. All carry `restricted`; none uses `SYSTEM`. Their special
factions import the corresponding `omega`, `dweller` or `threat` tags, while
normal human faction weapon lists do not.

An external native-engine comparison confirmed that all 31 of those weapons
remain catalog-visible and fit a compatible native weapon slot. Flaming Sword
does too. These checks establish catalog and mount compatibility only, not
eligibility for an ordinary bounty's faction pool.

Hall of Triumph's Flaming Sword likewise has no `SYSTEM` hint. It is an ordinary
small energy beam with `monster, dweller, restricted, codex_unlockable, no_drop`
tags, explicitly built into the Golden Dextral Shard. It also has no numbered
autofit category tags, so native category matching does not select it as a
replacement. This was also checked against `WeaponSpreadsheetLoader`: its
automatic category generation runs only when the entire loaded tag set is
empty, not merely when numbered category tags are absent. The Sword's existing
tags skip that fallback. Budai's Rift and Jet previously inherited `pd10, missile10`
and `kinetic15` respectively; these selection tags are now removed. The Sword's
built-in slot does not globally reserve the weapon;
no hidden Hall of Triumph catalog-filtering hook was found.

As an additional boss-only restriction, both custom weapons now use the native
`SYSTEM` hint, which excludes built-in
weapons from `SettingsAPI.getAllWeaponSpecs()` and
`SectorAPI.getAllWeaponIds()`. Native autofit also rejects this hint through
`WeaponSlotAPI.weaponFits()`, including when a saved faction still knows the
weapon ID. Explicit authored variant fittings use a separate native add-weapon
path and remain valid. Both rows have no faction, blueprint-package, numbered
autofit or range-selection tags, and need no private marker. Their tag fields
contain only `restricted, no_bp_drop, no_sell, no_dealer, no_drop, no_drop_salvage`.
These native exclusions cover blueprint raids, generated market stock, dealer
production and relevant drops. Keeping this nonempty exclusion set also prevents
the spreadsheet loader from synthesizing new autofit tags for an empty tag field.

The user's [Blueprint Modding reference](https://starsector.wiki.gg/wiki/Blueprint_Modding)
also prompted a separate blueprint-raid correction: both rows now use
`no_bp_drop`, replacing `no_bp`. The installed game's `Tags.NO_BP_DROP` and
`BlueprintGroundRaidObjectivePluginImpl` confirm this is the native raid
exclusion. `no_bp` is not a native exclusion, and the raid check does not
substitute `SYSTEM` or ordinary `no_drop` for `no_bp_drop`. This prevents a raid
from yielding either blueprint if a faction has acquired knowledge of it; it
does not identify the bounty's equipment-selection path. Neither boss row has
`base_bp`, `rare_bp` or a blueprint-package tag.

The existing IDs, weapon/projectile specifications, effects, combat statistics,
escaped tooltip percentage and explicit Budai variant fit remain intact.
Vanilla Dweller weapons and the deliberately mountable Heavy Adjudicator reward
are unchanged. There is no campaign migration or saved-loadout rewrite.

This is a development isolation change; restart Starsector to reload the weapon table.
It does not update the published 0.5.1 archive or remove weapons from an already
generated bounty.

`tools/BudaiWeaponIsolationRegression.java` checks the installed engine's weapon
list filtering, exclusion-only tags, absent autofit category, direct authored fit
and reward exception.
Engine internals used for verification stay confined to that external tool.

Verification passed on October 22, 2026: the native CSV parser accepted the
table; removing `SYSTEM` reproduced both general-list and compatible-slot
exposure, rather than the reported bounty generation; restoring it excluded
both weapons; explicit variant fitting retained
all six Rifts and two Jets. `BudaiAdjudicatorSalvageRegression` also passed.
Comparing the weapon table with Git HEAD confirmed that only these two weapons'
hints and tags changed, preserving all other fields and rows.

Run from the mod directory in PowerShell:

```powershell
$budaiCoreJars = @(Get-ChildItem ..\..\starsector-core\*.jar | ForEach-Object FullName)
$budaiNativeCp = $budaiCoreJars -join ';'
New-Item -ItemType Directory -Force out_budai_weapon_isolation_regression | Out-Null
& ..\..\jdk-23+7\bin\javac.exe --release 17 -encoding UTF-8 -cp $budaiNativeCp -d out_budai_weapon_isolation_regression tools\BudaiWeaponIsolationRegression.java
& ..\..\jdk-23+7\bin\java.exe -Xverify:none -cp "out_budai_weapon_isolation_regression;$budaiNativeCp" BudaiWeaponIsolationRegression
& ..\..\jdk-23+7\bin\java.exe --source 17 tools\BudaiAdjudicatorSalvageRegression.java
```

The native harness needs `-Xverify:none` to load this installed engine's
obfuscated classes under the bundled JDK. No game JVM configuration is changed.

## Wider vanilla code audit

The October 22 follow-up traced installed source and engine bytecode beyond the
CSV tags. No ordinary bounty path was found that imports a custom weapon from
the global catalog into an otherwise clean human faction's fitting pool.

- `PersonBountyIntel.spawnFleet` creates pirate or deserter-faction fleets.
  Contact bounty creators use their specified fitting faction. The displayed
  faction can differ from the data faction used for generation.
- `DefaultFleetInflater.inflate` builds fresh per-tier/category/size lists from
  the fitting faction's known weapons and checks their knowledge timestamp.
  Its `allWeapons` flag raises availability and the number of picks within that
  existing pool; it does not read every registered weapon. The corresponding
  flag on contact bounty triggers has the same meaning.
- `CoreAutofitPlugin` copies candidates from its delegate. Primary and fallback
  categories both use those candidates. Its reusable category caches do not
  contain weapon instances, and selections reset between top-level ships.
- `restricted` and `no_sell` are not prohibitions in that fitting path. A known,
  compatible weapon with matching category tags can be selected despite them.
  Explicit authored fittings and hulls/variants marked to skip autofit are
  separate paths. `CBRemnantPlus` deliberately adds one authored Tesseract; it
  does not choose an arbitrary special weapon from the global catalog.
- The native faction importer OR-matches the requested faction tags and adds
  explicitly listed weapon IDs. Its known-weapon path does not automatically
  add `base_bp` or infer access from `codex_unlockable`.
- Normal faction updates merge the configured knowledge into saved knowledge;
  they do not discard a weapon merely because its current CSV tags changed.
  A previously learned ID is therefore a distinct diagnostic variable.
- `BlackMarketPlugin.delayedLearnBlueprintsFromTransaction` accepts only sold
  items implementing `BlueprintProviderItem`. Selling loose guns does not
  teach pirates their blueprints. A supplied blueprint can teach a restricted
  weapon after the delay; that learning path does not recheck `restricted`.
- All tracked versions of the Budai Rift row, including its initial committed
  definition and the tooltip/substrate hotfix, already carried `restricted`
  and lacked ordinary faction/blueprint tags such as `base_bp`, `rare_bp` or
  `merc`. Repository history does not establish a prior normal human import.

These findings narrow the remaining evidence needed to the actual campaign's
fitting-faction knowledge and the affected member's variant/inflater state.
They do not establish which path produced the reported bounty. No faction
knowledge, saved loadouts, vanilla content or Hall of Triumph files were
rewritten during this audit.

Two isolated native-engine fixtures passed, including an independent rerun:

- The native CSV/JSON readers, real weapon registry, `SpecStore` faction import
  callback and `Faction` initialization processed all 21 installed core faction
  files using the original Budai tag rows. Only Dweller imported either boss
  weapon. Adding `codex_unlockable` left all 21 imported weapon sets identical.
  Empty tag/ID lists added no weapons, tag matching was OR-based, an explicit
  restricted ID was accepted, and knowing a hull did not teach its variant's
  weapons. An explicit grant to a native pirate faction survived the normal
  additive update and disappeared only on an explicit reset.
- The real `WeaponSpreadsheetLoader` processed the Hall of Triumph Sword row
  and an otherwise identical empty-tag control. The Sword retained exactly its
  five tags, had a null autofit category and matched no native autofit category.
  The empty-tag control received `beam7, LR` and matched the beam category.
  Both remained at zero OP, excluding zero cost as the reason for no category.

These were headless tests of installed engine methods with isolated registries
and settings fixtures. They did not start a campaign, simulate the reporter's
battle, or establish that the reporter's faction had learned the weapon.
Exploratory harnesses are kept in ignored `out_budai_vanilla_faction_audit/`
and `out_native_bounty_audit/`; extracted native source stays outside tracked
project sources.
