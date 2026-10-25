// Authoring and proofreading mirror for the rules-driven FOB Ithaca approach.
// Runtime completion is committed only by the final dialogue option.

VAR player_title = "Captain"

=== opening ===

The orbital ring comes into view. At this distance, it is nothing more than a vast black dash carved across the night sky. Several of your bridge crew swear at the sight. It is larger than most planets in the Persean Sector. The corpse of a colossus hanging in the twilight nebula of Ashen Verge.

+ [“Nav. Half-speed approach.”] -> reveal

=== reveal ===
“Aye, {player_title}.”

The macrobase continues to grow in your bridge opticals.

As you approach, something glimmers against the dark silhouette—a thin thread of teal light, barely visible at this distance. It cuts out of the side of the ring and off into the distance.

You and Sinni realize it at the same time.

FOB Ithaca.

It is alive.

"{player_title}--!" Sinni turns toward you.

+ [“Battle stations!”] -> battle

=== battle ===
Your officers scramble to their stations, strapping themselves into their anchor points as klaxons begin to blare throughout the ship.

“Battle stations, battle stations,” the PA system drones.

The tactical display switches to combat mode. Across your fleet, ships break their approach formation and take up defensive positions around the flagship. Acknowledgement signals begin arriving from the other ship captains.

+ [“Derelict drones I get, Sinni! How is FOB Ithaca still operational?!”] -> battle_2

=== battle_2 ===

“Automated systems?! I don't know!”

She pulls up her family’s notes as the orbital defense ring looms larger and larger on the viewscreen. FOB Ithaca has long since filled up the entirety of your bridge optics.

“Those are massive communications antennae...” one of your bridge officers wonders aloud.

Sinni gasps. Helen's notes remain forgotten in front of her.

“Those aren’t antennae,” she says.

+ [“Those are defense guns! Shields at full!”] -> defense_guns

=== defense_guns ===

Even from this distance, you can see Ithaca’s naval defense batteries turning in their carousels. The metal rings housing them must be larger than some orbital stations.

One after another, colossal turrets swing toward your fleet. Lightning arcs across their housings as teal light gathers in their barrels—the same light you glimpsed in the darkness.

Sinni starts firing off orders.

"That ring is double-sided! Keep us in the plane of the ring—get as close as you can! We cannot expose ourselves to the entire ring at once!"

Her hands race across the console.

"Energy spike detected!" your sensor officer shouts.

"Captain!" Sinni calls. "We're—"

+ [Key the fleetwide channel.] -> fan_out

=== fan_out ===

"Fan out!"

Teal-blue light consumes the bridge opticals. You throw an arm across your eyes. For an instant, everything is white.

Then something strikes your ship. The deck shudders, followed by a succession of smaller impacts rattling through the hull. When the glare finally fades, you lower your arm.

Your fleet is still there. Every ship.

And yet—

Broken pieces of a vessel drift past the bridge opticals. Shattered armor plating, blackened and perforated, tumbles through the space between your ships. A shattered bow section passes across your view. No insignia. No identification markings. As it turns, you recognize the strange, almost chitin-like arrangement of its armor plates.

A Threat-pattern ship.

It had been out there with you. It had followed your fleet all the way to Ithaca.

And FOB Ithaca had just killed it.

+ [Continue.] -> contacts

=== contacts ===

“Contacts!” your sensor officer shouts. “Multiple contacts emerging from the ring!”

The tactical display erupts with new signatures.

A formation of warships races out from FOB Ithaca, their drives blazing against the dark. They move with lockstep coordination—every vessel accelerating, turning, and braking as though they were one.

Within seconds, they surround your fleet.

+ [Continue.] -> warships

=== warships ===

The newcomers hold position at weapons range. Their ships are familiar—brutally functional hulls of the same classes still fielded by Hegemony fleets, built to old Domain naval specifications. Weapon emplacements sit beneath overlapping armor plates. Sensor arrays sweep across your fleet with mechanical regularity.

Fresh markings stand out against their dark hulls. A spear driven through a shield.

Beneath it, written in crisp Domain-era battle cant:

**XIV BATTLEGROUP — TASK FORCE SPARTAN**

“No targeting locks,” your weapons officer reports. “But every gun they have is trained on us.”

+ [“Open a channel.”] -> open_channel

=== open_channel ===

The bridge falls silent.

Static crackles through the speakers. A data handshake follows—ancient Domain authentication protocols, obsolete everywhere else in the Sector but still executed here with textbook precision.

First, an insignia flashes. The same one you saw on the banner at Sinni's office.

XIV BATTLEGROUP, 6TH SPECIAL OPERATIONS FORCE # font:victor14

TASK FORCE SPARTAN # font:victor14

MOLON LABE! # font:victor14

Then a face appears on the viewscreen.

The officer wears a dark naval uniform without ornament beyond his rank bars and the spear-and-shield insignia at his collar. His hair is closely cropped. His expression is neither surprised nor relieved.

“Unregistered fleet,” he says. “Cut thrust, disengage all active targeting systems, hold present formation.”

+ [Continue.] -> confirm_receipt

=== confirm_receipt ===
Around you, the bridge crew stare.

“They’re human,” someone whispers.

The Spartan officer’s gaze shifts fractionally toward the sound.

“Confirm receipt of instructions.”

+ [“Instructions received. We mean no—”] -> acknowledge

=== acknowledge ===

“Acknowledged.”

You glance at Sinni. She is staring, wide-eyed, at the officer.

For nearly two centuries, people have remained here—inside a macrocomplex that should have exhausted its fuel, food, replacement parts, and life-support systems generations ago.

You lean toward the transmitter.

+ [“I am the {player_title} of this fleet—”] -> identify

=== identify ===

“Identity declaration is unnecessary,” the officer interrupts. “Your command authorization, fleet composition and point of origin have been recorded.”

Before you can speak, the Spartan officer continues.

“Domain citizen,” the officer declares. Behind his image, other uniformed figures move between austere command stations. Not one turns to look at the outsiders who have just arrived from beyond their dead system.

The title lands heavily across the bridge.

“Strategos Menelaus of the Ithaca Defense Area requests your presence.”

+ [“And if I decline?”] -> decline

=== decline ===
The officer pauses, apparently taken aback by the question.

“The Strategos has requested your presence,” he repeats, as though the statement itself should answer every possible reply.

One of the Spartan vessels moves ahead of your formation. Its running lights blink in sequence, marking an approach corridor toward a narrow opening in the orbital ring. Beyond it, you catch your first glimpse of Ithaca’s interior.

Light.

Hundreds upon hundreds of lights. Habitation blocks glow behind armored glass. Transit lines race along the ring’s inner surface. Ship-sized maintenance drones move between the docks, hauling armor plates and replacement components. Others crawl across the ring’s surface or cluster around vessels under repair. Among them, a handful of armed defense drones patrol the approaches.

+ [Continue.] -> macrocomplex_awake

=== macrocomplex_awake ===
“Follow the pilot vessel,” the officer orders. “Deviation from the assigned corridor may be interpreted as hostile action.”

The transmission ends.

Sinni releases a breath she seems to have been holding since the first teal flash.

“{player_title},” she says quietly, still staring at Ithaca, “my great-grandmother’s records never mentioned a Strategos Menelaus.”

+ [“Strategos?”] -> strategos

=== strategos ===

“An old Domain appointment. Unified theater authority.” Her eyes move across the tactical display, taking in the orbital guns, the warships, and the impossible scale of the installation surrounding them. “Fleet command, ground forces, logistics, industry, civil defense—everything within a designated strategic area that could span an entire Sector.”

You look toward the vast inhabited curve of Ithaca.

+ [“The Fourteenth left him in command of all this?”] -> fourteenth

=== fourteenth ===

“They must have.”

Sinni’s voice falls quiet.

She looks back at the XIV Spartan ships surrounding your fleet. As one, they break formation, dividing into two great wings, each vessel adjusting its position in perfect coordination with the others. The ships open a corridor toward the center of the ring.

Your fleet follows. The central module emerges from behind the ring’s curvature.

Calling it a space station would be absurd. It is more like an entire moon suspended at the heart of Ithaca, its armored surface broken by docks, towers, defense emplacements, and vast illuminated apertures. Hundreds of transit lines stretch between it and the surrounding ring, binding both structures together like the spokes of an immense wheel.

Ahead, the pilot vessel guides you toward the central module.

Then you see the carnage.

+ [Continue.] -> ring_breach

=== ring_breach ===

An entire section of the defense ring has been shorn away. A gaping wound thousands of kilometers across breaks its circumference. Severed transit lines reach into empty space. Both ends of the breach are covered in mismatched armor plates, exposed structural braces, and temporary bulkheads.

The maintenance drones are thickest there.

Thousands of them swarm around the breach, hauling debris, sealing compartments, and fitting temporary supports across fractures too vast to comprehend. Against the scale of the damage, even the largest among them look like insects tending an open wound. The lights end at the edge of the breach. Beyond them lies only twisted metal and darkness.

Sinni stares at the devastation. She turns to you.

"Captain. They're alive."

+ ["They're losing."] -> END
