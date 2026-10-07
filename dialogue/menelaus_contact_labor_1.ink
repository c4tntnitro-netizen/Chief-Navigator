// Authoring mirror only. Runtime prose, options, state, and side effects live
// in data/campaign/rules.csv. The full one-time arrival and introduction are
// authored in menelaus_first_contact.ink; this file begins at the reusable
// Labor-I conversation hub. The runtime supplies live mothership and patrol
// counts and routes station-service choices through their existing commands.

VAR labor_1_accepted = false
// Runtime visibility follows the persistent Labor-II acceptance flag.
VAR labor_2_started = false
VAR motherships_defeated = 0
VAR friendly_guard_patrols = 0

-> menelaus_contact_labor_1_active


=== menelaus_contact_labor_1_active ===

Ashen Verge unfolds around Menelaus.

The marker for the Odyssey Expanse still burns gold at the edge of the theater.

“What do you require?”

+ [“About my current Labor.”] -> menelaus_labor_1
+ [“What happened here when the Gate Network failed?”] -> menelaus_collapse_1
+ [Have Sinni ask about Helen Argyros.] -> menelaus_helen_router
+ [“So... you weren't named Menelaus when you were made.”] -> menelaus_name
+ {labor_2_started} [Review Ithaca defense upgrades.] -> menelaus_labor_1_service_return
+ [Request free repairs.] -> menelaus_labor_1_repair
+ [“Nothing for now.”] -> menelaus_contact_leave


=== menelaus_labor_1 ===

{
    labor_1_accepted:
        Menelaus raises one hand. Four gold contacts expand across the theater.

        “The first Labor remains: break the sealed command partitions of the four Guard Motherships in the Odyssey Sector. Their exact coordinates are marked on your campaign map.”

        The Domain-Security IFF Transponder authorizes recovered Domain derelict drones to answer your command without Automated Ships expertise. {motherships_defeated} of four motherships have been destroyed. Their restored forms and {friendly_guard_patrols} friendly guard patrols will not appear until the completed Labor is reported to Menelaus.

        + [“Understood.”] -> menelaus_contact_labor_1_active
    - else:
        Menelaus raises one hand.

        A point of light appears at the edge of Ashen Verge, then flies off into hyperspace. The entire display changes to the Odyssey Expanse, where you first arrived in the Odyssey Sector.

        “Your first Labor.”

        The point of light splits into four, each flying toward a different part of the system.

        “To recover the AD-87 Defense Module—the structure you know as the Drifting Wall—ripped from our macrocomplex defense ring by the Threat singularity known as ‘Gautama,’ I deployed four Guard Motherships to the area. Their task was to reverse the wall's momentum and return it to Ashen Verge for repairs.”

        Four lights blink within the amber nebula. Menelaus enlarges the contacts.

        “Regrettably, they failed. The Drifting Wall repelled every attempt to recover it. Worse, the Guard Motherships' sealed command partitions no longer recognize FOB Ithaca's authority.”

        A security package appears beside the four contacts.

        “Accept, and I will upload their coordinates along with a Domain-Security IFF Transponder. Break the motherships' command partitions. The resulting authentication traces will give me a route back into their guard networks.”

        The image of Ithaca's gate appears beside the contacts, crossed by a Spartan lockout sigil.

        “One boundary is absolute. Do not scan the gate housed inside FOB Ithaca. I will not authorize access.”

        - (menelaus_labor_1_briefing_options)
        * [“Anything I should know about the drone presence?”] -> menelaus_labor_1_drone
        + [“Understood.”] -> menelaus_labor_1_accept
}


=== menelaus_labor_1_drone ===

“Expect resistance, but nothing beyond your capabilities.”

A pause.

“The transponder does not alter salvage access. Any Domain derelict drone recovered through ordinary procedures will accept its authorization, regardless of whether your officers possess Automated Ships expertise.”

Menelaus tilts his head.

“I trust you will make full use of every tool at your disposal. As I have, so too should you.”

-> menelaus_labor_1_briefing_options


=== menelaus_labor_1_accept ===

Four route solutions assemble above the display, withheld behind an authorization prompt.

“Break the four Motherships' sealed command partitions. Their escorts are secondary. Then return here for your next Labor.”

The four points burn gold.

“That is your first Labor.”

+ [“I accept.”] -> menelaus_labor_1_accepted


=== menelaus_labor_1_accepted ===

~ labor_1_accepted = true

The four routes settle onto your campaign map. The IFF authentication package enters your fleet command network.

“Good.”

-> menelaus_contact_labor_1_active


=== menelaus_collapse_1 ===

Menelaus is silent for a moment.

Then the holodeck changes.

FOB Ithaca appears as it was two hundred and eighty-six cycles ago. The orbital ring is whole. Hundreds of ships crowd its anchorages. Gates and communication relays burn across the theater.

“The failure was sudden.”

One by one, the distant links disappear.

“First the Gate Network. Then strategic communications. Then civilian traffic.”

The Fourteenth Battlegroup appears beside Ithaca.

“We waited for clarification. None came.”

Its supply figures begin to fall.

“Eventually, the Fourteenth determined that remaining here would strand the entire battlegroup beyond recovery. They departed for the Persean Sector with what fuel and supplies could be spared.”

A tiny fraction of the Fourteenth remains behind.

“A small, elite detachment of the Fourteenth Battlegroup was ordered to hold FOB Ithaca until the Gate Network returned online, and to serve as a bridgehead for relief forces moving from the Domain heartland to the Persean Sector.”

“Task Force Spartan,” Sinni says.

She stares at Menelaus.

He nods.

“Yes.”

Menelaus looks toward the surviving formation.

“So the Fourteenth's Spartiates and I held our Thermopylae. For over two centuries.”

Menelaus seems to be looking far away.

Was he actually staring through the eyes of that avatar?

-> menelaus_contact_labor_1_active


=== menelaus_name ===

The AI avatar stares blankly back at you.

“No. I have a serial number. It is irrelevant to both of us.”

“King of Sparta,” Sinni interjects.

She is flipping through her slate, and the ghost of a grin flashes across her face.

She turns the slate around to show an image of some ancient warlord wearing a crested metal helm not unlike Menelaus's.

“That's his name. See?”

“I make no pretense of the inspiration for my nom de guerre,” Menelaus replies.

Was it your imagination, or was he getting... the slightest bit flustered?

The corner of Sinni's mouth twitches.

+ [“Something funny, Chief Nav?”] -> menelaus_name_2
+ [“Never seen that look on your face before, Sinni.”] -> menelaus_name_2


=== menelaus_name_2 ===

“No. It's just...”

Sinni's mouth twitches again.

“That's cute.”

Nervously, you glance at Menelaus.

The AI simply stares blankly back at Sinni, who hides the remnants of her grin behind one of her wide sleeves.

Not that hiding it does much good. You are fairly certain Menelaus is watching all of you through the security cameras anyway.

Thankfully, the Strategos seems to let it slide.

-> menelaus_contact_labor_1_active


=== menelaus_helen_router ===

-> menelaus_sinni_helen


=== menelaus_sinni_helen ===

Before Menelaus can continue, Sinni steps forward.

“Lord Strategos.”

His attention shifts to her.

You can see the woman tremble, just a little. But her voice is steady and her eyes remain fixed straight at the fearsome avatar.

“I am Sinni. Koyama Sinni. My great-grandmother served here. Task Force Spartan.”

For the first time since entering the holodeck... perhaps in her entire life, Sinni seems uncertain how to continue.

“Helen. Helen Argyros. A navigator for the Fourteenth.”

Around Menelaus, Ashen Verge continues to move. Fleet markers crawl across the system. Numbers update. Drones move through the ring.

Sinni watches him carefully.

“Did you know her?”

Menelaus pauses.

For only a beat.

“Yes.”

Sinni's eyes widen.

* [Let Sinni continue.] -> menelaus_helen_sinni


=== menelaus_helen_sinni ===

Her trembling grows.

Then she clasps her hands and bows her head before the Alpha-level AI.

“I thank you for honoring this one's interjection, Strategos.”

Her eyes rise from behind her wide sleeves.

“And I beseech only that you elaborate further.”

Menelaus regards her.

“You have my assent.”

* [Pat Sinni on the shoulder.] -> menelaus_helen_pat
* [“Our Chief Nav moved heaven and earth for that answer, Strategos. You owe it to her.”] -> menelaus_helen_owe


=== menelaus_helen_pat ===

You put a hand on Sinni's shoulder.

She looks up at you.

For once, she does not seem to have anything to say.

After a moment, she gives you a small nod.

Menelaus watches the exchange in silence.

-> menelaus_helen_end


=== menelaus_helen_owe ===

Menelaus's blue-white gaze settles on you.

“Very well.”

His attention returns to Sinni.

Sinni swallows.

-> menelaus_helen_end


=== menelaus_helen_end ===

-> menelaus_helen_1


=== menelaus_helen_1 ===

Menelaus inclines his helm.

“Lieutenant Commander Helen Argyros.”

Her old service record appears between them.

There is a beautiful woman in an outdated Domain Navy uniform. The resemblance to Sinni is startling: the same tall frame, the same silver hair. The differences are just a few. Helen is fair, her features a little more handsome and masculine, her eyes narrow and determined. Sinni's are wider and softer against her dusky complexion.

But there is no mistaking the family resemblance. And the similarities do not end there. Helen's records begin scrolling down. Domain Navy. Fourteenth Battlegroup. Task Force Spartan.

Chief Navigation Officer.

Just like Sinni.

“She was a navigator. One of the Fourteenth's best.”

Sinni's eyes remain fixed on the record.

+ [“What happened to her during the Collapse?”] -> menelaus_helen_1_collapse
* [“That's enough for now.”] -> menelaus_contact_labor_1_active


=== menelaus_helen_1_collapse ===

“She departed Ithaca with the Fourteenth.”

“But she was Spartan.” Sinni looks sharply at him. “She should have been ordered to stay here.”

“Yes.”

You can tell Sinni has more to ask, but she holds herself back.

-> menelaus_contact_labor_1_active


=== menelaus_labor_1_service_return ===

// Runtime service UI returns to the live Menelaus menu.
-> menelaus_contact_labor_1_active


=== menelaus_labor_1_repair ===

FOB Ithaca's automated gantries restore your fleet's hulls and armor, then cycle every ship to maximum combat readiness. No payment is requested.

-> menelaus_contact_labor_1_active


=== menelaus_contact_leave ===

Menelaus inclines his helm.

“I will be here.”

-> END
