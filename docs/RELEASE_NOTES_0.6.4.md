# Chief Navigator 0.6.4

November 18, 2026. Starsector 0.98.5a-RC7.

- Reduce repeated campaign work. Research tracking, objective markers, fleet identity and legacy repairs now reconcile on relevant events. Population replacements use saved deadlines and cached slot checks; system references refresh on load and relevant world/location changes.
- Check Sinni's exploration vignettes once per second. Cache the stellar-category census and navigation targets, and inspect hostile fleets only when a scene is eligible. Opening relevant guidance still refreshes the information the player needs.
- Make One More Horizon a checklist of completed and remaining stellar categories. Remove named destination recommendations and quest map targets so the player chooses where to explore.
- Handle overlapping categories: a blue giant in nebula clouds can qualify for both Giant/Massive and Nebula. Choose an unfinished matching category and record only that category when its scene finishes. Mixed systems also consider companion stars. Preserve Trinary's first choice, authored story-system exclusions, existing progress, category rewards and the delayed finale.
- Keep all seven research components in cargo until the intended installation dialogue consumes them. Native mission-item handling prevents selling, storing or jettisoning them.
- Add `restricted` and `no_sell` to Tetsu Kannon, retaining its existing acquisition exclusions and test mission. Its builder preserves the same tags.

## Updating

Replace the previous Chief Navigator folder with this package and fully restart Starsector. Existing campaign progress is retained; a new save is not required. Requirements remain GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer. LunaLib, Peerless Skills and Hall of Triumph remain optional.

## Verification and limitations

The full Java 17 build, 909 native dialogue-rule checks and 28 targeted campaign/regression checks pass against this candidate. The user reports a completed local campaign run. Automated checks establish reduced repeated work; no measured FPS improvement or fix for the original player's worsening stutter is claimed. A comparison on that affected save is still needed.

One pre-combat Budai Disengage hang was reported. Windows recorded an application hang; repeating the action after restarting worked. Its cause remains undiagnosed, and this release includes no claimed fix for it. The survey-ship repeat-fight report (CN-033), mirrored Onslaught naval-laser report (CN-034), and Isa conversation placeholder remain unresolved. Sinni's accelerated-start rewrite is not included.

The release preserves all other runtime entries from 0.6.3, including the Domaindessy redesign and prior ship/combat fixes.
