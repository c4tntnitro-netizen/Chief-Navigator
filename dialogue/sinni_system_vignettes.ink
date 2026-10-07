// ============================================================
// SINNI - STELLAR SYSTEM VIGNETTES
// ============================================================
//
// Runtime should own:
// - whether Sinni is currently in the fleet
// - one-time completion state for each exploration category
// - tracking which available categories have been encountered
// - suppressing vignettes for already-completed categories
// - listing nearby systems for unfinished categories in the sightseeing Intel
// - omitting a category if no eligible system of that kind exists anywhere
//   in the generated Sector
// - retiring legacy final-request waypoints without consuming progress
// - triggering sinni_all_types_complete once all available categories are done
//   and one full campaign day has passed since the final scene's closing choice
// - persisting that delay across save/load without restarting it
// The former one-category-left request is retired and kept below as an archive.
//
// TRACKED CATEGORIES:
//
// 1. Nebula
// 2. Black Hole
// 3. Neutron Star
// 4. White Dwarf
// 5. Main-Sequence Star
//      - red dwarf
//      - orange star
//      - yellow star
// 6. Giant or Massive Star
//      - orange giant
//      - red giant
//      - blue giant
//      - blue supergiant
//      - red supergiant
// 7. Trinary Star System
//
// Sinni may record exact stellar classifications in her personal chart.
// Brown dwarf is no longer tracked. Giant and massive stars share one
// category, while their authored scenes remain subtype variants.
//
// SINNI VOICE:
//
// Sinni is an expert interstellar navigator.
//
// She understands what she is looking at before the player does.
// Her explanations should be concise and practical, with emphasis on what
// astronomical phenomena mean to someone actually moving through space.
//
// Maritime language is natural to her:
// chart, bearing, wake, tide, current, weather, sea room, horizon,
// sounding, dead reckoning, landfall.
//
// Do not make every sentence nautical.
// She is a mariner, not someone deliberately speaking in metaphors.
//
// Sinni's normal exploration detours are negligible and made under her
// ordinary navigational discretion.
//
// ============================================================

VAR player_title = "Captain"
VAR player_name = "Starsector"
VAR player_gender = "male"

// Runtime-only secret response gate. This becomes true after the player
// recovers the Devoured Ring's information about a path toward the Domain
// heartland; the sightseeing quest never advertises this condition.
VAR devoured_ring_path_known = false

VAR system_name = "Unknown"

// Exact stellar subtype supplied by runtime.
// Expected values:
// red_dwarf
// orange_star
// yellow_star
// orange_giant
// red_giant
// blue_giant
// blue_supergiant
// red_supergiant
VAR system_subtype = "yellow_star"

// Archived values for the retired final request.
VAR missing_type = "Massive Star"
VAR missing_system = "Unknown"
VAR missing_distance = "12.4"

-> sinni_system_menu


// ============================================================
// PREVIEW MENU
// ============================================================

=== sinni_system_menu ===

SINNI - SYSTEM VIGNETTES

* [Nebula.] -> sinni_nebula
* [Black Hole.] -> sinni_black_hole
* [Neutron Star.] -> sinni_neutron_star
* [White Dwarf.] -> sinni_white_dwarf
* [Main-Sequence Star.] -> sinni_main_sequence
* [Giant or Massive Star - Giant Variant.] -> sinni_giant_star
* [Giant or Massive Star - Massive Variant.] -> sinni_massive_star
* [Trinary Star System.] -> sinni_trinary
* [Completion.] -> sinni_all_types_complete
* [End preview.] -> END


// ============================================================
// NEBULA
// ============================================================

=== sinni_nebula ===

The stars disappear one by one as your fleet pushes deeper into {system_name}.

Soon there is nothing outside but darkness.

It is a sparse shift right now. Only a skeleton crew mans the panels, maintaining your fleet trajectory. You find Sinni at the navigation hub. She's staring out the command opticals at the blackness.

Great banks of dust drift past the bridge windows, sometimes translucent, sometimes thick enough to swallow the running lights of ships only a few kilometers away.

She is fiddling with a knob.

With a sudden blinding light, the bridge windows turn pure white. Then, slowly, shapes come into view. You are staring out at a great nebula, its vast folds and filaments traced and projected across the bridge windows in stunning detail.

"Look."

She seems pleased by this.

* ["What am I looking at?"] -> sinni_nebula_center

=== sinni_nebula_center ===

"We ingest the dust emissions around us. What you're seeing is a reconstructed far-infrared volume."

Sinni turns the knob another fraction. The image shifts. A dark ridge separates into two, revealing another immense sheet of dust behind it.

"The dust absorbs starlight and gives the energy back at longer wavelengths."

Another filament slips slowly out from behind the first. You realize that the nebula isn't merely scrolling past you. It is unfolding.

* ["It's moving."] -> sinni_nebula_moving

=== sinni_nebula_moving ===

"No. We are."

Sinni points toward one of the nearer clouds as it drifts rapidly across the structures behind it.

"Parallax. That cloud and the one behind it looked joined when we entered the system."

She checks the sensor reconstruction.

"They're about half a light-year apart."

Sinni's gaze fixes on the cloud before her.

"What you're seeing is depth becoming time. We cross in hours what the cloud takes millennia to change."

Another immense fold separates from the darkness.

"Something larger than the entire Samsarra solar system, breathing and twisting in the starry sea."

The fleet continues forward.

Another black wall separates into layers.

* ["That makes your job harder."] -> sinni_nebula_harder

=== sinni_nebula_harder ===

"Yes. But don't worry."

She indicates a series of faint lines.

"We can navigate by velocity. Gravity. Time."

Another bank of dust rolls across the enhanced opticals, swallowing several distant stars.

"We stride forward by the last light in our path."

+[skill gained: nebula plotting: -1% sensor profile.]

* [Continue.] -> sinni_system_menu

// BLACK HOLE

=== sinni_black_hole ===

You return to the bridge to find most of the navigation overlays missing.

The fleet is still precisely on course. Collision warnings remain active, the tactical plot is untouched, and the navigation computer continues counting down to your destination.

Only the forward observation feed has been cleared.

At its center hangs the black hole.

Its shadow is almost circular, but not quite. One side is subtly flattened, the opposite edge drawn farther out, giving the darkness a lopsided shape that becomes more apparent the longer you stare at it.

A band of incandescent matter cuts across its face.

That same band seems to climb impossibly upward behind the black hole, folding over the darkness into a brilliant arc before plunging down the other side. Beneath it, thinner rings of light crowd against the shadow.

Sinni stands at the navigation console, watching it.

* ["What happened to the overlays?"] -> sinni_black_hole_overlays

=== sinni_black_hole_overlays ===

"Turned them off."

* ["I gathered that."] -> sinni_black_hole_why
* ["Isn't that dangerous?"] -> sinni_black_hole_danger
* ["Genius. No warnings means no enemies."] -> sinni_black_hole_object_permanence

=== sinni_black_hole_why ===

"They were in the way."

She points toward the observation feed.

Your trajectory carries the fleet safely past the black hole before bending outward again.

The course passes almost perfectly beneath the distorted ring.

* ["Did you plan this?"] -> sinni_black_hole_route

=== sinni_black_hole_danger ===

"No."

Sinni taps one of the secondary displays.

The navigation computer is still running. Collision warnings remain active, the tactical plot is untouched, and your projected trajectory remains exactly where it was.

"I only turned off the things between us and the window."

She points toward the observation feed.

Your trajectory carries the fleet safely past the black hole before bending outward again.

The course passes almost perfectly beneath the distorted ring.

* ["Did you plan this?"] -> sinni_black_hole_route

=== sinni_black_hole_object_permanence ===

"No."

Sinni looks at you.

"They can still see us."

* ["Damn."] -> sinni_black_hole_object_permanence_2

=== sinni_black_hole_object_permanence_2 ===

"Sorry."

She points toward the observation feed.

Your trajectory carries the fleet safely past the black hole before bending outward again.

The course passes almost perfectly beneath the distorted ring.

* ["Did you plan this?"] -> sinni_black_hole_route

=== sinni_black_hole_route ===

"Yes."

* ["For the gravity assist?"] -> sinni_black_hole_assist

=== sinni_black_hole_assist ===

"Mostly."

You wait.

"Adds one minute, forty-one seconds."

* ["So you planned this for the view."] -> sinni_black_hole_view

=== sinni_black_hole_view ===

"Sorry."

You look toward the black disk.

The longer you stare at it, the less the image seems to make sense.

The accretion disk cuts straight across the darkness like a burning horizon, yet another image of it rises behind the black hole and curls over the top. Closer to the shadow, thinner bands of light seem to fold over themselves again and again.

* ["What am I actually looking at?"] -> sinni_black_hole_explain

=== sinni_black_hole_explain ===

"Look at this disk."

Sinni reaches down, and picks up your wrist. Carefully, she traces the disk's light, circling around the dark sphere in the center, until she ends pointing toward the darkness in the center.

"The event horizon is smaller. That's the shadow."

She draws a smaller circle within it.

"Light can pass much closer than this and still escape. Closer than that, it can orbit the hole."

Her finger moves inward.

"Closer still..."

She lets her hand fall.

"It doesn't come back."

* ["So this is what a black hole actually looks like."] -> sinni_black_hole_actual

=== sinni_black_hole_actual ===

"Yes."

Sinni glances toward you.

"Change our bearing and the view changes with it."

The warped disk crawls slowly around the shadow as your fleet continues along its course.

* ["Every sea has somewhere you don't sail."] -> sinni_black_hole_sea

=== sinni_black_hole_sea ===

Sinni looks toward the shadow.

For a while, neither of you says anything.

* ["Keep us safe, Sinni."] -> sinni_black_hole_response

=== sinni_black_hole_response ===

"Acknowledged."

You turn to leave, but Sinni stays.

She makes a minute correction to the fleet's bearing. But she doesn't take her eyes off the sight before her. 

+[skill gained: black hole mastery: -10% damage from black holes.]

* [Continue.] -> sinni_system_menu

// NEUTRON STAR

=== sinni_neutron_star ===

"Come starboard to zero-eight-five! Down ten!"

"Starboard zero-eight-five, down ten, aye!"

The bridge is a mess.

Your flagship rolls and pitches beneath you, dragging the fleet's drive bubble along with it. Somewhere behind the navigation pit, relay officers echo Sinni's commands across the fleet.

Red warnings flash across every display.

Ahead, the neutron star turns.

* ["Sinni! Are we going to make it?!"] -> sinni_neutron_star_make_it

=== sinni_neutron_star_make_it ===

"Yes, {player_title}!"

Sinni's eyes remain fixed on her console. The warning lights stain her silver hair blood-red.

A timer reaches zero.

"Next beam incoming!" Sinni yells. "Brace!"

"Brace!" the ship's PA echoes.

Everyone is already strapped in. Hands still tighten around restraints and console edges.

Through the command opticals, half the bridge windows sear white.

The pulsar beam catches the fleet.

Your drive field takes the worst of it, spreading the radiation load across the bubble, but the impact still shudders through the flagship. Displays flicker. Somewhere aft, something heavy comes loose with a distant metallic crash.

Then the light passes.

For half a second, the bridge is quiet.

Sinni is already looking at the clock.

"Thirty-one seconds."

* ["Until what?"] -> sinni_neutron_star_again

=== sinni_neutron_star_again ===

"Again."

You sigh. Out of relief or otherwise, you're not sure anymore.

Then you grit your teeth.

* ["Stand by for the next maneuver!"] -> sinni_neutron_star_again_2

=== sinni_neutron_star_again_2 ===

"Aye!" comes the answer from Sinni and across the bridge.

You prepare yourself again.

Thirty seconds.

Twenty-nine.

Twenty-eight.

The star turns.

+[skill gained: pulsar timing: +1% campaign acceleration.]

* [Continue.] -> sinni_system_menu

// WHITE DWARF

=== sinni_white_dwarf ===

You find Sinni alone at the navigation hub.

The command opticals are centered on the system's primary.

A white dwarf.

Small enough that even magnified, it looks less like a sun than a hole punched through the dark.

Sinni is staring at it.

"It was the whiteness of the whale that above all things appalled me. But how can I hope to explain myself here; and yet, in some dim, random way, explain myself I must, else all these chapters might be naught."

* ["Another old story?"] -> sinni_white_dwarf_moby
* ["How can a wail be white?"] -> sinni_white_dwarf_what

=== sinni_white_dwarf_moby ===

"Mm."

Sinni doesn't look away from the star.

"Moby-Dick. It's a story about a man hunting a white whale."

A pause.

"Old Earth."

* ["Wail?"] -> sinni_white_dwarf_what

=== sinni_white_dwarf_what ===

"'Whale.' W-H-A-L-E."

Sinni takes a slow sip from her tea.

"Old Earth sea creature."

Sinni taps her console. A novel by someone named Herman Melville begins scrolling down her screen.

A cover animation of the novel begins playing on one of the side displays.

A man, armored in a water-adapted work suit, stands on the deck of a primitive water transport, braced behind a long rifle. The laser sights on his weapon flicker into the darkness.

Then, beyond the rail, something enormous rises from the waves.

White shell.

Heavy claws.

A segmented body disappearing beneath the water.

It is a massive white crustacean.

* ["That's a whale?"] -> sinni_white_dwarf_whale_2_whale
* ["That looks like a Volturnian lobster."] -> sinni_white_dwarf_whale_2_lobster

=== sinni_white_dwarf_whale_2_whale ===

-> sinni_white_dwarf_whale_2

=== sinni_white_dwarf_whale_2_lobster ===

-> sinni_white_dwarf_whale_2

=== sinni_white_dwarf_whale_2 ===

"Probably."

Sinni watches the looping video play.

"Large ocean creature. As big as a frigate. Mm. It's a whale."

A pause.

* ["You don't actually know, do you?"] -> sinni_white_dwarf_whale_3

=== sinni_white_dwarf_whale_3 ===

"No. Do you?"

* ["No."] -> sinni_white_dwarf_whale_4
* ["Yes." (Lie)] -> sinni_white_dwarf_whale_6

=== sinni_white_dwarf_whale_6 ===

"Liar."

Sinni gives you a flat look.

* ["...No."] -> sinni_white_dwarf_whale_4

=== sinni_white_dwarf_whale_4 ===

The two of you stare at the animation for another moment.

Neither of you is particularly qualified to dispute it.

Sinni closes the book.

The immense white crustacean disappears.

The white dwarf remains.

* ["So what does any of this have to do with the star?"] -> sinni_white_dwarf_whiteness

=== sinni_white_dwarf_whiteness ===

"The white."

Sinni gestures toward the command opticals.

"It looks simple."

She lowers the exposure.

The glare contracts until the dwarf becomes a tiny, hard sphere of white light.

"Small. Clean. Quiet."

She brings up the system's gravitational plot.

The projected gravity well plunges around it.

"It isn't."

* ["How small is it?"] -> sinni_white_dwarf_size
* ["How massive?"] -> sinni_white_dwarf_mass

=== sinni_white_dwarf_size ===

"About the size of Old Earth."

She places a reconstructed terrestrial planet beside the star.

The two are disturbingly close in diameter.

* ["And the mass?"] -> sinni_white_dwarf_mass

=== sinni_white_dwarf_mass ===

"Most of a star."

The mass estimate appears beside it.

Almost a solar mass.

Sinni places an Old Sol comparison beside the dwarf.

The difference in size is absurd.

* ["So that's almost a sun packed into a planet."] -> sinni_white_dwarf_density

=== sinni_white_dwarf_density ===

"Yes."

She taps the gravity plot.

"Don't navigate by appearances."

Sinni compares the lobster with the star.

"White."

She points at the lobster.

"Dangerous."

Then the dwarf.

"Also dangerous. See?"

+[skill gained: remnant economy: -1% fleet supply upkeep.]

* ["I see."] -> sinni_system_menu

// MAIN-SEQUENCE STAR
// Red dwarf / orange star / yellow star

=== sinni_main_sequence ===

Sinni is writing when you find her.

She looks unusually relaxed right now. Her boots are off and she is sitting cross-legged on her navigator's chair. Another cup of that strange light-green tea has found its way into her hands.

"{player_title}."

* ["Sinni."] -> sinni_tea_name
* ["Chief Nav."] -> sinni_tea_title

=== sinni_tea_name ===

-> sinni_tea

=== sinni_tea_title ===

-> sinni_tea

=== sinni_tea ===

"Join me for some matcha."

* ["Sure."] -> sinni_tea_2
* ["Sure." (unscrew your flask)] -> sinni_tea_2_flask

=== sinni_tea_2 ===

Sinni produces another cup from within her sleeves.

Of course she already has one.

She pours from a small insulated canister and passes it over.

The drink is hot, bitter, and grassy.

-> sinni_tea_3

=== sinni_tea_2_flask ===

Sinni produces another cup from within her sleeves.

Of course she already has one.

She pours from a small insulated canister and passes it over.

While she is turned around, you add a shot from your flask to the cup.

The drink is hot, bitter, and grassy, but with just the right amount of kick to make it interesting.

-> sinni_tea_3

=== sinni_tea_3 ===

Sinni turns back toward the command opticals.

* ["You seem relaxed."] -> sinni_tea_4

=== sinni_tea_4 ===

She looks back. There's something weird on her face.

A smile. 

You damn near choke on your tea out of shock.

"This is a stable system," she says, a very self-satisfied look on her face. Sinni takes another sip. "Calm seas are when Navigators relax."

* ["Must be nice, not having work."] -> sinni_work_easy
* ["Keep talking. I want to know just how much pay to dock from you."] -> sinni_work_dock

=== sinni_work_dock ===

"Hai, hai." Sinni murmurs. "Moshiwake gozaimasen, 'Kancho-Sama.'"

+ ["Because I don't understand you, I'm going to have to interpret what you said as disrespect."] -> sinni_work_2

=== sinni_work_2 ===

Sinni clasps her hands and bows her head.

"As you will. And by the way, Captain?"

+ ["What is it?"] -> sinni_work_easy_2

=== sinni_work_easy ===

Sinni takes another sip.

"For me. Not for you."

* ["Pardon?"] -> sinni_work_easy_2

=== sinni_work_easy_2 ===

"Here be dragons."

She points toward one of the tactical displays.

"Human ones."

Sinni gives you another rare smile.

"Stable systems like these attract trouble of the human kind."

A pause.

"In other words, your job. Not mine."

Right on cue, the command center erupts in warning tones. The rest of your bridge crew begin spilling out of the nearby break room, fastening harnesses and taking their stations. 

Contacts are coming in fast. Sinni raises her cup.

"Do your best."

+[encounter generated: lone pirate raider; guaranteed recovery: pristine Kite (S).]

* [Continue.] -> sinni_system_menu

// GIANT STAR
// Orange giant / red giant

=== sinni_giant_star ===

Sinni has reconstructed {system_name} on the navigation display, as it may have been billions of years ago.

The swollen giant contracts into a much smaller main-sequence star. Planetary orbits appear around it, overlaid against their present-day positions.

Sinni advances the model.

The primary begins to swell.

One of the inner planets disappears beneath its surface.

* ["What happened to it?"] -> sinni_giant_star_explain

=== sinni_giant_star_explain ===

"The core ran low on hydrogen." She brings up a cutaway of the stellar interior. "Fusion moved outward into a shell. The core contracted."

The outer layers expand.

"The envelope expanded."

She advances the simulation again.

Another planet remains outside the star, but its estimated temperature climbs rapidly.

* ["So the whole system changed."] -> sinni_giant_star_changed

=== sinni_giant_star_changed ===

"Yes."

Sinni overlays the old and present temperate zones. They barely overlap.

"Same planets. Same orbits, mostly."

She looks through the command opticals toward the giant.

"Different sea."

* ["Why did it turn red?"] -> sinni_giant_star_color
* ["What happens next?"] -> sinni_giant_star_next

=== sinni_giant_star_color ===

"Temperature."

Sinni brings up the spectrum.

"The outer layers expanded, so the surface cooled."

{
    system_subtype == "orange_giant":

        She glances toward the star.

        "Orange, in this case."

    - else:

        She glances toward the star.

        "Red."
}

* ["And after this?"] -> sinni_giant_star_next

=== sinni_giant_star_next ===

"For a star around this mass?"

Sinni advances the model. The outer layers drift away into space.

A small white point remains at the center.

"It sheds the envelope."

She taps the remnant.

"Leaves the core behind."

* ["A white dwarf."] -> sinni_giant_star_white

=== sinni_giant_star_white ===

"Eventually."

She rewinds the model to the present.

The giant returns.

* ["We arrived somewhere in the middle."] -> sinni_giant_star_middle

=== sinni_giant_star_middle ===

"Yes."

Sinni looks out at it.

For once, she doesn't touch the controls.

+[skill gained: changing seas: -1% survey cost.]

* [Continue.] -> sinni_system_menu

// MASSIVE STAR
// Blue giant / blue supergiant / red supergiant

=== sinni_massive_star ===

{
    system_subtype == "red_supergiant":

        The star fills an alarming fraction of the command opticals.

        Its surface is mottled and uneven, great structures boiling across it on scales large enough to swallow entire worlds.

    - else:

        Even through the automatic filters, the star burns white-blue.

        Radiation warnings crowd one side of the navigation display while Sinni adjusts the fleet's safe approach envelope.
}

Beside the observation feed, Sinni has placed a scale comparison. A yellow dwarf appears beside the star.

Then your fleet.

You lean closer.

The fleet is almost impossible to find.

* ["Where are we?"] -> sinni_massive_star_where

=== sinni_massive_star_where ===

Sinni points.

A speck.

* ["That's us?"] -> sinni_massive_star_us

=== sinni_massive_star_us ===

"Yes."

She zooms out.

The fleet disappears.

* ["How does a star get this large?"] -> sinni_massive_star_mass

=== sinni_massive_star_mass ===

"Mass."

Sinni brings up a model of the stellar interior.

"More mass means more pressure in the core."

The fusion rate climbs.

"More pressure means faster fusion."

She gestures toward the luminosity estimate.

"More energy. More light."

The estimated lifetime appears beside it.

It is much shorter than you expected.

* ["And a shorter life."] -> sinni_massive_star_short

=== sinni_massive_star_short ===

"Much shorter."

{
    system_subtype == "red_supergiant":

        She points toward the swollen outer envelope.

        "This one is already late."

        "The core has moved on to heavier fuel. The outer layers expanded and cooled."

    - system_subtype == "blue_supergiant":

        She looks toward the brilliant star.

        "Very massive. Very luminous."

        "It spends fuel at a rate smaller stars can't."

    - else:

        She checks the radiation forecast.

        "Hot enough to flood the system with ultraviolet."

        Another stellar-wind warning appears.

        "And the wind isn't pleasant."
}

* ["What eventually kills it?"] -> sinni_massive_star_end

=== sinni_massive_star_end ===

Sinni advances the stellar model.

Concentric layers appear inside the star.

"Massive stars keep finding new fuel."

She points inward.

"Hydrogen. Helium. Carbon. Oxygen."

More layers appear.

"Heavier elements."

The sequence stops.

"Iron."

* ["What's special about iron?"] -> sinni_massive_star_iron

=== sinni_massive_star_iron ===

"Fusing it doesn't give the core more energy."

The model holds for a moment.

Then collapses.

"So gravity wins."

A shock front tears outward across the display.

You watch the simulated star come apart.

* ["Supernova."] -> sinni_massive_star_supernova

=== sinni_massive_star_supernova ===

"Yes."

The simulation continues.

The expelled material spreads into the surrounding system.

Sinni watches it.

* ["Shame we won't be here for it."] -> sinni_massive_star_shame

=== sinni_massive_star_shame ===

Her eyes remain on the slowly expanding model.

After a moment, she returns it to the present.

The enormous star reappears.

"Still worth seeing now."

+[skill gained: changing seas: -1% survey cost.]

* [Continue.] -> sinni_system_menu

// TRINARY SYSTEM

=== sinni_trinary ===

You arrive at the navigation hub in the middle of an argument.

Your former chief navigator is standing over the plotting table, the elderly woman's face red with frustration, while Sinni sits opposite her, one leg folded beneath her, tea in one hand and snacks in the other.

Enya has a cup and plate of snacks too, but both remain untouched. Between them hangs a model of the system.

Three suns move around a common center of mass, their projected paths twisting through one another like some elaborate piece of clockwork.

Your previous chief navigator points toward a highlighted trajectory.

"I followed procedure. Done this maneuver a thousand times."

Sinni takes a sip.

"Understood."

"Current ephemerides. Domain-standard propagation. Twelve percent reserve on drive-field tolerance."

"Understood."

The old navigator's jaw tightens.

"Then... 'Chief Navigator'. What exactly is wrong with it?"

* ["I'd also like to know."] -> sinni_trinary_problem
* ["Enya, Sinni is your ranking officer. Treat her like it."] -> sinni_trinary_getting_along
* [Let Sinni answer.] -> sinni_trinary_problem

=== sinni_trinary_getting_along ===

"Sir," your former navigator says flatly.

A pause.

"Apo... logies."

"Mm." Sinni nods.

* ["Right. What's wrong with the route?"] -> sinni_trinary_problem

=== sinni_trinary_problem ===

Sinni reaches across the table. The approved navigation solution shrinks to one side. Another set of trajectories appears beside it.

They look almost identical.

Almost.

* ["What's that?"] -> sinni_trinary_custom

=== sinni_trinary_custom ===

"My simulation."

Your former navigator looks at her.

"Your what?"

"A propagator."

Sinni expands a block of equations.

Then another. Then several pages of numerical output.

Enya stares at them.

"This is a three-body solver?"

"Yes."

A pause from Sinni.

"I wrote this one."

* ["Why?"] -> sinni_trinary_why

=== sinni_trinary_why ===

Enya leans closer.

"What did you change?"

Sinni closes most of the trajectory equations. Another model opens underneath them.

Fluid cells spread across all three stellar bodies, each sun subtly deforming under the gravity of the other two.

"Navier-Stokes-Garcia. I've modified the inputs to the equations."

Enya's expression changes slightly.

* ["The fluid model?"] -> sinni_trinary_garcia
* [Let Enya follow.] -> sinni_trinary_garcia

=== sinni_trinary_garcia ===

Sinni nods.

"Garcia applies the relativistic correction after the fluid step."

Enya folds her arms.

"Of course. The difference is negligible."

"For most problems, yes. But for here..."

Sinni opens her version beside the standard model.

"In a three-body system, it doesn't stay negligible."

* ["So you took relativity into account earlier."] -> sinni_trinary_relativity
* ["I'm lost."] -> sinni_trinary_relativity
* [Groan and rub your head. It hurts even looking at these equations.] -> sinni_trinary_relativity

=== sinni_trinary_relativity ===

Sinni advances both models.

At first, the three stars look identical in each. But then, the inner pair make a close passage. Their outer envelopes deform.

The difference is impossible to see until Sinni magnifies it.

"Relativity changes the pressure gradient."

Another timestep.

"That changes the flow."

Another.

"The next step starts from a different star."

Enya has stopped looking angry. The old woman steps closer to the display.

* ["And that changes our route?"] -> sinni_trinary_gravity

=== sinni_trinary_gravity ===

Sinni overlays the stars' mass distributions.

The difference looks trivial. Then she overlays their gravitational fields. Three immensely deep wells appear, moving around one another.

The divergence accelerates.

* ["That's a pretty big difference."] -> sinni_trinary_difference

=== sinni_trinary_difference ===

"Yes."

Sinni points toward the old solution.

"Because it puts us here."

Then to hers.

"We would actually be here."

The two markers are separated by an uncomfortable distance.

"It's safer too," Enya mutters. "We'd miss the worst of the stellar turbulence by almost four hundred hours." 

The redness has gone from the elderly woman's face.

* ["When did you write this?"] -> sinni_trinary_bad

=== sinni_trinary_bad ===

"When this humble navigator did her thesis at Eventide." Sinni bows her head. "Celestial physics. I enjoyed it."

Enya stares at her. A look of resignation spreads across the Lieutenant's face.

Sinni reaches for a snack.

* ["Of course you did."] -> sinni_trinary_old_nav

=== sinni_trinary_old_nav ===

Your former chief navigator looks back at the approved route. Then at the one Sinni calculated.

Enya is silent for several seconds.

Then she deletes her route.

* ["That's it? No argument?"] -> sinni_trinary_no_argument

=== sinni_trinary_no_argument ===

Enya gives you an irritated look.

"Changing course, commander."

She gestures toward Sinni's calculations.

"I'm not stupid. I know when someone's better once I see it."

Sinni lowers her cup slightly.

"I am not your better, Lieutenant Commander."

Sinni gestures toward the two routes.

"Just the route."

"Hmph." Enya grumbles.

Sinni gets up from her seat and raises her wide canton sleeves, bowing her head deeply.

"I look forward to continuing to learn from you, Lieutenant Commander. Senior partner."

* ["You could learn a thing or two as well, Enya."] -> sinni_trinary_replaced
* ["Good work. Both of you."] -> sinni_trinary_both
* ["I understood none of that."] -> sinni_trinary_math

=== sinni_trinary_replaced ===

Enya scowls, rolling her eyes.

"Kid, I liked you better before this new Chief Nav got here."

Sinni takes another sip.

"Mm."

-> sinni_trinary_end

=== sinni_trinary_both ===

Enya shrugs.

Sinni inclines her head.

Neither says anything else.

-> sinni_trinary_end

=== sinni_trinary_math ===

Sinni looks at you.

"Three suns."

She points toward the old route.

"Official math."

Then toward hers.

"Optimized."

-> sinni_trinary_end

=== sinni_trinary_end ===

The corrected route propagates across the fleet.

Acknowledgement pings from the other ships' navigators flicker on the nav console. Outside the command opticals, three suns continue their slow dance around one another.

Enya gathers her dataslate.

Before leaving, she glances once more at Sinni's equations.

"Send me the source, Chief Nav."

Sinni bows her head again.

"Of course, Lieutenant Commander."

Enya looks down, picks up the plate of snacks and stomps out of the room.

Sinni watches her go, then locks eyes with you.

She then reaches across the table to take Enya's untouched cup of tea.

* [Continue.] -> sinni_system_menu

// COMPLETION

=== sinni_all_types_complete ===

Later, you find Sinni at the navigation hub.

Her personal chart is open on the console.

Nebula.

Black hole.

Neutron star.

Brown dwarf.

White dwarf.

Main-sequence star.

Giant star.

Trinary system.

Beside each entry is a system name, a date, and a small link to her personal logs.

Sinni adds the last one.

She stares at the completed list for a while.

* ["That's all of them."] -> sinni_all_types_all
* ["You finally finished it."] -> sinni_all_types_finished
* [Say nothing.] -> sinni_all_types_silent

=== sinni_all_types_all ===

"Yes."

Sinni looks over the list again.

* ["Happy?"] -> sinni_all_types_happy

=== sinni_all_types_finished ===

"Mm."

She runs a finger down the entries.

"Every kind I wanted."

* ["Happy?"] -> sinni_all_types_happy

=== sinni_all_types_silent ===

You remain beside her.

After a while, Sinni speaks.

"Done."

There is unmistakable satisfaction in her voice.

* ["Happy?"] -> sinni_all_types_happy

=== sinni_all_types_happy ===

"Very."

She says it immediately.

Then she looks at you.

The silence that follows lasts long enough to become slightly awkward.

* ["You're welcome."] -> sinni_all_types_thanks
* ["You can just say thank you, Sinni."] -> sinni_all_types_thanks
* ["Worth the detour?"] -> sinni_all_types_worth

=== sinni_all_types_thanks ===

Sinni turns toward you and clasps her hands before her.

Then she bows deeply.

"{player_title}... my deepest gratitude."

A pause.

"For all of them."

She raises her head.

"It has been my honor to be in your service."

* ["Then what's next?"] -> sinni_all_types_end

=== sinni_all_types_worth ===

"Yes."

No hesitation.

She looks toward the final entry.

"Very."

* ["Then what's next?"] -> sinni_all_types_end

=== sinni_all_types_end ===

"Next..."

Sinni hesitates.

Then she looks back at the chart.

"I'd like to explore more. By your will, of course."

She runs a finger over the list.

"Every system is different. Every star. Even two of the same kind aren't the same place."

Her gaze drifts toward the command opticals.

"Then... I'd like to go beyond the Sector."

A pause.

"New lands."

* ["I want to see that too."] -> sinni_explore
{devoured_ring_path_known:
* ["Beyond even the Orion Knot. We'll see the entire universe. (Sinni Story Quest Completed)."] -> sinni_explore_2
}
* ["There's the Orion Knot. We'll figure out its secrets."] -> sinni_fob_ithaca
* ["First, we have work to do in the Persean Sector."] -> sinni_stay

=== sinni_explore ===

Sinni's expression brightens.

Not dramatically. With her, it never is.

But the slightest of smiles stays.

"Then I hope to remain your navigator for a long time."

She looks back toward the command opticals.

"There are still many systems in the Sector I have never seen."

A pause.

"And after those..."

She leaves the sentence unfinished.

-> sinni_all_types_close

=== sinni_explore_2 ===

You can't see Sinni's face.

Then there is a blur of brown and silver that lurches toward you.

Sinni's wide sleeves envelop your head. You can't see, you can barely hear... all you can sense is the smell of osmanthus.

"Captain... thank you."

When she pulls back, you swear you catch the flash of a wide, genuine smile.

And then Sinni is back to clasping her hands, her face passive.

-> sinni_all_types_close

=== sinni_fob_ithaca ===

Sinni nods.

"The Orion Knot."

The completed chart disappears.

In its place, another opens.

Old.

Very old.

Hundreds of navigational fixes trace a broken path away from the Persean Sector, scattered across hyperspace like breadcrumbs.

You have seen this chart before.

Helen's route.

Sinni runs two fingers along the line.

"She sailed it the other way."

The path terminates at a point far beyond the Sector.

FOB ITHACA.

Sinni studies the name.

* ["And we'd be following her route home."] -> sinni_fob_ithaca_2

=== sinni_fob_ithaca_2 ===

"In her footsteps. Backwards through time."

Sinni traces the route toward Ithaca.

A faint smile.

"I like that."

-> sinni_fob_ithaca_4

=== sinni_fob_ithaca_4 ===

She zooms in on the first surviving point along the route.

The Orion Knot.

For a moment, all trace of her earlier relaxation is gone.

Your chief navigator is back at work.

"I'll prepare the passage."

-> sinni_all_types_close

=== sinni_stay ===

"Of course."

There is no disappointment in her voice.

Sinni looks back toward the completed chart.

A pause.

"We have time."

-> sinni_all_types_close

=== sinni_all_types_close ===

Sinni closes the chart.

Then, after a moment, opens it again.

You pretend not to notice.

[Quest completed: ONE MORE HORIZON]

[Reward gained: +2 Burn speed cap; +1 base Burn level.]

* [Continue.] -> sinni_system_menu

// ARCHIVE: omitted historical knots; no active scene routes here.

=== sinni_nebula_navigate ===

"The sea. This."

She indicates a series of faint lines.

"Velocity. Gravity. Time."

Another bank of dust rolls across the enhanced opticals, swallowing several distant stars.

"We stride forward by the last light in our path."

+[skill gained: nebula plotting: -1% sensor profile.]

* [Continue.] -> sinni_system_menu

=== sinni_black_hole_far_side ===

"Behind it."

She traces the arc with one finger.

"Gravity bends the light around the hole before it reaches us."

Her finger moves toward the thin bands crowded against the shadow.

"Those are the disk too."

* ["Again?"] -> sinni_black_hole_again

=== sinni_black_hole_again ===

"Again."

Sinni enlarges the innermost rings.

"Some photons go most of the way around before escaping. Some go around more than once."

The bands compress toward the shadow until they are almost too thin to distinguish.

"Every orbit gives us another image."

* ["So we're seeing several sides of the disk at once."] -> sinni_black_hole_several

=== sinni_black_hole_several ===

"Yes."

Sinni looks toward the observation feed.

"Space doesn't care which direction you think something should be."

* ["And the black part?"] -> sinni_black_hole_shadow

=== sinni_black_hole_shadow ===

"Not the hole."

Sinni points toward the darkness in the center.

"The event horizon is smaller. That's the shadow."

She draws a smaller circle within it.

"Light can pass much closer than this and still escape. Closer than that, it can orbit the hole."

Her finger moves inward.

"Closer still..."

She lets her hand fall.

"It doesn't come back."

* ["Why isn't the shadow round?"] -> sinni_black_hole_spin
* ["Why is one side of the disk brighter?"] -> sinni_black_hole_beaming

=== sinni_black_hole_spin ===

"Spin."

Sinni enlarges the image.

"This one is rotating quickly."

She traces the flattened side of the shadow.

"Spacetime gets dragged around with it. Light moving with the rotation can orbit closer. Light fighting against it has to stay farther out."

* ["So the hole drags space with it."] -> sinni_black_hole_spin_2

=== sinni_black_hole_spin_2 ===

"Yes."

Sinni seems pleased.

"The sea has a current."

* ["And the bright side?"] -> sinni_black_hole_beaming

=== sinni_black_hole_beaming ===

Sinni points toward the brighter edge of the accretion disk.

"That side is coming toward us."

Then toward the dimmer side.

"That side is moving away."

* ["That makes this much of a difference?"] -> sinni_black_hole_beaming_2

=== sinni_black_hole_beaming_2 ===

"At this speed."

She brings up an estimate of the disk's orbital velocity.

A substantial fraction of light speed.

"Relativistic beaming. The approaching side gets brighter. The receding side gets dimmer."

She dismisses the figures.

"The whole thing is moving much faster than it looks."

* ["So this is what a black hole actually looks like."] -> sinni_black_hole_actual

=== sinni_brown_dwarf ===

The primary of {system_name} is barely visible through the bridge glass.

On the navigation display, Sinni has surrounded it with enough sensor annotations to obscure half the system.

Infrared emissions.

Magnetic-field estimates.

Mass calculations.

Orbital perturbations.

* ["Having trouble finding the star?"] -> sinni_brown_dwarf_find

=== sinni_brown_dwarf_find ===

"I found it."

She points through the observation window.

"That's it."

* ["Hardly looks like a star."] -> sinni_brown_dwarf_star

=== sinni_brown_dwarf_star ===

"It isn't."

A pause.

"Mostly."

* ["Failed star?"] -> sinni_brown_dwarf_failed

=== sinni_brown_dwarf_failed ===

"No."

The correction comes immediately.

"Too light to sustain ordinary hydrogen fusion."

Sinni enlarges the dim primary.

"Too massive to really be a planet. Some burn deuterium for a while."

She removes the infrared enhancement.

The object nearly vanishes.

* ["Then what do you call it?"] -> sinni_brown_dwarf_call

=== sinni_brown_dwarf_call ===

"A poor light."

Sinni studies the almost invisible object.

"There could be a lot of these between the stars. You wouldn't see most of them from far away."

* ["You like things that are hard to find."] -> sinni_brown_dwarf_hard

=== sinni_brown_dwarf_hard ===

"Yes."

That answer required no consideration at all.

She looks back toward the dim primary.

"Poor lights matter most on dark water."

+[skill gained: poor-light navigation: -1% fuel use.]

* [Continue.] -> sinni_system_menu

=== sinni_last_request ===

The navigation briefing ends.

Officers collect their slates and filter out of the command center.

Sinni remains.

You review the last of the supply projections.

She is still there.

You look up.

* ["Something else?"] -> sinni_last_request_2

=== sinni_last_request_2 ===

"Yes."

She does not immediately continue.

That alone is unusual.

Finally, Sinni steps forward and places her slate on the table.

A map of the Persean Sector appears.

Across it are marks for every kind of stellar system Sinni could find in the generated Sector.

All but one have been checked.

One has not.

{missing_type}.

* ["You've been keeping a list."] -> sinni_last_request_list

=== sinni_last_request_list ===

"Yes."

* ["Since when?"] -> sinni_last_request_since

=== sinni_last_request_since ===

"Since I joined you."

She expands the map.

Names and dates appear beside the completed marks.

One example of each kind that actually exists within the charted Sector.

* ["Your own chart."] -> sinni_last_request_chart

=== sinni_last_request_chart ===

"Yes."

She looks over the completed entries.

"Almost."

* ["We're missing one."] -> sinni_last_request_missing

=== sinni_last_request_missing ===

"Yes."

The map shifts.

Far from your present position, another system begins blinking.

{missing_system}.

"The closest {missing_type} I could find."

You inspect the proposed route.

It is nowhere near anything useful.

* ["That's {missing_distance} light-years away."] -> sinni_last_request_distance

=== sinni_last_request_distance ===

"Yes."

* ["Anything else there?"] -> sinni_last_request_anything

=== sinni_last_request_anything ===

"I don't know."

* ["Survey prospects?"] -> sinni_last_request_survey
* ["Salvage?"] -> sinni_last_request_salvage
* ["Bounties?"] -> sinni_last_request_bounties
* ["Anything we actually need?"] -> sinni_last_request_need

=== sinni_last_request_survey ===

"Probably."

* ["Anything else?"] -> sinni_last_request_anything_2

=== sinni_last_request_salvage ===

"Maybe."

* ["Anything else?"] -> sinni_last_request_anything_2

=== sinni_last_request_bounties ===

"No."

* ["Anything else?"] -> sinni_last_request_anything_2

=== sinni_last_request_need ===

"No."

-> sinni_last_request_why_setup

=== sinni_last_request_anything_2 ===

Sinni waits.

* ["Anything we actually need?"] -> sinni_last_request_need

=== sinni_last_request_why_setup ===

Sinni makes no attempt to improve the case.

The marker continues blinking in an otherwise empty reach of the chart.

* ["Then why are you showing me?"] -> sinni_last_request_why

=== sinni_last_request_why ===

Sinni looks down.

Her finger traces the route from the fleet to {missing_system}.

For once, there is no useful current to ride.

No convenient port along the way.

No gravity assist that makes the journey clever.

No hyperspace weather that turns it into the sensible route.

When she speaks, her voice is quiet.

"I want to see it."

There it is.

No argument about survey data.

No professional justification.

Just a request.

* ["You want to finish the chart."] -> sinni_last_request_finish
* ["There's exploration merit in an unsurveyed system."] -> sinni_last_request_merit
* ["How badly?"] -> sinni_last_request_badly

=== sinni_last_request_finish ===

"Yes."

Sinni looks over the marked categories.

"One mark left."

* ["And you want to make landfall."] -> sinni_last_request_landfall

=== sinni_last_request_landfall ===

"Yes."

A pause.

"Just once."

-> sinni_last_request_decision

=== sinni_last_request_merit ===

"There is."

* ["So we have a perfectly respectable operational justification."] -> sinni_last_request_merit_2

=== sinni_last_request_merit_2 ===

Sinni looks at you.

"That's not why I'm asking."

* ["I know. It's why I can justify saying yes."] -> sinni_last_request_merit_3

=== sinni_last_request_merit_3 ===

She considers this.

"That's your logbook."

A pause.

"Not mine."

-> sinni_last_request_decision

=== sinni_last_request_badly ===

Sinni looks back toward the lonely marker.

For several seconds, she says nothing.

Then:

"Enough to ask."

-> sinni_last_request_decision

=== sinni_last_request_decision ===

The chart waits between you.

One distant system.

A long stretch of open sea.

* ["Plot the course."] -> sinni_last_request_accept
* ["Not now."] -> sinni_last_request_later

=== sinni_last_request_accept ===

For the first time since opening the map, Sinni looks directly at you.

"Really?"

* ["You asked."] -> sinni_last_request_accept_2
* ["We're explorers. Sometimes that's enough."] -> sinni_last_request_accept_3
* ["One mark left."] -> sinni_last_request_accept_4

=== sinni_last_request_accept_2 ===

"Yes."

She takes back the slate.

The hesitation disappears.

A route begins drawing itself across the Sector.

-> sinni_last_request_started

=== sinni_last_request_accept_3 ===

Sinni considers that.

"Sometimes."

She takes back the slate.

"Good."

The first leg of the route appears.

-> sinni_last_request_started

=== sinni_last_request_accept_4 ===

Sinni looks at you for another moment.

Then at the chart.

"One."

She takes back the slate.

-> sinni_last_request_started

=== sinni_last_request_started ===

The course stretches across the Sector map.

It is long.

It is unnecessary.

Sinni studies it.

Then she nudges the first leg sideways.

A hyperspace current clips the new trajectory.

Projected fuel expenditure drops by less than one percent.

Of course.

She checks the route one final time.

"Fair winds."

[Objective updated: ONE MORE HORIZON]

[Travel to {missing_system} and visit its {missing_type}.]

* [Continue.] -> sinni_system_menu

=== sinni_last_request_later ===

"All right."

Sinni closes the map.

There is no complaint in her voice.

No argument.

No attempt to make the journey sound more important than it is.

She picks up her slate.

* ["Sinni."] -> sinni_last_request_later_2

=== sinni_last_request_later_2 ===

She stops.

* ["Keep the coordinates."] -> sinni_last_request_later_3

=== sinni_last_request_later_3 ===

"I will."

Of course she will.

* [Continue.] -> sinni_system_menu
