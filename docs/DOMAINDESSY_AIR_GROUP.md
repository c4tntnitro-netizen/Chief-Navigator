# Domaindessy air group

The existing **Odyssey (Ithaca)** prototype uses the approved supplied hull sprite, four fixed fighter bays, built-in Surveying Equipment and conventional large energy mounts. The medium missile mount below the hangar remains removed. Its former naval lasers are retired.

| Installed research | Fighter wings, bays 1–2 | Bomber wings, bays 3–4 |
| --- | --- | --- |
| None | Two Broadsword wings | Two Khopesh wings |
| Interception Command Matrix | Two Lightning Superiority Fighter wings | Two Khopesh wings |
| Siege Munitions Compiler | Two Broadsword wings | Two Trident wings |
| Matrix + Compiler | Two Lightning wings | Two Trident wings |
| Compiler + Fortress Autoloader Core | Two Broadsword wings | Two Poseidon wings |
| All three | Two Lightning wings | Two Poseidon wings |

Autoloader requires the Compiler's Tridents before it can supply Poseidons. Autoloader alone leaves the Khopesh wings in place. The other Wall research components do not alter this air group. Normal campaign research and the existing `MaxUpgradeWallModules` override select the same configurations.

Lightning is a recolored stock Thunder with its original geometry and statistics, two craft per wing, a Light Dual Autocannon, the stock **Ion Cannon (High Delay, Fighter)** and a dedicated miniature Cloudswarm. The Cloudswarm has eight replenishing rounds, an eight-round burst, 800 range and 100 fragmentation damage per missile. It prioritizes hostile missiles, then fighters and combat drones, and excludes ordinary ships. Native Locust projectiles retain cold ejection followed by custom PD guidance. Its nominal ammunition regeneration is 0.8 rounds/second with an eight-round reload size.

Poseidon is a recolored stock Trident with its original geometry, statistics, two craft per wing and ordinary bomber AI. Both missile mounts use the stock two-round Atropos rack in place of the single-round version: **four torpedoes per bomber**, eight carried by each wing. It uses normal bomber return/rearming behavior. The proposed individual Recall ability was replaced by this upgrade.

The air-group hullmod selects among six predefined hull skins before ship creation and refreshes the owned variant's fixed wings. Shared hull specifications and stock fighter definitions are never edited. Native damaged hulls retain their corresponding damaged research profile and variant D-mods. Existing prototype variants lose only the retired Odyssey naval lasers; conventional player-fitted weapons are preserved. A configuration failure is logged once and stops further retries for that variant.

`tools/build_odyssey_ith_assets.py` installs the exact approved carrier master and delegates air-group data to `tools/build_domaindessy_air_group.py`. Fighter recolors are baked using masked color transfer, retaining exact source alpha and pixels outside the paint masks. There is no runtime tint. The fighter art references, masks, comparison and provenance are under `graphics/source/domaindessy_air_group_20261117`.

Verification covers all 128 research masks on normal and damaged native variants, fixed-wing locks, stock/shared-spec isolation, legacy laser cleanup, conventional weapon preservation, the actual pre-creation callback and its failure guard. Native RC7 loaders also validate all carrier/fighter skins and generated damaged skins, fighter variants, wing CSV, initialized missile damage, two-round Atropos ammunition, and profile selection through the actual public settings catalog. Miniature PD checks cover target priority/restrictions, ejection, guidance and target loss. These are automated checks; live combat appearance and fighter behavior still require in-game review.

Fully restart Starsector, then use `AddShip chief_navigator_odyssey_ith_Standard` for a fresh prototype. Its air group reflects installed research. No campaign reward or acquisition route was added by this redesign.

Both Ithaca prototypes are excluded from normal production, generated market stock (including black markets), arms dealers and random fleets. The Onslaught and all six Odyssey research skins use `restricted`, `no_sell`, `no_dealer` and `no_bp_drop`, alongside their existing autofit and identification tags. Neither has a blueprint-pool tag, faction-known entry or ship-role entry. Kleon's scripted Onslaught transfer remains available.

Acquisition rules were checked through the Galatia MCP's [ship-data tagging guide](https://galatia-academy.dev/wiki/ship-data-csv) and [ship distribution guide](https://galatia-academy.dev/wiki/adding-your-first-ship), then against the installed RC7 loader, `BaseSubmarketPlugin`, `CustomProductionContract` and `ShipBlueprintBrowser`. Skins replace their parent's tags; native damaged skins preserve `restricted` but discard most other tags, so their exclusion also relies on the absence of distribution entries. Colony production uses known blueprints directly: this is a normal-distribution exclusion, not a guard against console-forced blueprint knowledge or another mod explicitly granting a ship. Existing ships and save contents are not removed.
