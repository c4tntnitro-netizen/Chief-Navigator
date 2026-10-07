// Authoring mirror for the rules-driven, one-time Avici arrival scene.

VAR player_title = "Captain"

-> avici_enter


=== avici_enter ===

When your fleet exits the jump point there is nothing.

You feel the same suffocating pressure of abyssal hyperspace.

You see no evil. Hear no evil.

Speak--

+ ["What is this...?"] -> avici_view


=== avici_view ===

An impossible sight unfolds before you.

A sudden chill runs down your spine.

+ ["Nav! Hold position! Send out an emergency report to the entire fleet!"] -> avici_stop


=== avici_stop ===

"Aye! Holding position!"

You hear Sinni echo the order, and acknowledgement signals flicker across her screen.

+ ["Sinni. I need answers."] -> avici_view_3
+ ["Nav. Did... did we fail the jump?"] -> avici_view_3


=== avici_view_3 ===

Sinni opens her mouth.

But no words come out.

The system is a nightmare.

Great pillars of twisted space spill out of several jump points, warping the light around them. Within each distortion is a core of pitch-black abyssal hyperspace.

You soon realize-- your own fleet is caught in one of those pillars, exerting the same immense pressure upon your drive bubble, but somehow, impossibly-- within real space.

The distortion pillars curl around one another before converging into vast pools of abyssal hyperspace that swallow the stars behind them whole.

Somehow, the supermassive black hole in the middle looks almost innocuous by comparison.

+ ["Sinni? Sinni!"] -> avici_view_4


=== avici_view_4 ===

"Ah-- Aye, {player_title}."

+ ["Nav. Is it safe to travel outside of this... in-system hyperspace?"] -> avici_view_5


=== avici_view_5 ===

"I... don't think so," Sinni replies.

She puts a gloved hand over her mouth, staring at the boundary of the black distortion around your fleet.

"This should be impossible."

Her hands return to the console.

"Hyperspace does not 'spill' into real space. You cannot have a... pool of it sitting inside a star system."

She magnifies the edge of the distortion. Her eyes grow wider and wider as she looks over the readings on her console.

"You can't... that's not how that... works."

Sinni goes quiet.

+ ["What happens if we cross that boundary?"] -> avici_view_6


=== avici_view_6 ===

"I... don't know."

Sinni traces the boundary with one finger.

"A jump point mediates the transition between geometries. Whatever this is..."

She hesitates.

You look toward the black edge surrounding the fleet.

+ ["Could it tear us apart?"] -> avici_view_7


=== avici_view_7 ===

Sinni does not answer immediately. She types rapidly on the console in front of her.

A figure appears.

Sinni doesn't seem to like it.

"Possibly."

A few people on the bridge stop what they are doing.

"Or nothing happens. Or we transition normally. Or half the ship crosses and the other half..."

She stops.

"Don't move the fleet."

Sinni stares at the boundary for several seconds. Then her eyes sharpen.

"We test it."

* ["How?"] -> avici_view_8


=== avici_view_8 ===

"Dummy munition."

Sinni turns toward weapons.

"Something with telemetry."

Your weapons officer looks toward you.

You nod.

A few moments later, a training missile appears on the tactical display.

Sinni plots a straight course through the edge of the abyssal distortion and into the clear space beyond.

"Low velocity."

"Fire in the hole," your weapons officer calls out. "Fire in the hole! Fire in the hole!"

"Five,"

"Four,"

"Three,"

"Two,"

"One."

The munition launches. It labors through the crushing pressure of the abyssal hyperspace around it.

Everyone watches.

It crosses the boundary.

Nothing happens.

The missile continues on its course, telemetry streaming back without interruption. Until the object disappears into the distance.

For several seconds, nobody says anything.

Then everyone on the bridge turns toward you.

Sinni too.

+ ["Full speed ahead."] -> avici_end
+ ["All ships, advance slowly. Keep telemetry on."] -> avici_end
+ ["Even though I walk through the darkest valley, I will fear no evil. We advance."] -> avici_end


=== avici_end ===

"Aye!" your bridge echoes.

For one long moment, the fleet hangs at the boundary.

Then your ships cross the distortion without incident.

The pressure on the drive field vanishes.

Behind you, the black pool continues to churn as though nothing remarkable has happened.

Later, Sinni proposes a name for the system that sticks.

Avici.

The lowest hell.
