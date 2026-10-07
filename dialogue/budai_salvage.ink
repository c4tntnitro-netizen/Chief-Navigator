// Runtime authority: data/campaign/rules.csv, ChiefNavigatorBudaiSalvage_*.

VAR player_title = "Captain"

=== opening_isa ===
Budai does not leave a corpse. Its distortion collapses inward, then convulses.

Armor plate, cable, structural members, and fused machinery begin emerging from its body in a slow, expanding cloud.

The bow of an Onslaught Mk.I rolls free. The battleship has been hollowed to a rib cage, its armor polished from the inside. Two forward gun assemblies remain buried in the keel.

+ [Continue.] -> assess_isa

=== opening_salvage_chief ===
Budai does not leave a conventional wreck. Its distortion collapses inward, then convulses.

Armor plate, cable, structural members, and fused machinery begin disgorging from the mouth in a slow, expanding cloud.

The bow of an Onslaught Mk.I drifts out of the distortion as that same distortion fades. By the time the last bits of twisted light and black-red smoke fade into nothing, the desiccated corpse of the Hegemony's pride and strength is now free. The battleship has been hollowed to a rib cage, its armor polished from the inside. Two forward gun assemblies remain buried in the keel.

+ [Continue.] -> assess_salvage_chief

=== assess_isa ===
Isa joins the salvage chiefs at the cut line. She identifies the assemblies as Heavy Adjudicators: Domain flak cannon feeds built through the Onslaught's frame rather than mounted on it.

She studies the distorted keel on three different scans.

“Eeeverything's FUBAR'd except for the main guns. I think I can extract the port one no problem. That starboard one though...” Isa chews her lip, thinking. “Get me a vacsuit, {player_title}. I'll see what I can do.”

+ [Begin the extraction.] -> extract_isa

=== assess_salvage_chief ===
Your salvage chief identifies the assemblies as Heavy Adjudicators: Domain flak cannon feeds built through the Onslaught's frame rather than mounted on it.

The mechanisms are intact, but the wreck is now part gun carriage and part Budai-digested shell. Extracting either weapon will mean cutting away what remains of the ship around it.

+ [Begin the extraction.] -> extract_salvage_chief

=== extract_isa ===
The operation takes hours. Salvage crews peel the keel away in sections while Isa has temporary recoil frames welded around each feed assembly. Nothing recoverable remains of the Onslaught when the second gun comes free.

Two modularized Heavy Adjudicators are transferred to the fleet's weapon stores. Their improvised mounting frames are jerry-rigged to fit standard large ballistic hardpoints.

+ [Continue.] -> END

=== extract_salvage_chief ===
The operation takes most of a watch. Salvage crews peel the keel away in sections and weld a temporary recoil frame around one feed assembly. Nothing recoverable remains of the Onslaught when the gun comes free.

A single modularized Heavy Adjudicator is transferred to the fleet's weapon stores. Its improvised mounting frame is jerry-rigged to fit standard large ballistic hardpoints.

+ [Continue.] -> END
