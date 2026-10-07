// Authoring mirror for the Sanzu exile encounter.
// Runtime prose and choices live in data/campaign/rules.csv.
// Aid costs 20 supplies. Their own fleet attempts the trip; no new contact.

-> contact

=== contact ===
The patrol's captain answers from a bridge lit by emergency lamps. Behind him, someone has taped a ration schedule over the expedition insignia.

“Voss took our stores and sent us out here. When we came back empty, he struck us from the rolls. We've kept these ships running by stripping the ones we had to leave behind.”

He sends you a short list of repair materials. “Twenty supplies would get us moving again. We'll try for FOB Ithaca. I hear they still take people in.”

* [Give them 20 supplies for the journey to FOB Ithaca.] -> aid
* [Attack the patrol.] -> fight

=== aid ===
Your shuttles deliver the supplies and a copy of Ithaca's coordinates. The captain acknowledges receipt, then turns to pass the bearings to his navigator.

“Thank you, Captain. We'll make what we can of it.”

The patrol breaks formation as its crews prepare to depart for Ashen Verge.

+ [Continue.] -> END

=== fight ===
// Hand off to native fleet combat; no further authored dialogue.
-> END

=== underway ===
// Recontact after aid: no second donation or rescue decision.
The captain confirms that the patrol is still making its way to FOB Ithaca. “The supplies are holding. We'll keep going.”

+ [Continue.] -> END
