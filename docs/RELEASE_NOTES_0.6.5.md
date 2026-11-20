# Chief Navigator 0.6.5

November 19, 2026. Starsector 0.98.5a-RC7.

A small campaign performance update on top of 0.6.4.

- After Ungaikyo is defeated, the cleanup that removes leftover rematch fleets and markers now stops once a pass finds nothing left. In 0.6.3 this sweep searched the whole Sector several times a second for the rest of the campaign; 0.6.4 ran it only on fleet and location events. It now runs once per loaded save and keeps retrying only while a busy fleet blocks it.
- FOB Ithaca's Spartan guard upkeep (refilling, repairing and readying the guard fleets) now runs once per campaign tick instead of once for every Starving Threat patrol in Lacedaemon.

Gameplay, story, data, art and audio are unchanged from 0.6.4. The unreleased nebula-painting vignette is not included.

## Updating

Replace the previous Chief Navigator folder with this package and fully restart Starsector. Existing campaign progress is retained; a new save is not required. Requirements remain GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer. LunaLib, Peerless Skills and Hall of Triumph remain optional.

## Verification and limitations

Measured on the author's machine with Java Flight Recorder on a save just after the Ungaikyo kill: Chief Navigator used about 33% of campaign game-thread CPU on 0.6.3, 4-6% on 0.6.4, and about 3.4% on this build, with garbage-collection pauses under 1 ms. These numbers come from one machine and one save. The original player's stutter report remains unconfirmed and this release does not claim to fix it.

The candidate runtime is the exact build tested in game. All 29 targeted campaign checks pass, including a new regression for both changes. The survey-ship repeat-fight report (CN-033), the left-behind research component question (CN-021) and the pre-combat Budai Disengage hang remain open.
