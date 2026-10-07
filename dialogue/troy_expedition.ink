// Authoring mirror for the rules-driven Waypoint Troy interaction.

=== anchorage ===
The cooperative expedition anchorage holds its docking arms open. Its automated stores can retain ships and cargo until your return.

Sinni's route can carry no more than 30 deployment points of ships. Storage is free; anything left here will remain available when you return.

+ [Access ship and cargo storage.]
+ [Ask Sinni for advice about preparing the expedition.] -> advice
+ [Authorize Sinni to open the route.]
+ [Leave.]

=== advice ===
Sinni studies the expedition manifest.

"Use Efficiency Overhaul wherever it fits. Insulated Engine Assembly and other hullmods that reduce your sensor profile will make the crossing less dangerous. Bring at least one dedicated supply ship and one fuel ship. If either runs dry out there, no one is coming to refill it."

+ [Back.] -> anchorage
+ [Leave.]

=== transit ===
Sinni guides your fleet through a treacherous series of slipstreams and gravity boosts. After expending half your supplies and fuel, your fleet arrives at the Orion Knot Sector.

Sinni flags FOB Ithaca in Ashen Verge on the expedition map.

+ [Continue.]

=== alpha_odyssey_arrival ===
The bridge opticals show nothing.

No stars. No dust. Not even the faint distortion of hyperspace—only a depthless black pressing against the ship from every direction.

+ [Continue.] -> alpha_odyssey_arrival_approach

=== alpha_odyssey_arrival_approach ===
Sinni leans over the navigator's console, her face washed pale by its instruments. Her hands move constantly, making tiny corrections whenever the darkness strikes back.

Something scrapes along the drive bubble.

The sound travels through the hull: a slow, metallic shriek that rises beyond hearing and then plunges into a guttural moan.

Around you, officers exchange uneasy glances.

It sounds less like tortured machinery than some immense animal calling from just beyond the edge of reality.

Another impact shudders through the deck.

"Hold together," Sinni whispers.

Whether she means the fleet, the ship, or herself is impossible to say.

+ [Continue.] -> alpha_odyssey_arrival_breakthrough

=== alpha_odyssey_arrival_breakthrough ===
There is no charted path through the terminus.

No beacon to follow.

Only half-deciphered coordinates, Helen's logs and Sinni's instinct guide the fleet through a place where every instrument insists there is nothing—and where that nothing keeps clawing at the drive field.

A warning flashes across her console.

Sinni corrects.

The darkness screams.

Then, ahead of the fleet, something changes.

At first it is only a pinprick on the bridge opticals, so faint it might be another failing sensor.

It sharpens into a slender wound of orange light.

With every passing second, it widens, spilling fire across the black.

+ [Continue.] -> alpha_odyssey_arrival_reveal

=== alpha_odyssey_arrival_reveal ===
"The far side," Sinni breathes.

The fleet bursts through.

Darkness vanishes in a blaze.

A brilliant orange star fills the bridge opticals, immense and incandescent, its light washing across worlds that have turned in silence for generations.

Bands of glowing dust stretch through the system like molten rivers.

Distant planets gleam beneath the light of their sun, while ancient debris fields catch the radiance and scatter it in a million golden sparks.

For more than two centuries, no human being has looked upon this sky.

Now your fleet drifts beneath it.

For several long moments, nobody on the bridge speaks.

Sinni stares at the system as if she scarcely believes it is real.

+ ["...We're here. Outside the Persean Sector."] -> alpha_odyssey_arrival_2

=== alpha_odyssey_arrival_2 ===
"YEAH!"

Sinni shoots out of her chair, both fists in the air.

For one stunned heartbeat, the entire bridge stares at her.

Then somebody else cheers.

Another voice joins them.

The bridge erupts.

+ [Continue.] -> alpha_odyssey_arrival_celebration

=== alpha_odyssey_arrival_celebration ===
Officers are on their feet.

Someone pounds both hands against a console.

A relay operator tears off their headset and throws it into the air before scrambling to catch it again.

The fleet comms explode with overlapping voices.

"We made it!"

"Contact confirmed!"

"Tell the deck crews we did it!"

+ [Continue.] -> alpha_odyssey_arrival_commands

=== alpha_odyssey_arrival_commands ===
One by one, acknowledgement signals begin flashing across the tactical display.

For a few seconds, the whole expedition glitters within that orange nebula.

+ ["All ships—welcome to the other side."] -> alpha_odyssey_end
+ ["Mark the charts. Humanity is back."] -> alpha_odyssey_end
+ ["Enjoy the view."] -> alpha_odyssey_end

=== alpha_odyssey_end ===
The fleet settles into formation.

Navigation fixes begin propagating from ship to ship.

New coordinates.

New stellar data.

New names waiting to be written onto old charts.

+ [Continue.] -> alpha_odyssey_end_2

=== alpha_odyssey_end_2 ===
For the first time in more than two centuries, a human fleet has crossed beyond the Persean Sector and survived the passage.

At the navigation hub, Sinni reaches for the controls again.

Her hands are still shaking.

She pauses.

+ [Continue.] -> alpha_odyssey_end_3

=== alpha_odyssey_end_3 ===
"Great-Grandmother," you hear her whisper.

"We're coming home."

A moment later, FOB Ithaca appears on the expedition map, flagged in Ashen Verge.

+ [Continue.] -> alpha_odyssey_map_notice

=== alpha_odyssey_map_notice ===
You can ask Sinni for guidance or open her custom starscape map of the Orion Knot through Contacts. Her map is also available in the Adept of the Stars and Waves Intel entry.

+ [Continue.] -> END
