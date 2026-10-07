// Authoring and proofreading source for the full one-time Menelaus first contact.
// Runtime prose, choices, routing, portraits, and persistent state live in
// data/campaign/rules.csv under ChiefNavigatorMenelausFirst*.

VAR player_title = "Captain"
VAR sara_dead = false

=== fob_ithaca_shuttle ===

Your command shuttle leaves your flagship under Spartan escort.

The central module of FOB Ithaca grows until it fills every viewport, a vast sea of black against black, its bulk swallowing up the stars in the background.

Up close, the central module stops resembling a station. Armor cliffs rise for kilometers. Docking apertures glow between batteries large enough to engage capital ships.

Maintenance drones move across the surface in endless streams. Some are little larger than a person. Others haul armor plates larger than your shuttle. A team of them, illuminated only by sparks in the night, are repairing a tear so large in the module you could fly a Wolf through it.

+ [Continue.]

A transmission crackles through the cabin speakers.

"Inbound shuttle, maintain present vector. Berth assignment transmitted."

Your pilot acknowledges and banks toward a docking aperture opening in Ithaca's black armor. The maneuvering thrusters pulse beneath your feet, and then the walls swallow you.

Stars disappear behind the shuttle. Guidance lamps streak past the windows as you descend through layer after layer of the macrocomplex.

There is a heavy jolt as docking clamps seize the shuttle. The engines spool down, their vibration fading from the deck beneath your boots.

For the first time since entering Ashen Verge, everything is still.

You unfasten your restraints. Your bodyguards do the same.

Across the cabin, Sinni is already standing.

* ["Stay in the shuttle, Sinni. Coming here is dangerous enough."] -> fob_ithaca_sinni_stay
* ["I assume you're coming."] -> fob_ithaca_sinni_assume
* ["Ready?"] -> fob_ithaca_sinni_ready


=== fob_ithaca_sinni_stay ===

"No."

You stop.

Sinni rises from her seat.

She simply said no.

* ["No?"] -> fob_ithaca_sinni_insists
* ["Huh. That sounded like insubordination."] -> fob_ithaca_sinni_insists


=== fob_ithaca_sinni_assume ===

"Yes."

Immediate.

Sinni rises and straightens her sleeves.

-> fob_ithaca_sinni_go


=== fob_ithaca_sinni_ready ===

"Yes."

Sinni is already standing, apparently more ready than you are.

-> fob_ithaca_sinni_go


=== fob_ithaca_sinni_insists ===

Sinni clasps her hands before her.

"Captain."

The concession does not follow.

"I have traveled the heavens many times over to reach here."

The look in her eyes is serious.

"I am going with you."

There is no anger, no pleading, no compromise.

Just Sinni.

* ["Was that a request?"] -> fob_ithaca_sinni_request
{ sara_dead:
    * ["Okay. Yvan. Try to keep our Chief Nav alive."] -> fob_ithaca_sinni_go
- else:
    * ["Okay. Sara. Try to keep our Chief Nav alive."] -> fob_ithaca_sinni_go
}


=== fob_ithaca_sinni_request ===

"No."

A beat.

"Respectfully."

{ sara_dead:
    * ["Okay. Yvan, respectfully don't leave Sinni out of your sight."] -> fob_ithaca_sinni_go
- else:
    * ["Okay. Sara, respectfully don't leave Sinni out of your sight."] -> fob_ithaca_sinni_go
}


=== fob_ithaca_sinni_go ===

{ sara_dead:
    Yvan rises and checks his sidearm, then inclines his head.

    "Understood."
- else:
    Sara rises and checks her sidearm.

    "Got it. I'll make sure our Chief Navigator doesn't discover any exciting new ways to get shot."

    Sinni gives her a flat look.

    "I don't intend to."

    "That's what makes them exciting."
}

The outer hatch unlocks with a heavy thump as pressure equalizes, then retracts into the shuttle wall.

Three Spartans are waiting on the other side: two armed marines and an officer carrying a battered data slate. There is no honor guard, no ceremony.

The officer looks first at you, then at Sinni.

"Captain. Chief Navigator."

Sinni inclines her head.

"Correct."

"Follow me."

He turns without another word.

Beyond him, the docking gallery is enormous and mostly empty. Automated tugs move fuel and ammunition between the few occupied berths while repair drones crawl over damaged hulls and disappear into open maintenance shafts.

There are people here, but not many. A pilot helps guide cargo into storage. An officer with lieutenant's bars is elbow-deep in an open maintenance panel. Two marines carry ration crates down a service corridor.

Nobody seems particularly interested in the fleet that just arrived from beyond their sky. Everyone is working.

Your escort leads you toward a waiting monorail.

-> fob_ithaca_monorail


=== fob_ithaca_monorail ===

The monorail pulls away from the docks.

For a while, Ithaca almost resembles a conventional military station. Workshops, barracks, medical compartments, and storage galleries slide past the windows. A mess hall has perhaps a dozen people inside.

But the same pattern follows you everywhere: too much station, too few people.

A pilot walks past carrying a diagnostic unit. A marine pushes a supply cart. An officer in engineering coveralls has half her body buried in an open maintenance panel.

Nobody here seems to have only one job.

Then the lights outside stop.

Blackness swallows the windows—not space, but structure.

The monorail races through an entire district with its lights turned off. Habitation towers, transit platforms, workshops, and warehouses loom beyond the glass, visible only when the train's running lights sweep across them.

Something moves below. Then another. Soon you make out hundreds of maintenance drones crawling through the darkness, opening access panels, replacing conduits, and tending machinery no human being is there to see.

* ["Power failure?"] -> fob_ithaca_quiet
* ["What's out there?"] -> fob_ithaca_quiet
* [Keep watching.] -> fob_ithaca_quiet


=== fob_ithaca_quiet ===

"Quiet district," your escort says without looking up from his data slate.

* ["Quiet district?"] -> fob_ithaca_quiet_2


=== fob_ithaca_quiet_2 ===

"No permanent personnel assignment. Life support is reduced to maintenance levels."

He gestures toward the dark city outside.

"Strategos maintains pressure integrity, transit, structural systems, and essential fabrication. The drones handle the rest."

* ["How much of Ithaca is like this?"] -> fob_ithaca_quiet_3


=== fob_ithaca_quiet_3 ===

"Ninety-two percent."

You look back through the window.

Entire districts pass in darkness, swaths of metal complexes larger than most settlements in the Persean Sector. Machines move through all of it.

Sinni approaches the rail window, her reflection hanging over the black glass.

Before she can react, her reflection vanishes as something immense passes over the monorail.

At first, you mistake it for part of Ithaca's superstructure. Then welding sparks flare across its underside.

* ["What is that?"] -> fob_ithaca_onslaught
* [Look up.] -> fob_ithaca_onslaught


=== fob_ithaca_onslaught ===

A prow emerges from the darkness, followed by meters of armor and then the unmistakable silhouette of an Onslaught.

The battleship hangs above the transit line, pristine and completely unpainted, its unfinished hull suspended between rows of nanoforges. There are no work lamps, no scaffolds, no human beings.

A slab of armor slides from one of the forge arrays, still glowing faintly from manufacture. Tug drones seize it and carry it across the cavern, where other machines catch the plate against the Onslaught's flank.

A moment later, welding arcs flash through the dark. For an instant, the entire battleship is visible before darkness swallows it again.

Another armor plate emerges from the nanoforges.

* ["They're building an Onslaught."] -> fob_ithaca_onslaught_2


=== fob_ithaca_onslaught_2 ===

"Construction allocation SPN-17," the Spartan officer says, checking his slate. "Scheduled completion in twenty-three days."

* ["Twenty-three days?"] -> fob_ithaca_onslaught_3


=== fob_ithaca_onslaught_3 ===

"Assuming no further loss of industrial capacity."

He goes back to his slate.

-> fob_ithaca_after_onslaught


=== fob_ithaca_after_onslaught ===

The Onslaught vanishes behind you, though its welding flashes remain visible in the distance for several seconds.

Sinni watches until the last one disappears.

"Two hundred years," she murmurs.

You glance toward her.

Another dark habitation block slides past the window.

The monorail begins to slow as light returns beyond the windows. Ahead, a fortified transit platform emerges from the darkness.

Your escort stands.

"We have arrived."

-> fob_ithaca_strategos_approach


=== fob_ithaca_strategos_approach ===

The platform opens directly into a military complex.

Your escort leads you and your retinue through three security checkpoints before you reach a pair of tall black doors at the end of the corridor.

There is no office number or command insignia, only a single word written above them in old Domain script.

**STRATEGOS**

The Spartan officer stops.

"Strategos Menelaus will receive you now."

The doors open.

* [Enter.] -> menelaus_holodeck


=== menelaus_holodeck ===

The room beyond is circular, windowless, and completely empty.

No desk. No command staff. No tactical table. You catch your bodyguards thumbing their weapons nervously.

Actually, why didn't your escorts confiscate them?

Sinni looks around.

"It's a holodeck."

Stars hang overhead and beneath your feet. Ithaca's orbital ring curves through the chamber while the central module hangs at its heart. Fleet positions begin appearing around it, followed by defense networks, supply routes, industrial output, power flow, and casualty estimates.

Thousands of streams of information fold through one another faster than you can follow.

Then they move aside.

A humanoid face assembled from overlapping geometric planes slowly begins rendering into view at the center of the chamber.

The features are almost human.

A model of a Corinthian helm has been worked into the avatar, its cheek guards and brow breaking apart into the same layered, crystalline geometry. A broad transverse crest stretches across the crown. Beneath it, the face seems less sculpted than calculated into existence. Plates of light shift by fractions of a degree as you look at him, suggesting cheekbones, a mouth, the bridge of a nose.

Only the eyes remain completely stable.

One burns a shocking blue, the other white.

The machine's presence is overwhelming. The avatar practically unmakes you with its gaze. You have the strange sensation that you are not standing in a room containing Menelaus, but inside something that has chosen to give you its attention.

Like standing before a god.

His gaze settles on you.

"{player_title}."

Its voice is thunderous, calm, and unmistakably human.

"I am Menelaus, Domain Intelligence Nexus VG1239-GH1729."

The geometry of his face shifts almost imperceptibly.

"I have held the office of Strategos of the Orion Knot Theater for the last two hundred and eighty-six cycles."

* ["Strategos Menelaus."] -> menelaus_first_business
* ["I wasn't expecting... this."] -> menelaus_unexpected


=== menelaus_unexpected ===

"I thought not."

Menelaus regards you for another moment.

* ["You're an Alpha Core."] -> menelaus_1
* ["You're an AI."] -> menelaus_1
* ["Ludd help us. You're an abomination."] -> menelaus_3
* ["Strategos, then."] -> menelaus_first_business


=== menelaus_1 ===

"Yes."

The answer comes without hesitation.

"I am classified as an 'Alpha'-level artificial intelligence. Issued at Jupiter Station by the Amystris lab."

A constellation of blue-white geometry turns slowly behind him.

* ["Strategos, then."] -> menelaus_first_business
* ["Ludd help us. You're an abomination."] -> menelaus_3


=== menelaus_3 ===

The avatar's visage darkens. Literally.

"You will address me as STRATEGOS, as befits my station."

The avatar looms overhead, not by stepping forward, but by simply growing.

Wordlessly, Sinni reaches out and grabs your hand.

The face of Menelaus expands until those impossible eyes hang above you like stars. The holodeck darkens around him, leaving the blue-white architecture of his form burning against the void.

{ sara_dead:
    Several of your bodyguards recoil.
- else:
    Several of your bodyguards recoil. Sara's pistol clears its holster, but Yvan catches her wrist and slaps the weapon back down before she can level it.

    As if that toy would be of any use here.
}

Menelaus lets the moment stretch, allowing you and your retinue to quail beneath his wrath.

"Are we understood?" he asks.

* ["Understood, Strategos."] -> hmm
* ["Apologies. Point taken."] -> hmm
* [Say nothing.] -> hmm


=== hmm ===

"Onto my request."

-> menelaus_first_business


=== menelaus_first_business ===

"I am in need of your assistance."

The words are almost absurd coming from something that fills the room like a god.

Menelaus raises one hand, and Ithaca contracts beneath you. The orbital ring becomes a thin circle of light. The central module shrinks with it, followed by the moons, planets, and finally the whole of Ashen Verge.

Then red begins to spread across the system.

Hundreds of Threat contacts populate the map. Some are marked as destroyed. Others pulse steadily at the edges of Spartan patrol zones, while entire regions of the system are shaded in warning colors.

The wound you saw in Ithaca's orbital ring glows brightest of all.

"Task Force Spartan retains tactical superiority within the immediate Ithaca Defense Area."

A number appears beside the ring and begins counting downward.

"That condition will not persist."

-> menelaus_status_menu


=== menelaus_status_menu ===

* ["You're losing."] -> menelaus_losing
+ ["What do you need from me?"] -> menelaus_need


=== menelaus_losing ===

"Yes."

Menelaus brings up the schematics of a ship.

"For two hundred and forty-six cycles, we maintained our position with minimal losses. Until the arrival of this."

The vessel rotates slowly between you.

Menelaus narrows his eyes.

"We have designated it 'Gautama.' A vector of the same entities responsible for the megadeath incident at Hipparcos during the third cycle of the 204th Assembly."

The image of Ithaca appears beside it.

"This... thing penetrated our defenses and sheared an entire module from the orbital defense ring."

+ ["We saw it. The Drifting Wall module in the Alpha Odyssey sector. That was from here."] -> menelaus_losing_2


=== menelaus_losing_2 ===

Menelaus lowers his helm.

Was that shame?

"It is my greatest failure."

A series of cryogenic vaults illuminate across the macrocomplex.

"It was because of Gautama that I reactivated Task Force Spartan from cryosuspension. They have shored up the defenses as best they can. For now."

+ [Continue.] -> menelaus_status_menu


=== menelaus_need ===

Menelaus dismisses the tactical display.

"I need you to act as an independent roaming force."

The map of Ashen Verge changes around him. Spartan formations collapse back toward Ithaca, their patrol routes clustering tightly around the surviving defensive perimeter.

"Task Force Spartan cannot be everywhere. Every ship I send beyond the defense area weakens Ithaca, and every casualty costs me personnel I cannot replace."

His gaze settles on you.

"You suffer from neither limitation."

Distant points illuminate across the theater: wrecks, Threat concentrations, abandoned installations, lost supply depots. Places far beyond the reach of the Spartan pickets.

"I have objectives throughout the Orion Knot which must be completed, and no force I can presently spare to complete them."

Menelaus raises one hand.

"I call them my Labors."

The word hangs in the holodeck with more ceremony than anything else he has said.

"Undertake them at your discretion. Destroy what threatens this theater. Recover what has been lost. Restore what can still be restored. And where circumstances demand judgment, exercise yours."

A series of Domain seals appear around him.

"Accomplish my Labors, and by the authority vested in me by the Domain of Man, I will compensate you accordingly."

Ship schematics, weapons inventories, industrial stockpiles, and ancient Domain credentials flicker into existence around him.

"Materiel. Ships. Access to Ithaca's arsenals and fabrication capacity. Salvage rights. Requisition authority. Military clearances."

Menelaus lowers his hand.

"Authority, where I possess the right to grant it."

A pause.

"And wealth enough that I doubt you will find the arrangement unrewarding."

* ["You're hiring me as a mercenary."] -> menelaus_need_mercenary
* ["You can actually grant Domain authority?"] -> menelaus_need_authority
* ["And if I don't want to work for you?"] -> menelaus_need_refuse
* ["Understood. Show me the Labors."] -> menelaus_need_accept


=== menelaus_need_mercenary ===

"If that terminology pleases you."

There is the faintest shift in the geometry of his face.

"I prefer 'independent auxiliary.'"

-> menelaus_need_return


=== menelaus_need_authority ===

"I am Strategos of the Orion Knot Theater."

For once, Menelaus sounds almost offended that the answer should require explanation.

"Within this theater, there are no authorities the Domain placed above mine."

His gaze hardens.

"None have come to relieve me of my duties in two hundred and eighty-six cycles."

-> menelaus_need_return


=== menelaus_need_refuse ===

"Then you will refuse."

Menelaus says it without anger.

"I have neither the personnel nor the inclination to compel you."

A pause.

"Although I suspect you did not cross the Persean Abyss merely to turn around."

Sinni turns to look at you.

She nods.

-> menelaus_need_return


=== menelaus_need_return ===

* ["Understood. Show me the Labors."] -> menelaus_need_accept
+ [Continue.] -> menelaus_status_menu


=== menelaus_need_accept ===

"Good."

Ashen Verge unfolds around Menelaus once again.

"Then let's begin."

+ [Continue.] -> END
