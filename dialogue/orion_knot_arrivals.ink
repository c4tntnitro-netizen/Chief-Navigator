// Authoring mirror for Sinni's Sanzu arrival vignette.
// Runtime authority: data/campaign/rules.csv.
// The existing Troy crossing and Avici scenes remain in their own sources.
// Every arrival waits for the common no-active-hostile-tracking safety gate.
// Completion is recorded only on the final choice.
// Ashen Verge, Devoured Reach, and Last Light have no arrival vignettes.

VAR player_title = "Captain"
// Runtime reads Avici's native visited flag, including a deferred arrival scene.
VAR saw_avici = false

-> preview

=== preview ===

* [Sanzu.] -> sinni_sanzu
* [End preview.] -> END


=== sinni_sanzu ===

“{player_title},” Sinni calls from navigation. One hand presses against her headset. “Crossing the system jump point now.”

+ [“Open opticals.”] -> sinni_sanzu_opticals
+ [“Let's see where we are.”] -> sinni_sanzu_opticals


=== sinni_sanzu_opticals ===

At your command, the bridge shutters begin to unfold.

Blue-white light spills through the widening seams. The optical filters darken automatically, reducing the glare until the stars beyond resolve into view. On Sinni's display, the fleet's drive bubble makes final contact with realspace.

Then you hear something you shouldn't.

Was that... wind?

A low rushing sound passes through the hull.

You have just enough time to look up before the entire ship is thrown sideways.

Loose crew go tumbling across the bridge. Restraints snap taut. Your chest slams against your belts hard enough to drive the air from your lungs.

Around you, alarms erupt. Sinni has both hands braced against her console.

“What the—”

The ship groans around you, metal complaining against stresses it was never meant to feel. Then, finally, the groaning stops.

The rushing doesn't.

For now, at least, the chaos has subsided.

+ [“Damage!”] -> sinni_sanzu_damage


=== sinni_sanzu_damage ===

“D-deck reports are still coming in, {player_title}!”

Your DCA stares at the diagnostic board, taps a few keys, then stares again.

“...No damage.”

The alarms slowly peter away.

“No hull deformation. No pressure loss.”

“Drive field stable,” Sinni calls out.

She is looking toward the forward opticals.

Something outside is moving.

At first, it looks like a shifting nebula, thick light-blue dust streaming over your drive bubble like an ocean flowing across glass.

Then your fleet's drive bubble breaks free of the current.

+ [“Holy shit.”] -> sinni_sanzu_river
+ [“Nav... What is this?”] -> sinni_sanzu_river


=== sinni_sanzu_river ===

“It... it has to be...”

Sinni is staring out the opticals, transfixed.

It is an endless current.

The nebula clouds are forming a river, light-years long, stretching far beyond the horizon. You can see planets caught inside it, even a distant star, their silhouettes warped behind sheets of pale blue-white dust.

Sinni stares at the navigation display for several seconds before her hands begin moving again.

“Give me time.”

You have your fleet hold position for a day. The current, once your nav team runs their calculations, is trivial to counteract with sublight travel, and your tac officer delivers a report expressing confidence in the concealment capabilities of the flowing nebula clouds. Just keep a careful watch against the current, he warns. Enemies might ride the river to attack.

You later find Sinni sitting at the lip of the opticals. Datapads surround her, complicated equations and reference material glowing all around her. There is a cup of tea next to her, but it looks like it's gone cold.

“It's a black hole,” Sinni says without turning around. She must have seen your reflection in the opticals. “The current is nebular material being pulled in from the Alpha Odyssey and Ashen Verge systems.”

+ [“So this system is a black hole system?”] -> sinni_sanzu_avici


=== sinni_sanzu_avici ===

“No.”

{saw_avici:
    “Do you remember that black hole system galactic south of here? The one with abyssal hyperspace spilling into realspace?”

    + [“How could I forget.”] -> sinni_sanzu_avici_seen
- else:
    “Based on my calculations, there is... something like a black hole galactic south of here.” Sinni shakes her head. “It's the only model that makes sense. But the size of that black hole...”

    + [“How big?”] -> sinni_sanzu_avici_unseen
}


=== sinni_sanzu_avici_seen ===

Sinni expands the regional map across the opticals.

Alpha Odyssey.

Ashen Verge.

Your present system.

Then Avici.

Thin blue lines begin appearing across the map, tracing the motion of the nebular material your fleet has been sailing through. Every line bends toward the same system.

“Avici.”

She looks out through the opticals.

“The current isn't coming from here. We're crossing material already falling toward it.”

-> sinni_sanzu_avici_common


=== sinni_sanzu_avici_unseen ===

“Something big. Very big.”

She overlays the current's direction across the regional chart.

The vectors from Alpha Odyssey and Ashen Verge continue southward, converging beyond the limits of your explored map.

“The model only works if there's a gravitational source out there vastly larger than anything in these systems.”

Sinni stares at the projection.

“But to draw material across interstellar distances... if my estimate is even close...”

She trails off.

+ [“How large?”] -> sinni_sanzu_avici_unseen_2


=== sinni_sanzu_avici_unseen_2 ===

“Ultramassive. At least.”

Sinni looks toward the southern edge of the chart.

“Nothing smaller fits the model.”

-> sinni_sanzu_avici_common


=== sinni_sanzu_avici_common ===

Sinni draws another line through Alpha Odyssey, then another through Ashen Verge.

“The nebulae there are moving too. Slowly enough that you'd never notice from inside them, but they're all drifting in the same direction.”

+ [“So this whole river is feeding it.”] -> sinni_sanzu_avici_3
+ [“It's pulling material out of entire systems.”] -> sinni_sanzu_avici_3
+ [“Everything is being drawn away...”] -> sinni_sanzu_avici_3


=== sinni_sanzu_avici_3 ===

“It's just so much bigger than us,” Sinni says, holding one hand toward the current. “In a year, more material will pass through this river than the entire mass of Eventide. Our grandchildren's grandchildren would never live to see the first of what passes us today reach the void.”

For a while, she simply watches it.

Then Sinni turns and fixes you with a look.

“{player_title}, where you move, the Sector trembles. The tides turn in your wake; the sea calms at your command.”

+ [“...But?”] -> sinni_sanzu_small
+ [“I like this conversation.”] -> sinni_sanzu_small


=== sinni_sanzu_small ===

Sinni gives a small smile.

“And yet... even if you were to light the entire Sector ablaze, nothing would change out here.”

She turns back toward the opticals. The pale current rolls on, utterly indifferent.

“The world moves on.”

Sinni watches it for another moment.

“Here, the sky is vast and the Emperor is far away.”

+ [“Pardon?”] -> sinni_sanzu_final
+ [“Another Eventide saying?”] -> sinni_sanzu_final


=== sinni_sanzu_final ===

“Never mind.”

Sinni gathers her datapads and finally notices the forgotten cup of tea beside her.

“Tell the fleet we can move at any time. This system is safe to traverse.”

She picks up the cold tea anyway and disappears back toward her quarters.

// Runtime's final Continue commits complete_sanzu before dismissing the scene.
-> END
