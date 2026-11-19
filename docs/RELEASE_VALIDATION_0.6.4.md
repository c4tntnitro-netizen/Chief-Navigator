# Chief Navigator 0.6.4 validation

The candidate is based on public 0.6.3 commit `4e42cebbae2c78c4ced22968d14465dc57e69517`.

## Build and runtime scope

`build.ps1 -Release` compiles the isolated source checkout for Java 17. Starsector's native CSV loader accepts all 909 unique dialogue rules. The runtime changes 18 existing class entries and adds five; 341 entries from 0.6.3 remain byte-identical. All 37 compiled members of the 20 affected source families match the tested development runtime by bytes or bytecode instructions. Unchanged nested helpers retain their original bytes.

The candidate runtime SHA-256 is `08c2dd5ef1eec982121fc52abf3802e833eeadda07e06218e111509c7163789f`. It is the exact locally tested runtime. Uninstalled combat drafts are excluded.

## Campaign checks

Run `python tools/verify_campaign.py` from a source checkout under the installed game. The runner locates the complete game installation, uses the selected JAR and writes individual logs under `out_campaign_check`.

All 28 checks pass: campaign/load lifecycle; committed research recovery and upgrade access; cached population deadlines and ownership; quest objective and defender reconciliation; Sinni navigation/map targets; category classification, completion, overlap and Kite rewards; native fleet/pursuit behavior; Budai routing/roster/movement; Ungaikyo and Gautama rematch rosters; final-Labor spawning and music.

The performance checks use `CampaignPerformanceRegression --campaign-only`. They verify repeated idle calls avoid recurring world/cargo/identity searches, category caches reset with a new sector, and vignette checks retain a one-second cadence. Overlap checks cover unfinished-match selection, final-choice-only credit, interrupted scenes, reloads, mixed trinary stars, category deduplication, explicit nebula centers, story/Core exclusions and reward eligibility.

This is a targeted suite, not a claim that every historical test passes. Previously documented legacy runner incompatibilities and baseline failures remain outside this change.

## Data and player evidence

Only seven research-component tag cells and Tetsu Kannon's tag cell change in gameplay data. Dialogue, art, audio, other ship stats and other weapons remain unchanged. The prototype builder retains the corresponding Tetsu Kannon tags.

The user reports a completed local campaign run, with one pre-combat Budai Disengage hang followed by a successful retry after restarting. That is not proof that the original player's worsening stutter is resolved. No quantitative CPU/FPS claim is based on the invalid negative CPU/GC readings in a supplied VisualVM Monitor screenshot. No cause or fix is assigned to the Budai hang without a frozen-process trace.

Packaging checks committed inputs, referenced assets, archive integrity and every payload checksum. Local staging, Git publication and GitHub release publication are tracked separately on the scrum board.
