// Authoring mirror only. Runtime prose and choices live in data/campaign/rules.csv.
// Recruitment uses native confirmation for one story point and transfers an
// actual random surviving patrol member; this mirror does not spend points.

VAR patrol_name = "Guard Patrol"
VAR drone_count = 3
VAR ship_name = "Guard Drone"

=== patrol ===

A short authentication exchange brings {patrol_name} onto your tactical display. Its drones carry Menelaus's restored IFF and hold their course while your fleet approaches. You can authorize the reassignment of a random drone to your command for one story point; the patrol currently has {drone_count} ships available.

{ drone_count > 0:
    + [Authorize a drone transfer.] -> transferred
}
+ [Leave.] -> END

=== transferred ===

The patrol releases {ship_name} from its formation. Your officers verify the Domain-Security authorization and bring the drone onto the fleet network as it takes station with your ships.

+ [Continue.] -> patrol

=== rejected ===

The transfer cannot be completed. No story point has been spent.

+ [Continue.] -> patrol

=== wall_labor_five_support ===

The Drifting Wall acknowledges your approach and opens a channel to its guard network. Two Guard Bastions detach from the station's defensive formation and take station alongside your fleet. Their orders are to accompany you through the defense of Ithaca; they remain under Menelaus's command.

+ [Leave.] -> END
