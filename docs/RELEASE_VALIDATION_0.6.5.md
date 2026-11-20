# Chief Navigator 0.6.5 validation

The candidate is based on public 0.6.4 commit `fa5a335706752d6d83f90bbf0c76fdb785721c53`.

## Runtime scope

The runtime is the 0.6.4 release JAR (`08c2dd5ef1eec982121fc52abf3802e833eeadda07e06218e111509c7163789f`) with exactly two entries replaced: `chiefnavigator/quest/IthacaThreatPolicy.class` and `chiefnavigator/quest/OdysseyPredatorScript.class`. Both come byte-for-byte from the development build tested in game. The other 362 entries are byte-identical to 0.6.4.

Compiling the two changed source files from this checkout for Java 17 against the 0.6.4 runtime produces byte-identical outer classes. Their unchanged nested helper classes match by bytecode instructions and keep their original 0.6.4 bytes.

The candidate runtime SHA-256 is `7d44f45cba696855b5c69cb43e789d0f1ee626cd723b010d8a3e35e693bd3783`.

## Campaign checks

Run `python tools/verify_campaign.py` from a source checkout under the installed game. All 29 checks pass, the 28 from 0.6.4 plus `CampaignUpkeepOnceRegression`. The new test checks that the post-defeat Ungaikyo cleanup stops after a clean pass, keeps retrying while blocked, re-checks after a new load, and that guard upkeep runs once per campaign timestamp per system, never serves another system from its cache and runs uncached without a clock.

## Data and player evidence

No data, dialogue, art or audio changes. Starsector's native CSV loader accepts all 909 dialogue rules. The user tested the package locally (dev folder disabled, 0.6.5 installed) and reported that it works. Java Flight Recorder captures after an Ungaikyo kill measured Chief Navigator at about 3.4% of campaign game-thread CPU with this runtime, against 33.2% on 0.6.3 on the same kind of save.

`build.ps1 -Release` currently stops under Windows PowerShell 5 at its option-punctuation check because the BOM-less script's em-dash pattern is decoded with the system code page; the dialogue line it flags ends in a correct em dash and is unchanged since 0.6.4. The release JAR was assembled and verified as described above instead of rebuilt by that script.
