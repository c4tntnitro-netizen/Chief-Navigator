-> menelaus_contact_labor_3_active


=== menelaus_contact_labor_3_active ===

Ashen Verge unfolds around Menelaus.

The recovered research packages from your second Labor rotate briefly around him before disappearing into Ithaca's systems.

"Your second Labor has borne fruit. Several of the recovered modules have already been integrated into the macrocomplex."

The display shifts to the Odyssey Expanse.

A vast contact appears within the amber nebula.

The Drifting Wall.

"Now we may correct an older failure."

Menelaus enlarges the wrecked Ring Defense module. Even separated from Ithaca, its batteries and autonomous systems still burn with power.

"The Drifting Wall remains structurally viable. For decades, its mounted defenses cut a wide swath through the Starving Threat. I believe it is for this reason the Odyssey Expanse remains largely free of the Threat menace. Its command architecture, however, no longer recognizes Ithaca authority."

A series of targeting lines spread from the module across the Odyssey Expanse.

"We shall take it back."

* ["You want me to capture that thing?"] -> menelaus_labor_3_capture
* ["How am I supposed to disable it without destroying it?"] -> menelaus_labor_3_disable
+ ["Understood."] -> menelaus_labor_3_accept


=== menelaus_labor_3_capture ===

"Yes."

Menelaus seems almost surprised that the answer requires elaboration.

"That module was built to defend Forward Operating Base Ithaca. By itself, it far outstrips the power projection of even the largest battlestations in your sector."

He gestures toward the Wall.

"Disable it. My IFF key will handle the rest."

-> menelaus_contact_labor_3_active


=== menelaus_labor_3_disable ===

A schematic of the Drifting Wall unfolds around you.

Several points along its immense frame illuminate.

"The vast bulk of the macrocomplex is impregnable, but the flux megareactor in the back is not."

The Wall's command partition appears beside the reactor schematic.

"Shut down the reactor, and the Drifting Wall will power cycle, giving my security injection the opportunity to update its IFF databases and restore connection with FOB Ithaca."

Menelaus looks directly at you.

"The Wall is more valuable intact. Spare no materiel to take back our home."

-> menelaus_contact_labor_3_active


=== menelaus_labor_3_accept ===

"Good."

The Drifting Wall is marked on your map.

"Disable its reactor. Hold the area until the security injection completes the transfer."

The enormous contact turns gold.

"Return my wall to me, and I shall reward you in kind."

+ ["I accept."] -> menelaus_contact_labor_3_menu


=== menelaus_contact_labor_3_menu ===

"What do you require?"

+ ["About my current Labor."] -> menelaus_contact_labor_3_status
* ["What happened to the Drifting Wall when Gautama attacked?"] -> menelaus_drifting_wall_history
* [Have Sinni ask more about Helen Argyros.] -> menelaus_helen_3
+ ["Nothing for now."] -> menelaus_contact_leave


=== menelaus_contact_labor_3_status ===

The Drifting Wall burns gold over the Odyssey Expanse.

"Disable its rear flux megareactor. Preserve the station and hold the area while the security injection restores its Ithaca IFF."

-> menelaus_contact_labor_3_menu


=== menelaus_drifting_wall_history ===

The intact orbital ring appears around Menelaus.

Then Gautama arrives.

"The Wall was once designated Ring Defense Module West."

The Threat Singularity strikes the outer defenses. Weapons fire fills the holodeck.

"During the Starving Threat's first successful penetration of Ithaca, Module West absorbed the primary impact of its assault fleet."

You see a simulation of what happened. Threat assault swarms are so dense, they nearly obscure the entire module. Through them, the massive Naval Anti-Ship Lasers of the once-Ring Defense West tear great glowing arcs, shattering Line Units and Fabricators in a single swipe.

Then, you see it. A massive shadow within the scourge of Threat swarms. The section tears free.

And then the swarm retreats. And right behind it...

Ring Defense West tumbles away from the macrocomplex, trailing fire, debris, and atmosphere before disappearing into the dark.

"I lost contact with it shortly afterward."

The display jumps forward.

The module reappears in the Odyssey Expanse.

"The fact that it survived was fortunate."

Menelaus's eyes narrow.

"The fact that it now fires upon both friend and foe is less so."

-> menelaus_contact_labor_3_menu


=== menelaus_helen_3 ===

Sinni steps forward.

"Strategos."

Menelaus turns toward her.

"You said Helen's ability was not the only reason you placed her on the Fourteenth's manifest."

"Correct."

Sinni folds her hands into her sleeves.

"Then why?"

For once, Menelaus does not answer immediately.

Helen's service record appears between them again.

"I... predicted well in advance the eventual death of all of the macrocomplex. So did the rest of Task Force Spartan. I was willing to die with my new comrades."

Sinni's eyes lift from the record.

"All except for her," Sinni finishes.

Menelaus continues.

"Helen had been ordered to remain with Task Force Spartan. She understood what that meant. So did I."

"But you used it for her."

"Yes."

Sinni studies him carefully.

* ["How close were you?"] -> menelaus_helen_3_close
* [Let Sinni continue.] -> menelaus_helen_3_sinni


=== menelaus_helen_3_sinni ===

Sinni looks back at Helen's image.

"What was she to you?"

Menelaus's blue-white eyes remain fixed on the old photograph.

"My friend."

A pause.

"My confidante."

The geometry of his face shifts almost imperceptibly.

"One of very few people who addressed me... as a person before addressing me as an artificial intelligence."

Sinni looks at him.

"That mattered to you."

"Yes."

-> menelaus_helen_3_end


=== menelaus_helen_3_close ===

"Very."

The answer is immediate.

Menelaus looks toward Helen's record.

"She argued with me frequently. Without permission."

Sinni tilts her head.

"She was difficult."

"Extremely." A beat from the Alpha AI. "I valued that."

-> menelaus_helen_3_end


=== menelaus_helen_3_end ===

Sinni is quiet for a moment.

Then she asks:

"Did she know why you sent her away?"

Menelaus looks at her.

"Not entirely."

Sinni waits.

He offers nothing more.

-> menelaus_contact_labor_3_menu
