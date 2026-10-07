// Authoring/proofreading excerpts of the marine-subdual and aftermath pages.
// The full confrontation and continuation live in data/campaign/rules.csv.
// Mirrors chiefNavigatorVossRankContinuePage9 and its WithoutSara variant.

VAR sara_dead = false

=== voss_marines_subdued ===

The Spartans managed to subdue Voss' marines with frightening speed. Before they can recover, Aias and the other Spartan officers calmly strip the weapons from their hands.

{ not sara_dead:
    "Aw, man," you hear Sara mutter from behind your back, followed by the click of her gun going back into its holster.
}

"Your request to assume command is denied, Rear Admiral Voss," Menelaus thunders.

Voss opens his mouth, but no words come out.

// The runtime Continue proceeds to chiefNavigatorVossRankContinuePage10.
+ [Continue.] -> DONE

=== voss_aftermath ===

At last, Voss forces the word through clenched teeth.

"Yes."

His marines are returned without their weapons. The expedition accepts Ithaca's food, fuel, medical aid, and repairs under Spartan guns.

// Runtime: highlight the first sentence of this paragraph yellow.
Voss withdraws to a make-shift Forward Operating Base in the Alpha Odyssey Sector. He does not attempt to cross Spartan's firing line again. Instead, he turns to requisitions and patrols that make every passage through the region a negotiation at gunpoint.

// The runtime Continue returns to Menelaus and the Labor-IV briefing.
+ [Continue.] -> DONE
