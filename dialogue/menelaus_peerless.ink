// Runtime authority: data/campaign/rules.csv, chiefNavigatorPeerless*.
// Entry: Menelaus's completed-Labors menu, after the final debrief.
VAR domain_epic = false
VAR peerless_helmsmanship = false
VAR elite_helmsmanship = false

=== peerless_entry ===
* [Discuss the Domain Epic distinction.] -> peerless_view

=== peerless_view ===
{ domain_epic && peerless_helmsmanship:
    -> peerless_selected
- domain_epic:
    -> peerless_available
- else:
    -> peerless_offer
}

=== peerless_offer ===
Menelaus opens a service record bearing your name. Beneath it, two words appear in gold.

DOMAIN EPIC

"I retain the authority to confer this distinction. You have earned it."
* [Accept the Domain Epic title.]
    ~ domain_epic = true
    -> peerless_view
* [Return to the Labors.] -> DONE

=== peerless_available ===
// PeerlessSkillsCMD open: shared text training dialogue.
-> DONE

=== peerless_selected ===
// PeerlessSkillsCMD open: shared text training dialogue.
-> DONE
