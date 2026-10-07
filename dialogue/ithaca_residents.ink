// Authoring mirror for Ithaca's rescued residents.
// Runtime prose, choices, and person-local topic flags live in rules.csv.
// League name tokens remain literal here; sara_dead mirrors the global flag.
// Asking the rescued League survivors about their future at FOB Ithaca
// grants 254 crew once, not on pickup, delivery, or merely opening the call.

VAR player_title = "Captain"
VAR sara_dead = false
VAR league_met = false
VAR league_asked_sanzu = false
VAR league_asked_attack = false
VAR league_asked_future = false
VAR league_crew_joined = false
VAR kshaya_met = false
VAR kshaya_asked_settling = false
VAR kshaya_asked_past = false
VAR kshaya_asked_future = false
VAR kshaya_asked_sara = false

// Post-Labor V hunts start only after Menelaus's final debrief is committed.
// These repeatable Kleon topics reveal each hunt location independently.
// Runtime availability/completion use the existing permanent boss-defeat flags.
=== kleon_hunts ===

+ ["Where can I find Budai?"] -> kleon_hunt_budai
+ ["What happens now with the Starving Threat?"] -> kleon_hunt_ungaikyo

=== kleon_hunt_budai ===

Kleon brings up Avici on his tactical slate.

"We've heard of some anomaly down galactic south. We'd appreciate it if you could check it out."

-> kleon_hunts

=== kleon_hunt_ungaikyo ===

Kleon marks Last Light.

"Telemetry revealed what Gautama actually is. It's a web. The strands are just nanometers wide but they span light years. We think we've found the central processing unit of this web here in Last Light. Destroy it, and put an end to this nightmare."

-> kleon_hunts


# league victory
=== league_victory ===

Your fleet smashes the Persean League survivors.

What remains of their formation breaks apart beneath the pressure. Some ships turn and run. Others lose drive entirely, tumbling end over end as the Sanzu current catches them.

Within minutes, organized resistance is over.

The tactical display is littered with crippled hulls and escape pods, all of them drifting slowly downstream through the pale-blue current.

Your comm officer looks toward you.

"There are enemy survivors out there, {player_title}. A lot of them."

Rescue beacons begin appearing across the display.

Some are coming from intact escape pods.

Others are little more than suit transponders, already being carried away from the battlefield.

You have the opportunity to deploy rescue shuttles and recover as many enemy crew as you can before the current scatters them beyond reach.

+ [Deploy the shuttle crews.] -> league_rescue
+ [Leave the raiders to their fate.] -> league_abandon


=== league_rescue ===

"Launch rescue shuttles."

Your flight deck comes alive.

Small craft spill from the fleet, fanning out between the drifting wrecks while your surviving combat ships hold position against the current.

One by one, the rescue beacons begin disappearing from the tactical display.

Not all of them.

But many.

League crew are dragged from lifeboats, pulled from damaged compartments, and hauled aboard shuttles still wearing the uniforms of the fleet that had been trying to kill you minutes earlier.

The Sanzu carries the wreckage onward.

Your people bring back everyone they can. Once onboard, a person identifying themselves as the surviving fleet's XO sends you a message from the brig. One of your bodyguards hands you the datapad.

They are asking for safe passage to the nearest safe station.

{not sara_dead:
    "Hah. Like they'd do the same for us." Sara jerks her head toward the viewport. "Cap'n, how's about we send them on their way? They wanna go home so bad, they can float."
}

+ [Log their requests.] -> END



=== league_abandon ===

"Leave them."

The fleet reforms around you while the Sanzu current slowly claims the battlefield.

Crippled ships drift away first. Then the escape pods.

Then the scattered suit transponders, their signals growing weaker as the current carries them farther downstream.

Eventually, the tactical display clears.

Your fleet moves on.

-> END



// Enter league_greeting or kshaya_greeting for review.

=== league_greeting ===

    $PersonRank $personName receives your call from Ithaca's temporary barracks, where uniform coats hang from the pipework with their League insignia removed. The commander sets a repair roster aside.

    "Captain, my crews haven't forgotten your recovery boats coming back for the pods."

    -> league_hub



=== league_return ===

$PersonRank $personName answers from the barracks, with a Spartan maintenance schedule pinned behind the desk.

"Captain, what do you need?"

-> league_hub


=== league_hub ===

    + ["How did your fleet end up in Sanzu?"] -> league_sanzu

    + ["Why did you order the intercept?"] -> league_attack

    + ["What will your people do now?"] -> league_future

+ [Return to the comm directory.] -> END


=== league_sanzu ===

$PersonRank $personName looks confused for a second.

"Sanzu? That nebula system with the weird current?" $personName sighs, rubbing their chin.

"We got wind that the Hegemony was making some massive salvage expedition to their old transit base," $personLastName says. "Command Council decided we couldn't let them retrieve their mothballed ships and weapons."

"So you tried to get ahead of them," Sinni says blankly. "You had fast ships. Lots of supplies."

"...Aye, that we did." $PersonRank $personName looks quite defeated at this point. "We got to the general area first. But we spent so long wandering the darkness. And those... lights. Those things."

The survivor shudders. They must have met the Dwellers.

"We lost so many. We thought we were somewhat safe once we found that 'Sanzu' place." The survivor shakes their head. "I failed my fleet. If I knew safe harbor was here, just a few hyperspace days away..."

+ ["Then you met the Starving Threat."] -> league_sanzu_2


=== league_sanzu_2 ===

"Looks like you guys know everything already." A bitter look crosses $PersonRank $personName's face. "Anything else you want to guess right?"

+ [Continue.] -> league_hub


=== league_attack ===

~ league_asked_attack = true

$personLastName keeps a hand on the repair roster.

"It wasn't personal. Our fleet was on the brink. We were stranded, starving... I apologize, {player_title}, but I knew what I was ordering."

+ ["You meant to attack without warning."] -> league_attack_condemn
+ ["I'm responsible for my men too. I get it."] -> league_attack_understand
+ ["What makes you any different than a pirate?"] -> league_attack_close


=== league_attack_condemn ===

"Yes," $personLastName says. "I hoped surprise would make up for the state of my ships."

-> league_hub


=== league_attack_understand ===

A look of surprise crosses the survivor captain's face.

"I... appreciate that."

+ [Continue.] -> league_hub


=== league_attack_close ===

The commander folds the repair roster away.

"Nothing, I guess. I used to think those raiders were scum of the earth too. Now that I've been on the other side..."

The survivor hesitates.

"When we get back home, I have some thinking to do."

-> league_hub


=== league_future ===

~ league_asked_future = true

$personLastName looks down at the repair roster.

"Home, if we can get there. Most of my people still have families in League space."

The survivor pauses.

"After what happened out here, though... I don't know if all of us are going back to what we were doing before."

{ not league_crew_joined:
    "Quite a few want to join your fleet, Captain. They haven't forgotten who came back for them."
    ~ league_crew_joined = true
}

-> league_hub


=== kshaya_greeting ===

{kshaya_met:
    -> kshaya_return
- else:
    ~ kshaya_met = true

    Mira Sen receives your call from a fabrication room converted into family quarters. Food plants grow beneath the work lamps while she repairs a nutrient valve and Toma sorts washers beside her.

    "Captain. Toma, say hello."

    The boy keeps arranging his washers.

    "I am saying hello."

    -> kshaya_hub
}


=== kshaya_return ===

Mira answers from the workbench, where Toma has cleared a place among his washers for a cup of seedlings.

"Hello, Captain."

-> kshaya_hub


=== kshaya_hub ===

    + ["How are you settling in?"] -> kshaya_settling

    + ["What will you do here?"] -> kshaya_future
{sara_dead:
    + ["What will you tell Toma about Sara?"] -> kshaya_sara
}
+ [Return to the comm directory.] -> END


=== kshaya_settling ===


"Toma slept in the storage locker for the first week," Mira says. "Ithaca's pumps frightened him. On Kshaya, we listened for them to stop; here the hum never ends."

Toma reminds her that the locker had a door.

She smiles at him.

"We sleep together in the bunk now, most nights."

-> kshaya_hub


=== kshaya_past ===


Mira sets the valve aside and keeps her voice level for Toma.

"I grew up in the congregation. The ration slate showed our portions, never the stores. Sometimes a name disappeared and the portions grew. They said Budai had chosen."

Toma asks whether dinner will be late, and she turns to him.

"No, it won't."

-> kshaya_hub


=== kshaya_future ===


"I've been assigned to hydroponics," Mira says, turning the repaired valve to check its seal. "They need someone who can keep the feed lines working."

Toma lifts a paper cup from beneath the bench to show you the pale shoot inside.

"It's a tree."

Mira tells him it's a bean, but he moves it nearer the lamp.

"Not yet."

-> kshaya_hub


=== kshaya_sara ===

Mira's hands grow still around the valve.

"The truth. I caught Yvan's sleeve and begged him to take us. Sara pushed Toma behind an access column when the shooting started. She was hit before they could pull her clear."

Toma looks up from the bench.

"She put me behind the wall."

+ ["Sara chose to act."] -> kshaya_sara_choice
+ ["You asked for help. The decision to act was mine."] -> kshaya_sara_responsibility
+ [Say nothing.] -> kshaya_sara_silence


=== kshaya_sara_choice ===

Mira nods, keeping her hand on the valve.

"Then I will tell him that, too."

-> kshaya_sara_end


=== kshaya_sara_responsibility ===

Mira studies you before answering.

"Yes, Captain. It was."

-> kshaya_sara_end


=== kshaya_sara_silence ===

Mira lays the valve on the bench and rests a hand on Toma's hair as he returns to his washers.

-> kshaya_sara_end


=== kshaya_sara_end ===

~ kshaya_asked_sara = true

Mira watches Toma finish his row of washers before she speaks again.

"I will tell him her name, and what she did."

-> kshaya_hub
