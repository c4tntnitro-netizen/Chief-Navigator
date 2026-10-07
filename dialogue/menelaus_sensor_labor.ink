// Authoring mirror only. Runtime prose and options live in data/campaign/rules.csv.

VAR sinni_helen_stated = false

=== menelaus_sensor_labor ===
Menelaus appears beneath the scarred Greek helm. A tactical map of Devoured Reach turns beside him, almost every approach webbed with Starving Threat contacts.

"Your fourth Labor is observation. A black-box sensor package is already integrated into your fleet systems. It occupies no cargo space. Reach the Mara Observation Array in Devoured Reach and run it while no hostile fleet is tracking you."

"If your fleet lacks a phase ship, I have uploaded the location of the TTS Penumbra, a recoverable Doom-class cruiser adrift in Sanzu. Its recovery is optional."
-> END

=== sensor_array_clear ===
Your fleet reaches the Mara Observation Array. The surrounding Threat contacts have not detected your approach.

[Deploy the sensor package]
Your operations crew runs Menelaus's black-box package through the array. It transmits one compressed burst toward Ithaca, then deletes itself from your fleet systems.

Return to FOB Ithaca and report to Menelaus before he opens the fifth and final Labor.
-> END

=== sensor_array_detected ===
Your fleet reaches the Mara Observation Array, but a hostile fleet is tracking your movements. Holding position would expose the operation.
-> END

// Excerpt of the Labor-IV briefing's leave-Sinni exchange.
=== menelaus_leave_sinni ===
"There is one further request."

His gaze shifts from you to Sinni.

"Leave Chief Navigator Sinni at Ithaca."

Sinni goes still.
~ sinni_helen_stated = false

+ ["Sinni is instrumental to our fleet. She comes with us."] -> menelaus_leave_sinni_refuse
+ ["Sinni?"] -> menelaus_leave_sinni_choice
+ ["Something's different. Why are you asking now?"] -> menelaus_leave_sinni_why

=== menelaus_leave_sinni_refuse ===
{ not sinni_helen_stated:
    Sinni meets Menelaus's gaze. "I am not Helen."
}

Menelaus regards you for a moment.

Then inclines his helm.

"Your command. Your decision."

Sinni looks toward you.

A small bow behind her sleeves.

"Captain. Our fates are one."
-> menelaus_leave_sinni_end

=== menelaus_leave_sinni_choice ===
You look toward Sinni.

Menelaus turns to her as well.

Sinni does not hesitate.

"No."

Menelaus's eyes narrow slightly.

"Chief Navigator—"

{ sinni_helen_stated:
    "Lord Strategos. With respect."
- else:
    "Lord Strategos. With respect. I am not Helen."
}

Sinni raises her clasped hands, bowing behind her wide sleeves. But she raises her eyes to stare down Menelaus from behind her silken veils.

"I have endured the crashing waves of the Abyss and fought through the slings and arrows of marauders and monsters to get where I am today."

Sinni is talking faster and faster.

"I live by my Captain's fleet. I shall die with it too."

She bows her head, then releases her salute, standing up straight. Her face turns neutral again.

"I am going."

Menelaus is silent for a moment.

Then a single word.

"Very well."
-> menelaus_leave_sinni_end

=== menelaus_leave_sinni_why ===
~ sinni_helen_stated = true
"You've never asked me to leave one of my officers behind before."

Menelaus says nothing.

"Why Sinni?"

His gaze moves toward the red mass of the Devoured Reach.

"Because this Labor is exceptional."

+ ["So this is some suicide mission."] -> menelaus_leave_sinni_why_reply

=== menelaus_leave_sinni_why_reply ===
"No." Menelaus raises the helm of his avatar. "I value your fleet immensely, Captain. I have confidence in your capabilities and fully expect your triumphant return."

You wait. Menelaus's attention returns to Sinni.

"But still. If there is even the slightest risk to the Chief Navigator, I must ask."

Sinni's expression changes. Only slightly. But she is even harder to read now than usual. Her face is stony and unmoved.

"Lord Strategos." Sinni bows, lowering her head. "I am not Helen."

Menelaus does not answer the name.

+ ["Request denied. She's coming with us."] -> menelaus_leave_sinni_refuse
+ ["Sinni?"] -> menelaus_leave_sinni_choice

=== menelaus_leave_sinni_end ===
Menelaus dismisses the matter.

The Mara Observation Array burns gold against the Devoured Reach.

"That is your fourth Labor."

// Runtime acceptance records Labor IV and opens its reusable menu.
+ ["I accept."] -> END
