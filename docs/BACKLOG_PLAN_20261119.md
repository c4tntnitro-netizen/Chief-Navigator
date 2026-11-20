# Backlog plan — November 19, 2026

Read-only review of `docs/PROJECT_BOARD.md`. **No game files, jars, rules or
saves were changed.** Nothing here is reproduced in game; the bug sections are
code-reading theories with file references, ranked by likelihood.

## Summary

| Card | What I found | Ready to build? | Runtime cost of the fix |
| --- | --- | --- | --- |
| CN-034 Mirrored Onslaught lasers | Likely a data typo-level cause: the ship laser has the `STRIKE` AI hint the fortress gun had removed for the same "won't fire" problem | Yes, after your OK | None (data only) |
| CN-021 Research station persists | Station is removed normally; the real bug is the opposite — a left-behind component can **expire forever** in cargo pods | Yes, pick an option | One-time, per salvage |
| CN-033 Survey ships fought twice | Any non-kill ending resets the mothership to full strength; plus a rare orphaned-proxy path | Needs your design call | None / one-time on load |
| CN-019 Debug command cleanup | Move 8 test commands into an optional "debug" mod folder | Yes | None |
| CN-002 Menelaus Continue | **Already fixed** by a later rewrite — the redundant line no longer exists | Close card | — |
| CN-029 At the Gates lore | Four text touchpoints found | Writing pass, your voice | None |
| Others (CN-001, 014–017, 024, 026–028, 030, 031, 035) | Need design/art decisions first | No | — |

---

## CN-034 — Mirrored Onslaught (ITH) doesn't fire its naval lasers

**Update (Nov 19):** user confirmed the AI does fire the lasers, just
reluctantly. Not broken; this is now a tuning question (make it fire more
readily), not a bug fix. User chose to leave the `STRIKE` hint as is; no
change planned.

**Leading theory (data, verified):** in `data/weapons/weapon_data.csv` the two
ship lasers (rows 29 and 31) have AI hints
`STRIKE, DO_NOT_CONSERVE, DANGEROUS, SYSTEM`. The fortress turret (row 11) has
only `DO_NOT_CONSERVE, DANGEROUS`. AGENTS.md already records that the sustained
naval beam "must use `DO_NOT_CONSERVE, DANGEROUS` without the opportunistic
`STRIKE` hint" — that was the fix when the fortress guns wouldn't fire. The ship
laser's hints are hard-coded with `STRIKE` in
`tools/build_onslaught_ith_assets.py:99`, so every rebuild writes it back.

`STRIKE` tells the AI to save the weapon for an opening on a vulnerable target.
With a 3 s charge, 5 s burst and 30 s cooldown, the AI may simply never commit.
**This would affect any AI-piloted Onslaught (ITH), not only mirrors** — you
probably only notice on mirrors because you fly your own by hand. A quick check:
give your Onslaught (ITH) an officer and let it fly on autopilot.

**Second suspect:** flux. The linked pair costs ~15.75k flux per full cycle on
a ~24.6k pool; an aggressive mirror with every other gun firing is often too
high on flux to start a burst it can't finish.

**Ruled out:** mirror construction hold-fire is released correctly
(`GautamaFinalRematchBattleCreationPlugin.java:908-909, 1029-1030`), no weapon is
force-disabled, and `DriftingWallBeamEffect` / `IthacaResearchUpgrades` don't
check owner or fleet member. (Side note: mirrors inherit *your* installed
research upgrades, since the mask is sector-wide. Intended?)

**Plan:**
1. Remove `STRIKE` from both ship-laser rows and from
   `tools/build_onslaught_ith_assets.py` / `build_odyssey_ith_assets.py` so a
   rebuild can't put it back. Decide on `SYSTEM` (vanilla TPC has it and still
   fires, so probably keep).
2. Headless test: native-parsed hints match the fortress rule; an enemy-owned
   clone activated through the mirror path has hold-fire off and AI present.
3. In game: Ungaikyo fight with the Onslaught (ITH) in fleet, plus one
   officer-piloted friendly test.

**Performance:** none — data only. Minor optional cleanup:
`DriftingWallBeamEffect.hasLingeringBeam` walks every beam in combat each idle
frame per ITH gun; it could skip when the gun hasn't fired recently. Small.

---

## CN-021 — Research station "persists" when the component is left behind

**What the code does:** the mod never keeps a salvaged station alive and never
recreates one on load (`OdysseyExpanseSystem.ensureResearchStations`,
lines ~1752-1817, only runs at first generation). Vanilla salvage
(`SalvageEntity`) turns the station into a debris field and fades it out — but
the fade only runs while the game is **unpaused**, so it lingers on screen
until you close the dialog and a second passes. The debris field has ordinary,
finite loot. So unlimited repeat salvage is unlikely.

**The real bug (likely):** anything left in the loot window goes into **cargo
pods**, and vanilla cargo pods expire after some days unless stabilized
(`CargoPodsEntityPlugin`, `maxDays` check). The mod marks the station
"salvaged" when the loot window *shows* the component, and nothing ever
re-creates the component. If the pods expire:

- that component is lost for good;
- Labor II can still finish (any 4 of 6), but the **Avici 7th station never
  appears**, since it needs all 6 originals recovered.

**Options (pick one):**

- **A. Pods never expire (recommended).** After a research salvage, a one-shot
  script finds the pods next to that station and sets them to never expire,
  plus a map marker ("Unclaimed Ithaca component"). Simple, matches what the
  player sees.
- **B. Re-provision.** If a component is in neither your cargo nor installed,
  Menelaus's ledger offers a recovery lead and spawns a small salvage site.
  More code, more save-state.
- **C. Auto-transfer.** Force the component into cargo on salvage. Least code,
  but takes choice away.

**Performance:** A runs once per research salvage (seven times per campaign)
and scans one system's entity list once. Zero per-frame cost.

**Test:** extend `tools/AviciResearchStationRegression.java` — salvage with the
component left behind, check the station is fading, the pods contain the
component and never expire, and reload makes no new station.

---

## CN-033 — Survey ships (Guard Motherships) can be fought twice

**How it works now:** clicking a mothership builds a temporary fight fleet
(mothership + 3 frigates) — `ChiefNavigatorMothershipCMD.java:45-93`,
`MenelausTrial.java:696-741`. When the dialog closes, the temp fleet is always
deleted, and the mothership counts as beaten **only if its main hull died**
(`MenelausTrial.java:774-784`).

**Ranked causes:**

1. **Partial win then reset (most likely).** Kill the escorts or the bow
   module, retreat or lose, and the next click gives a brand-new full-strength
   fight. All damage is lost. Feels like "fighting it twice."
2. **Two engagements in one dialog.** Vanilla lets you re-engage while the
   station survives. Damage carries over; this is normal vanilla behavior.
3. **Orphaned temp fleet (rare).** If the dialog ever exits without the
   cleanup hook (an exception, unusual exit), the temp fleet stays as a real
   aggressive hostile fleet that can intercept you — a full fight that never
   counts. The only orphan cleanup lives in a one-time world-setup routine, so
   it effectively never runs again.

**Design question for you:** should mothership damage persist between
attempts? Options:

- **Keep it as is** (each attempt is fresh, like a vanilla station defense) and
  only fix cause 3.
- **Persist damage:** keep the surviving temp fleet hidden and frozen at the
  POI and reuse it on the next engage, instead of deleting it.

**Fix for cause 3 regardless:** on each game load, look up the four known
temp-fleet IDs directly and remove any that are idle. Four direct lookups,
once per load — no sector-wide scan.

**Test:** extend `tools/SmallDerelictSpawnRegression.java` with the four
outcomes (escorts only / module only / main hull / empty fleet) and an
orphaned-proxy-on-load case.

---

## CN-019 — Debug console command cleanup

Nine commands in `data/console/commands.csv`. Console Commands lists anything in
any enabled mod's `commands.csv`, so a settings toggle would still list them.

**Plan:**
- Keep in the main mod: `InitializeNavigatorWorlds` (a real player recovery
  tool documented in the README).
- Move the other eight (`RefreshContactBounties`, `AddContactBountyProgress`,
  `AddTopographyData`, `SpawnIthacaStrike`, `SpawnSTInvasionFleets`,
  `ForceFinalLabor`, `DestroyIthacaGateStation`, `MaxUpgradeWallModules`) into a
  tiny optional mod folder, "Chief Navigator Debug", containing only a
  `mod_info.json` (depends on Chief Navigator) and its own `commands.csv`. The
  classes stay in the main jar, so no second jar.
- Update the release packager to build the debug folder as a separate ZIP, and
  update README lines 70-71 that mention these commands.

**Performance:** none. **Question:** do you want `MaxUpgradeWallModules` and
`ForceFinalLabor` kept public as "skip" tools for players? The README
advertises both.

---

## CN-002 — Menelaus Continue cleanup → close it

The card described a Continue that only revealed "What do you need from me?".
That line no longer exists anywhere in `rules.csv` or the Ink files. The current
Continue after "They have shored up the defenses as best they can." now leads to
new prose ("Menelaus dismisses the tactical display..."). Suggest moving it to
Done after a glance in game.

---

## CN-029 — Reconcile with "At the Gates" (writing pass)

Places that say the Threat fed on the Ring for centuries:

- `data/strings/descriptions.csv` line 6 — Devoured Ring description
  ("beneath centuries of Starving Threat accretion").
- `rules.csv` ~line 2950 — Labor IV: "after centuries without sufficient
  feedstock."
- `rules.csv` ~line 2937 — "The Threat predates the Collapse by centuries."
  (Probably fine — that's the Threat's age, not the feeding.)
- `rules.csv` ~line 1768 — components "consumed long ago" from the Ring.
- Matching Ink mirrors, plus the Labor IV report and AGENTS.md lore notes
  ("spent centuries consuming the ring").

This is your prose, so I'd draft replacements for you to approve rather than
write them blind. No runtime cost.

---

## Needs your design first

- **CN-001 Static Sinni dialogue** — card says agree on intended behavior first.
- **CN-014 System names for 1.0.0** — board text is stale ("Lacedaemon ->
  Lacedaemon"); only Bouvo Olympus -> Helios still seems pending. Confirm.
- **CN-015/016/017 Gautama finale** — deferred; art and sequence decisions.
- **CN-024 Wall livery** — art pass.
- **CN-026/027/028/030 Expedition, Ithaca faction, Combat Ithaca, Kleon side
  missions** — content not defined yet.
- **CN-031 Sinni as SiC XO** — needs her XO abilities and SiC's API checked.
- **CN-035 Accelerated start** — story outline needed.

## Board housekeeping

- CN-003 is marked Done but still sits under Backlog; CN-023 sits under Done but
  says Needs Testing.
- CN-022 and CN-025 appear in both Backlog prose and the Done column.
- CN-018 is retired but still has a full section (fine as history).

## Performance notes

None of the proposed fixes add per-frame work. The worst is CN-021 option A: one
scan of one system, seven times per campaign. The upkeep fix from earlier
today (`ChiefNavigator.upkeep-once-20261119.jar`) still needs a live check after
restart.
