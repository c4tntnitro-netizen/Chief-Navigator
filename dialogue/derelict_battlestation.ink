// Authoring and proofreading mirror for the Domain Derelict Battlestation.
// Runtime state, prose, and choices live in data/campaign/rules.csv.
// A single linear inspection; legacy area-inspection flags are no longer read.
// Completion is recorded only when leaving the final page.

VAR concluded = false

{ concluded:
    -> return_visit
- else:
    -> approach
}

=== approach ===

The Domain Derelict Battlestation circles Old Milix in a slow, lightless roll. Its docking spines are empty, its weapon housings bare, and its identification beacon repeats a Fourteenth Battlegroup challenge code that has been obsolete for centuries.

Upon boarding, your operations team finds residual power in the stationkeeping grid. There is enough to cycle a docking collar and wake portions of the internal network, but no response from command and no sign of an active crew.

+ [Continue.] -> inspect_hull

=== inspect_hull ===

A shuttle carries your survey team along the station's armored equator. Micrometeorites have stippled the plating, and several docking accidents left shallow collision scars. There is no beam scoring, no penetrator channel, and no ruptured seam around any magazine.

The empty emplacements tell a clear story. Mounting bolts were released from inside, feed mechanisms disconnected at their service couplings, and power conduits sealed beneath numbered caps. Even the point-defense housings were removed under yard control.

Inside one housing, your team finds an inspection strip bearing the serial number of the missing gun and the transport that received it.

+ [Continue.] -> inspect_hab

=== inspect_hab ===

The habitation decks are cold and perfectly dry. Your marines find no bodies, no barricades, and none of the hurried damage left by decompression or boarding action.

Blankets, pressure suits, medical packs, and portable scrubbers are gone. Personal lockers stand open with their doors tied back for inspection. In the galley, a handwritten ration table records successively smaller portions beside a list of outbound transport numbers.

The final line has no ration figure at all.

+ [Continue.] -> inspect_command

=== inspect_command ===

Your engineers bypass a fused security bus and bring one command console back to life. Most tactical records were purged, but the logistics archive remains intact.

Transfer orders bearing Fourteenth Battlegroup seals fill the display. Field coils went to tankers, reactor shielding was cut into plates for supply transports, and guidance packages, ammunition loaders, and the reserve fuel inventory were divided among ships assigned to a departure column.

The destination: the Persean Sector.

Estimated time: According to Sinni, far too optimistic.

The last station order contains only two instructions: confirm the final transport clear, then maintain orbit for returning units. Beneath it, an automated acknowledgement request repeats at widening intervals. No reply follows. The next automated request is due in another fifty-eight cycles.

+ [Continue.] -> conclusion

=== conclusion ===

Your operations chief compiles the survey. No battle destroyed the station. The Fourteenth expended it to keep the departure column moving: guns, field coils, shielding, and fuel went out aboard the transports, then the crew followed.

The magazines are empty. Portable stores and life-support equipment left with the transports. What remains of the reactor is locked into the stationkeeping grid, its shielding cut too close to minimum tolerance to isolate or remove safely.

As your last shuttle clears the docking collar, a stationkeeping thruster fires to correct the battlestation's orbit. The identification beacon sweeps across your receiver once more, transmitting its obsolete Fourteenth Battlegroup challenge and waiting for an answer.

+ [Leave.]
    ~ concluded = true
    -> END

=== return_visit ===

The Domain Derelict Battlestation continues its empty circuit of Old Milix. A stationkeeping thruster fires as you approach, and the identification beacon repeats its obsolete Fourteenth Battlegroup challenge.

+ [Leave.] -> END
