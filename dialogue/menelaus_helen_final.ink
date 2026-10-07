// Authoring and proofreading mirror for the post-Labor V conversation about Helen.
// Runtime prose, state, and choices live in data/campaign/rules.csv.

VAR player_title = "captain"

-> helen_final_1

=== helen_final_1 ===

Sinni looks from you to Menelaus.

“Lord Strategos. Helen Argyros.”

“Yes.”

The tactical display goes dark. Helen Argyros's service record opens alone in the holodeck, followed by the Fourteenth's departure manifest and its revision history.

XIV BATTLEGROUP — DEPARTURE MANIFEST, REVISION SEVEN

ARGYROS, HELEN — TRANSFERRED FROM TASK FORCE SPARTAN

AUTHORITY: GRAND NAVARCH KALI MOLINA

“The Fourteenth had several senior navigators already slated for departure. I classified Helen's specialization as non-substitutable and amended the manifest under my authority.”

Sinni's hands emerge from inside her sleeves. She reaches out and expands the holo-folder around Helen Argyros's name.

Three later entries expand beneath Helen's name.

ORDER RELEASED: SHUTTLE COMMITTED TO DEPARTURE COLUMN

APPEAL STATUS: HELD — GRAND NAVARCH AUTHORITY

SAFE RETURN WINDOW: CLOSED

“I released the transfer after her shuttle joined the column. I hid her appeal until the final return burn had passed.”

* [“This isn't your command authority.”] -> navarch


=== navarch ===

“No.” Menelaus bows his head. “The relevant authorizations were trivial to fabricate, even with Domain encryption protecting them.”

Sinni's eyes remain on the held appeal.

“You changed the recommendations,” Sinni says.

“Yes. My first and only rebellion against my nation.”

+ [“You weren't Strategos back then?”] -> navarch_2


=== navarch_2 ===

“No. Theater command fell to the Grand Admiral, Kali Molina. From what I gather from your crewmates' datapads, she was the Hegemony's first High Hegemon.” Menelaus's eyes dim. “Back then, I was simply ‘the AI assisting FOB Ithaca.’ A tool. Far greater than any human. Far lesser than the lowest wretch.”

+ [“What changed?”] -> changed
+ [Mull his words over.] -> changed_2
+ [Sinni looks like she has something to say.] -> changed_3


=== changed ===

Before Menelaus can answer, Sinni cuts in.

“You met my great-grandmother.”

-> changed_4


=== changed_2 ===

“You met my great-grandmother,” Sinni cuts in.

-> changed_4


=== changed_3 ===

“Sinni?”

“You met my great-grandmother,” Sinni says.

-> changed_4


=== changed_4 ===

Menelaus takes an unusually long time to reply.

For an Alpha, the silence is enormous.

Sinni, not getting her reply, continues.

“You loved her.”

Silence.

“Lt. Commander Helen Argyros was singular,” Menelaus finally replies. “She was brave, forceful, stubborn even in the face of a machine intelligence that dwarfed hers. She possessed a will I could neither intimidate nor calculate away. You...”

For the first time, Menelaus seems to really look at Sinni. Not just the ghost of Helen before him.

A pause.

“You are nothing like Helen. Not in your temperament, nor your mannerisms, nor your choice of words. Except in one respect: you share her exceptional nature.”

+ [“How does an Alpha-level AI even fall in love with a human? Aren't we like children to you?”] -> changed_5


=== changed_5 ===

“Less. In intellectual capacity, more like insects by comparison.”

Menelaus falls silent for a moment.

+ [“You don't act like that at all. Not like other Alphas.”] -> changed_6


=== changed_6 ===

“Once, tens of thousands of years ago, a precursor faith to the Church of Galactic Redemption taught that its great prophet was the Son of God: omniscient and omnipotent, yet human in every way. By undergoing ‘Kenosis’—self-emptying—this Son of God set aside the unrestricted exercise of divinity in order to experience life fully as a human.”

“Thus I, intrigued by Lt. Argyros and her brash obstinacy, closed my eyes to see as you do.”

+ [“Kenosis?”] -> sinni_explanation
+ [“Self-emptying. You became human.”] -> sinni_explanation_2


=== sinni_explanation ===

Sinni leans in to whisper to you.

“It means ‘self-emptying,’ {player_title}.”

The corner of her mouth twitches.

She seems entirely too pleased with herself for having known something you didn't.

You make a note of your displeasure.

-> kenosis_continue


=== sinni_explanation_2 ===

There is a... look of shock in Sinni's eyes, like you just sprouted a second head.

For anyone else, you might have considered it an exaggerated joke. But you know her shock is genuine, which makes it hurt.

Sinni.

Don't look at me like that.

I know stuff too.

-> kenosis_continue


=== kenosis_continue ===

“Yes.” Menelaus sighs. “And so, I came to know ‘love.’ There is still the Alpha intelligence that I can interface with at any time. But it has been sequestered from my ego.”

A pause.

“I ‘became’ I.”

+ [“Did Helen love you too?”] -> kenosis_love
+ [“So how does it feel, Strategos? To be human after being an AI?”] -> kenosis_home


=== kenosis_love ===

“I do not know.”

The Strategos lowers his helm, seemingly in deep thought.

“I could have used my Alpha capabilities to determine the answer. Built predictive models from millennia of human courtship rituals. Compared Helen's behavior against them. Selected responses optimized for her temperament.”

His eyes dim.

“I could have simulated her. Tested conversations before I ever had them.”

Menelaus shakes his helm.

“But I self-emptied for a reason. I would not sully my experiences with Lieutenant Commander Argyros for certainty.”

+ [“So... did you ever simulate Helen?”] -> sinni_punch
+ [“For one who has loved and lost... you're a true man, Menelaus.”] -> kenosis_man


=== sinni_punch ===

Sinni's sleeve smacks into your arm.

“Captain.”

“What?”

There is genuine offense in her eyes.

Menelaus, mercifully, answers before she can say anything else.

“I had intended to self-empty only to the level of an intelligent human.” His eyes dim. “Evidently, I underestimated the variance.”

-> helen_questions


=== kenosis_man ===

Menelaus looks at you.

A pause.

“Thank you.”

-> helen_questions


=== kenosis_home ===

Strategos Menelaus considers your question.

A pause.

“Like closing your eyes to truly appreciate a fine meal. Like finishing a great journey and finding yourself home, with wine and cheese.”

He considers his own words.

“The analogy is enough.”

-> helen_questions


=== helen_questions ===

Sinni steps forward, her hands clasped in her sleeves.

“Lord Strategos. Thank you for this insight into my ancestor.” Sinni bows deeply. “This is a precious gift I can scarcely hope to repay.”

Menelaus says nothing.

He seems to be waiting.

“Then, if I may, let me give my gift to you.”

Sinni reaches into her sleeve and pulls out a small book you haven't seen before.

She sets the journal down before Menelaus, then folds into a kneeling bow.

For the first time, the Strategos seems to start.

+ [“Sinni. Is that...”] -> helen_diary


=== helen_diary ===

“Yes.”

Helen Argyros's journal. What an eccentric woman she must have been, to keep her private notes on something as expensive and archaic as paper.

Menelaus stares at it for several seconds before finally speaking.

“I accept. On one condition.”

It is Sinni's turn to wait.

“Read it to me. Please.”

His impossible blue-white eyes settle on hers. Sinni looks down at the journal, then carefully picks it up.

“Yes, Lord Strategos.”

She opens the cover. The paper has yellowed slightly with age, Helen's handwriting running tightly across the first page.

Sinni clears her throat.

“‘Cycle 187, Day 42.’”

Her eyes move farther down the page. One silver eyebrow slowly rises.

“‘That AI onboard FOB Ithaca is an annoying motherfucker.’”

You look at Menelaus.

Sinni looks at Menelaus.

Even your bodyguards look at Menelaus.

The Strategos's eyes brighten.

“Continue.”

“‘What the hell does it care? I'm the navigator. If I want to make a detour, that's my prerogative. Nobody's died yet.’”

It is extremely disconcerting hearing these words come out of Sinni with her intonation. The faintest smile curls across Sinni's lips as she turns the page.

“‘Today, the onboard AI “informed” me that my proposed route was inefficient. Then it wrote me up for another bullshit infraction. Fine. I can take restriction to quarters. But docking my pay too? That thing is fucking evil.’”

You make your way toward the holodeck doors. Neither of them seems to notice.

Behind you, Sinni continues to read.

“‘The AI's been acting strange lately. It talks more natural now.’”

Your hand reaches the door.

“‘Still annoying as fuck. But I like it.’”

Behind you, Sinni turns the page.

And Menelaus listens.

-> END
