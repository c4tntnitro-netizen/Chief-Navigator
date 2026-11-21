# Chief Navigator 0.6.6 validation

The candidate is based on public 0.6.5 commit `15163fab5ed8edca9bf114ee116485972a697462`. Its source changes come from development commit `8e89cbc2b17bb2a4933ae6a6fd8563798c4f178e`, limited to the nebula-painting vignette and the November 20 changes. Three source-only development edits that were never built into a tested JAR (Domaindessy air-group caching, Drifting Wall beam log gating and Hegemony base routing) are not included.

## Runtime scope

The runtime is the 0.6.5 release JAR (`7d44f45cba696855b5c69cb43e789d0f1ee626cd723b010d8a3e35e693bd3783`) with eight entries replaced and three added: `SilentWakeFlowEntityPlugin`, `ChiefNavigatorLunaSettings`, `ChiefNavigatorSettings`, `IthacaSectionEncounter`, `OdysseyExpanseSystem`, `SinniOneMoreHorizonIntel`, `SinniSystemVignetteInteraction` and `SinniVignetteRewards` replaced; `SinniBurnCap`, `CompleteOneMoreHorizon` and `ChiefNavigatorSinniNebulaCMD` added. The other 356 entries are byte-identical to 0.6.5. Every entry is byte-identical to the development build the user tested in game (`jars/ChiefNavigator.release-20261120.jar`, `6da44d7ce55e4dc90aceef5b270ec19226e455aa9b10f5ae3b49d2fb4066b34e`).

Compiling the eleven changed source files from this checkout for Java 17 against the candidate runtime produces byte-identical outer classes. Their five unchanged nested helper classes match by bytecode instructions and keep their original bytes.

The candidate runtime SHA-256 is `c6c052c49a8d0653c96ac7a4770fbaa0a7fa93be783354a85f22f28bb232f493`.

## Campaign checks

Run `python tools/verify_campaign.py` from a source checkout under the installed game. All 31 checks pass: the 29 from 0.6.5 plus `SinniBurnCapRegression` and `WallModuleDriftRegression`. `SanzuCloudRenderRegression` runs separately because it needs OpenGL, the LWJGL natives, the LunaLib test stub in `tools/stubs` and the previous JAR as its argument. Against the 0.6.5 JAR, all 84 renders are pixel-identical, per-frame garbage falls from about 113 KB to zero, and the LunaLib setting checks pass.

`SinniNebulaPaintingRegression` uses Starsector 0.98.5a-RC7 rule-engine class names and does not compile against the installed RC8 game. It passed when the vignette was built on November 18. On RC8 the nebula rules are covered by the native CSV loader below.

## Data and player evidence

Starsector's native CSV loader accepts all 930 dialogue rules (909 in 0.6.5, minus 3 retired nebula rules, plus 24 new ones), and the paginator finds no oversized pages. The data changes are the nebula vignette (rules, Ink, the illustration and its settings entry), the One More Horizon reward line, the LunaLib setting rows and the console command row. The user played this exact runtime locally and reported it working before publication.

`build.ps1 -Release` still stops under Windows PowerShell 5 at its option-punctuation check, as described for 0.6.5. The release JAR was assembled and verified as described above instead of rebuilt by that script.
