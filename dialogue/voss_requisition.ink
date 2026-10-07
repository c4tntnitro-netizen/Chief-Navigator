// Authoring and proofreading mirror for Oren Voss's patrol requisition.
// Runtime prose, cargo transfer, grace period, and battle routing live in
// data/campaign/rules.csv and HegemonyExpeditionInteraction.java.

VAR player_title = "Captain"
VAR player_name = "Player"
VAR can_pay = true
VAR asked_voluntary = false
VAR asked_rations = false
VAR grace_active = false
VAR hegemony_hostile = false
VAR outmatched = false

{
    hegemony_hostile:
        -> hostile_hunt
    - grace_active:
        -> grace
    - else:
        -> opening
}


=== opening ===

The fleet before you is little more than a dark speck among the stars. Within minutes, that speck grows and spreads into a shifting cloud of naval ships and supply vessels.

{
    outmatched:
        They surround your far smaller fleet, engulfing your host like a great tide washing over a rock.
    - else:
        They nearly collide with your own naval host. Like waves breaking against the shore, they probe every gap in your formation.
}

Their hulls nearly blot out the stars around you.

"{player_title}..." Sinni calls out. "The enemy is attempting an encirclement."

Sinni assumed them enemies. She may be proven right yet.

The Hegemony Expeditionary Fleet closes in a disciplined spread, every drive plume kept low and every weapon brought to readiness without firing a ranging shot. A priority channel cuts across your comm board.

Admiral Oren Voss appears beneath the seal of the Hegemony Expeditionary Command. His uniform is cleaner now, though it hangs more loosely from his gaunt frame than it did at Ithaca. The burn along his jaw has healed into a livid pink sear. Behind him, bare conduit and ration crates crowd what was once a proper command compartment.

{
    not hegemony_hostile:

        Voss offers no pleasantries.

        “You again. This is a lawful emergency requisition. Under Domain Maritime Law 0081ac, surrender sixty megakilos of standard ship maintenance units and one hundred twenty g-u of fuel.”

        An aide passes him a slate. Voss does not look at it.

        “Your fleet will be entered as a voluntary contributor and granted safe passage through Waypoint Troy.”

        -> requisition_hub

    - else:

        There is a dangerous glint in Voss's eyes.

        “You. I've been waiting for this, rebel.”

        Voss raises his hand. Behind him, his command staff leap into action.

        "I'll give you one chance. Power down your weapons and prepare to receive boarding parties."

        + ["Why are you attacking us? We mean no harm!"] -> hostile_appeal
        + ["This is an unlawful boarding, Admiral, and you know it."] -> hostile_law
        + ["Sure, we'll power down weapon systems. Come and see."] -> hostile_defiance
}


=== requisition_hub ===

{ not asked_voluntary:
    + [“Voluntary? Your guns are trained on us.”] -> voluntary
}
{ can_pay:
    + [Transfer 60 supplies and 120 fuel.] -> paid
}
+ [“We don't have what you're demanding.”] -> cannot_pay
+ [Refuse the requisition.] -> refuse


=== voluntary ===

~ asked_voluntary = true

Voss's left eye twitches.

“Then you understand the urgency.”

He leans toward the pickup.

“We will get our supplies from you. Either logged as 'voluntary' or 'salvage'.”

One of his officers lowers their eyes to the tactical display.

“Choose how this is entered.”

-> requisition_hub


=== cannot_pay ===

Voss stares at you. His jaw tightens against the scar.

“Your incompetence isn't our problem. The Hegemony is the Domain of Man; is All of Humanity.” Voss sighs, clearly done with negotiations. "If you won't join Humanity, then die for it."

He cuts the channel with a sharp motion. His fleet's formation contracts around your projected course. Fire-control emissions sharpen across the spectrum.

+ [Defy the requisition.] -> battle


=== refuse ===

For an instant, Voss says nothing. Then his face hardens.

“Refusal?” His voice rises around the word. “Materiel required by this expedition is not yours to withhold. You are deciding only whether you give it to our boarding parties or our salvage crews.”

The channel remains open while his patrol forms a firing line.

{ can_pay:
    + [Transfer 60 supplies and 120 fuel.] -> paid
}
+ [“We don't have what you're demanding.”] -> cannot_pay
+ ["Then come and take them."] -> hostile_defiance


=== paid ===

Cargo lighters cross between the fleets under armed escort. Your quartermaster confirms the loss: sixty units of supplies, one hundred twenty units of fuel.

Voss watches the transfer counter reach zero. Was there a hint of displeasure across his face?

“Your contribution has been entered as voluntary. Expeditionary Command will recognize your fleet as a participant in the common defense for fourteen days.”

He cuts the channel without pleasantries.

The patrol breaks formation with the same economy it showed while surrounding you.

+ [Continue.] -> END


=== grace ===

The patrol recognizes the contribution certificate in your transponder and alters course before entering weapons range.

Voss appears only long enough to verify the seal.

“Your contribution remains current. Use the time left on it.”

+ [Continue.] -> END


=== hostile_appeal ===

Voss's expression does not change.

"You aren't owed an explanation. If you want to save yourself and your people, power down weapons now and prepare to receive boarding parties." His eyes narrow. "Under Domain Law, you'll be given a fair and summary trial."

You see Sinni turn her head at this, staring at you. Wordlessly, she shakes her head.

+ [Say nothing.] -> hostile_final


=== hostile_law ===

Voss's jaw tightens. For a moment, he looks furious. Then his visage twists into a grin.

There it is. His true colors.

"You're right, rebel." Voss clenches his hand, opening and closing it over and over. "There is no law here but ordnance. No god but strength. No gospel but gunpowder."

+ ["You're insane. You want to settle a vendetta here?"] -> hostile_law_2


=== hostile_law_2 ===

"This is the perfect place." Voss's teeth begin to show. "We are going to kill you now. No quarter. All that will be left of your fleet is a line in a paragraph in a report."

Voss closes the channel. No more words.

-> hostile_final


=== hostile_defiance ===

Voss stares at you for a moment.

Behind him, his command staff move at once. Across your tactical display, the Hegemony formation tightens around your fleet.

Sinni watches the closing vectors.

"{player_title}."

Her voice is perfectly calm.

"They're coming."

Voss leans toward the comm pickup.

"Come, {player_title} {player_name}. Let's test your mettle."

The channel cuts.

+ [Prepare for battle.] -> battle


=== hostile_final ===

The Hegemony formation contracts around your fleet.

Across the tactical display, weapon locks multiply.

No further demands arrive.

+ [Prepare for battle.] -> battle


#this is for when the player has reputation with Hegemony Expeditionary Fleet < 0
=== hostile_hunt ===

"{player_title}." Sinni's voice is flat. "They've locked weapons."

Across the tactical display, the entire expeditionary formation wheels toward you.

At its center, Admiral Oren Voss's flagship accelerates with the rest.

A priority channel opens for less than a second.

Voss appears on-screen.

"You."

The channel cuts.

His fleet opens fire.

+ [Battle stations.] -> battle

=== battle ===

-> END

