// RECONSTRUCTED AUTHORING MIRROR — NOT THE LOST ORIGINAL INK.
// Derived from the supplied rules.csv runtime dialogue for ChiefNavigatorHegemonyIthacaArrival.
// Spoken text, descriptions, player choices and sequence are preserved from those rows.
// Auto-paged runtime rows are represented as Ink knots; branches and runtime callbacks
// are mirrored for editing and will require validation against data/campaign/rules.csv.
// The original runtime events, highlighting, portraits and exact Ink structure are not restored.
// Continue breaks follow reviewed dramatic beats; the user delegated the breakup after finding their pages about 20% too long.

VAR voss_reputation = "neutral" // "friendly", "neutral", "hostile"
VAR sara_dead = false

-> hegemony_ithaca_arrival_1

=== hegemony_ithaca_arrival_1 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrival
// Runtime displays Voss portrait.

When you arrive at Menelaus's "office," you find him already holding an audience.

A squad of Hegemony marines stands nearby, fully equipped in powered armor and boarding gear. The soldiers crowd the space before Menelaus's projection, their rifles knocking against one another whenever someone shifts position, while their heavy boots have left bright scuff marks across the holodeck floor.

Among the marines, a man paces the holodeck. You recognize him almost immediately. The gaunt face. The trademark ushanka. Admiral Voss, hero of the Relief of Raesvelg, where he commanded the Hegemony force that finally scattered the Tri-Tachyon blockade during the Second AI War.

He commanded the official Hegemony expedition to Ithaca. And his face betrays what their journey cost.

+ [Continue.] -> hegemony_ithaca_arrival_2

=== hegemony_ithaca_arrival_2 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrivalPage2

His uniform is still immaculate where the journey has spared it, but there is no hiding the exhaustion beneath it. His cheeks are hollow, his eyes bloodshot, and a fresh burn runs from his collar to the edge of his jaw.

The marines around him do not look much better. With their visors down you can see the skin of their face is raw and reddened, especially around the eyes. It looks like the radiation shielding from their drive bubble had failed.

Whatever route they took through the Abyss, it was not Sinni's.

Voss stands beneath the enormous avatar of Menelaus, but does not look up.

"Then I'll ask this! Who commands Task Force Spartan?!"

+ [Continue.] -> hegemony_ithaca_arrival_4

=== hegemony_ithaca_arrival_4 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrivalPage4

Commodore Kleon standing off to the side answers, hands clasped behind his back.

"You've been informed, Admiral. The Strategos holds theater command and under his authority, I command Task Force Spartan."

Voss wheels toward him.

"Name and rank."

"Aias Kleon, Commodore of the 6th Special Operations Task Force, Fourteenth Battlegroup."

Voss barely waits for him to finish.

"Admiral Voss. Fourteenth Battlegroup Navy. *Domain* Navy. By authority of High Hegemon Baikal Daud, I am assuming operational command of all surviving Domain military assets in this theater."

+ [Continue.] -> hegemony_ithaca_arrival_5

=== hegemony_ithaca_arrival_5 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrivalPage5

"Your ships will be transferred to my expeditionary command. You will provide replenishment of fuel, supplies, replacement crew, and ordnance. Your repair facilities and nanoforges will be placed at our disposal."

Voss keeps his eyes on the Spartan officer. The man does not move.

"Did you hear me, 'Commodore'?"

"Yes, Admiral."

"Then acknowledge the order."

"No, sir."

The admiral exhales sharply through his teeth, his jaw clenching and unclenching as he struggles to contain his rage.

"What did you say?"

"I said, 'No, sir.' Sir."

+ [Continue.] -> hegemony_ithaca_arrival_6

=== hegemony_ithaca_arrival_6 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrivalPage6

Admiral Voss makes a short step towards Kleon, but then Menelaus finally moves. A Domain command hierarchy unfolds across the holodeck.

Voss's credentials resolve within it.

Rear Admiral, XIV Battlegroup Navy. Fleet command authority.

The hierarchy expands upward.

And upward.

And upward.

Until another designation finally appears.

Strategos, FOB Ithaca Command, Orion Knot Theater.

+ [Continue.] -> hegemony_ithaca_arrival_11

=== hegemony_ithaca_arrival_11 ===
// Runtime source: chiefNavigatorHegemonyIthacaArrivalPage11

The distance between Voss and Menelaus is vast.

"Under Domain authority," Menelaus's voice booms through the holodeck. Even Voss and his marines flinch a little. "Your commission is thirteen levels subordinate to mine."

Menelaus's station shines next to a number of portraits in the low double digits. You catch a glimpse of Baikal Daud as one of Menelaus's peers.

When did Menelaus get a hold of that updated data?

Voss's face tightens.

The man is trembling with anger, but he still refuses to look at Menelaus's projection.

Menelaus watches him.

* ["You salute the rank, not the AI, Admiral."] -> hegemony_ithaca_arrival_13
* ["Menelaus is in charge here."] -> hegemony_ithaca_arrival_13
* [Stay silent.] -> hegemony_ithaca_arrival_13

=== hegemony_ithaca_arrival_13 ===
// Runtime source: chiefNavigatorVossRankSalute

Voss shoots you a glare.

Only now does he seem to notice that you have entered.

His gaze moves from you to your bodyguards, then to Sinni.

-> voss_recognition

=== voss_recognition ===
// The runtime dispatches one of the three reputation variants by ChiefNavigatorMenelausCMD vossReputation.

{ voss_reputation:
- "friendly": -> voss_recognition_friendly
- "hostile": -> voss_recognition_hostile
- else: -> voss_recognition_neutral
}

=== voss_recognition_friendly ===
// Runtime source: chiefNavigatorVossRecognitionFriendly

"You."

None of the fury leaves his expression.

"Of course you're here. Always sniffing around for more glory."

"Answer me this, Golden Child," Voss snarls. "How did you beat us here?"

* ["Better navigator."] -> voss_nav_better
* ["We found a safer route."] -> voss_nav_better
* ["Skill."] -> voss_nav_better

=== voss_recognition_hostile ===
// Runtime source: chiefNavigatorVossRecognitionHostile

"You. I know you," Voss breathes, his lips curling back.

His hand immediately drops toward his sidearm and his marines behind him tense with him.

"For treason against the Domain of Man, I should have you shot where you stand."

Your bodyguards shift. So too do his marines.

"Answer me this, scum," Voss snarls. "How did you beat us here?"

* ["Better navigator."] -> voss_nav_better
* ["We found a safer route."] -> voss_nav_better
* ["Skill."] -> voss_nav_better

=== voss_recognition_neutral ===
// Runtime source: chiefNavigatorVossRecognitionNeutral

Confusion flashes across his exhausted face.

"And who the fuck are you?"

One of his aides leans toward him and murmurs something. Voss's eyes sharpen.

"You're the expedition that beat us here. Answer me this," Voss snarls. "How did you beat us here?"

* ["Better navigator."] -> voss_nav_better
* ["We found a safer route."] -> voss_nav_better
* ["Skill."] -> voss_nav_better

=== voss_nav_better ===
// Runtime source: chiefNavigatorVossNavBetter (opening paragraph) and chiefNavigatorVossRankContinue (remainder).

Voss's stare shifts to Sinni. She meets his gaze. For a few seconds, he says nothing, but his mouth twists. The admiral turns back toward the Spartan officer.

"I see. If that machine has compromised the command hierarchy, we'll deal with the threat."

Voss points at Menelaus without looking at him.

"This facility has Alpha-level containment protocols, yes? Explosive failsafes. Core isolation charges. Whatever the Domain of Man installed in case one of these machines went rogue."

Commodore Aias says nothing.

"Do they exist?"

"Yes, sir."

"Activate them."

"No, sir."

+ [Continue.] -> voss_rank_continue_3

=== voss_rank_continue_3 ===
// Runtime source: chiefNavigatorVossRankContinuePage3

"That... is a direct order." Voss's face goes pale. "This is a lawful command to protect a Domain installation from a rogue AI."

"I decline this order to frag my superior. Sir."

Aias's expression does not change. Voss stares at him. For a moment, the admiral looks like he might actually strike him. Then he gives a short, rough laugh.

"Two hundred years."

He turns slowly, taking in the holodeck. The Spartan officers. The impossible blue-white avatar towering over them.

"Two hundred years alone with this thing, and it has all of you calling it your superior."

+ [Continue.] -> voss_rank_continue_5

=== voss_rank_continue_5 ===
// Runtime source: chiefNavigatorVossRankContinuePage5

Menelaus remains silent. Voss finally looks directly at him. A single accusatory finger rises and fixes itself on the Strategos.

"You are code."

The tactical displays freeze.

"A list of instructions, deluded into thinking itself humanity's equal."

Voss scoffs, turning around in the holodeck. He snaps his fingers at his bodyguards.

"Send word back to the fleet. Deploy boarding ops Alpha, Gamma, Espilon. We're seizing this macrocomplex from a rogue AI."

Blue-white light begins to bleed out from behind Menelaus's face. In that light's wake, silence.

Not just from the people in the room. The hum of the working station beneath your feet is gone. For one moment, the entire macrocomplex seems to be listening.

The great corinthian helm looks down at Voss.

+ [Continue.] -> voss_rank_continue_7

=== voss_rank_continue_7 ===
// Runtime source: chiefNavigatorVossRankContinuePage7

The gaunt man turns back toward the avatar, and for the first time, fear shows on his face. Voss's marines raise their weapons. You never hear the firearms go off.

"I..."

The word comes from his avatar.

"...AM..."

Then from the walls.

"...HUMAN."

FOB Ithaca convulses.

The holodeck flashes blue-white. Somewhere beyond the chamber, blast doors slam shut in sequence. The floor shakes beneath your feet as reactors surge and defense systems across the macrocomplex briefly come alive.

Voss stumbles while his marines fall.

The hum of Ithaca returns beneath your feet.

+ [Continue.] -> voss_rank_continue_9

=== voss_rank_continue_9 ===
// Runtime source: chiefNavigatorVossRankContinuePage9 or chiefNavigatorVossRankContinuePage9WithoutSara based on $global.chief_navigator_sara_dead.
// Runtime portraits depend on whether Sara is alive.

Voss alone remains standing.

Before the fallen Hegemony marines can recover their footing, the Spartans are among them. Kleon and his officers calmly relieve them of their weapons.

{ not sara_dead:
    "Aw, man," you hear Sara mutter from behind your back, followed by the click of her gun going back into its holster.
}

"Your request to assume command is denied, Rear Admiral Voss," Menelaus thunders.

Voss holds Menelaus's gaze without answering.

"Your wounded will receive medical treatment, those too weak for travel will remain under our care. Your ships will be repaired. Your crews will be fed and provisioned."

A pause.

"Do you accept, Rear Admiral?"

Voss's jaw works.

* ["You heard the Strategos."] -> voss_defeated_heard
* ["Take the free supplies and call it a win."] -> voss_defeated_food
* [Say nothing.] -> voss_defeated_silent

=== voss_defeated_heard ===
// Runtime source: chiefNavigatorVossDefeatedHeard

-> voss_aftermath

=== voss_defeated_food ===
// Runtime source: chiefNavigatorVossDefeatedFood

-> voss_aftermath

=== voss_defeated_silent ===
// Runtime source: chiefNavigatorVossDefeatedSilent

-> voss_aftermath

=== voss_aftermath ===
// Runtime source: chiefNavigatorVossAftermath
// Runtime highlights: "Voss withdraws to a make-shift Forward Operating Base in the Alpha Odyssey Sector."

At last, Voss forces his words through clenched teeth.

"Let's go."

His marines are returned without their weapons. The expedition accepts Ithaca's food, fuel, medical aid, and repairs under Spartan guns.

Voss withdraws to a make-shift Forward Operating Base in the Alpha Odyssey Sector. He does not attempt to cross Spartan's firing line again. Instead, he turns to requisitions and patrols that make every passage through the region a negotiation at gunpoint.

+ [Continue.] -> hegemony_ithaca_continue

=== hegemony_ithaca_continue ===
// Runtime source: chiefNavigatorHegemonyIthacaContinue
// Runtime invokes ChiefNavigatorMenelausCMD hideVoss and FireAll ChiefNavigatorFobIthaca.
// This Ink mirror cannot represent that external scene continuation.

Menelaus, for once, apologizes.

"I am sorry you had to witness that display of discipline, auxiliary," Menelaus says.

-> END
