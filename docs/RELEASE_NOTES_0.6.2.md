# Chief Navigator 0.6.2

November 17, 2026. Starsector 0.98.5a-RC7.

- Adds the Onslaught (ITH), also called the Domainslaught, with two built-in naval lasers, charge-up pulses and Ithaca research support. After Labor V's final debrief, ask Aias Kleon about the Ithaca Onslaught prototype to claim it once.
- Updates Bouvo Olympus with a wider nebula background and black filaments around its sun. The experimental planet cocoons and connecting webs are not included.
- Includes the Ithaca Odyssey prototype with its supplied Domain livery, three fighter bays, Surveying Equipment and two naval lasers. It is available for console testing with `AddShip chief_navigator_odyssey_ith_Standard`; no campaign reward is added.
- Includes the standalone **Tetsu Kannon Test** mission: three breakaway Threat walls protect a dormant core that awakens and fabricates Exodus Assault Units and Fabricators. Includes the healthy-launcher smoke fix. This prototype is not assigned to a campaign encounter.
- Preserves the updated Ungaikyo encounter and current naval weapon fixes, including RC7-compatible charge-pulse damage initialization.
- Makes Peerless Skills optional for Chief Navigator. The final Labor reward remains available when it is enabled.

## Installation and requirements

Replace the previous Chief Navigator folder with this package and restart Starsector. Requires GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer. LunaLib, Peerless Skills and Hall of Triumph are optional.

Some Hall of Triumph installations call PeerlessSkillsCMD directly and fail at startup without Peerless Skills. That is a separate Hall of Triumph compatibility issue; this download does not modify another mod. Use a Hall of Triumph build with optional Peerless support, or enable Peerless Skills alongside that older build.

## Verification and known issue

The author completed the full local release playtest and authorized publication on November 17. The release preserves the tested runtime. Automated checks cover native CSV loading, naval charge pulses and damage initialization, Bouvo rendering, and the Tetsu Kannon/Exodus suite; the archive is verified file by file.

**Potential bug (CN-033):** survey ships may be fought twice. Reported during playtesting; cause and exact reproduction remain unconfirmed. This release does not claim to fix it.

Isa's Ithaca engineering conversation remains a placeholder and grants no new upgrade.
