# Chief Navigator 0.6.0

**Changes since the original 0.5.0 release. November 11, 2026.**

This release combines the subsequent 0.5.0 revisions, 0.5.1 updates, and the current expedition update. The later sections contain spoilers for the Labors and their ending.

## Requirements

- Starsector 0.98a.
- GraphicsLib 1.10.2 or newer.
- Combat Chatter 1.15.0 or newer.
- **Peerless Skills 0.1 or newer is now required**.
- LunaLib is optional for the configurable expedition fleet limit. Hall of Triumph remains optional for Isa's existing integration.

## Sinni recruitment and officer skills

- Sinni's initial proposal now requires an integrated Janus Device, completion of the vanilla Hyperspace Topography ability tree at 700 points, and the THREAT-detection sensor calibration obtained during the Onslaught Mk.I investigation. Carrying an unintegrated Janus Device does not meet the gate-travel prerequisite.
- Restore the earned topography requirement after it was possible to recruit her too early. Previously accepted expeditions remain accepted, and spending data on the repeatable topography payout does not erase earned eligibility.
- Newly created Sinni officers start at **level 2**, with an **eight-level cap** and two permanent, initially non-elite skills:

| Skill | Normal effect | Elite effect |
| --- | --- | --- |
| Adept of the Waves | +50% maneuverability; +70 speed for frigates, +40 for destroyers, +30 for cruisers, and +25 for capitals | Another +15 speed and the Helmsmanship-style zero-flux boost while not generating flux, including while venting or overloaded |
| Adept of the Stars | 15% less shield, armor, and hull damage taken | +1 ship-system charge, +25% charge regeneration, +25% system range, and 33% less system cooldown |

- Both skills support normal elite upgrades and stack with Helmsmanship and Systems Expertise.
- Preserve the exact level, experience, skills, skill points, and cap of Sinni officers already present in a save. The new starting package is applied only when she is first created.
- Reuse an existing Sinni officer if her contact index has been lost, avoiding a replacement person.
- Show the Shrouded Dweller answer in her introduction only after the player has encountered Dwellers.

## Troy and expedition preparation

- Add an optional LunaLib setting for the Troy fleet deployment-point limit, adjustable from **1 to 1,000 DP**, with **50 DP** as the default.
- Apply the configured limit to opening the route and crossing Troy Terminus in either direction.
- Update Sinni's introduction and Eventide tea briefing, including the additional exchange for an expedition limit of at least 240 DP.
- Replace Ambush Stance's forced-pursuit behavior with an improvement to ordinary Running Dark. At its topography unlock, Running Dark reduces detection range by **65%**, while retaining normal activation, movement restrictions, and cleanup.

## Dialogue and story presentation

- Rewrite FOB Ithaca's approach and Menelaus's first audience, including the shuttle arrival, guard exchanges, holodeck introduction, and player responses.
- Revise the Labor I questions and Helen conversations, including their repeatable questions and follow-up routes.
- Rewrite the Labor II briefing and optional conversations about the defense components, Spartan personnel, Gautama, and Helen.
- Revise Labor III's briefing and associated history and Helen conversations.
- Expand Labor IV's briefing into the supplied conversation about the survey package, Anatolia, the Threat, Sinni, and Helen. Retain its explicit acceptance and the return routes from optional questions.
- Replace the Labor IV report with the Gate-reconstruction investigation. Either opening answer reaches the two interpretation choices; both explanations continue into the discovery of the four approaching formations.
- Rewrite Menelaus's final-Labor offer and acceptance, followed by Kleon's mobilization orders and field briefing. All three farewell replies converge on the same departure.
- Replace the final victory acknowledgement with the supplied shorter debrief.
- Rewrite the Sinni epilogue, adding the memorial concourse, revised sleep and tea exchanges, player-dependent pronouns in the poem, and the Pai Chiu drinking reactions.
- Revise Voss's confrontation and its player responses, including the cast shown when Sara is absent.
- Update the Hegemony exile, Ithaca resident, regional-arrival, and friendly-drone dialogue.
- Simplify Menelaus's upgrade-ledger greeting.
- Keep long scenes divided at paragraph boundaries, correct malformed quotation and paragraph formatting, and align read/unread highlighting with the revised choices. Intermediate Continue pages do not finish an answer's read history.

## System names and navigation

| Previous system name | Current system name |
| --- | --- |
| Ashen Verge | Lacedaemon |
| Devoured Reach | Anatolia |
| Last Light | Bouvo Olympus |

- Apply the names throughout system labels, stars, jump points, travel menus, dialogue, Intel, research leads, and Sinni's guidance.
- Existing campaigns receive a one-time label update on the same saved systems and linked objects. Their positions, identities, destinations, and quest progress are retained.
- Improve handling of duplicate Odyssey Expanse hyperspace exits, including duplicates following the same native exit orbit at different radii.
- Remove the extra descriptive subtitles from Sinni's regional chart.
- After Ithaca's Gate has been scanned, other eligible scanned gates offer direct travel to it. The option uses normal Janus access, fuel cost, and transit behavior, allowing travel without selecting the remote system on the gate map.
- Update Sinni's guidance when the Gate is scanned. She directs the player to Captain Aias Kleon while either post-Labor hunt remains unfinished, then returns to open exploration.

## Research and Ithaca upgrades

- Add a **seventh research station** in Sanghata orbit, Avici, revealed after the six original defense components have been recovered.
- Count installed components and components actually recovered into player cargo toward that discovery. Merely opening the salvage screen does not count as recovering one.
- Add the **Abyssal Focusing Lens**. Installing it through Menelaus upgrades Ithaca's naval beams with native Abyssal Glare rift explosions and a teal-to-red fringe around their white cores.
- Keep Labor II's requirement at **four distinct installed components**. The remaining upgrades, including the Lens, stay optional.
- Extend the upgrade ledger and combat-upgrade test command to all seven components.
- Give the original six components their own cargo icons.

## Avici enemies and Budai

- Add Chief Navigator versions of the **Shrouded Banshee**, **Shrouded Brain**, and **Insatiable Hunter**, adapted from Shroud Expansion Pack with permission from TIDAL. Credit to TIDAL and RoaringWriter.
- Avici now supports **three Lesser, two Greater, and one Transcendent Manifestation** patrol slot. They become available on the player's first visit.
- Manifestations patrol locally using ordinary detection and remain inside Avici. A destroyed patrol can be replaced after 30 campaign days.
- The Transcendent patrol can include an ordinary Insatiable Hunter, separate from Budai.
- Budai remains the enlarged Shrouded Maw and now receives a Transcendent Manifestation escort with all additional Maws omitted. Escort losses are not replenished.
- Existing undefeated Budai fleets from the interim Hunter version receive the Maw restoration and escort once, when idle. Defeated, player-owned, foreign, and busy fleets are excluded.
- Keep the enemy adaptation independent of SEP's quests, officers, rewards, and weapon package. A separate SEP installation can coexist with it.
- Remove Budai's special Hungering Rift from substrate weapon assembly and fix its tooltip formatting error. Strengthen exclusion tags on his special weapons without changing their combat statistics.

## Domain drones and Ithaca combat

- Replace new authored Combat Guard patrol and escort loadouts with native Domain drone variants.
- Menelaus now grants **Automated Ships** once after the first Labor is accepted. A player who already has the skill receives **one skill point** instead.
- Retire new per-ship Domain-Security IFF authorization. Newly recovered drones use ordinary automated-ship rules and limits; existing legacy authorization remains available for saved hulls.
- Reduce **Cyclops Strike** from nine Fabricators to **five**, retaining its other support roles.
- Remove the ten medium secondary weapon fittings from the Drifting Wall foundation and Ithaca's corresponding foundation. Their colossal naval weapons and missile systems remain.
- Restore cooperative friendship and permanent player recognition for owned Ithaca defenders so transponder state does not block joining their battles.
- Mark the final-fight defense stations as allies for combat control.
- Fix colossal naval-gun artwork rendering beneath overlapping missile housings.
- Bound oversized naval-weapon damage flashes while retaining full-size combat artwork.
- Remove the extra mount-cap overlays from Ithaca's active Wall artwork.
- Rework Gautama's escort-swarm assembly so fragments transfer out of his own cloud rather than appearing fully formed at the launch point.
- Change Ungaikyo's local hull from a Fabricator to an **obsidian Overseer** with the corresponding native weapon fit.
- Fix generated reflections and their modules entering combat below their proper combat-readiness cap because of missing crew.

## Final Labor progression and aftermath

- Keep Labor IV incomplete until the final report Continue. The report and the final-Labor offer do not activate the invasion.
- Start Labor V, its Intel entry, and its campaign music only when the player selects **Accept the final Labor**.
- Fix Heavenly Strike failing to appear after the three perimeter Strikes are defeated when the Breach Defense Station has already been destroyed.
- Recover eligible older campaigns stuck at 3/4 when all three lane victories are recorded and no final-fleet claim remains.
- Without a surviving Breach Defense Station, Heavenly Strike approaches Ithaca's Gate and attempts a **30-day scan**. Its progress is saved and pauses during battles, interaction ownership, and time away from the Gate.
- Losing a player battle no longer immediately fails the Labor. The scan provides time to regroup.
- Completing the scan triggers the defeat scene and the fleet's withdrawal through Ithaca Gate. Retire the former failure outcome that launched recurring incursions through gates across the Sector.
- Spawn the two reserve battlestations only when the canonical Breach Defense Station is alive at the start of the final battle.
- Completing Menelaus's victory debrief now grants **one Peerless point**, once. The post-Labor hunts and separate Sinni ending continue through their existing completion sequence.
- The ending cuts the current music immediately, waits **five real seconds** rather than ten, and then begins its soundtrack. Dialogue pagination and campaign pause do not restart the delay.
- Unlock occasional Lacedaemon exploration playback of PeriTune's **Glistening Ripples** after the final Menelaus debrief. Before story completion, the ambient playlist contains only ordinary exploration music.

## Compatibility and bug fixes

- Support first installation into an existing campaign by generating Troy and the Odyssey region once. Campaigns that already contain Chief Navigator state retain their existing worlds and progression.
- Improve resized-map placement and guidance using the campaign's dimensions and saved Odyssey Expanse location.
- Add an explicit recovery command for clean campaigns in which the expedition's worlds were never installed. It refuses partial worlds and campaigns that have already committed departure or Labor progress.
- Prevent native random missions and contact Derelict bounties from selecting the authored Odyssey region.
- Prevent internal Ithaca weapons from appearing in Nexerelin mining caches.
- Keep Ithaca's temporary cargo-market object valid during native UI dismissal, fixing a crash path reproduced with Nexerelin and BMO.
- Supply the faction and person context expected by native reputation rules in custom contact interactions.
- Hide the Hegemony Expeditionary Fleet from faction Intel. Its occupied forward base denies entry; the abandoned-base interaction remains available.
- Preserve historical system, ship, and quest identifiers needed by existing campaigns.

## Build and package updates

- Reduce the installable download by excluding inactive music and obsolete artwork while retaining source material and credits in the repository.
- Keep legacy hull definitions registered when their unused textures are retired. Those obsolete hulls use a transparent compatibility sprite; current encounters use the intact artwork.
- Build development JARs in a separate staging directory and install into an available slot, avoiding overwriting an archive held open by Starsector.
- Enforce Java 17-compatible class files, update AI configuration construction for the installed 0.98a API, and validate dialogue through Starsector's native CSV loader.
- Extend the developer commands for world-installation recovery, contact-bounty diagnostics, and seven-component combat testing.

## Updating an existing campaign

Replace the old mod folder with the 0.6.0 release package, install the required dependencies, and restart Starsector.

Existing quest progress is retained. The system-name update changes labels in place. Existing Sinni officers retain their saved progression, and already-shown one-time conversations and endings are not replayed. Previously modded saves with missing static world content are not rebuilt automatically.
