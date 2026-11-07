// Authoring mirror. Runtime: data/campaign/rules.csv.
-> menelaus_contact_labor_4_active


=== menelaus_contact_labor_4_active ===

The Lacedaemon System unfolds around Menelaus. The strategic display withdraws from Ithaca, crossing several systems before settling over a region almost entirely obscured by hostile contacts.

ANATOLIA SYSTEM

The display magnifies the system. Thousands of enemy signatures crowd its inner reaches, overlapping until entire orbital regions appear as solid masses of red. Around the perimeter, smaller formations move along irregular patrol routes, occasionally separating from the swarm and disappearing into the surrounding hyperspace.

At the center, however, there is nothing but a grey fog. 

"Your fourth Labor will take you into the Anatolia system, the largest concentration of Starving Threat forces we have identified in the entire sector."

A series of reconnaissance tracks appears over the system chart. One by one, they terminate among the hostile contacts, their final telemetry records flickering briefly before disappearing. None reaches the obscured center.

"We can track enemy movements along the perimeter, but none of our reconnaissance missions has returned useful intelligence from the obscured center. We cannot assess their strength or activities there."

Menelaus dismisses the failed reconnaissance tracks. In their place, a single deployment coordinate illuminates deep within the hostile perimeter.

A compact surveillance package materializes above his outstretched hand. Its armored casing separates into several layers, revealing tightly packed sensor arrays beneath the ablative plating.

"Your fleet will deliver this burst sensor package to the designated position."

The deployment marker continues blinking amid the red.

-> menelaus_labor_4_briefing_options


=== menelaus_labor_4_briefing_options ===

+ ["You want me to take that into the middle of the swarm?"] -> menelaus_labor_4_middle
+ ["What does the package do?"] -> menelaus_labor_4_package
+ ["Understood."] -> menelaus_labor_4_accept



=== menelaus_labor_4_middle ===

"Not the middle."

Menelaus magnifies Anatolia's central region. Within the grey, a single orbital track emerges, circling a dark celestial body.

MARA

Several points illuminate along the orbit, separated by considerable distances. Lines extend between them, forming the outline of an incomplete sensor array.

"Over dozens of cycles, we have deployed components into orbit around Mara, one at a time. Their protective casings are made from fragments of the adamantine structure torn free during Gautama's assault on Ithaca. The material is resistant to the Threat's defabrication swarms."

The existing components turn blue. A final, unoccupied position flashes gold.

"Your package will establish the array's control and transmission link. Once activated, the instruments will conduct a full-spectrum survey and relay their findings to Ithaca."

The deployment coordinate remains highlighted.

-> menelaus_labor_4_briefing_options


=== menelaus_labor_4_package ===
"The orbital instruments have remained dormant to avoid detection. Your package will synchronize the array and initiate a full-spectrum survey of Anatolia's interior."

"Once your fleet has withdrawn, the array will transmit its findings to Ithaca in a compressed data burst."

-> menelaus_labor_4_briefing_options


=== menelaus_labor_4_accept ===

"Good."

The surveillance package is transferred to your fleet manifest, followed by its installation instructions and a set of deployment coordinates within the Anatolia system.

"Reach the marked position, deploy the package, and withdraw. Do not engage the swarm."

Menelaus appears ready to conclude the briefing, but the tactical display remains suspended around him, the enemy formations continuing their slow movements across the system chart.

"There is one further request."

He turns his attention away from the display and toward Sinni, who has remained quietly at your side throughout the briefing.

"Keep Chief Navigator Koyama at Ithaca."

* ["Why are you asking now?"] -> menelaus_labor_4_sinni_why


=== menelaus_labor_4_sinni_why ===

"You've never asked me to leave one of my officers behind before."

Menelaus offers no explanation. Behind him, the simulated patrol routes continue to shift around Anatolia's outer approaches.

"Why Sinni?"

His gaze moves toward the grey region at the center of Anatolia, still opaque amid the hostile contacts.

"Because this Labor is exceptional."

+ ["So this is some suicide mission."] -> menelaus_labor_4_sinni_why_2

=== menelaus_labor_4_sinni_why_2 ===
"No. I would not assign this operation to your fleet if I did not believe you capable of completing it. I fully expect your return."

You wait for him to elaborate, but Menelaus has already turned his attention back to Sinni, who stands with her hands folded neatly within her sleeves.

"But still, if there is even the slightest risk to the Chief Navigator, I must ask."

The change in Sinni's expression is almost imperceptible. She looks from the strategic display to Menelaus, her usual reserve giving way to something harder to read. After a few seconds, she draws her hands together and bows.

"Lord Strategos. I am not Helen."

Menelaus regards her for a moment, but does not answer the name.

* ["Nav's instrumental to our fleet. We need her."] -> menelaus_labor_4_sinni_refuse
* ["Sinni?"] -> menelaus_labor_4_sinni_choice



=== menelaus_labor_4_sinni_refuse ===
Sinni catches your eye. For an instant, you think you glimpse a smile at the corner of her mouth, before her expression settles back into its customary reserve.

"I live and die by my Captain's fleet."

Menelaus regards you for several seconds, then inclines his helm without pressing the matter.

-> menelaus_labor_4_sinni_end


=== menelaus_labor_4_sinni_choice ===

You leave the decision to Sinni. Menelaus turns toward her as well, but she has already made up her mind.

"No."

Menelaus's eyes narrow slightly.

"Chief Navigator Koyama—"

"Lord Strategos. With respect."

Sinni keeps her hands clasped and meets the Strategos's gaze directly, while the projected warships continue their slow passage between them.

"I live and die by my Captain's fleet."

Menelaus considers her answer for several seconds before turning his attention back toward the system chart.

-> menelaus_labor_4_sinni_end


=== menelaus_labor_4_sinni_end ===

Menelaus restores the strategic display to its previous scale, the dense concentrations of enemy contacts returning to surround the grey center of Anatolia. Near Mara, the deployment coordinate continues to flash gold, awaiting the arrival of your fleet.

"Very well. That is your fourth Labor."

+ ["I accept."] -> menelaus_contact_labor_4_menu



=== menelaus_contact_labor_4_menu ===

The Anatolia system hangs behind Menelaus, the simulated enemy swarm staining the holodeck red.

"What do you require?"

+ ["About my current Labor."] -> menelaus_contact_labor_4_status
* ["What do you know about the Anatolia system?"] -> menelaus_reach_history
* ["What exactly is the Starving Threat?"] -> menelaus_threat_origin
* [Ask more about Helen Argyros.] -> menelaus_helen_4
+ ["Nothing for now."] -> menelaus_contact_leave


=== menelaus_contact_labor_4_status ===

Menelaus brings the deployment coordinate near Mara to the forefront of the chart.

"Install the burst sensor package at the designated position, then withdraw. The array will activate after your fleet is clear. Avoid enemy contact."

-> menelaus_contact_labor_4_menu


=== menelaus_reach_history ===

The hostile contacts disappear, replaced by an archival Domain survey. Mining stations and refinery complexes crowd the inner orbits, connected by a dense network of freight routes converging upon the system's Gate.

"It once supplied most of the Orion Knot's strategic ore. Tens of thousands worked its mines and refineries, and plans were underway to establish a shipbuilding macrocomplex there to complement Ithaca. The system was important enough to warrant one of the theater's two Gates."

The Gate goes dark. The interstellar freight routes disappear with it, leaving only the traffic between Anatolia's industrial stations.

"After the Collapse, the first losses appeared unremarkable. Missing freighters, silent mining stations, automated facilities failing to report. We attributed them to piracy, equipment failures, and the general breakdown of post-Collapse interstellar logistics."

Freighter transponders vanish along the outer routes, followed by the stations they were scheduled to visit. Within a few cycles, entire industrial zones have fallen silent.

Menelaus watches the old map empty.

"We were wrong."

Red contacts begin appearing among the abandoned facilities. They spread across the shipping lanes, multiplying until almost every inhabited orbit is lost beneath them.

"By the time we understood that these losses shared a common cause, we had already lost the system."


-> menelaus_contact_labor_4_menu




=== menelaus_threat_origin ===

"The Threat predates the Collapse by centuries."

An Onslaught Mk.I appears beside a modern warship, their sensor identification profiles displayed side by side. The older vessel's records contain an entire library of hostile signatures absent from the modern database.

"The Domain fought these machines for generations. We drove them beyond civilized space, but we never succeeded in exterminating them. In time, the enemy passed out of living memory, and its recognition profiles disappeared from modern shipboard systems."

Menelaus isolates a series of signatures from the Mk.I's archives. A corresponding set of unidentified contacts on the modern display resolves into Threat vessels.

"The modification you recovered from the Mark One restored those identification protocols."

The warships disappear, replaced by a sectional model of an ancient Threat vessel. Its hull peels apart to expose a labyrinth of chambers and swollen, tumor-like structures, threaded with cables that branch and divide like bundles of nerves. The machinery is so densely interwoven that it is impossible to distinguish one piece from another.

"The Threat is a runaway autonomous fabrication ecology. Self-replicating machinery that consumed available matter, manufactured more of itself, and continued expanding wherever sufficient resources could be found."

* ["And the Starving Threat?"] -> menelaus_starving_threat
+ ["That's all."] -> menelaus_contact_labor_4_menu


=== menelaus_starving_threat ===

"The Starving Threat is what remains of that machinery after centuries without sufficient feedstock."

The projection advances through centuries of decline. Armor plates are stripped away and drawn into the vessel's fabrication chambers, followed by redundant machinery and entire sections of the hull. Its remaining systems rearrange themselves around the dwindling supply of usable material, until the once-massive vessel has been reduced to the skeletal form of the warships you have encountered in the Orion Knot.

"They can still replicate, repair themselves, and adapt. But without sufficient material to sustain those processes, every replacement must come at the expense of something else."

The display calls up two destruction records, one showing an ancient Threat vessel breaking apart into Reclamation Swarms, the other a Starving Threat vessel dispersing into Defabricates.

"Locusts. They adapt even to starvation."

Menelaus restores the strategic display of Anatolia, where thousands of hostile contacts remain clustered around the system's obscured center.

-> menelaus_contact_labor_4_menu



=== menelaus_helen_4 ===

Sinni waits until Menelaus has finished attending to the tactical display before stepping out from beside you.

"Lord Strategos."

"You have another question about your ancestor?"

"You said you sent Lieutenant Argyros with the Fourteenth for another reason." 

Menelaus nods, and Helen Argyros's personnel record appears between them, complete with service history, citations, shipboard assignments, and the usual collection of administrative remarks.

* ["What was she to you?"] -> menelaus_helen_5

=== menelaus_helen_5 ===

The AI considers the question. He has been quite willing to discuss Helen's military career, but this appears to require a different sort of answer.

"A trusted officer."

Sinni waits. Menelaus allows the answer to stand for a few moments before apparently deciding that it is insufficient.

"A confidant."

Sinni has barely moved since asking the question, and shows no sign of intending to move until she receives an answer worth having.

Menelaus regards her with an expression that, on a human face, might have been one of exasperation.

"You have inherited her habit."

"Habit?"

"Of obstinacy. Though Helen was more colorful with her prose."

The corner of Sinni's mouth moves.

* ["That still wasn't a complete answer, Strategos."] -> menelaus_helen_4_press
* [Let Sinni decide whether to press him.] -> menelaus_helen_4_sinni


=== menelaus_helen_4_press ===

Menelaus returns his attention to Helen's portrait. The accompanying service record closes, leaving only the image of the Domain officer suspended between them.

"It is the answer I am prepared to give today."

Sinni looks at the portrait for a moment longer, then back at Menelaus. She seems neither surprised nor particularly satisfied with the result.

-> menelaus_helen_4_end


=== menelaus_helen_4_sinni ===

Sinni studies Menelaus for a while, as if deciding whether there is anything to be gained by continuing the interrogation. The Strategos offers no encouragement.

"Another time."

"Yes."

-> menelaus_helen_4_end


=== menelaus_helen_4_end ===

Helen's record closes, taking with it the face of an officer who has been dead for nearly two centuries. Sinni returns to her place beside you while Menelaus restores the operational chart of Anatolia, the red contacts reforming around its obscured center.

-> menelaus_contact_labor_4_menu


=== menelaus_contact_leave ===

"Very well."

The tactical display begins to dissolve around you. Enemy contacts disappear, followed by orbital tracks, navigation markers, and finally the stars themselves, until the familiar scuffed floor and equipment housings of the holodeck emerge from beneath the projection.

Menelaus remains, his enormous blue-white avatar standing amid the suddenly rather ordinary surroundings.

"Attend to your Labor."

For a moment, he seems to have concluded the audience. His gaze remains on you, however, and there is something almost deliberate about the interval before he speaks again.

"I will be here when you return."

The avatar dissolves into geometric light. The holodeck falls silent.

-> END
