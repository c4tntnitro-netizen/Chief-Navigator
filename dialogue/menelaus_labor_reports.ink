// Authoring mirror only. Runtime prose and options live in data/campaign/rules.csv.

=== menelaus_labor_i_report ===
Menelaus receives the four destruction records without comment. The tactical display holds each mothership's last position as an empty marker.

Then all four markers flare teal.

Across the Odyssey Sector, dormant recovery frames converge on the wreck sites. Broken armor rotates into alignment. Drive cores relight. Four replacement command partitions accept Menelaus's IFF and begin rebuilding the motherships where they fell.

For once, the battered helm turns sharply.

“Unexpected. Their destruction propagated the authentication farther than seizure would have.”

New contacts spill from each restored hull: two derelict drone patrols per mothership, spreading out along independent routes through the system.

“The restored Guard recognizes you. Its drone patrols will assist you against hostile fleets. You may recover disabled hostile drones through ordinary battle salvage. The unmoored packs remain severed from Ithaca and will engage my patrols on contact.”

“Labor I is complete.”

Your first-Labor reward was the Automated Ships skill, or one skill point if you already had it. Normal automated ship limits apply.

- [Continue.] -> END

=== menelaus_labor_ii_report ===
Menelaus reviews the four installed component records one by one. Ithaca's defense wall answers each diagnostic in turn.

“Four upgrades are installed and answering Ithaca's fire-control network. That satisfies the Labor.”

“Any outstanding station signatures will stay in your map data. Components beyond the four required are optional, but the wall can still use them. I will maintain the integration ledger.”

“Labor II is complete.”

- [Continue.] -> END

=== menelaus_labor_iii_report ===
// Progression-only transition. Runtime commits Labor III immediately and
// continues into the Hegemony arrival scene without a separate debrief page.
-> END

=== menelaus_labor_iv_report ===

When you return to Ithaca, Menelaus is already examining the final transmission from your surveillance array.

The Devoured Ring hangs in the holodeck, surrounded by the wreckage of its old superstructure and the unfamiliar machinery that has grown around it. Menelaus has overlaid the image with a series of Domain engineering schematics, highlighting the surviving control assemblies and their counterparts in the surrounding constructions.

The similarities are difficult to miss. Components that ought to have been consumed long ago have instead been stripped from the Ring and incorporated into structures of the Threat's own making.

"Do you see it, auxiliary?"

* ["Are they eating it?"] -> menelaus_labor_iv_uncertain
* ["What are they doing?"] -> menelaus_labor_iv_uncertain


=== menelaus_labor_iv_uncertain ===

"Deconstructing. That is their modus operandi."

Menelaus highlights the surviving control assemblies within the ruined Ring. Beside them, corresponding structures illuminate within the great mass of Threat machinery surrounding it.

"However. Observe what they have preserved."

* ["They're adapting the Gate's technology?"] -> menelaus_labor_iv_learn
* ["Could they be trying to destroy it?"] -> menelaus_labor_iv_destroy


=== menelaus_labor_iv_learn ===

"Precisely. The same process by which they have incorporated humanity's ship hulls into their own designs."

-> menelaus_labor_iv_revelation


=== menelaus_labor_iv_destroy ===

"No. Destruction alone cannot account for these structures. The Threat has also reproduced components of the Gate's control systems."

-> menelaus_labor_iv_revelation


=== menelaus_labor_iv_revelation ===

The ruined Ring fades, leaving only the machinery the Threat has constructed around it. Menelaus superimposes the original Domain schematics, and the resemblance becomes apparent.

"They have reproduced much of the Gate's transit architecture. They need not comprehend its function to replicate it."

Half the projected transit topology resolves into a complete configuration. The remainder stays dark.

"But they possess only one side of the topology. The missing parameters must come from another Gate."

Sinni has been watching the reconstruction in silence.
“Then if they reach Ithaca's Gate—”

“The threat will pour from every Gate.” Menelaus finishes. “Humanity will die.”

A priority alert interrupts the holodeck. The image of Ithaca's Gate recedes as new contact reports arrive from the outer approaches of Lacedaemon, followed by another series from a different direction.

At first the returns are difficult to make sense of. Individual Threat signatures overlap until the tactical display begins grouping them into vast concentrations, each growing as more reports come in. Four formations emerge from the confusion, with numbers far exceeding anything Ithaca's patrols have encountered.

One by one, the sensor reports change from tentative to confirmed.

All four formations are approaching Ithaca's defensive perimeter.

+ [“They're already here.”] -> menelaus_labor_5_realization_3

=== menelaus_labor_5_realization_3 ===

“Yes.”

The Gate reconstruction vanishes altogether. Around Menelaus, Ithaca's defense stations begin reporting readiness, while orders spread across the tactical map to the surviving ships of Task Force Spartan.

“Auxiliary...”

He looks toward you, his attention leaving the incoming reports for the first time.

“This is your final Labor. You have proven that neither the strength of lions nor of bulls shall hold you.”

Behind him, the four great enemy formations continue to accumulate contacts, the perimeter defenses little more than a thin blue line between them and Ithaca.

* [Continue.] -> END

=== menelaus_final_labor_offer ===

A tactical plan of Ithaca replaces the wider battle map. Three Hostswarm concentrations are marked beyond the North, South, and East Bastions, with the fourth holding farther out.

“The fifth Labor begins on your order. Once you commit, the three forward formations will lay siege to their assigned Bastions. Each position can hold for approximately two months. Any Hostswarm still intact at the end of that period will destroy its Bastion and turn on Task Force Spartan.”

The three defense positions illuminate in blue. Beside them, Spartan's surviving ships are listed with their current readiness and personnel strength.

“Task Force Spartan will hold the Fourth Strike at bay until all three perimeter formations have been destroyed. At that point, every surviving Spartan formation will fight to the last.”

Menelaus leaves the force allocations before you.

“Accept only when your fleet is prepared to defend Ithaca.”

* [Accept the final Labor.] -> menelaus_final_labor_accepted
* [Leave.] -> END

=== menelaus_final_labor_accepted ===

Menelaus issues the authorization. Command signals spread outward from Ithaca, and acknowledgements begin returning from the Bastions and Task Force Spartan as the defense plan comes into effect.

He looks toward you.

“Auxillary.”

Around him, more acknowledgements arrive from the Bastions and the ships under his command.

“Fight with us until the last light.”

* [“We'll stop them.”] -> menelaus_labor_5_depart

=== menelaus_labor_5_depart ===

The holodeck releases you. The stars of the Lacedaemon System fade, followed by the defensive overlays and Menelaus's enormous avatar, until nothing remains but the circular chamber and its scuffed floor.

The doors open on a corridor already full of movement. Captain Aias Kleon is waiting just outside, speaking into his comm as he walks. He barely breaks stride when he sees you.

“—affirmative. Full revival authorization. Code Eight, Five, Niner, Alpha-Foxtrot-Alpha. Spartan Actual, over.”

A reply crackles through his earpiece. Kleon motions for you to follow.

“Medical teams to cryogenic blocks one through nine. Triage at the exits. If they're breathing, find them a station.”

He listens briefly, then answers before the officer on the other end has finished.

“Override the rotation schedule. Every man to the last.”

"--Yes, sir!" You catch out buzzing out of the man's ear.

Kleon ends the call and turns toward you.

“{player_title} {player_name}. Come with me.”

-> menelaus_labor_5_field_briefing

=== menelaus_labor_5_field_briefing ===

Personnel hurry through the corridor in every direction, carrying weapons and boarding equipment, others maneuvering supply carts toward the transit platforms. Bulkhead displays have switched from station notices to mobilization orders. Farther into Ithaca, alarms sound as districts kept on reduced operation for two centuries begin coming back to life. The ground lights up with mustering stations marked out in lit-up red boxes, and lines of ship crew fill out the blocks before marching as one to their berth assignments.

Kleon brings up a tactical display on his slate without stopping.

Four massive concentrations of Threat contacts appear around FOB Ithaca. The forward three are on separate approaches; the last remains well behind them.

“We've isolated four Hostswarms. Three are heading for the Bastions. One each for North, South, and East.”

The fourth concentration is a dense knot of red signatures. Among them, one familiar contact stands out.

Gautama.

* [“They're holding one back.”] -> menelaus_labor_5_hostswarms

=== menelaus_labor_5_hostswarms ===

“Yes.”

Kleon enlarges Ithaca's defensive perimeter. The North, South, and East Bastions illuminate, each with its own overlapping fields of heavy batteries.

“Your fleet will reinforce the Bastions. Fight inside our defensive coverage wherever possible. Let the guns blunt the weight of the assault and use your ships to destroy what gets through. Protect each wall module's reactors at all cost.”

The three attack routes burn red against the blue defensive positions.

“Destroy each Hostswarm before it brings down its Bastion.”

He selects the fourth formation. Its projected course runs past the perimeter Bastions and into the old wound in Ithaca's eastern ring, where Gautama tore through the macrocomplex during its previous assault.

“The first three attacks will occupy our defenses and spend our ammunition. Then the last Threat Strike comes through here.”

Kleon taps the damaged section of the ring.

“Here's the main assault.”

* [“That breach in the wall.”] -> menelaus_labor_5_breach

=== menelaus_labor_5_breach ===

Captain Kleon closes the map around the eastern breach. Task Force Spartan formations move into position across the opening, their projected strength rising and falling as the display accounts for losses sustained at the Bastions.

He studies the force allocation before committing the last of the available ships to the line.

“There is where we will make our stand.”

There is no reserve.

* [“And if the line breaks?”] -> menelaus_labor_5_line

=== menelaus_labor_5_line ===

“We won't." Aias Kleon holds a fist over his breast. "Good luck, Auxilary. Molon Labe.”

* [“You too, Commodore.”] -> menelaus_labor_5_line_2
* [“Ha. Worry about yourself.”] -> menelaus_labor_5_line_2
* [“Molon Labe!”] -> menelaus_labor_5_line_2


=== menelaus_labor_5_line_2 ===
Kleon does not slow down. His comm crackles again, and he accepts the call without taking his eyes off the corridor ahead.

“Spartan Actual. Go.”

An officer begins reading back readiness figures. Kleon listens for a moment before interrupting.

“Confirm revival counts against ships ready to sortie. Get the reserve pilots into their fighters. I want ammunition and fuel reports from every Bastion. Have every commander acknowledge their manuever assignments before the Hostswarms cross the perimeter.”

He turns at the next junction, already issuing another order, and disappears among the personnel hurrying toward their stations.

Sinni watches him go. Then she looks toward you and, without saying anything more, reaches out to squeeze your hand.

"Let's go."

* [Begin the final Labor.] -> END

=== menelaus_final_labor_debrief ===
“Ithaca remains. Task Force Spartan remains. You returned when the work was finished. Thank you, Captain.”

He studies the surviving formation reports in silence.

"The prohibition on Ithaca's gate is lifted. Task Force Spartan will open the cordon, and the gate will answer your scan.”

The helm inclines by a fraction.

“Your labors are done. But our battles never end. Go, Captain. See what your victories have opened.”

* [Leave Menelaus to his command.] -> END

=== menelaus_labors_complete ===
Menelaus inclines the projected Corinthian helm.

“Your final Labor is complete. The prohibition on Ithaca's gate is lifted. Task Force Spartan will open the cordon when you approach; you may scan it.”

The tactical display behind him has already changed. Ungaikyo's black filament web spans the eaten worlds of Anatolia, with one arm reaching into Bouvo Olympus and knotting around the derelict Guardian. Its local Overseer moves there with a Third Strike. Every other Starving Threat mass is swelling toward First, Second, or Third Strike strength, while a full Shrouded escort gathers around Budai.

“The Labors are complete,” Menelaus says. “The consequences are not.”

- [Leave.] -> END
