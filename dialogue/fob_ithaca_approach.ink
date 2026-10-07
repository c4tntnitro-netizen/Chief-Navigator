// Authoring and proofreading mirror for the rules-driven FOB Ithaca approach.
// Runtime completion is committed only by the final Continue option.

VAR player_title = "Captain"

=== opening ===

The orbital ring comes into view.

At this distance, it is nothing more than a vast black dash carved across the night sky.

“Sinni. Take us in.”

“Aye, {player_title}.”

+ [Continue.] -> reveal

=== reveal ===

As you approach, something glimmers in the distance—the faintest trace of teal against the darkness.

“Is that—”

You and Sinni see it at the same time.

FOB Ithaca.

It is alive.

+ [Continue.] -> battle_stations

=== battle_stations ===

“Battle stations!”

Half the bridge jumps in their seats. Officers scramble to their stations, strapping themselves into their anchor points as klaxons begin to blare throughout the ship.

“Battle stations. Battle stations,” the PA system drones.

“Sinni! Derelict drones are one thing. How is FOB Ithaca still operational?”

“I don’t know, {player_title}!”

+ [Continue.] -> weapons_reveal

=== weapons_reveal ===

She frantically pulls up her family’s notes as the orbital defense ring looms larger on the viewscreen.

“Those are massive communications antennae...” one of your bridge officers murmurs aloud.

Ordinarily, that would earn them a sharp admonishment.

But now you see it too.

“Those aren’t communications antennae,” Sinni gasps.

+ [“Those are defense guns! Full power to shields!”] -> defense_guns

=== defense_guns ===

Even from this distance, you can see Ithaca’s naval defense batteries turning in their carousels. The metal rings housing them are broader than most orbital stations.

One after another, the colossal turrets swing toward your fleet.

Teal light begins to gather in their barrels—the same light you glimpsed in the darkness.

+ [Continue.] -> defense_orders

=== defense_orders ===

Sinni starts firing off orders.

“That ring is double-sided! Keep us in line with its plane—get as close as you can! We cannot expose ourselves to the entire ring at once!”

Her hands race across the console.

“Myrmidon Five, you’re too far above us! Adjust bearing five-eight-niner down!”

+ [Continue.] -> energy_spike

=== energy_spike ===

“Energy spike detected!” your sensor officer shouts.

“Captain!” Sinni gasps, her face suddenly pale. “We’re—”

+ [“I know!” You key into the fleetwide channel. “All ships, fan out—!”] -> fan_out

=== fan_out ===

Teal-blue light consumes the viewscreen.

You throw an arm across your eyes.

Something explodes.

When the glare finally fades, you lower your arm.

Your fleet is still there.

Every ship. Every drive signature.

+ [Continue.] -> wreckage

=== wreckage ===

And yet—

Broken pieces of a vessel drift past the bridge, their armor twisted and pitted with the same strange wounds found on Threat ships.

Something else had been out there with you.

And FOB Ithaca had just killed it.

The wreckage continues to drift past.

A shattered bow section tumbles through the void, its armor blackened and perforated. There is no insignia on its hull. No running lights. No atmosphere bleeding from the rents in its plating.

Whatever it was, it had been waiting for you.

+ [Continue.] -> contacts

=== contacts ===

“Contacts!” your sensor officer shouts. “Multiple contacts emerging from the ring!”

The tactical display erupts with new signatures.

A formation of warships races out from FOB Ithaca, their drives blazing against the dark. They move with impossible coordination—every vessel accelerating, turning, and braking as though controlled by a single hand.

Within seconds, they surround your fleet.

Sinni stares at the display.

“They’ve boxed us in.”

+ [Continue.] -> warships

=== warships ===

The newcomers hold position at weapons range. Their ships are angular, brutally functional things, built according to Domain naval principles but altered by two centuries of isolated manufacture.

Ancient hull patterns reinforced with unfamiliar armor. Weapon emplacements nested beneath overlapping plates. Sensor arrays sweep across your fleet with mechanical regularity.

Fresh markings stand out against their dark hulls.

A spear driven through a star.

Beneath it, written in crisp Domain-era battle cant:

**FOURTEENTH BATTLEGROUP — TASK FORCE SPARTAN**

+ [Continue.] -> weapons_trained

=== weapons_trained ===

“No targeting locks,” your weapons officer reports. “But every gun they have is trained on us.”

+ [“Open a channel.”] -> open_channel

=== open_channel ===

The bridge falls silent.

Static crackles through the speakers. A data handshake follows—ancient Domain authentication protocols, obsolete everywhere else in the Sector but still executed here with textbook precision.

Then a face appears on the viewscreen.

Human.

+ [Continue.] -> officer_orders

=== officer_orders ===

The officer wears a dark naval uniform without ornament beyond his rank bars and the spear-and-star insignia at his collar. His hair is closely cropped. His expression is neither surprised nor relieved.

He regards you with the clinical focus of someone identifying an unknown contact on a firing range.

“Unregistered fleet,” he says. “Cut thrust. Disengage all active targeting systems. Hold present formation.”

His voice is curt, almost mechanical.

+ [Continue.] -> confirm_receipt

=== confirm_receipt ===

Around you, the bridge crew stare.

Sinni has gone completely still.

“They’re human,” someone whispers.

The Spartan officer’s gaze shifts fractionally toward the sound.

“Confirm receipt of instructions.”

+ [“Instructions received. We mean no—.”] -> acknowledge

=== acknowledge ===

“Compliance acknowledged.”

The surrounding warships adjust their formation. The maneuver is so exact that every engine flares at the same instant.

You glance at Sinni. She is staring at the officer as though he were a ghost.

Two hundred years.

For nearly two centuries, people have remained here—inside a macrocomplex that should have exhausted its fuel, food, replacement parts, and breathable atmosphere generations ago.

You lean toward the transmitter.

+ [“I am the {player_title} of this fleet—.”] -> identify

=== identify ===

“Identity declaration is unnecessary,” the officer interrupts. “Your command authorization, fleet composition, biological profiles, and probable point of origin have been recorded.”

Before you can speak, the Spartan officer continues.

“The hostile vessel shadowing your approach has been neutralized. Its presence does not alter your clearance status.”

The officer offers no explanation.

+ [Continue.] -> domain_citizen

=== domain_citizen ===

Behind his image, other uniformed figures move between austere command stations. Not one turns to look at the outsiders who have just arrived from beyond their dead system.

“Domain citizen,” the officer says.

The title lands heavily across the bridge.

“Strategos Menelaus of the Ithaca Defense Area requests your presence.”

Sinni’s eyes widen at the title.

“A request?” you ask, glancing at the warships surrounding you.

The officer remains impassive.

“Correct.”

+ [“And if I decline?”] -> decline

=== decline ===

For the first time, something shifts behind his eyes.

Not anger. Not amusement.

Confusion, perhaps—quickly suppressed.

“The Strategos has requested your presence,” he repeats, as though the statement itself should answer every possible objection.

One of the Spartan vessels moves ahead of your formation. Its running lights blink in sequence, marking an approach corridor toward a narrow opening in the orbital ring.

Beyond it, you catch your first glimpse of Ithaca’s interior.

+ [Continue.] -> ithaca_interior

=== ithaca_interior ===

Light.

Thousands upon thousands of lights.

Habitation blocks glow behind armored glass. Transit lines race along the ring’s inner surface. Vast foundries burn white-hot between cultivated stretches of green.

Ship-sized maintenance drones move between the docks, hauling armor plates and replacement components. Others crawl across the ring’s surface or cluster around vessels under repair. Among them, a handful of armed defense drones patrol the approaches.

+ [Continue.] -> macrocomplex_awake

=== macrocomplex_awake ===

The macrocomplex is not merely surviving.

It is awake.

It is working.

And somewhere inside it, Strategos Menelaus is waiting for you.

+ [Continue.] -> officer_warning

=== officer_warning ===

“Follow the pilot vessel,” the officer orders. “Deviation from the assigned corridor may be interpreted as hostile action.”

The transmission ends.

Sinni releases a breath she seems to have been holding since the first teal flash.

“{player_title},” she says quietly, still staring at Ithaca, “my great-grandmother’s records never mentioned a Strategos Menelaus.”

+ [“Strategos?”] -> strategos

=== strategos ===

“An old Domain appointment. Unified theater authority.” Her eyes move across the tactical display, taking in the orbital guns, the warships, and the impossible scale of the installation surrounding them. “Fleet command, ground forces, logistics, industry, civil defense—everything within a designated strategic area.”

You look toward the vast inhabited curve of Ithaca.

+ [“The Fourteenth left him in command of all this?”] -> fourteenth

=== fourteenth ===

“Yes.”

Sinni’s voice falls quiet.

She looks back at the Spartan ships surrounding your fleet. As one, they break formation, opening a corridor toward the center of the ring.

Your fleet follows.

The central module emerges from behind the ring’s curvature.

+ [Continue.] -> central_module

=== central_module ===

Calling it a space station would be absurd. It is a moon suspended at the heart of Ithaca, its armored surface broken by docks, towers, defense emplacements, and vast illuminated apertures.

Hundreds of transit lines stretch between it and the surrounding ring, binding both structures together like the spokes of an immense wheel.

Ahead, the pilot vessel guides you toward the central module.

Then you see the carnage.

+ [Continue.] -> ring_breach

=== ring_breach ===

An entire section of the ring has been shorn away by some tremendous force. A gaping wound thousands of kilometers across breaks its circumference. Severed transit lines reach into empty space. Atmosphere glitters as it escapes from ruptured habitation blocks.

The maintenance drones are thickest there.

Thousands of them swarm around the breach, hauling debris, sealing compartments, and fitting temporary supports across fractures too vast to comprehend. Against the scale of the damage, even the largest among them look like insects tending an open wound.

+ [Continue.] -> losing

=== losing ===

The lights end at the edge of the breach.

Beyond them lies only twisted metal and darkness.

Sinni stares at the devastation.

“They are losing.”

+ [Continue.] -> END
