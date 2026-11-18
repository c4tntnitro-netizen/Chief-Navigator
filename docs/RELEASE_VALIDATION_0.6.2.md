# Release validation: 0.6.2

The author reported completion of the full release playtest and authorized publication on November 17, 2026. This release targets Starsector 0.98.5a-RC7 and Java 17.

The shipped JAR is byte-identical to the tested 0.6.2-rc1-local JAR. All 338 classes rebuild from the release sources. Twenty-five nested/helper classes differ in debug metadata but have matching disassembled instructions and signatures. One development-only Hegemony-base interception branch was absent from the tested JAR; publication source was aligned to the tested implementation, with the development source retained locally. The rebuilt archive is not substituted for the playtested archive.

The build validates native CSV parsing, options and command registration. The RC7 validator now uses the installed parser, and javac uses a source argument file to avoid Windows command-line limits. Native CSV loading accepts 909 rules; pagination reports no oversized pages. The 42 changed/new rule records and 13 removed records were reviewed against the preceding publication: Helen conversation/ending revisions, fleet repair wording, removal of retired Peerless menus, the Onslaught reward and the existing Isa placeholder. Authored Ink accompanies the runtime dialogue.

Fresh checks passed for Bouvo rendering, naval charge-pulse behavior and native RC7 damage initialization, and the nine current Tetsu Kannon/Exodus checks. Archive generation verifies committed inputs, image/music references, each ZIP member and checksums. User playtesting is distinguished from these automated checks.

CN-033 remains an unconfirmed report that survey ships can be fought twice. Isa's engineering conversation remains an authored placeholder. No fix for either is claimed.
