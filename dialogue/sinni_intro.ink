// Adept of the Stars and Waves — Sinni's bar introduction
// Authoring/proofreading source for data/campaign/rules.csv.
// Runtime side effects are marked with tags and implemented by
// ChiefNavigatorSinniIntroCMD.

VAR player_is_female = false
VAR player_name = "Ang"
VAR player_title = "Captain"

-> start

=== prompt ===
A waitress approaches you with a note in hand.
+ [Take the note from the woman drinking tea at the bar.] -> start

=== start ===
"Captain {player_name}?" The waitress hands you the slip of paper. "The woman at the end of the bar wants to speak to you."

Waiting there is a tall woman with silvery hair and a dusky complexion, surrounded by untouched drinks and shots. As you make your way over, you catch a number of patrons shooting glances at you. She holds a thick mug filled with steaming green tea between her hands and watches you approach with an unreadable expression.

+ [Take one of the drinks.] -> drink
+ [Hold the note up. "You have a job for me?"] -> note

=== drink ===
You gesture at the vast array of drinks by her side.

Wordlessly, she picks out a shot glass from around her, and sets it in front of you.

You down it.

{ player_is_female:
    A man swears loudly behind you.
    + ["Thanks."] -> drink_2_male
- else:
    A man whistles loudly behind you.
    + ["Thanks."] -> drink_2_male
}

=== drink_2_male ===
"They keep buying them for me," the woman says, taking another sip from her mug. Her oversized sleeves have the cut and patterning of some Eventide cantons. She gestures toward the remaining drinks. "As you please."
+ ['Redistribute' the rest of the drinks.] -> drink_3_male

=== drink_3_male ===
"Hey guys?" You say, leaning towards your bodyguards. "The counter's looking kind of cluttered."

They don't need to be asked twice. Damn vultures. The woman's would-be suitors are clearly unhappy about the arrangement, but what are they going to do? You're the only one with an armed retinue.
-> drinks_cleared

=== drinks_cleared ===
By the time your bodyguards are finished, the bar is considerably emptier.
+ ["So what's this note?"] -> note

=== note ===
The woman takes a sip from her mug.

"Yes."

She looks you over for another moment. "I need a fleet."
+ ["For what?"] -> note_2

=== note_2 ===
She takes another sip.

"To go to a place."
+ ["Where?"] -> note_3

=== note_3 ===
"Far."
+ [Make a coin gesture.] -> coin
+ ["How much?"] -> coin

=== coin ===
She holds her arms out wide.
+ ["I can work with that."] -> deal
+ [Nod approvingly.] -> deal

=== deal ===
The woman sets her mug down.

"I need to go outside the Sector."

That gets your attention.
+ ["You don't want to go into Abyssal Hyperspace."] -> abyss

=== abyss ===
The woman cocks her head.

"You've been?"
+ ["There are... things out there."] -> monsters
+ ["Everyone knows that."] -> knowledge

=== monsters ===
Her eyes light up.

"What things?"
+ ["These... creatures. Made of pure energy. Hostile."] -> dwellers
+ ["These... machines. Called 'Threat'. It's some kind of artificial, self-replicating swarm."] -> threat

=== knowledge ===
She takes another sip from her mug. This time she doesn't reply.
+ ["And you're still planning to go?"] -> still_going

=== dwellers ===
She stares at you.

"Alive?"

"...Yes. Absolutely."

Her attention sharpens.

"Interesting."
+ ["That's not the word I'd use."] -> still_going

=== threat ===
She goes very still.

"Self-replicating?"

"That's what it looked like."

She takes a slow sip from her mug. You can see the cup is empty.

+ ["You don't seem worried."] -> still_going

=== still_going ===
"I'm planning an expedition," the woman says. She pours herself more tea. "Outside the Sector. Far enough that most captains would turn back before the charts stopped helping."

She studies you over the rim of her cup.

"You look like someone who can keep a fleet alive after the edge of a map."
+ ["And you need my fleet."] -> still_going_2

=== still_going_2 ===
"Yes."

The woman ponders her cup.

"Come to Eventide. I have an office overlooking the mirror harbor. We can take tea there, and I can explain what the expedition actually entails."
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

[Reward: +2 Burn speed cap; +1 base Burn level.]
+ [Leave.] -> leave

=== eventide_arrival ===
Your shuttle descends through Eventide's measured night, past ranks of orbital mirrors and the lights of the harbor. Sinni's address leads to a quiet office above the water, furnished in dark wood and screened from the city by broad windows.

She is already seated at a low table. A second cup waits opposite her.

"You came."

Sinni fills your cup, then rests both hands around her own.

"Now I can tell you why I needed a fleet."
+ ["I'm listening."] -> eventide_plan

=== eventide_plan ===
"The Hegemony is assembling an expedition beyond the Persean Sector," Sinni says. "They are following records left by the Fourteenth Battlegroup. Records of the forces they abandoned when the gates failed."

She turns her cup once against the grain of the table.

"My ancestor Helen served with Task Force Spartan at their destination. When the Fourteenth abandoned Ithaca and crossed the Abyss, she went with them. She wrote an account of the journey."

Sinni lays one hand on the closed folio beside her.

"Their commanders know there was a destination. Helen's record tells me how the survivors came out. I rebuilt the route in reverse."
+ ["Where are they going?"] -> eventide_destination

=== eventide_destination ===
"Forward Operating Base Ithaca. A Domain macrobase in the Orion Knot, built to service the forces that carried the Fourteenth into the Sector."

The lights beyond the window slide as Eventide's mirrors turn overhead.

"If anything remains there, the Hegemony intends to claim it. We can arrive first."
+ ["How do we reach it?"] -> eventide_route

=== eventide_route ===
"Waypoint Troy," Sinni says. "A temporary terminus at the far southeast edge of the Sector. From there I can guide a small expedition through the Abyss and into the Orion Knot."

She slides a manifest across the table.

"The route will only hold an active fleet worth thirty deployment points. There is an anchorage at Troy where we can store everything else until we return."
+ ["What should I bring?"] -> eventide_preparation

=== eventide_preparation ===
"Efficiency Overhaul wherever it fits. Insulated Engine Assembly, or anything else that reduces your sensor profile. At least one dedicated supply ship and one fuel ship."

Sinni takes a measured sip.

"The route is survivable. Being noticed along it may not be."
+ [Add Waypoint Troy to the navigation plot.] -> eventide_complete

=== eventide_complete ===
Sinni touches two fingers to the table. A point appears at the far southeast edge of the Sector map, followed by the identification code for a cooperative anchorage.

"Waypoint Troy," she says. "Meet me there when your fleet is ready."

Waypoint Troy is marked on your navigation plot. # action:brief
+ [Leave Sinni's office.] -> END

=== leave ===
# action:leave
-> END
