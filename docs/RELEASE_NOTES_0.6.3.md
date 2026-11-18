# Chief Navigator 0.6.3

November 18, 2026. Starsector 0.98.5a-RC7.

- Redesign the Odyssey (Ithaca), or Domaindessy, around four fixed fighter wings: two Broadswords and two Khopeshes. Remove its naval lasers and restore conventional large energy mounts. Keep the supplied Domain livery, built-in Surveying Equipment and removed aft medium missile mount.
- Interception Command Matrix upgrades the Broadswords into Lightning Superiority Fighters: recolored Thunders with miniature Cloudswarm point defense, a Light Dual Autocannon and a high-delay fighter Ion Cannon.
- Siege Munitions Compiler upgrades the Khopeshes into Tridents. Fortress Autoloader Core then upgrades those Tridents into recolored Poseidons, each carrying two double Atropos racks for four torpedoes per bomber. Autoloader Core requires the Compiler for this upgrade. Fighters retain native flight and bomber behavior; no custom recall system is added.
- Exclude both Ithaca prototypes, including every Odyssey research configuration, from normal production, generated market and black-market stock, arms dealers, blueprint drops and random fleets. Kleon's one-time Onslaught transfer after Labor V remains available.

## Updating

Replace the previous Chief Navigator folder with this package and restart Starsector. Requires GraphicsLib 1.10.2 or newer and Combat Chatter 1.15.0 or newer. LunaLib, Peerless Skills and Hall of Triumph remain optional.

Existing Ithaca Odysseys switch to the fixed air group and lose their retired built-in naval lasers. Refit the two large energy mounts after updating; other player-fitted weapons are preserved. The fresh default variant mounts an Autopulse Laser and High Intensity Laser. The Odyssey remains available for console testing with `AddShip chief_navigator_odyssey_ith_Standard`; this update adds no campaign reward for it.

## Verification and known issues

The full Java 17 build and all 909 native dialogue-rule checks pass. Native carrier checks cover all 128 research masks on normal and damaged hulls, built-in wing locks, PD target restrictions, actual weapon/wing loading and Poseidon torpedo capacity. Acquisition checks verify all seven prototype profiles and the native military production selector. The archive preserves every previous runtime entry and adds only the three carrier implementation classes. This update has automated verification; no new live combat playtest is claimed.

CN-033, the report that survey ships may be fought twice, remains unresolved. Isa's Ithaca engineering conversation remains a placeholder.
