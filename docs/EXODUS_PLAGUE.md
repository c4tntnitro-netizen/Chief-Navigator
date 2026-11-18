# Exodus Plague

**Exodus Plague** (`chief_navigator_exodus_plague`) is the only attack of the
special Assault Units fabricated by the Tetsu Kannon prototype (legacy Threatxus ID). Its awakened core
uses **Fabricate Threat Units**: it prioritizes an Assault Unit when none are
alive or building, otherwise builds up to three stock Fabricators before
filling the remaining Assault Unit slots (three maximum). Gautama's original, final and
reincarnating configurations retain native **Extraction Protocol**.

The old Defabricator Stream weapon, its fitted pair and the added WS036/037
launcher mounts are removed. That old stream mission remains removed; the
current **Tetsu Kannon Test** mission pits the player against its breakaway shell and awakened core.

| Setting | Value |
| --- | --- |
| Windup / active / cooldown | 1 / 5 / 20 seconds |
| Streams | Two emitters at the native Assault Unit's forward ports, local `[50,+21]` and `[50,-21]` |
| Packet emission | 200 per emitter per full activation; average 40/sec, peak 50/sec |
| Forward speed / range | 900 / 7,500 units |
| Stream envelope | Full core 220 units widening to 286 per emitter; narrowed leading/trailing tips |
| Packet impact / durability | 50 kinetic damage / 60 HP |
| Conversion | Every third emitted packet is eligible on hostile hull or shield contact |
| Defabricate cap | Ten active per emitter, twenty across the system |
| Defabricate lifetime | 20 unpaused seconds after release |
| Flux cost / ammunition | Zero firing flux; no fragment supply or consumable item required |

The first and last second smoothly ramp packet density and width, making the
moving attack build into its core and thin back to a narrow tail. Each emitter
uses its own jittered schedule; both still emit 200 packets per complete attack.
The three-second middle emits 50/sec per side to preserve the former total.
Lateral placement favors the core and thins toward the sides. Tip narrowing
applies to actual launch lanes, weaving and native fragment clouds in flight.
Packets retain randomized launch spacing and smooth, independent weaving.
They remain real missiles that PD can intercept. Intercepted eligible packets
release nothing; conversion is not transferred to the next packet. Each
emitter keeps its shot sequence and active cap across activations. Destroyed
or expired swarms free cap slots, and hits at the cap do not queue releases.
Native Attack Swarm AI and fragment transfer continue after conversion.

Impact handoff corrects native wing deployment's two seconds of fighter
pre-advancement: the newborn body is restored to the impact origin and arrival
heading, with bounded incoming momentum and a fresh native AI decision.
Transferred fragments keep their identities, world positions and velocities.
Nearby releases from each emitter join native formations of up to four rather
than each becoming a mutually repelling wing leader. Expiring one swarm removes
only that member; the last member retires its wing. This prevents the displaced
gather-then-ring presentation without changing the twenty-swarm system cap.
Conversion occurs on successive eligible impacts, never in an end-of-attack
queue; long-range packets still have their normal travel time.

Player control aims at the mouse pointer. The preserved custom system AI activates
against a visible enemy within range. Pause, phase, death, construction and
overload/venting block emission. The system applies no old Extraction Protocol
backoff, speed boost, forced vent or reload effects.

An internal, SYSTEM-hidden packet helper supplies the native RC7 missile and
initialized damage. It is not a selectable or fitted weapon. All acquisition
exclusions remain, and no faction imports it. The system explicitly supplies
the missile's range, weapon metadata and native missile damage modifiers when
spawning without a fitted gun. This avoids both the RC7 raw-spec damage issue
and a null weapon in projectile-range calculation. Production uses public APIs.

Restart Starsector and open **Missions → Tetsu Kannon Test** to fight the enemy
fabricator and its Exodus Assault Units. The units have no ordinary weapon
mounts; their custom navigation approaches enemies and activates this system.
Native construction gates the attack until assembly is complete, including
the game's deployment warmup. Existing saved ship system instances are not
migrated.

Verification: Java 17 compilation; actual native fragment cloud transfer and
cleanup; tapered density and physical width; independent seeded timing;
10/60/120-Hz system firing with the full 400-packet budget; every-third sequence and independent caps
across activations; randomized trajectory bounds; target/AI/pause/state guards;
native RC7 system JSON/CSV, helper missile damage and clone, hidden catalog,
tooltip and restored three-skin Extraction Protocol inheritance. Existing Gautama carrier/reincarnation
screen, original gun fits, hardpoints and engines, Wall/Ithaca weapons and RC7
naval pulse loading/damage checks pass. The scoped Gautama weapon contract
passes; its previously documented unrelated full-dialogue assertion remains
outside this weapon/system change. Live visual review remains pending restart.
The handoff regression reproduces native pre-deployment displacement and fails
against the original build; it verifies corrected birth locations, preserved
fragment motion, progressive impacts during firing, native formation grouping,
individual expiry and unchanged per-emitter limits.
