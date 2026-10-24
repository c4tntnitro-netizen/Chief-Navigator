// Authoring mirror for the rules-driven Waypoint Troy interaction.
VAR troy_dp_limit = 50 // Runtime value comes from the LunaLib fleet-limit setting.

=== anchorage ===
The cooperative expedition anchorage holds its docking arms open. Its automated stores can retain ships and cargo until your return.

Sinni's route can carry no more than {troy_dp_limit} deployment points of ships.

+ [Access ship and cargo storage.]
+ [Ask Sinni for advice about preparing the expedition.] -> advice
+ [Authorize Sinni to open the route.]
+ [Leave.]

=== advice ===
Sinni is on the bridge, hunched over a console screen. She is looking over her proposed expedition route through the Persean Sector one more time.

You ask her for advice in preparing the fleet for the journey to the Orion Knot.

"Refit your ships for operational efficiency. Any augmentations that reduce our sensor profile will make our expedition less dangerous. Bring at least one dedicated supply ship and one fuel ship. Fuel and supplies may be challenging to come by in the Orion Knot. If either runs dry out there, no one is coming to refill it."

Sinni flicks through your fleet's specifications.

"And I am no tactician," she says, studying your ships' armaments. "But..."

+ ["But?"] -> advice_2
+ [You sense Sinni has more to say.] -> advice_2

=== advice_2 ===

"I would concentrate as much power as you can into your flagship." Your chief navigator bows her head, holding her hands within those wide canton sleeves. "Just my thoughts."

+ ["Appreciated."] -> anchorage
+ ["Sure."] -> anchorage
+ [Leave.]

=== transit ===
Sinni guides your fleet through a treacherous series of slipstreams and gravity boosts. By the time you approach the far end of the passage, half your supplies and fuel are gone.

+ [Continue.]

=== alpha_odyssey_arrival ===
The bridge opticals show nothing.

No stars, no dust. No light, no waves. Not even the faint distortion of hyperspace. Only an endless, depthless, all-consuming void that clings to your flagship from every direction. It is like breathing water. It is being buried alive.

This suffocating darkness has been your fleet's home for nearly a month.

Sinni leans over the navigator's console, her face washed pale by the light of its instruments. Her hands never stop moving, making tiny corrections whenever the slightest hint of signal or motion drifts into her sensor's view.

Something scrapes along the drive bubble. The vibration carries through to the hull: a slow, grinding shriek that rises and rises, before plunging into a guttural moan. Those extranatural sounds and scrapes never got easier to listen to. Not even after a month. Your bridge officers exchange uneasy glances. Another impact sends a shudder through the deck and a flinch through your crew.

"Hold together," Sinni whispers.

+ [Continue.] -> alpha_odyssey_arrival_breakthrough

=== alpha_odyssey_arrival_breakthrough ===
There is no charted path through the terminus.

Only half-deciphered coordinates, Helen's logs, and Sinni's navigational instincts guide the fleet through the darkness. The instruments offer little that can be trusted. It is a trial of faith as much as it is a test of science.

A warning flashes across your console.

Ahead of the fleet, something appears on the bridge opticals.

At first it is only a pinprick of white against the black. But that light grows and grows, spilling fire across the darkness. You can finally see it. The jump point.

It is blinding.

The fleet bursts through.

Darkness flees.

A brilliant orange star fills the bridge opticals, immense and incandescent, its light washing across worlds that have turned in silence for generations. Bands of glowing dust stretch through the system like molten rivers. Distant planets gleam beneath the light of the sun, while ancient debris fields catch the radiance and scatter it in a million golden sparks.

No human being has gazed upon this sky since the Collapse. Now you are here.

+ ["This is it. Outside the Persean Sector."] -> alpha_odyssey_arrival_2

=== alpha_odyssey_arrival_2 ===

"YEAH!" Sinni shoots out of her chair, both fists in the air.

For one stunned heartbeat, the entire bridge stares at her.

Then somebody else cheers.

Another voice joins them.

The bridge erupts.

Your officers are on their feet. Someone pounds both hands against a console. A relay operator tears off their headset and throws it into the air before scrambling to catch it again. The fleet comms explode with overlapping voices in a complete breakdown of communications discipline. Cheers. Prayers. Gratitude.

One by one, acknowledgement signals begin flashing across the tactical display.

+ ["Fleet-- Welcome to the Orion Knot Sector."] -> alpha_odyssey_end

=== alpha_odyssey_end ===
Your fleet settles into formation.

Navigation fixes begin propagating from ship to ship.

New coordinates.

New stellar data.

New names and paths and routes waiting to be written onto forgotten star charts.

+ [Continue.] -> alpha_odyssey_end_2

=== alpha_odyssey_end_2 ===
For the first time in more than two centuries, a fleet has crossed beyond the Persean Sector and survived the passage.

At the navigation hub, Sinni reaches for the controls again. Her hands are still shaking.

She pauses. And then she fixes your next navigation destination.

Forward Operating Base Ithaca. Ashen Verge system.

A forgotten remnant of the mighty 14th Battlegroup. There, the Battlegroup should have abandoned countless ships, vast stores of supplies, and more. All yours for the taking.

You can ask Sinni for guidance or open her custom starscape map of the Orion Knot through Contacts. Her map is also available in the Adept of the Stars and Waves Intel entry.

+ [Continue.] -> END
