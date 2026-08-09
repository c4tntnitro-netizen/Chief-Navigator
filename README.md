# Chief Navigator

Chief Navigator is a standalone Starsector quest mod about trusting a master smuggler-navigator named Sinni with an expedition fleet and following her through a once-in-a-lifetime route across the Abyss.

The route begins at a transient departure point called **Waypoint Troy** and ends at **FOB Ithaca**, a planet-scale Domain military macrobase associated with the Fourteenth Battlegroup's arrival in the Persean Sector. The crossing is meant to be a long attritional test of navigation, supplies, and fleet preparation rather than a normal point-to-point trip.

At Ithaca, the apparent derelict field awakens into an Explorarium defense reserve fighting a new THREAT incursion. These deliberately limited, post-THREAT Domain drones cannot win unaided. The player's climactic objective is not merely to destroy the enemy, but to make the derelicts win and preserve as much of Ithaca's irreplaceable reserve as possible.

The authoritative inherited concept and its decision boundaries are recorded in [docs/CONCEPT.md](docs/CONCEPT.md). Implementation is intentionally limited to a loadable skeleton until the quest architecture is designed against those decisions.

## Current scaffold

- Starsector `0.98a` metadata
- Minimal Java mod plugin
- Local PowerShell build script using Starsector's bundled JDK and API jars
- Design notes preserving the prior concept discussion

## Build

From PowerShell in this directory:

```powershell
.\build.ps1
```

The build produces `jars/ChiefNavigator.jar`.

