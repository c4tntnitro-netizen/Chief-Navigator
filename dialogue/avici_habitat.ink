// Authoring and proofreading mirror for the rules-driven Kshaya Habitat encounter.
// Runtime state and side effects are owned by ChiefNavigatorAviciHabitatCMD.
// Runtime prose and options live in data/campaign/rules.csv.

VAR player_title = "Captain"
VAR danger_level = 0
VAR heavy_weapons = false
VAR ate_food = false
VAR rescued_mother = false
VAR fight_occurred = false
VAR habitat_destroyed = false
VAR habitat_abandoned = false
VAR sara_dead = false

-> inhabited


=== inhabited ===

A battered habitat circles Kshaya's ash-gray curve.

Its registry describes a self-contained home for over ten thousand people: agricultural decks, workshops, schools, habitation rings, everything necessary to support a small city indefinitely.

Passive scans count fewer than a hundred living human beings.

Most of the structure is dark.

Entire habitation blocks are deep navy blue on your thermals. Several docking arms hang open to vacuum, and the agricultural section shows no meaningful heat signature at all. What little power remains has been rerouted toward a handful of compartments deep inside the station.

+ ["Continue."] -> inhabited_2


=== inhabited_2 ===

Your fleet pings the habitat.

Once.

Then again.

No response.

The transponder still broadcasts its ancient registry code, repeating the same identification packet it must have been transmitting for generations.

A sensor officer looks up from their console.

"Life support is active in the central habitation ring. No orbital defense emissions detected."

Another pause.

"No distress beacon either."

"Ops. Fine-grained sweep."

"Aye, {player_title}."

A millimeter-wave sweep passes through the habitat. The ops screen sends back fewer than a hundred signatures.

Over ten thousand.

Now fewer than a hundred.

"{player_title}," Ops says. "How shall we proceed?"

+ ["We don't want hostilities. Concealed weapons and personal armor only."] -> inhabited_light
+ ["...No risks. Full boarding kit."] -> inhabited_heavy


=== inhabited_light ===

~ heavy_weapons = false

"Understood."

The camera changes to a view of your ops team, some of them your personal bodyguards, preparing in the shuttle bay.

Your boarding party trades combat harnesses for clothes loose enough to conceal sidearms and light armor beneath them.

-> boarding


=== inhabited_heavy ===

~ heavy_weapons = true
~ danger_level += 1

"Understood."

The camera changes to a view of your ops team, some of them your personal bodyguards, preparing in the shuttle bay.

The bay fills with the hard clicks of weapons being checked and power armor being sealed. Rifles, breaching charges, grenades.

-> boarding


=== boarding ===

Your boarding shuttle leaves the flagship.

No controller answers your approach, but the habitat accepts the docking request automatically. One of the surviving docking collars opens ahead of you.

The shuttle slips inside.

Docking clamps engage with a heavy metallic thud.

"Atmosphere breathable," your pilot reports. "Pressure equalized."

The hatch opens.

Warm, stale air rolls into the shuttle.

Your team goes aboard.

The concourse beyond was built for hundreds of people at a time. Dead kiosks line the walls beneath faded advertisements and transit maps. Dust covers most of the deck, disturbed by narrow trails of recent footprints.

The deeper your team moves into the habitat, the warmer it becomes.

Then someone steps into the corridor ahead.

Another follows.

Then several more.

-> cult_reception


=== cult_reception ===

The survivors are thin, their clothes patched together from generations of older clothes. Strips of yellow cloth hang from wrists, necks, and doorways, each marked with the same insignia. It looks like a gaping maw inscribed within a circle.

"Gun," Yvan mutters through his mic. "Gun left. The short one with the limp."

"Gun right," Sara whispers. "That kid with the veil."

Through the Ops cams, you can see them squint as the flashlights of your boarding team cross their eyes. Aggressively so; some moan in discomfort at the brightness. Have these people not seen light before?

An elderly man stands at their front.

His eyes move across your boarding party.

{heavy_weapons:
    They linger on the rifles, armor, and sealed helmets.

    The people behind him draw closer together. Nobody reaches for a weapon.

    Yet.
}

"Visitors," he says.

The word moves through the crowd in whispers.

Visitors.

Visitors.

The old man smiles.

"Budai provides."

* ["Yvan. Ask them where the rest of the habitat is."] -> cult_survivors
* [Ask them why they didn't answer our hails.] -> cult_hails


=== cult_hails ===

Yvan repeats your query, word for word.

"We heard you."

The old man's smile remains.

"We were simply preparing our humble home for our guests."

Someone behind him laughs softly.

-> cult_survivors


=== cult_survivors ===

Yvan looks over the crowd.

"We counted fewer than a hundred people aboard. This habitat was built for over ten thousand."

The old man nods.

"Eighty-seven."

"And the rest?"

The old man doesn't answer. Instead, he gestures toward the corridor behind him.

"Come. You have traveled far. You should join us."

-> cult_meal


=== cult_meal ===

They receive your team in what was once a communal dining hall.

Most of the tables have been dismantled for parts. The survivors gather around the few that remain beneath painted images of the same smiling maw. At the center, a small fire burns.

Metal bowls are placed before the boarding party.

Rice.

Root vegetables.

Strips of heavily seasoned meat.

The old man takes his seat across from your team.

"Please."

* ["Don't piss them off. Take the food."] -> cult_food_eat
* ["Don't eat it."] -> cult_food_decline


=== cult_food_eat ===

~ ate_food = true
~ danger_level += 1

Some of your team take a few bites.

Across the table, Sara frowns.

She picks up one of the remaining pieces and passes a small bioscanner over it.

The device chirps.

Sara freezes.

"Sergeant," she whispers. You can hear her voice tremble with anger.

Before anyone can stop her, Sara pulls out her gun and shoves it in the old man's face. "It's human protein, these bastards."

The leader doesn't flinch.

"We don't fear death."

+ [Continue.] -> cult_truth_2


=== cult_food_decline ===

The old man inclines his head.

"As you wish."

He eats from his own bowl.

Around him, the others begin doing the same.

* ["What happened to everyone who lived here?"] -> cult_truth


=== cult_truth ===

The old man looks toward the faded image of Budai above the hall.

"The agricultural decks failed before my grandfather's grandfather was born. Then the reserves failed. Then the machines that made the reserves last."

His spoon rests against the bowl.

"Budai taught us that nothing given to the congregation should be wasted."

"When one of us dies, they remain with us."

* ["You eat your dead."] -> cult_cannibal
* ["We can evacuate you. All of you."] -> cult_evacuation


=== cult_truth_2 ===

The old man looks toward the faded image of Budai above the hall.

"The agricultural decks failed before my grandfather's grandfather was born. Then the reserves failed. Then the machines that made the reserves last."

His spoon rests against the bowl.

"Budai taught us that nothing given to the congregation should be wasted."

"I'll teach you all about being wasted," Sara breathes.

Usually by now she'd be admonished by Wei or Yvan.

Not this time.

All of your team is thumbing their weapons now.

* ["Team. Get the hell out. Now."] -> mother_contact_0


=== cult_cannibal ===

"Yes."

No shame.

"Those who came before sustain those who remain."

The old man folds his hands.

"One body. One congregation."

-> cult_evacuation


=== cult_evacuation ===

"We have food. Medicine. Ships," Yvan offers. "We can take every living person aboard this station."

For the first time, unease moves through the room.

The old man raises one hand.

The murmuring stops.

"No."

* ["Your habitat is dying."] -> cult_refusal
* ["There are eighty-seven of you left."] -> cult_refusal
* ["Why?"] -> cult_refusal


=== cult_refusal ===

"Budai has kept us alive."

The old man looks toward the smiling figure on the wall.

"He will decide when we are permitted to leave."

There is nothing uncertain in his voice. The conversation is over.

Your team prepares to return to the shuttle.

-> mother_contact


=== mother_contact_0 ===

Slowly, carefully, your team begins extracting from the mess hall, guns raised and trained on every one of the cannibals.

-> mother_contact


=== mother_contact ===

Your team is halfway back to the docking concourse when a woman catches Yvan's sleeve.

Thin. Younger than most of the survivors.

A boy of perhaps four or five stands behind her, clutching the back of her robes.

"Please."

She looks over her shoulder before continuing.

"Take us with you."

"{player_title}?" Yvan thumbs his mic, watching the corridor behind her. "What's the call?"

{danger_level > 0:
    Farther down the corridor, several cultists are already watching your party. The mother is not in view, hidden by the corner.

    They have been watching since the moment your team left the dining hall.
}

* ["Why won't they let you leave?"] -> mother_why
* [Decide what to do.] -> mother_rescue_choice


=== mother_why ===

"Because we belong to the congregation."

She looks toward her son.

"Alive."

A pause.

"And afterward."

The boy presses closer to her.

"Please."

The mother's pleading gets more and more desperate.

"We were going to be next."

-> mother_rescue_choice


=== mother_rescue_choice ===

{danger_level == 0:
    // GOLD HIGHLIGHT
    + [It doesn't look like you're being followed. Quietly get the mother and her son to the shuttle.] -> rescue_sneak
}

{heavy_weapons:
    // GOLD HIGHLIGHT
    + [Go guns blazing. Get them out.] -> rescue_fight_heavy
}

{danger_level > 0 && not heavy_weapons:
    + ["They're coming with us."] -> rescue_fight_light
}

+ ["Tell them we'll come back for them."] -> rescue_leave


=== rescue_sneak ===

~ rescued_mother = true

Nobody is watching your party closely enough.

Not yet.

You send the boarding team ahead in small groups while Sara takes the woman and her son through a maintenance passage running parallel to the main concourse.

For several minutes, nothing happens.

Then your comm clicks.

"Shuttle secure," Sara whispers. "Both aboard."

Your team makes its way through the abandoned station.

By the time anyone realizes the habitat has two fewer inhabitants, the docking clamps have already released.

Kshaya falls away beneath the shuttle.

The woman holds her son against her chest as the shuttle returns to your fleet.

-> rescue_end


=== rescue_fight_heavy ===

~ rescued_mother = true
~ fight_occurred = true

You watch through Yvan's helmet cam as he looks toward the cultists gathering farther down the corridor.

Then toward your boarding team.

You came prepared for this.

+ ["Red team." Your Ops calls through his mic. "Weapons hot."] -> rescue_fight_heavy_2


=== rescue_fight_heavy_2 ===

"Gladly, Tac-com," Yvan breathes.

Your team moves as one.

Power armor seals. Rifles rise. The corridor erupts in gunfire. You watch as Sara wraps the mother in an embrace and starts running her and the boy toward the shuttle.

The cultists have numbers.

Your team has power armor, grenades, and two hundred years' worth of small-arms advancement over the cultists' antiquated weapons.

The violence is brief.

By the time the last shell casings stop clinking, over a dozen cultists are left moaning or choking on the ground. Most aren't moving at all.

"Red team, move!" your Ops snaps.

Your team withdraws toward the shuttle through smoke and emergency lighting.

No one follows.

-> rescue_end


=== rescue_fight_light ===

~ rescued_mother = true
~ fight_occurred = true
~ sara_dead = true

// The selected line is already printed by Starsector before this destination.
The woman grabs her son.

The cultists move almost immediately.

So does your security detail.

Concealed sidearms clear clothing. Someone fires from farther down the corridor, and suddenly the narrow passage is nothing but bodies, shouting, and muzzle flashes.

Sara shoves the boy behind an access column.

A shot catches her beneath the arm.

She staggers.

Another hits her before Wei can pull her back.

"Sara!" He yells, but she goes down.

There is no time to stop.

Your team fights its way toward the docking concourse, dragging Sara, the mother, and her son with them.

Yvan is the last aboard.

The shuttle hatch slams shut. Only when the clamps release does anyone look toward Sara's seat.

It is empty.

She lies motionless on the shuttle deck instead.

-> rescue_sara_dead


=== rescue_sara_dead ===

Nobody speaks on the flight back.

The mother holds her son.

Wei stares at the deck.

Yvan still has Sara's blood across one sleeve.

Kshaya's habitat shrinks behind the shuttle.

You got them out.

At the cost of Sara.

-> rescue_end


=== rescue_leave ===

"No."

Yvan shakes his head.

Over comms you can see Sara yelling up a storm into her mic, but Ops silences her before she can start.

The mother's face empties.

For a moment, she says nothing.

Then she releases Yvan's sleeve.

Your boarding party continues toward the shuttle.

Through Yvan's helmet cam, you watch the woman and her son remain standing in the corridor.

The cultists close around them.

-> rescue_end


=== rescue_end ===

The shuttle clears the habitat and returns to your fleet.

On the tactical display, the old station continues its silent orbit around Kshaya.

Eighty-seven lives.

{fight_occurred:
    Fewer now.
- else:
    {rescued_mother:
        Now eighty-five.
    }
}

Ops looks toward you.

"What do we do with the habitat, {player_title}?"

* ["Saturation bombardment. Burn that fucking place down."] -> habitat_satbomb
* ["Leave it."] -> habitat_leave


=== habitat_satbomb ===

~ habitat_destroyed = true

"Understood."

Your fleet turns toward the habitat.

Targeting solutions spread across the tactical display.

For a few seconds, the battered station continues its orbit as it has for generations. Then your fleet opens its fuel compartments.

Antimatter fuel canisters fall, proximity charges blinking on them. Compartments rupture one after another, atmosphere venting in great white plumes before ignition turns them orange.

The central ring breaks.

The surviving lights disappear.

When the bombardment ends, what remains of the habitat is a burning wreck drifting above Kshaya.

No life signs remain.

* [Leave.] -> END


=== habitat_leave ===

~ habitat_abandoned = true

// The selected line is already printed by Starsector before this destination.
Your fleet turns away.

Kshaya's habitat dwindles behind you until its few remaining lights disappear among the stars.

Whatever happens there now is no longer your decision.

* [Leave.] -> END


=== habitat_return_empty ===

When you come back, the habitat is completely empty. Nothing remains, but you swear on your boarding team's cams you spot the faintest wisps of red-tinged smoke.

* [Leave.] -> END


// Runtime-only reentry mirrors. These preserve the authored encounter when a
// player closes the interaction at one of its persistent save checkpoints.
=== resume_mother_waiting ===

Your boarding party is halfway back to the docking concourse. The frightened mother and her young son wait in the shelter of the corridor corner while Yvan watches for your decision.

* ["Why won't they let you leave?"] -> mother_why
* [Decide what to do.] -> mother_rescue_choice


=== resume_orbital_decision ===

-> rescue_end


=== habitat_bombed_return ===

The ruined habitat drifts above Kshaya in broken, blackened sections. No lights remain, and your sensors detect no life aboard.

* [Leave.] -> END
