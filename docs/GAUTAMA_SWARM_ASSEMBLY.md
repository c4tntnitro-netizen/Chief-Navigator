# Gautama swarm assembly

Gautama's carrier escorts and detached reincarnation escorts now assemble from
the surrounding native fragment cloud. `GautamaSwarmLaunch` uses
`RoilingSwarmEffect.transferMembersTo`, the same handoff used by vanilla
`SwarmLauncherEffect`. Existing fragment objects retain their world positions,
velocities and fade state while their destination offsets change. The hidden
fighter keeps its normal launch position and motion.

The custom fragile-swarm hullmod holds a fresh escort's initial cloud fill and
respawn until its first handoff. After assembly, ordinary native fragment
respawn resumes. Reconstructed Gautama calls the same handoff explicitly for
its detached wings.

Ordinary Gautama previously had no registered fragment cloud. Its new assembly
cloud uses native hull-outline placement and rendering but is cached privately
and excluded from the weapon-ammunition and fragment-exchange maps. Existing
registered construction clouds remain registered. Each handoff restores the
source's prior active fragment count, so launch visuals do not spend or grant
weapon ammunition. Refill pieces appear around the native hull outline rather
than its center. Keep a private cloud for its entire incarnation: native cloud
expiry unmaps the attached ship, so replacing it with another registered cloud
while it remains alive is unsafe.

The ordinary twenty-bay/five-second carrier screen and reconstructed twenty
escorts at 1.25-second intervals are unchanged, as are fragility, ownership,
launch velocity, combat cleanup and construction gating. No hullmod or weapon
loadout changes are involved.

The development build and these headless regressions passed:

- `GautamaSwarmLaunchRegression`: real native fragment identities, world motion,
  reserve preservation, private-cloud isolation, and staged launch behavior.
- `GautamaSwarmScreenRegression`: fragile hull/armor and quiet hidden carriers.
- `GautamaConstructionRegression`: construction, gating and staged-screen rules.
- `StarvingSwarmRegression`: existing death swarms, ownership and cleanup.

In-game movement and appearance still need a visual check.
