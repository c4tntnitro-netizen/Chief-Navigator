-> menelaus_contact_labor_3_active


=== menelaus_contact_labor_3_active ===

Ashen Verge unfolds around Menelaus. The research packages recovered during your second Labor appear alongside a schematic of Ithaca, their installation markers lighting up in sequence before the display folds them away.

"Your second Labor has borne fruit. Several of the recovered modules have already been integrated into the macrocomplex, and I estimate that their combined effect has increased Ithaca's force-projection capability by at least eighty percent."

The display shifts to the Odyssey Expanse, where a single enormous contact hangs amid the amber clouds of the nebula.

The Drifting Wall.

"Now we may correct an older failure."

Menelaus enlarges the severed Ring Defense module. Its hull still bears the damage inflicted when it was torn from Ithaca, but some of its naval batteries remain powered, and the autonomous systems governing them continue to operate after decades adrift.

"Structurally, the Drifting Wall remains viable. For decades, its guns have continued to engage the Starving Threat wherever their vessels entered weapons range, and I believe the losses they have inflicted go some way toward explaining why the Odyssey Expanse remains relatively free of their incursions."

Menelaus brings up the module's defensive coverage, and a series of firing arcs spreads across the surrounding nebula.

"In that respect, the Wall still serves the purpose for which it was built. Unfortunately, its command architecture no longer recognizes Ithaca's authority, and its weapons make no distinction between our vessels and those of the enemy."

The immense module remains suspended between you.

"We will take it back."

-> menelaus_labor_3_briefing_menu


=== menelaus_labor_3_briefing_menu ===

* ["You want me to capture that thing?"] -> menelaus_labor_3_capture
* ["How am I supposed to disable it without destroying it?"] -> menelaus_labor_3_disable
+ ["I accept."] -> menelaus_labor_3_accept


=== menelaus_labor_3_capture ===

"Yes."

Menelaus rotates the image of the Wall, bringing its immense naval batteries into view.

"That module was built to defend Forward Operating Base Ithaca against attacks by entire fleets. Even separated from the ring, its surviving batteries possess more firepower than any battlestation in the Persean Sector, and it has spent decades holding the Starving Threat at bay."

He gestures toward the surrounding nebula, where the Wall's defensive coverage extends across the approaches to the Odyssey Expanse.

"Once we restore its command authority, those defenses will be ours again. More importantly for your purposes, the module can serve as a fortified base of operations for your fleet, allowing you to maintain a permanent presence in the Expanse without having to return to Ithaca after every sortie."

-> menelaus_labor_3_briefing_menu


=== menelaus_labor_3_disable ===

A schematic of the Drifting Wall unfolds around you, revealing the immense armor surrounding its batteries and command systems.

"An assault against the Wall's primary defenses would be prohibitively costly, and destroying the module would defeat the purpose of recovering it. Fortunately, there is another approach."

The schematic rotates, highlighting a reactor assembly near the aft section of the module.

"The aft flux megareactor is considerably less protected than the rest of the installation. Disabling it will force the Wall to initiate an emergency power cycle, and during the subsequent restart, my security injection will have an opportunity to access its command partition and restore Ithaca's authorization credentials."

The reactor schematic gives way to a diagram of the Wall's command network, with several systems highlighted in red.

"Once the transfer is complete, its IFF registry will recognize your fleet as friendly and the module will return to our control. Your task will be to keep the area secure until that process has finished."

Menelaus regards the Wall for a moment.

"Auxiliary, spare no materiel to take back our home."

-> menelaus_labor_3_briefing_menu


=== menelaus_labor_3_accept ===

"Good."

The Drifting Wall is marked on your map.

"Disable its reactor and hold the area until my security injection completes the transfer. The module must remain intact."

The enormous contact turns gold.

"Return my wall to me, and I shall reward you in kind."

+ [Continue.] -> menelaus_contact_labor_3_menu


=== menelaus_contact_labor_3_menu ===

"What do you require?"

+ ["About my current Labor."] -> menelaus_contact_labor_3_status
* ["What happened to the Drifting Wall when Gautama attacked?"] -> menelaus_drifting_wall_history
* [Have Sinni ask more about Helen Argyros.] -> menelaus_helen_3
+ [Review Ithaca defense upgrades.] -> menelaus_upgrade_ledger
+ [Request free repairs.] -> menelaus_labor_3_repair
+ ["Nothing for now."] -> END


=== menelaus_contact_labor_3_status ===

The Drifting Wall burns gold over the Odyssey Expanse.

"Disable its aft flux megareactor, preserve the module, and hold the area while my security injection restores Ithaca's command authority and IFF registry."

The Drifting Wall is $chiefNavigatorWallStatus.

-> menelaus_contact_labor_3_menu


=== menelaus_drifting_wall_history ===

The intact orbital ring appears around Menelaus, its immense circumference still unbroken as the first waves of Threat contacts close on its western defenses.

"The Wall was once designated Ring Defense Module West. During the Starving Threat's first successful penetration of Ithaca, it absorbed the primary assault against the orbital ring."

The display draws closer, resolving swarms of Threat vessels so numerous that they nearly conceal the module beneath their formations. Its massive naval anti-ship lasers sweep across the attackers, tearing apart Threat units in brilliant arcs, but every gap in the flood of burning debris is filled by a fresh hull almost at once. Live Threat vessels mix with wreckage until it becomes impossible to tell which is tumbling and which is still firing.

Weapons fire fills the holodeck as the assault continues. Through the mass of smaller hulls, a much larger shadow begins to emerge, moving toward the junction between Module West and the rest of the ring.

Gautama.

A succession of structural failures races along the connection, and the enormous module shears away from Ithaca, severing transit lines and venting atmosphere as it begins to tumble through the surrounding wreckage.

The surviving Threat swarms withdraw, leaving Ring Defense West to drift farther and farther from the macrocomplex, trailing fire and debris before disappearing into the stars and dark.

"I lost contact with it shortly afterward."

The display jumps forward. The module reappears in the Odyssey Expanse.

"The fact that it survived was fortunate."

It rotates in the night.

"Take it back."

-> menelaus_contact_labor_3_menu


=== menelaus_helen_3 ===

Sinni steps forward, folding her hands into her sleeves as Menelaus turns toward her.

"Lord Strategos."

"You said Helen's ability as a navigator was not the only reason you placed her on the Fourteenth's manifest."

"Correct."

"Then why?"

Menelaus does not answer immediately. Helen's service record appears between them once more.

"When the ring network failed, Battlegroup VI was just a few jumps away. Grand Navarch Kali Molina and her flag staff had every reason to believe that communications would eventually be restored, and the Fourteenth remained at Ithaca for some time in expectation of relief."

"My own assessment was less optimistic. The telemetry available to me suggested a catastrophe extending far beyond the Orion Knot, and by the time the Fourteenth departed, I had concluded that relief from Battlegroup VI was unlikely to arrive."

Sinni looks up from the record.

"Did you tell the Grand Navarch?"

"I provided her with the available telemetry, but not my conclusions. My projections indicated that disclosing the probable loss of Battlegroup VI would have increased the likelihood of the Grand Navarch delaying the Fourteenth's departure, or diverting ships toward a search operation I had little reason to believe would succeed."

Menelaus regards Sinni for a moment.

"Between truthfulness and survival, I judged that preserving the Battlegroup took precedence."

"That decision did not alter my own obligation. Task Force Spartan had been ordered to remain at Ithaca, and I intended to share their fate, whatever that might be."

"Helen too?"

"Yes. Helen had been ordered to remain with us as well, and she would have done so. She would have served with us, fought with us, and, if it came to that, died with us. Molon Labe."

* ["That motto. What does it mean?"] -> menelaus_helen_3_motto_ask
* ["'Come and take them,' right?"] -> menelaus_helen_3_motto_know
+ [Let Sinni continue.] -> menelaus_helen_3_motto_end

=== menelaus_helen_3_motto_ask ===

"Come and take them," Menelaus replies. "The answer given to the Persians when they demanded the Spartans surrender their weapons. It has served as the motto of Task Force Spartan since its founding."

-> menelaus_helen_3_motto_end


=== menelaus_helen_3_motto_know ===

"Correct. An ancient declaration of defiance, made in the expectation that those who spoke it would not survive the battle."

-> menelaus_helen_3_motto_end


=== menelaus_helen_3_motto_end ===

Sinni looks back at Helen's record.

"So you arranged for her transfer."

"I did."

Sinni studies him carefully.

* ["How close were you?"] -> menelaus_helen_3_close
* [Let Sinni continue.] -> menelaus_helen_3_sinni

=== menelaus_helen_3_sinni ===

Sinni looks back at Helen's photograph.

"What was she to you?"

Menelaus's blue-white eyes remain fixed on the old image.

"My friend."

A pause.

"My confidante."

"Most Domain officers came to me with orders, requests, or problems requiring an answer. Helen would sometimes seek me out when she had none of those things in mind, and we would talk nonetheless. Those conversations held no significance for me at the time."

Menelaus regards the photograph.

"It was only much later that I understood what those moments meant. I did not yet possess the capability."

+ ["'Capability'? What does that mean?"] -> menelaus_helen_3_capability
+ [Let the matter rest.] -> menelaus_helen_3_end

=== menelaus_helen_3_capability ===

"Not now," Menelaus replies.

His attention returns to Helen's photograph.

You look between Sinni and Menelaus, but neither of you presses the matter further.

-> menelaus_helen_3_end

=== menelaus_helen_3_close ===

"Very."

The answer is immediate.

Menelaus looks toward Helen's record.

"She argued with me frequently. Without permission."

"She was difficult," Sinni says, tilting her head.

"Extremely," Menelaus agrees. "I came to value that."

-> menelaus_helen_3_end


=== menelaus_helen_3_end ===

Sinni is quiet for a moment.

Then she asks:

"Did she know why you sent her away?"

Menelaus looks at her.

"No."

Sinni waits.

He offers nothing more.

-> menelaus_contact_labor_3_menu

=== menelaus_upgrade_ledger ===

// The native upgrade ledger returns to the current Labor through its existing dispatcher.
-> END

=== menelaus_labor_3_repair ===

FOB Ithaca's automated gantries restore your fleet's hulls and armor, then cycle every ship to maximum combat readiness. No payment is requested.

-> menelaus_contact_labor_3_menu
