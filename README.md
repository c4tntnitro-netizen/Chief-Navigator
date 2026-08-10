# Chief Navigator

Chief Navigator is a standalone Starsector quest mod about trusting a master smuggler-navigator named Sinni with an expedition fleet and following her through a once-in-a-lifetime route across the Abyss.

The route begins at a transient departure point called **Waypoint Troy** and ends at **FOB Ithaca**, a planet-scale Domain military macrobase associated with the Fourteenth Battlegroup's arrival in the Persean Sector. The crossing is meant to be a long attritional test of navigation, supplies, and fleet preparation rather than a normal point-to-point trip.

At Ithaca, the apparent derelict field awakens into an Explorarium defense reserve fighting a new THREAT incursion. These deliberately limited, post-THREAT Domain drones cannot win unaided. The player's climactic objective is not merely to destroy the enemy, but to make the derelicts win and preserve as much of Ithaca's irreplaceable reserve as possible.

The authoritative inherited concept and its decision boundaries are recorded in [docs/CONCEPT.md](docs/CONCEPT.md). Implementation is intentionally limited to a loadable skeleton until the quest architecture is designed against those decisions.

## Current scaffold

- Starsector `0.98a` metadata
- Java mod plugin
- Playable **Hyperspace Odyssey** campaign-ability prototype
- Local PowerShell build script using Starsector's bundled JDK and API jars
- Design notes preserving the prior concept discussion

## Hyperspace Odyssey prototype

The ability is granted automatically on game load for testing. Activate it in
hyperspace while facing the desired direction. The fleet remains stationary for
three campaign days, then travels at extreme speed until it intersects a storm,
slipstream, another fleet, or reaches the 30-light-year test limit.

The prototype validates charging, directional launch, and collision termination.
Fuel cost, CR damage, route visualization, Sinni recruitment gating, and the
scripted leap to Waypoint Troy are intentionally deferred.

## Build

From PowerShell in this directory:

```powershell
.\build.ps1
```

The build produces `jars/ChiefNavigator.jar`.
