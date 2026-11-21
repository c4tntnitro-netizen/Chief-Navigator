# Chief Navigator 0.6.6

November 20, 2026. Starsector 0.98.5a-RC7.

- **One More Horizon reward:** in hyperspace, your fleet's burn can exceed the usual limit of 20, up to 30. The extra speed works like a gentle hyperspace storm push: once the fleet is at full speed and heading for its destination, it keeps counting the same burn bonuses normal travel uses (Sustained Burn, skills, Drive Field Stabilizers, Hyperfield Optimization), and clouds and deep hyperspace reduce it the same way. The +1 base Burn level stays. The old "+2 Burn speed cap" never changed your speed and has been removed. Campaigns that already finished One More Horizon get the new reward automatically.
- **Ithaca wall modules:** the three wall modules and the Breach Defense Station stay in place. They kept an old campaign velocity, so they crept about 40 units every background update and were snapped back, logging a warning about twice a second for the rest of the campaign. The warning now appears at most once per station per load.
- **Sanzu clouds:** the cloud renderer draws exactly the same image about twice as fast and stops allocating memory every frame. A new optional LunaLib setting, **Chief Navigator → Campaign → Sanzu clouds** (on by default), turns the cloud banks off; the glints and the vapor flowing off the planets stay.
- **Sinni's nebula vignette:** a new scene, ending with *Spitzer and Hubble Create Colorful Masterpiece* (NASA/JPL-Caltech; NASA/ESA, T. Megeath, M. Robberto), credited in the dialogue, the README and `THIRD_PARTY_NOTICES.md`. Both closing choices record the nebula and its −1% sensor-profile reward. Campaigns that already saw the nebula vignette keep their progress.
- **Console:** `CompleteOneMoreHorizon` finishes One More Horizon at once, for testing.

## Updating

Replace the previous Chief Navigator folder with this package and fully restart Starsector. Existing campaign progress is retained; a new save is not required. Requirements remain GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer. LunaLib, Peerless Skills and Hall of Triumph remain optional.

## Known issues

- With a destination set straight ahead, the burn push can hold a faster speed source (Generate Slipsurge, a slipstream or a storm strike) down to its own cruising speed while it is active.
- After a sharp slowdown inside that same push, the fleet can jump back up to its previous cruising speed instead of accelerating again.

## Verification and limitations

On the author's machine, VisualVM sampling over a minute in Sanzu put the cloud renderer at 11.5% of the game thread before this update and 5.8% after. Over a minute of hyperspace flight with the burn push active, the burn-cap code recorded no samples at all. Over a full Labor V run, the wall-module warning appeared three times right after loading and never again. These numbers come from one machine.

The runtime is the exact build tested in game. All 31 targeted campaign checks pass, including new regressions for the burn push and the wall modules, and an offscreen OpenGL test confirms the Sanzu renderer is pixel-identical to 0.6.5. The survey-ship repeat-fight report (CN-033), the left-behind research component question (CN-021) and the pre-combat Budai Disengage hang remain open.
