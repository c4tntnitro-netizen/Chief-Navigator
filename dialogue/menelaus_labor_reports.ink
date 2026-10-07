// Authoring mirror only. Runtime prose and options live in data/campaign/rules.csv.

=== menelaus_labor_i_report ===
Menelaus receives the four destruction records without comment. The tactical display holds each mothership's last position as an empty marker.

Then all four markers flare teal.

Across the Odyssey Sector, dormant recovery frames converge on the wreck sites. Broken armor rotates into alignment. Drive cores relight. Four replacement command partitions accept Menelaus's IFF and begin rebuilding the motherships where they fell.

For once, the battered helm turns sharply.

“Unexpected. Their destruction propagated the authentication farther than seizure would have.”

New contacts spill from each restored hull: two Guard patrols per mothership, spreading out along independent routes through the system.

“The restored Guard recognizes you. You may approach its patrols and authorize the transfer of a drone to your fleet. The unmoored packs remain severed from Ithaca and will engage my patrols on contact.”

“Labor I is complete.”

You can command recovered FOB Ithaca drones without the Automated Ships skill.

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
When you get back, you find Menelaus with your sensor reports open.

Is it your imagination, or... does the AI's avatar look grim?

Menelaus opens the sensor package's final transmission. The Devoured Ring appears as a lattice of consumed mass, failed topology models, and centuries of Starving Threat traffic.

“The package completed its work before deleting itself. The Threat have been trying to reconstruct a Janus Device from the ring they consumed. They failed because they possess only one side of the topology.”

“And the closest intact Gate left is...”

Menelaus stares at the Gate that Task Force Spartan guards.

You realize.

+ [“If they scan Ithaca's Gate, then—.”] -> menelaus_labor_5_realization_2

=== menelaus_labor_5_realization_2 ===

“The Persean Sector will die,” Menelaus finishes. “Humanity will die.”

Suddenly, the simulation of Ashen Verge burns red.

Hundreds.

Thousands.

Countless red alerts begin appearing along the edge of Ashen Verge.

All moving inward.

+ [“They're already here.”] -> menelaus_labor_5_realization_3

=== menelaus_labor_5_realization_3 ===

“Auxiliary...”

His blue-white eyes burn into yours.

“Defender of the Domain.”

The surveillance data collapses into a tactical map. Threat concentrations converge on FOB Ithaca in numbers greater than anything you have yet encountered.

“This is your final Labor. You have proven that neither the strength of lions nor of bulls shall hold you.”

Over Menelaus's head, more and more red alerts spread like a bloody starry sky.

“Fight with us until the last light.”

- [Continue.] -> END

=== menelaus_final_labor_offer ===
“The fifth Labor begins only on your order. Three massive Starving Threat formations are gathering beyond Ithaca's perimeter bastions. Once you accept, each formation will begin a siege lasting approximately two months. Any formation still alive at its deadline will destroy its assigned bastion and turn on Task Force Spartan; Spartan losses will not be replaced.”

“Gautama will not commit the Heavenly Strike until all three perimeter formations are dead. Then every surviving Spartan formation will jump directly to the center engagement. Accept only when you are prepared to defend Ithaca.”

- [Accept the final Labor.] -> menelaus_final_labor_accepted
- [Leave.] -> END

=== menelaus_final_labor_accepted ===
Menelaus faces you.

His blue-white eyes burn into yours.

“Hero of Ithaca. Defender of the Domain.”

The surveillance data collapses into a tactical map. Threat concentrations converge on FOB Ithaca in numbers greater than anything you have yet encountered.

“This is your final Labor.”

A pause.

“Save us all.”

- [“We'll stop them.”] -> menelaus_labor_5_depart

=== menelaus_labor_5_depart ===
The holodeck releases you.

Stars fade. Ashen Verge disappears. Menelaus dissolves into blue-white geometry until only the empty circular chamber remains.

The doors open.

Captain Aias Kleon is waiting outside. The commander of Task Force Spartan is already talking into his comm.

“—yes, all personnel. Full revival authorization. Code Eight, Five, Niner, Alpha-Foxtrot-Alpha.”

He starts walking before you reach him.

“Medical teams to cryogenic blocks one through nine. Combat personnel report directly to assigned stations on revival. No acclimation period unless medically required. Not unless they're puking up a lung.”

An answer crackles through his earpiece.

“Override the normal schedule. I want every able body awake.”

Kleon cuts the channel and glances toward you.

“{player_title} {player_name}. Come.”

-> menelaus_labor_5_field_briefing

=== menelaus_labor_5_field_briefing ===
The corridor outside has transformed since you entered the holodeck.

Personnel rush past in both directions. Bulkhead displays flash mobilization orders. Somewhere deeper inside Ithaca, alarms begin sounding one district after another as sections of the macrocomplex wake from centuries of reduced operation.

Captain Kleon brings up a tactical display on his slate.

“Menelaus gave you the strategic picture. Here's the battle.”

Four enormous Threat formations appear around FOB Ithaca.

“We've isolated four Hostswarms.”

He highlights three.

“One is moving on North Bastion. One on South. One on East.”

The fourth remains farther out, a vast knot of red signatures collecting behind the others. You recognize one of the signatures in the last one.

Gautama.

- [“They're holding one back.”] -> menelaus_labor_5_hostswarms

=== menelaus_labor_5_hostswarms ===
“Exactly.”

Kleon enlarges FOB Ithaca.

The three Bastions illuminate around the macrocomplex, each surrounded by batteries, minefields, defense drones, and Spartan ships.

“Your fleet will reinforce the Bastions. Fight with our defenses. Let the guns, mines, and drones do what they can.”

The three attack vectors burn red.

“Defend against the assault on North. South. East.”

Then he highlights the fourth Hostswarm.

It begins moving.

Straight toward the enormous wound in Ithaca's eastern ring.

“The fourth comes afterward.”

The breach Gautama tore through the macrocomplex appears before you.

“The Bastion attacks are attritional. They want our ships committed, our batteries hot, our ammunition depleted.”

His finger taps the breach.

“Then they'll put everything they have through here.”

- [“That hole in the wall.”] -> menelaus_labor_5_breach

=== menelaus_labor_5_breach ===
Captain Kleon closes the map around the eastern breach. Task Force Spartan formations begin appearing inside it.

Every surviving ship.

Every awakened crew.

“That's where Task Force Spartan makes its stand.”

You look at the force allocation.

There is no reserve behind them.

- [“And if the line breaks?”] -> menelaus_labor_5_line

=== menelaus_labor_5_line ===
“We won't.”

Kleon does not slow down. Then his mic crackles again.

“Spartan Actual. Go.”

He strides away, already issuing orders.

“Confirm revival counts. Get the reserve pilots into their fighters. I want every Bastion reporting weapons-ready before those Hostswarms cross the perimeter.”

Sinni watches him disappear into the flow of personnel.

Then she looks toward you.

“Captain.”

Her face is calm again.

She reaches out and squeezes your hand.

“Let's go.”

- [Begin the final Labor.] -> END

=== menelaus_final_labor_debrief ===
“Ithaca remains. Task Force Spartan remains. You returned when the work was finished. Few commanders do.”

He studies the surviving formation reports in silence.

// Optional unread question; either route reaches the same acknowledgement and departure.
- [“What did you learn about Gautama?”] -> menelaus_debrief_gautama
- [Continue.] -> menelaus_debrief_acknowledgement

=== menelaus_debrief_gautama ===
The projection of his battered Corinthian helm is steady, but the tactical display behind it is a ruin of extinguished Threat contacts and shattered defensive formations.

“The Heavenly Strike is destroyed,” he says. “Gautama was destroyed with it. Our battle telemetry has corrected a long-standing error: Gautama was never the complete organism.”

The tactical display contracts, then opens across the whole of Devoured Reach. Black traces finer than the display can resolve link the eaten worlds.

“Its true form is a web of nanometer-scale black filaments spanning light-years. We have designated it ‘Ungaikyo.’ Gautama was a coalescence—a local knot drawn tight enough to become a warship.”

One filament pulls away from the eaten worlds and reaches across the chart to Last Light. A knot brightens beside the derelict Guardian there, surrounded by the contacts of a Third Strike.

“Ungaikyo is gathering around the Guardian now. It has learned the architecture of your fleet. Expect it to call reflections of your warships out of the web, and to rebuild them while its local Fabricator survives. The wreck cannot be approached until that knot is destroyed.”

His gaze returns to Ithaca's casualty reports.

- [Continue.] -> menelaus_debrief_acknowledgement

=== menelaus_debrief_acknowledgement ===
“Your fifth Labor is complete. The prohibition on Ithaca's gate is lifted. Spartan will open the cordon, and the gate will answer your scan.”

The helm inclines by a fraction.

“Hercules was permitted to rest after his Labors. I suspect neither of us will be granted the same courtesy. Go, Captain. See what your victories have opened.”

- [Leave Menelaus to his command.] -> END

=== menelaus_labors_complete ===
Menelaus inclines the projected Corinthian helm.

“Your final Labor is complete. The prohibition on Ithaca's gate is lifted. Task Force Spartan will open the cordon when you approach; you may scan it.”

The tactical display behind him has already changed. Ungaikyo's black filament web spans the eaten worlds of Devoured Reach, with one arm reaching into Last Light and knotting around the derelict Guardian. Its local Fabricator moves there with a Third Strike. Every other Starving Threat mass is swelling toward First, Second, or Third Strike strength, while a full Shrouded escort gathers around Budai.

“The Labors are complete,” Menelaus says. “The consequences are not.”

- [Leave.] -> END
