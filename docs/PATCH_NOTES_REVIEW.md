# Cumulative patch notes review

Working draft: [Patch notes from 0.5.0 to current development](PATCH_NOTES_0.5.0_TO_CURRENT_DRAFT.md).

## Comparison baseline

- Baseline: the original v0.5.0 tag, commit 52d00c0a54309a7b1f5cee9861d75427cb58680d.
- Current scope: the installed 0.5.2-dev working tree on November 11, 2026, including subsequent 0.5.0 and 0.5.1 fixes.
- Active build at the start of this review: jars/ChiefNavigator.next.jar, SHA256 8dfb2b472f26bfdf3f2f3ee6496920af5fc0817546b35c05df988885d84e1ab7.
- The same-version release downloads received later revisions while the original tags were retained. These are cumulative notes from the original tag, not a claim that every item is newly unreleased.
- No release version, tag, download, JAR, manifest, or gameplay data is changed by preparing these notes.

## Evidence by subject

| Subject | Primary references |
| --- | --- |
| Original release and subsequent releases | [0.5.0 notes](RELEASE_NOTES_0.5.0.md), [0.5.1 notes](RELEASE_NOTES_0.5.1.md), original tag contents and local Git history |
| Recruitment and saved officer preservation | [SinniBarEvent](../src/chiefnavigator/quest/SinniBarEvent.java), [SinniContact](../src/chiefnavigator/quest/SinniContact.java), [SinniOfficerTraining](../src/chiefnavigator/quest/SinniOfficerTraining.java) |
| Skill values and elite tiers | [Adept of the Waves](../src/chiefnavigator/skills/AdeptOfTheWavesSkill.java), [Adept of the Stars](../src/chiefnavigator/skills/AdeptOfTheStarsSkill.java), their skill definitions |
| Troy limit and Ambush Stance | [Luna settings](../data/config/LunaSettings.csv), [Ambush Stance](../src/chiefnavigator/abilities/SinniAmbushStanceAbility.java) |
| Avici patrols and permission scope | [Avici development notes](DEV_AVICI_DWELLERS.md), [population controller](../src/chiefnavigator/quest/AviciDwellerPopulation.java), [Budai escort](../src/chiefnavigator/quest/BudaiFleetComposition.java), [third-party notices](../THIRD_PARTY_NOTICES.md) |
| Seventh station and Lens | [research controller](../src/chiefnavigator/quest/AviciResearchStationScript.java), [component state and ledger](../src/chiefnavigator/weapons/IthacaResearchUpgrades.java), special-item data |
| Domain drone reward | [native skill reward](../src/chiefnavigator/campaign/DomainSecurityIFFAuthorization.java), MenelausTrial loadouts, legacy skin definitions |
| Finale and saved progression | [invasion controller](../src/chiefnavigator/quest/OdysseyPredatorScript.java), [final battle](../src/chiefnavigator/quest/IthacaFinalLaborBattleCreationPlugin.java), [MenelausTrial](../src/chiefnavigator/quest/MenelausTrial.java) |
| Station alliance and joining | [Task Force Spartan relations](../src/chiefnavigator/quest/TaskForceSpartanFaction.java), [Ithaca sections](../src/chiefnavigator/quest/IthacaSectionEncounter.java) |
| Ungaikyo and generated readiness | [current hull skin](../data/hulls/skins/chief_navigator_ungaikyo.skin), [reflection battle controller](../src/chiefnavigator/quest/GautamaFinalRematchBattleCreationPlugin.java) |
| Dialogue, latest report and ending | [runtime rules](../data/campaign/rules.csv), [Labor reports](../dialogue/menelaus_labor_reports.ink), [ending](../dialogue/labor_5_ending.ink); validation records in out_labor4_debrief_20261111 and out_ending_scene_20261111 |
| System names and gate guidance | [world labels](../src/chiefnavigator/quest/OdysseyExpanseSystem.java), [Sinni guidance](../src/chiefnavigator/quest/SinniNavigationGuidance.java), [direct gate command](../src/data/campaign/rulecmd/ChiefNavigatorIthacaGateCMD.java) |
| Cargo compatibility | [storage dismissal](../src/chiefnavigator/quest/FobIthacaStorageDialog.java), [native rule context](../src/chiefnavigator/quest/RuleDialogSupport.java) |
| Naval presentation and swarm assembly | [turret renderer](../src/chiefnavigator/weapons/ColossalNavalTurretRenderer.java), [Gautama swarm launch](../src/chiefnavigator/hullmods/GautamaSwarmLaunch.java) |
| Music and build installation | [ending music](../src/chiefnavigator/quest/MenelausEndingMusic.java), [campaign music](../src/chiefnavigator/quest/FinalLaborMusic.java), [JAR installer](../tools/install_build_jar.ps1) |

The out_patch_notes_20261111 directory records the original release files, complete local history, tracked changes, baseline identity, active runtime hashes, and a presence check for the new runtime class families.

## Claims deliberately excluded

- The five Labors, six authored systems, independent post-Labor hunts, reflection DP limits, Gautama's reincarnation pity system, and most Maw combat mechanics already existed in 0.5.0. They are not presented as newly added.
- Budai's interim Insatiable Hunter boss implementation has been reverted. The current boss is the Maw; the ordinary Hunter remains an Avici patrol enemy.
- The proposed Wall survey-ship livery is an art candidate, not the installed Wall. Gautama's proposed evolving Lv2 through Lv4 forms and future reincarnation markers are also not installed.
- No new Combat Ithaca side route, dedicated Ithaca faction, or per-Labor Kleon ship/crew side missions are claimed.
- Requiring Janus integration for recruitment does not establish that the older Devoured Ring history has been reconciled with every event in At the Gates. The wider lore revision remains separate work.
- The latest rename is Last Light to Bouvo Olympus. The old work-board reference to Helios is not the current implementation.
- The separate local Nexerelin startup hotfix and changes inside the Peerless Skills mod are not Chief Navigator release contents.
- Boss-only weapon tags and substrate/mining fixes do not establish that every third-party bounty generator is covered. The reported ordinary-bounty fitting issue remains an open investigation.
- The research-station persistence report is not described as fixed by the Lens's actual-recovery tracking.

## Checks before publication

1. Choose the release version and decide which remaining development content belongs in that release. Reconcile these cumulative notes with the final package rather than treating all local development files as automatically publishable.
2. Confirm the supported Peerless Skills version and its distribution instructions. The current manifest declares 0.1; the locally connected installation has received separate development work.
3. Complete the live checks for Avici enemy visuals and balance, seventh-station discovery, Ungaikyo's Overseer body and reflections, final-fight station control, direct gate travel, and the newly wired report and ending. Existing native regressions are recorded; this documentation task does not constitute a new playthrough.
4. Keep the reproduced cargo-dismissal fix narrowly described. It reproduces a Nexerelin/BMO invalid-market crash path, not every reported storage or comm failure.
5. Review legacy saves separately: preserve saved Sinni stats; verify the one-time system labels; keep one-time endings consumed; exercise the explicit idle Budai conversion only where applicable. Do not imply wholesale retroactive replacement of saved encounter rosters.
6. Retain the active uncapped artwork and the port's attribution. Exclude unregistered concept art and private test/reproduction outputs from the runtime package.
7. Build and validate the final release artifact, then confirm package contents, Java compatibility, asset references, checksums, and the supported dependency set. No final release ZIP or live release verification was performed for this draft.
8. Refresh stale README and work-board statements before publication, including the wrapped old system name, old development/publication snapshots, and completed rename status. PostLaborHuntIntel also still describes Budai as hunting alone; reconcile that line with the installed escort.

## Latest focused verification already recorded

- System renames: seven regressions and native dialogue loading, with saved-label identity checks.
- Labor IV report and Labor V lead-in: all 15 supplied nodes, 12 native branch combinations, explicit acceptance, read history, one-time reward and completion.
- Ending: all 18 supplied nodes, 576 actual-shell/native paths across both player pronouns, one-time completion, music, portrait coverage, and 908 native CSV rules.

These checks support the corresponding changes. They do not replace the final release build or the outstanding live tests.
