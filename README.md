# Chief Navigator

**Version 0.5.0 — Starsector 0.98a.** Download the installable ZIP from
[GitHub Releases](https://github.com/c4tntnitro-netizen/Chief-Navigator/releases/latest).

After completing Labor V's final debrief, **Hunt Budai** and **Hunt Ungaikyo** begin as separate missions. Speak to **Captain Aias Kleon** through FOB Ithaca's comm directory for each location: Avici for Budai, and the derelict Guardian in Last Light for Ungaikyo. Each briefing updates its own quest and map marker; each permanent boss defeat completes only that hunt. Already defeated targets are skipped. The missions add no fleets or rewards and leave existing encounter gates, rosters and the ending cinematic unchanged.

Chief Navigator is a standalone Starsector quest mod centered on **Adept of the Stars and Waves**, a quest about trusting a master smuggler-navigator named Sinni with an expedition fleet and following her through a once-in-a-lifetime route across the Abyss.

Sinni reconstructs that route from the surviving account of her ancestor Helen, a Task Force Spartan servicewoman who left FOB Ithaca with the withdrawing Fourteenth Battlegroup. Helen recorded the journey from Ithaca to the Persean Sector; Sinni leads the player back along it in reverse.

The route begins at a transient departure point called **Waypoint Troy** and ends at **FOB Ithaca**, a planet-scale Domain military macrobase associated with the Fourteenth Battlegroup's arrival in the Persean Sector. The crossing is meant to be a long attritional test of navigation, supplies, and fleet preparation rather than a normal point-to-point trip.

At Ithaca, the apparent derelict field awakens into an Explorarium defense reserve fighting a new THREAT incursion. These deliberately limited, post-THREAT Domain drones cannot win unaided. The player's climactic objective is not merely to destroy the enemy, but to make the derelicts win and preserve as much of Ithaca's irreplaceable reserve as possible.

The inherited concept and its decision boundaries are recorded in [docs/CONCEPT.md](docs/CONCEPT.md).

## Included content

- Starsector `0.98a` metadata
- Java mod plugin and priority bar quest introducing Sinni
- Sinni's proposal is an always-shown native `PortsideBarEvent` at every market bar. Its creator is registered and repaired with the same lifecycle used by Hall of Triumph, while all dialogue remains authored in `rules.csv`; accepting adds her directly to the player's officer roster, creates a permanent callable Contacts-menu entry, and marks a private tea meeting at Eventide. Her contact briefing names the live quest objective and system and supplies a compass course from the player's current location. After the expedition arrives in the Orion Knot, her contact can launch a full-size custom chart menu of its six systems using the same dialog pattern as Hall of Triumph's Ship Gallery, without creating or selecting another Intel entry. The Eventide office briefing reveals the expedition details and marks Waypoint Troy only after the meeting is complete
- A bounds-independent Orion Knot chart launched from Sinni's contact after arrival, drawn from the six authored systems' relative campaign coordinates and showing the player's position while inside the Knot
- Required Combat Chatter integration with a unique Sinni voice set and distinct opening alerts for Scylla Strike, Siren Strike, Cyclops Strike, Heavenly Strike, Budai, Ungaikyo, and the Hegemony/Persean League Expeditionary Fleets. A successful ordinary Gautama reincarnation flashes `WARNING: Reincarnation`, then `Retreat Advised.` on a separate line. In the final Labor fight, his first successful reincarnation instead flashes `WARNING: Multiple Reincarnations Detected` once per battle.
- Recruiting Sinni extends the vanilla Hyperspace Topography tree with tiers at 1,050, 1,200, 1,350, 1,550, and 1,750 points: Generate Slipsurge becomes usable at any gravity well; Ambush Stance can force pursuit battles when attacking from sensor-obscuring terrain; Slipsurge Mastery makes generated currents twice as wide and three times as long; Storm Rider halves hyperspace-storm CR damage; and Echo Mapping drops a temporary Remnant-style warning beacon that refires Active Sensor Burst around its fixed location after 3, 6, and 9 days, revealing local contacts and drawing nearby hostiles to the beacon
- Persistent Waypoint Troy system at the southeast edge of the Sector
- Troy expedition anchorage and a marketless colony-style FOB Ithaca station with fleet, cargo, refit, Starsector's native Comm Directory, free persistent storage, and free full-fleet repairs, plus a hard 30-DP limit in both directions through Troy Terminus
- Recoverable, one-time Rogue Combat Guard flotillas confined to the Odyssey Expanse, Ashen Verge, and Last Light as the expedition's local source of replacement hulls
- At most two neutral Persean exploration patrols in comparatively safe Sanzu, joined by at most two battered Hegemony exile patrols only after the main expedition arrives; the exiles periodically choose between aggression and flight
- Six one-use derelict research stations spread across the Odyssey Expanse, Sanzu, and Ashen Verge
- Five separate, sequential Menelaus Labor Intel entries: Guard Motherships, delivery and installation of any four distinct Ithaca defense components, Drifting Wall, a stealth sensor-package deployment in Devoured Reach, and the four-wave defense of Ithaca. All six research stations and components remain individually tracked and recoverable, with the final two optional. Accepting Labor I grants a Domain-Security IFF Transponder that makes recovered Domain derelict drones usable without Automated Ships while leaving recovery access unchanged. Sinni's live guidance mentions the optional TTS Penumbra hunt before recovery and, afterward, recommends the phase ship for the Mara survey-communications deployment without replacing Mara as the primary objective. Each completed required entry closes as the next opens.
- Six recoverable frigate-to-cruiser wrecks caught in the planetary wakes of Sanzu
- Every Sanzu battle uses softly rendered natural nebula terrain plus a sparse layer of large irregular cloud banks, drawn from an exact-alpha baked blue color grade of vanilla's atlas
- Troy Terminus wormhole, opened by Sinni after the anchorage manifest check
- A reserved six-system Odyssey Sector hole in remote Abyssal hyperspace, far southeast and beyond the vanilla campaign-map rectangle: the Odyssey Expanse, Avici, Sanzu, Ashen Verge, Last Light, and the starless Devoured Reach
- Sixteen permanently visible, map-marked normal jump points: three for every Odyssey system except Devoured Reach, whose sole threshold is kept clear of Starving Threat feeding masses; Avici's trio sits wholly inside the pocket's clear terrain and its primary jump point retains its larger size
- The Troy route's Alpha Odyssey endpoint is the single nonstandard entrance: **Foxtrot Terminus Aperture**
- Permanent hyperspace system anchors for all six Odyssey systems, plus permanently rendered hollow worlds in Devoured Reach
- A moving clear pocket around Avici's orbital habitat that removes both the black fog and its Abyssal campaign penalties throughout the approach
- A clutter-free local route from Avici's existing jump point to Sanzu, then from Sanzu's single confluence to Ashen Verge; these passages add no hyperspace-map jump points
- Early drifting-wall encounter built as one continuous station frame with seven independently breakable armored inserts; one surviving insert carries a mega-turret and destroying all seven defeats the fragment
- Three persistent FOB Ithaca bastion stations: independently reinforcing, fully armed seven-emplacement gun lines with teal Explorarium drone screens, exposed cyan control cores, and an 8,000-unit connected rear bulkhead that physically prevents rear flanking
- Ithaca's bastions gradually repair surviving hulls and modules, or rebuild destroyed stations after 30 campaign days. Repairs pause during Labor V and remain blocked after final failure. The Labor V opening fully restores all three before spawning the perimeter Strikes, and missing fleets that never deployed cannot count as victories.
- Shared Wall/Ithaca Hammer batteries fire ten torpedoes with twenty-second refire (eight with the Autoloader), 10,000 range and a tight 10-degree spread. The Atropos upgrade retains the same launch pattern. Ithaca's intact modules have four fixed Cloudswarm arrays (slots 1, 3, 4 and 6), with the right pair mirroring the left; slots 2 and 5 are removed. Other missile/naval nerfs and earned research upgrades remain unchanged.
- Starving THREAT fleets confined to their authored realspace battlefields, including Ithaca's attackers and the 24-fleet feeding mass in Devoured Reach: depleted hulls trade 33% hull for 15% armor, gain 5x maneuverability, and release attack swarms on death. Starving Hive Units retain the exact native twelve-weapon fit, geometry, Fragment Volley, fragment reserve/regeneration bonuses and timid support AI, while keeping the shared Starving depletion/death traits. Detached Attack Swarms fight normally while a real allied hull is deployed, then clear once they are the side's only active units so they cannot hold real reinforcements off the field. In ordinary battles, each such hull death may construct Gautama once if he has not already appeared: 10% for a frigate, 20% for a destroyer, 30% for a cruiser, and 50% for a Fabricator. Labor V's perimeter battles and the permanent Ungaikyo encounter suppress this roll; its center battle instead permits repeated Gautama constructions, adding ten percentage points per prior failed eligible death and resetting the pity count after each success. In Ashen Verge, an additional First or Second Strike materializes 11,000–15,000 units from Ithaca every 25–35 days and uses Nexerelin's hidden-target station-assault queue to drive directly at one of its three bastions.
- Starving Fabricators construct only Starving variants and spend half the normal amount of their own CR per construction. Gautama keeps Energy Lash and never equips the Fabricator ship system. The separate post-Labor Ungaikyo encounter uses a dark stock-derived Fabricator and presents THREAT construction effects around the fleet reflections it creates.
- Gautama is scaled to 150%, continuously replenishes a screen of up to twenty extremely fragile Attack Swarms, and appears in the unique **Heavenly Strike** fleet. Their five-second carrier refit keeps the screen renewable, while 25% hull, 150% hull/armor damage taken, and suppressed ship-level death explosions make individual swarms disposable without reviving the old explosion pile-up. Labor V removes Gautama from the Devoured Ring and commits him only after all three Ithaca perimeter invasion fleets are destroyed. That battle destroys Gautama and exposes Ungaikyo as the larger filament organism behind it.
- After Labor IV is reported, Menelaus warns the player about Labor V but leaves it dormant until the red **Accept the final Labor** option is selected. Acceptance creates its Intel entry and opens the Labor with **Scylla Strike**, **Cyclops Strike**, and **Siren Strike** taking up siege positions outside Ithaca's north, east, and south bastions. The instrumental version of **My Gospel is Gunpowder** loops on the campaign map through the perimeter phase and approach to the gate; all three Strike battles instead loop **Abyssal Rhapsody** by Lappy. Each lane has a 60-day campaign clock: if its Strike survives, its bastion is destroyed and that formation attacks its assigned Task Force Spartan fleet. TFS casualties and destroyed formations remain lost for the rest of the Labor. Gautama is prohibited from all three perimeter rosters. His Heavenly Strike does not spawn until all three perimeter Strike fleets are actually destroyed; it enters due west to attack the persistent Gate Defense Station in Ithaca's breached ring, while every surviving TFS formation teleports into a defensive arc around the station. Its supporting force is tripled to thirty-three hulls and enters as three staged, mixed waves, each combining one Fabricator, one Hive, and nine assault, line, overseer, and skirmish escorts. The center engagement adds two zero-DP low-tech reserve battlestations, giving the defenders three colossal naval lasers mounted directly over the intact middle foundations; all three use a material-selective oxidized-aqua recolor across their parent and modules and inherit Ithaca's recovered component upgrades. The nearby north, east, and south campaign ring sections are explicitly excluded, so no extra Wall module enters the Gautama fight. Only that final battle switches to the vocal version, with state-aware music keepers repairing transition-time audio requests. After Gautama dies, eligible Starving Threat losses can reconstruct him repeatedly: the normal 10/20/30/50% hull-size chance gains ten percentage points for every prior failed roll, caps at certainty, and resets on success. Defeating the Heavenly Strike ends the invasion controller but leaves Labor V open as **Return to Menelaus** at FOB Ithaca. Finishing his debrief completes the quest and, after its interaction closes, opens the authored branching Sinni epilogue as a separate one-time 480×300 illustrated scene using `graphics/illustrations/chief_navigator_sinni_ending_v4.png`.
- The `ForceFinalLabor` console command is a test shortcut that exposes Labor V without deleting or falsely completing earlier Labor content, restores its three bastions and full TFS opening rosters, resets only the four-wave encounter, and immediately materializes the three opening Third Strikes.
- Each of the six derelict research stations drops one unique Ithaca defense component—and no AI cores: Colossal Traverse Macroservos, Naval Cycling Regulator, Caged-Arc Projector, Interception Command Matrix, Siege Munitions Compiler, or Fortress Autoloader Core. Components remain inert cargo until transferred to Menelaus at FOB Ithaca; installation consumes the item and permanently activates only its matching upgrade. Labor II requires delivery and installation of any four distinct components. Menelaus's contact menu continues to track all six installed, recovered, and missing parts and names the exact system and planet orbited by every unrecovered component's station; the final two remain optional and recoverable. The missile batteries carry Hammer torpedoes by default, while the compiler converts them to guided Atropos torpedoes. If Isa has ever joined the officer roster, her permanent naval package adds 50% shield damage and one half-strength, non-propagating refraction beam on enemy-shield contact. The `MaxUpgradeWallModules` console command enables all six research upgrades without removing research stations or advancing Labor II.
- Completing the final Labor releases FOB Ithaca's gate for normal scanning and reveals that Gautama was only a local coalescence of Ungaikyo, a nanometer-scale black filament web spanning light-years through Devoured Reach. Ungaikyo gathers in Last Light around the derelict Guardian as a unique dark Fabricator with only five initial escorts: one Hive, Overseer, Line, Assault, and Skirmish unit, with no escort Fabricators. The wreck cannot be recovered until Ungaikyo is permanently defeated. At battle start it summons military ships from the player's campaign roster up to a separate 240-DP cap; if those military selections total less than 200 DP, civilian slots are replaced by same-size stock THREAT units until the summon reaches 200 DP or exhausts the roster. These combat-only reflections cost zero DP and do not reduce the escort's deployment allowance. Destroyed reflections are reconstructed one at a time with the THREAT construction animation, remain unrecoverable, and collapse with Ungaikyo. A third-party ship failure quarantines only that source. Gautama reconstruction is suppressed, and Ungaikyo's defeat permanently retires the encounter and unlocks the Guardian's normal recovery interaction. Completion also grows ordinary authored Starving Threat fleets to a stable First, Second, or Third Strike roster; Ungaikyo is exempt. Losing the center battle instead begins renewable Starving Threat incursions from every gate in the Sector, including Core World gates.
- Budai is an enlarged, blue-shifted Shrouded Maw that acts as a persistent vengeance hunter inside Avici. His controller receives the player's exact live position without using campaign sensors while both fleets are there. He has 1.4x vanilla Maw hull (30% less than his previous hull), half armor, widened overdriven Pseudoparticle Jets, Convulsive Lunge, and oversized Hungering Rifts with 1.5x missile durability. He uses Fearless AI, refuses retreat orders and fleet withdrawal, holds his ground even while venting, and uses ordinary offensive lunges instead of backward escape jumps. He has no passive HP regeneration. Strictly below 50% HP his music switches irreversibly from Heart of Corruption to The Savor of Tomorrow, even if his hull later rises above half. At or below 50% hull he gives a harsh screech and makes four lunges in random directions and over random distances, each honoring Budai's twelve-second cooldown and native activation readiness. Heavy native smoke pours out throughout that phase; after the fourth lunge, the smoke eases back to normal while his distortion radius grows from 1,000 to 4,000 over five combat seconds. His Hungering Rift fire rate doubles after the fourth lunge. No leap clears or shortens its live remaining cooldown, including after the fourth. After the player defeats Budai, he disgorges a consumed Onslaught Mk.I in a one-time salvage scene. The wreck is cut apart rather than recovered, yielding two mountable clones of the vanilla Heavy Adjudicator with Isa's help, or one without her. They keep the stock weapon's art, projectile, sound, firing behavior, ammo, and combat statistics; only the built-in-only restriction is removed, and their 500,000-credit base value makes them the most expensive ballistic weapons in the game. Isa helps with the extraction if she has joined the fleet. Budai is permanently defeated and never respawns. He is always authored as a single lone boss, with no post-Labor escort escalation.
- The Devoured Ring uses a custom eaten-away gateway sprite but is never scannable or part of the Janus network. Inspecting it instead recovers navigation information pointing from the Orion Knot toward the Domain heartland
- Persistent, plainly detectable Task Force Spartan fleet in the Odyssey Expanse: one XIV Onslaught and two XIV Dominators, continuously restored outside battle, followed after 60 seconds by a leading Eagle XIV and two flanking Lashers entering through the combat map's bottom border
- Menelaus and FOB Ithaca's three Spartan guard fleets belong to a hidden Task Force Spartan faction with fixed cooperative standing toward the player and fixed hostile standing toward both Starving Threat factions
- Shelved Time Omega prototype retained in code and assets but no longer spawned; existing owned prototype fleets are retired safely
- One Damocles mortar and one Metal Storm VLS integrated into the naval-gun wall line
- Local PowerShell build script using Starsector's bundled JDK and API jars
- Design notes preserving the prior concept discussion

## Installation

Download `Chief-Navigator-0.5.0.zip` from
[the 0.5.0 release](https://github.com/c4tntnitro-netizen/Chief-Navigator/releases/tag/v0.5.0)
and extract its `Chief Navigator` folder into `Starsector/mods`.
The mod folder must contain `mod_info.json` directly. When updating, replace
the previous mod folder with the new one rather than merging its files.
Install GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer, then
enable Chief Navigator and those dependencies in the Starsector launcher.
The included `jars/ChiefNavigator.jar` is already compiled for Starsector 0.98a.

## Minimal quest test

Meet Sinni at any market bar and accept her proposal. She immediately joins as
an officer and invites you to her office on Eventide. Complete the tea briefing
there to reveal Waypoint Troy, then enter hyperspace and travel normally to its
location at approximately `(+59,000,
-45,000)`. Dock at the Troy Expedition Anchorage, store ships and cargo as
needed, and reduce the active fleet to no more than 30 deployment points.
Choose **Authorize Sinni to open the route**, then fly to the newly opened
**Troy Terminus** and cross it. The manifest is checked again at the wormhole.
It leads directly to the generated Odyssey Expanse nebula inside a reserved
six-system Odyssey Sector hole far southeast of the ordinary Sector in remote Abyssal hyperspace. The drifting wall and Task Force Spartan
are separate encounters in the entry nebula. Avici is a nearby black-hole
system. Before Labor V, Gautama exists only in the **Heavenly Strike** fleet at
the Devoured Ring, surrounded by hollow planets and a renewable mass of
Starving Threat. After Labor V, Ungaikyo's one-time encounter moves to Last
Light and coils around the locked Guardian wreck until the player defeats it permanently.
Budai's Convulsive Lunge uses the regular Maw's full 5.5-second activation cycle
(2-second charge-up, 3-second active phase, 0.5-second wind-down), not the former
shortened 2.1-second cycle. Each leap is followed by a twelve-second cooldown, with no scripted resets.

Budai's bubble reduces eligible outside-origin damage by 30%. Heavy smoke is
limited to his half-hull lunge phase and the five-second transition back to normal
emission as his bubble expands. The bubble has no red tint.

Sanzu's nebular haze halves every fleet's sensor range throughout the system,
including sheltered planetary wakes and space beyond the visible river.

Budai's encounter has no comm link, transponder demands, or human-faction
reputation disclaimer. Its native battle preview identifies Budai rather than
a generated officer; normal ambush, battle, retreat, and salvage choices remain.
This presentation applies only to the owned, undefeated Budai encounter.

Budai first spawns only after Sinni's Avici arrival vignette is completed,
100,000 units from the system center, without escorts before or after Labor V.
Avici has no Starving Threat patrol spawns or gate incursions; existing owned
local Starving fleets retire once clear of combat, transit, and interactions.
Budai hunts only inside Avici and returns
to his Kshaya-area lair when the player leaves; he never follows through jump
points. Ashen Verge holds FOB Ithaca, and Last
Light is the sparse white-dwarf system containing the Guardian prize and Ungaikyo's final patrol.

## Build

Gautama's three versions share the bounds, four engines, and nine main gun
positions from the user-authored `graphics/ships/odyssey_bosses/gautama.ship`.
Gun-compatible slot sizes and firing arcs retain the existing thirteen-weapon
loadout; the four additional fragment hardpoints remain separate.
All four engines use vanilla THREAT style for white flares, without changing
the editor export's engine geometry.
All three versions have 40,000 base hull. Reincarnation keeps its 35-second
construction sequence, with effective armor building linearly from 0% to 100%.

For hands-on instructions for positioning, scaling, rotating, replacing, and
rebuilding the complete FOB Ithaca campaign sprite, see the
[FOB Ithaca campaign-sprite placement guide](docs/FOB_ITHACA_PLACEMENT.md).

Chief Navigator requires GraphicsLib `1.10.2` or newer for the naval shell's
impact distortion. The local build expects GraphicsLib to be installed in the
adjacent `mods/GraphicsLib` folder.

From PowerShell in this directory:

```powershell
.\build.ps1
```

The development build selects an available `ChiefNavigator.next.jar` or
`ChiefNavigator.next2.jar` and updates `mod_info.json` to use it. To build the
stable jar included in GitHub downloads, close Starsector and run:

```powershell
.\build.ps1 -Release
```

The release build produces `jars/ChiefNavigator.jar`. Both modes validate the
dialogue CSV with the installed game's native loader before compiling.

After committing the release build and metadata, run `python tools/package_release.py`
to create the installable ZIP and SHA-256 checksum in `dist/`. Packaging includes
all runtime data, referenced graphics, registered audio, credits and license notices;
art masters, source recordings, tools and generated build files remain in the repository.

Ungaikyo keeps Fragment Swarm's mechanics but uses vanilla Threat classification
to omit the blotchy player-retrofit overlay from its dark hull sprite.

Ungaikyo's Reflection Swarm retains its automatic opening construction and
resurrections while adding an autonomous backward Extraction Protocol. It
activates under high flux, nearby enemy pressure, or missile/collision danger,
uses the stock five-second extraction with a twelve-second cooldown, and
requires no Overseer. Construction continues throughout the maneuver.

Its battle opens with Lappy's **Lord of Ashes**, then switches once
to **The Savor of Tomorrow** when the Ungaikyo Fabricator falls strictly below
50% hull. Both cues loop; reflection deaths do not trigger the transition.
Ungaikyo uses Cautious combat AI. Generic mirror AI receives Fearless settings
in place, retaining its existing ship/system context; required custom AI,
including fighter AI, keeps its implementation, configuration and personality.
Every new or rebuilt mirror receives a native Search and Destroy order, with no
forced movement or fleet-wide Full Assault.

Each reflection generation tracks its own modules, detached armor plates,
fighters, drones and explicitly related transformations. Cleanup retains those
references and retries failed removals, including after Ungaikyo dies; a source
cannot rebuild until its previous generation is gone. Nonfatal construction or
AI-handoff errors quarantine only the affected source. This is not a sandbox
for arbitrary third-party combat callbacks or entities without public lineage.

For known incompatible ships, add hull IDs to
`chiefNavigatorMirrorExcludedHullIds` in `data/config/settings.json`, or add
the `chief_navigator_no_mirror` tag to a hull or variant. Base-hull exclusions
cover skins and D-hulls; an excluded nested module excludes the whole parent.
Excluded sources consume no summon budget and get no substitute reflection.

## License

Chief Navigator's original code, dialogue, documentation, gameplay additions
and original assets are available under the [MIT License](LICENSE), copyright
2026 c4tntnitro-netizen. Third-party music, Starsector-derived assets and other
third-party material retain their owners' terms and are excluded from that
grant. See [Third-party materials](THIRD_PARTY_NOTICES.md) for the scope and
recorded terms.

## Music credits

Every bundled music file is credited below, including inactive and source
versions. Paths in this table are relative to `sounds/music/`.

| Music and artist credit | Bundled files | Use and source |
| --- | --- | --- |
| **Rising Up — Prod. Ryini Beats** | `chief_navigator_alpha_odyssey_exploration.ogg`; `source/chief_navigator_alpha_odyssey_exploration_no_leadin.ogg` | Alpha Odyssey exploration. [Recording](https://www.youtube.com/watch?v=ZqttIDUBYlg). |
| **Heart of Corruption — Music by Lappy** | `chief_navigator_budai_battle.ogg` | Budai opening and ordinary Gautama battles. [Recording](https://www.youtube.com/watch?v=gBaNsd3Ycc8). |
| **Watcher of the Cycle -Scene02- (NoVox) — Music by zippy** | `chief_navigator_sanzu_exploration.ogg`; `chief_navigator_sanzu_exploration.mp3` | Sanzu exploration; official June 2026 members-only source download. [Recording](https://www.youtube.com/watch?v=R-4y_0oisEo). |
| **KARMA — Music by zippy** | `KARMA_1Loop.ogg`, `KARMA_Loop_A.ogg`, `KARMA_Loop_B.ogg`; corresponding `.mp3` files | Inactive former Avici playlist. Artist credit comes from the supplied file metadata. |
| **My Gospel is Gunpowder (instrumental) — Lappy and Autodidactic Studios, featuring The Epoch House Choir** | `chief_navigator_final_labor_mission.ogg` | Labor V campaign phase. [Instrumental recording](https://www.youtube.com/watch?v=skkZwYFoa-0). |
| **My Gospel is Gunpowder — Lappy and Autodidactic Studios, featuring Christina R and The Epoch House Choir** | `chief_navigator_final_labor_fight.ogg` | Labor V center battle. [Vocal recording and license](https://www.newgrounds.com/audio/listen/1497762). |
| **Abyssal Rhapsody — Music by Lappy** | `chief_navigator_final_labor_perimeter.ogg` | Scylla, Siren and Cyclops Strike battles. [Recording](https://www.youtube.com/watch?v=gW2_9Vz_aFI). |
| **Lord of Ashes — Music by Lappy** | `chief_navigator_ungaikyo_opening.ogg` | Ungaikyo opening. [Selected recording](https://www.youtube.com/watch?v=4nrI88fmc-I). |
| **The Savor of Tomorrow — Music by Lappy** | `chief_navigator_ungaikyo_enraged.ogg` | Budai and Ungaikyo below-half-hull cue. [Recording](https://www.youtube.com/watch?v=vLWEkvpPBDY). |
| **Glistening Ripples — Music by PeriTune (Sei Mutsuki)** | `chief_navigator_sinni_ending.ogg`; `source/Peritune_Glistening_Ripples.zip` | Labor V ending after ten seconds of silence. [Track and official download](https://peritune.com/blog/2026/03/21/glistening-ripples/); [selected recording](https://www.youtube.com/watch?v=DUWzOMtUoIE). |
| **Vanilla Shrouded Dweller encounter cue — Starsector / Fractal Softworks and its music creators** | No bundled copy; native `music_dweller_encounter_hostile` cue | Avici exploration. Supplied by the installed [Starsector game](https://fractalsoftworks.com/). |

The music recordings are excluded from the MIT license. Keep these artist
credits, source links and their applicable terms. See
[Third-party materials](THIRD_PARTY_NOTICES.md#music) and
[Audio sources](sounds/chief_navigator/AUDIO_SOURCES.md) for source and license
records.

Alpha Odyssey's exploration theme is **Rising Up**, produced by **Ryini
Beats**. It is used under the artist's stated terms permitting free
noncommercial use with credit.

- [Rising Up — Ryini Beats](https://www.youtube.com/watch?v=ZqttIDUBYlg)

Budai's opening battle theme is **Heart of Corruption** by **Lappy**, also used
for Gautama in ordinary battles. Labor V's authored soundtrack takes precedence.

- [Heart of Corruption — Lappy](https://www.youtube.com/watch?v=gBaNsd3Ycc8)

Avici's exploration theme uses Starsector's vanilla **Shrouded Dweller
encounter music**. It loops in the system and resumes after combat; Budai's
battle themes remain unchanged. The former KARMA tracks by **zippy** are
retained as inactive source assets.

Sanzu's exploration theme is **Watcher of the Cycle -Scene02- (NoVox)** by
**zippy**. The official short-form track loops while the player remains in
Sanzu and resumes after combat.

All runtime music is stored as Ogg Vorbis, the streaming format supported by
Starsector. The original supplied MP3 files are retained only as source media.

- [Watcher of the Cycle -Scene02- — zippy](https://www.youtube.com/watch?v=R-4y_0oisEo)

Labor V's campaign phase and center breach use **My Gospel is Gunpowder** and
its instrumental version by Lappy
and Autodidactic Studios, featuring The Epoch House Choir and, on the vocal
version, Christina R. The vocal release is listed
under [CC BY-NC-ND 3.0](https://creativecommons.org/licenses/by-nc-nd/3.0/).
Both source recordings were converted to Ogg Vorbis without altering their compositions; the instrumental source is credited separately.

- [Instrumental source](https://www.youtube.com/watch?v=skkZwYFoa-0)
- [Vocal source and license record](https://www.newgrounds.com/audio/listen/1497762)

**Scylla Strike**, **Siren Strike**, and **Cyclops Strike** loop Lappy's
**Abyssal Rhapsody** during combat. Ungaikyo's opening theme is Lappy's
**Lord of Ashes**. These full recordings are converted to Ogg Vorbis without
altering their compositions; Chief Navigator does not claim ownership or a
blanket redistribution license.

- [Abyssal Rhapsody — Lappy](https://www.youtube.com/watch?v=gW2_9Vz_aFI)
- [Lord of Ashes — Lappy](https://www.youtube.com/watch?v=4nrI88fmc-I)

Budai and Ungaikyo's second battle cue is **The Savor of Tomorrow** by **Lappy**. Chief
Navigator does not claim ownership of the composition or recording.

- [The Savor of Tomorrow — Lappy](https://www.youtube.com/watch?v=vLWEkvpPBDY)

The Labor V ending scene cuts all music immediately when its illustration
opens. After ten real-time seconds of silence, it plays **Glistening Ripples**
by **PeriTune (Sei Mutsuki)**. It loops during
the scene without restarting on dialogue choices, then returns to campaign
music on closing. Closing before ten seconds cancels the cue. The unchanged
Ogg comes from the artist's official download, under PeriTune's March 2026
terms permitting noncommercial game bundling; it is not a standalone music
redistribution or a claim of ownership.

- [Glistening Ripples — PeriTune](https://peritune.com/blog/2026/03/21/glistening-ripples/)
- [Selected YouTube recording](https://www.youtube.com/watch?v=DUWzOMtUoIE)
- [PeriTune usage and bundling terms](https://peritune.com/about/)
