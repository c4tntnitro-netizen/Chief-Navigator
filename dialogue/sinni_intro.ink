// Adept of the Stars and Waves — Sinni's bar introduction
// Authoring/proofreading source for data/campaign/rules.csv.
// Runtime side effects are marked with tags and implemented by
// ChiefNavigatorSinniIntroCMD.

VAR player_is_male = true
VAR player_name = "Ang"
VAR player_title = "Captain"
VAR troy_dp_limit = 50 // Runtime value comes from Chief Navigator's LunaLib fleet-limit setting.

-> start

=== prompt ===
A waitress approaches you with a note in hand.
+ [Take the note from the woman drinking tea at the bar.] -> start

=== start ===
"Captain {player_name}?" The waitress hands you the slip of paper. "The woman at the end of the bar wants to speak to you."

Waiting there is a tall woman with silvery hair, surrounded by untouched drinks and shots. As you make your way over, you catch a number of patrons shooting glances at you. She holds a thick mug filled with steaming green tea between her hands and watches your approach with a still, neutral expression.

+ [Take one of the drinks.] -> drink

=== drink ===
You gesture at the vast array of drinks by her side.

Wordlessly, she picks out a shot glass from around her, and sets it in front of you.

You down it.

{ player_is_male:
    A man swears loudly behind you.
    + ["Thanks."] -> drink_2_male
- else:
    A man whistles loudly behind you.
    + ["Thanks."] -> drink_2_male
}

=== drink_2_male ===
"They keep buying them for me," the woman says, taking another sip from her mug. Her oversized sleeves have the cut and patterning of some Eventide cantons. She motions toward the remaining drinks. "As you please."
+ ['Redistribute' the rest of the drinks.] -> drink_3_male

=== drink_3_male ===
"Hey, guys," you say, leaning toward your bodyguards. "Counter's looking kind of cluttered."

Your men don't need to be asked twice. The marines make quick work of the array of shots and cocktails. The woman's would-be suitors are clearly unhappy about the arrangement, but a glance at your armed retinue appears to settle things.

By the time your bodyguards are finished, the bar is considerably emptier.
+ ["So what's this note?"] -> note

=== note ===

The woman sets down her mug and draws a dataslate from her sleeve. She tilts it toward you, and a chart opens on your own device. The Persean Sector appears, then shrinks as the view pulls back and pans southwest. Familiar systems slide off the screen. The map keeps scrolling into Abyssal Hyperspace, long after the last familiar system has disappeared.

A red warning box flashes over the chart, but the map continues moving beneath it.

DOMAIN NAVIGATION ADVISORY WARNING # font:small_insignia # style:archive_warning

\/\/ datastream resetting... # font:small_insignia # style:archive_warning

Far beyond the Sector, a small cluster of star systems comes into view. You barely have time to register their names before another message obscures them.

NAVIGATION ACCESS DENIED. Please apply for augmented Domain Interstellar Navigation Access to enable navigation for this area. # font:small_insignia # style:archive_denied

That gets your attention.

+ ["You're trying to go outside of the sector."] -> abyss

=== abyss ===

"Yes."

The woman cocks her head.

"You've been?"

+ ["There are... 'things' out there."] -> monsters

=== monsters ===
Her eyes light up.

"What things?"
+ ["These creatures. Made of pure energy. Hostile."] -> dwellers
+ ["These machines. Called 'Threat'. It's some kind of artificial, self-replicating swarm."] -> threat

=== dwellers ===
She looks at you.

"Creatures? Alive?"

+ ["Absolutely."] -> endgame

=== threat ===
She goes very still.

"Self-replicating?"

+ ["That's what it looked like."] -> endgame

=== endgame ===
The woman turns towards the bar, staring a hole into the wall. She takes a slow sip from her mug. Her empty mug.

+ ["You don't seem worried."] -> still_going

=== still_going ===
"I'm planning an expedition. I need your fleet," the woman says.

She studies you over the rim of her cup.

"Take us to Eventide. I have an office overlooking the mirror harbor, where I will elaborate further."
-> decision

=== decision ===
* ["Right. What's your name?"] -> name
* ["One last thing... why did you pick me?"] -> why_me
+ [Take Sinni on as an officer.] -> accept
+ [Decline for now.] -> leave

=== name ===
"Sinni. Koyama Sinni." She holds her hand out. "I'm a navigator, of sorts. And I know you, {player_title} {player_name}."

-> decision

=== why_me ===
Sinni cocks her head.

"You smell of the starry sea."

-> decision

=== accept ===
Sinni joins your fleet as an officer. Before leaving the bar, she transfers a private Eventide address and a suggested time to your comm pad.

"Tea. Eventide. Then I'll show you where we're going."

Eventide is marked on your navigation plot. # action:accept

[Quest started: ADEPT OF THE STARS AND WAVES]

[Quest started: ONE MORE HORIZON]

+ [Leave.] -> leave

=== eventide_arrival ===
Your shuttle descends through Eventide's sky, past ranks of orbital mirrors and the lights of the harbor. Sinni's address leads to a quiet office above the water, furnished in dark wood and screened from the city by broad windows.

A navy-blue banner hangs behind the desk, with a spear emblazoned upon a shield. Inscribed below the spear and shield is a short motto and a designation:

"XIV Battlegroup, 6th Special Operations Force

Task Force Spartan

Molon Labe!" # font:victor16

Sinni strides in, then sits across from you. A holoscreen flickers on her desk and she gives the updates on the screen a single glance before closing it.

"Thank you for making this trip."

Sinni fills your cup, then rests both hands around her own.

"Now. About your fleet."

+ ["I'm listening."] -> eventide_plan

=== eventide_plan ===
"The Hegemony is assembling an expedition beyond the Persean Sector," Sinni says. “When the Gates failed, the Fourteenth was stranded beyond the Persean Sector. Facing annihilation, the Fourteenth fled Ithaca to begin their exodus through the Persean Abyss. HEGCOM believes now is the time to make the journey back.”

She turns her cup once against the grain of the table.

"But I have something they don't. Records of my great-grandmother. Lone survivor of the Fourteenth's elite detachment, Navigator Helen Argyros of Task Force Spartan."

Sinni gestures at the blue and white livery behind her. It hangs in the dim light of the office.

+ ["Ithaca?"] -> eventide_destination

=== eventide_destination ===

"Forward Operating Base Ithaca. A Domain macrobase in the Orion Knot Sector, built to support Domain Navy forces in transit to frontier sectors."

Light sweeps across the office as Eventide's mirrors turn overhead. The autoshades descend, returning the room to its former dimness.

"If anything remains there, the Hegemony intends to claim it."

Sinni leans forward, weaving her fingers together. "We have Helen's records. I can get us there faster."

+ ["How do we reach it?"] -> eventide_route
+ ["Stealing from the Heggies? I like this plan."] -> eventide_route_0
+ ["Ludd preserve us. You want to race the Hegemony through the Abyss?"] -> eventide_route_1

=== eventide_route_0 ===
"Careful." Sinni leans back, resting her cup against her cheek. "Were I more patriotically inclined, Captain..."

+ ["Alright, alright. Where do we get started?"] -> eventide_route

=== eventide_route_1 ===
"Scared, Captain?"

+ ["More like intrigued. Where do we get started?"] -> eventide_route
+ ["Excited. Where do we get started?"] -> eventide_route

=== eventide_route ===
"Waypoint Troy," Sinni says. "A terminus. From there I can guide an expedition through the Persean Abyss and into the Orion Knot Sector."

Another sweep of the mirrors. Light, then dark.

She slides a dataslate across the table, and a manifest pops up.

"The route will only hold a small fleet. Up to {troy_dp_limit} deployment points. There is an anchorage at Troy where we can store everything else until we return."

+ ["What should I bring?"] -> eventide_preparation
+ { troy_dp_limit >= 240 } ["Doesn't seem like much of a limitation."] -> eventide_little_limit

=== eventide_little_limit ===
"Thank you for your praise." Sinni bows behind her sleeves.
+ ["What should I bring?"] -> eventide_preparation

=== eventide_preparation ===
"I'd recommend you tool your ships for operational efficiency. Sensor profile reduction. One supply ship and one fuel ship. At least."

Sinni cross-references the expedition manifest with your fleet profile. Worryingly, many of your largest, most capable ships are automatically greyed out.

"Being noticed may not be survivable. We should avoid engagements."

+ [Add Waypoint Troy to the navigation plot.] -> eventide_complete

=== eventide_complete ===
Sinni touches two fingers to her desk. The holoscreen springs up again, mapping the Persean Sector. A point appears at the far southeast edge of the Sector map, followed by the identification code for a cooperative anchorage.

"Depart when you are ready."

Waypoint Troy is marked on your navigation plot. # action:brief
+ [Leave Sinni's office.] -> END

=== leave ===
# action:leave
-> END
