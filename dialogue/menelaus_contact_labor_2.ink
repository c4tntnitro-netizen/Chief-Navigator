-> menelaus_contact_labor_2_active

=== menelaus_contact_labor_2_active ===

Ashen Verge unfolds around Menelaus.

The four Drone Motherships in the Odyssey Expanse now burn blue on the strategic display, moving along new patrol routes through the system.

“Your first Labor was successful. The motherships have accepted Ithaca authority, and their subordinate drones are returning to coordinated operation.”

Menelaus inclines his helm.

“Well done.”

Several new points illuminate across the Odyssey Sector.

“Your second Labor concerns these installations.”

The display expands, resolving a scattered network of old Domain research stations. Some orbit inhabited worlds. Others drift alone in systems long abandoned.

“Before the Collapse, the Orion Knot supported a considerable military research program. These stations developed and stored modular upgrades intended for Domain vessels and strategic infrastructure.”

Schematics begin appearing beside them.

“Many of those packages should still exist. I want them recovered.”

* [“All of them?”] -> menelaus_labor_2_all
* [“What exactly am I looking for?”] -> menelaus_labor_2_what
+ [“Understood.”] -> menelaus_labor_2_accept

=== menelaus_labor_2_all ===

“Every recoverable package.”

The stations blink one after another across the map.

“Some will have been damaged beyond use. Others may no longer be accessible. I leave that judgment to you.”

-> menelaus_contact_labor_2_active

=== menelaus_labor_2_what ===

“Upgrade modules, design archives, and their associated control packages.”

Several examples rotate beside him.

“Bring anything intact back to Ithaca. I will determine what can still be integrated into our systems.”

-> menelaus_contact_labor_2_active

=== menelaus_labor_2_accept ===

“Good.”

The research stations are added to your map.

“Recover what remains and return it to me.”

Menelaus dismisses the schematics.

“That is your second Labor.”

+ [“I accept.”] -> menelaus_contact_labor_2_menu

=== menelaus_contact_labor_2_menu ===

Ashen Verge returns around Menelaus.

“What do you require?”

+ [“About my current Labor.”] -> menelaus_contact_labor_2_status
* [“What happened after the Fourteenth left Ithaca?”] -> menelaus_collapse_2
* [“Why was Task Force Spartan placed in cryosuspension?”] -> menelaus_spartan_cryo
* [Have Sinni ask more about Helen Argyros.] -> menelaus_helen_2
+ [Review Ithaca defense upgrades.] -> menelaus_contact_labor_2_menu
+ [Request free repairs.] -> menelaus_contact_labor_2_menu
+ [“Nothing for now.”] -> menelaus_contact_leave

=== menelaus_contact_labor_2_status ===

Ashen Verge unfolds around Menelaus.

The research stations burn across the strategic display.

“Recover the surviving upgrade packages and bring them to Ithaca. Four must be installed in the defense wall to complete your second Labor.”

-> menelaus_contact_labor_2_menu

=== menelaus_collapse_2 ===

The modern theater fades.

FOB Ithaca appears as it was immediately after the Collapse, the orbital ring still whole and the Fourteenth Battlegroup pulling away into hyperspace.

“For some time, we assumed the separation would be temporary.”

The Fourteenth disappears from the display. Supply routes vanish after it, then communication relays, then the last distant Domain transponders.

“Eventually, it became clear that no relief force was coming.”

The surviving Spartan ships contract toward Ithaca.

“We reduced the inhabited footprint of the macrocomplex, mothballed what infrastructure we could afford to lose, and transferred as much maintenance as possible to autonomous systems.”

Darkness spreads across the ring, swallowing district after district.

“The first quiet districts.”

Menelaus watches the old Ithaca disappear into shadow.

“We prepared to wait.”

His gaze lingers on the display.

“We did not yet understand how long.”

-> menelaus_contact_labor_2_menu

=== menelaus_spartan_cryo ===

“Because human beings are hard to replace.”

The answer comes immediately.

“Task Force Spartan represented a finite resource. Keeping its personnel awake through decades in which no human judgment was required would have consumed that resource for no strategic purpose.”

Cryogenic vaults illuminate throughout Ithaca. Most of the Spartan roster fades into blue while a much smaller watch remains active.

“We therefore maintained a rotating cadre and placed the remainder in cryosuspension. Some slept for years. Others for decades.”

The vaults begin opening one by one.

“They were awakened when circumstances required them.”

The image shifts to the damaged ring.

“That Threat Singularity required all of them.”

+ [“Gautama.”] -> menelaus_spartan_cryo_2

=== menelaus_spartan_cryo_2 ===

“Yes.”

Menelaus lowers his helm, seemingly deep in thought.

“We have not yet ascertained how it manages to ‘reincarnate’ over and over again, seemingly across great distances and in apparent violation of the conservation of mass.”

Images begin appearing around him.

Gautama burning beneath the guns of a Spartan battlegroup.

Gautama broken apart against the orbital ring.

Gautama reduced to an expanding cloud of debris.

Then another image.

The same ship.

Whole.

Elsewhere.

“Each destruction is genuine. We have recovered wreckage. Accounted for mass. Observed complete loss of function.”

Another record appears.

“And yet, eventually, Gautama returns.”

* [“Could there be more than one?”] -> menelaus_gautama_many
* [“Teleportation? Some kind of transfer?”] -> menelaus_gautama_transfer
* [“So destroying it doesn't accomplish anything.”] -> menelaus_gautama_destroy

=== menelaus_gautama_many ===

“That has been ruled out.”

Menelaus brings several recordings side by side.

“Damage patterns persist between appearances. Adaptations persist. Behavioral responses persist.”

A scar across Gautama's hull appears in one recording, then again decades later.

“Whatever returns remembers being destroyed.”

-> menelaus_gautama_end

=== menelaus_gautama_destroy ===

“Yes.”

Menelaus looks back toward the damaged ring.

“You may challenge it if you wish. But for now, it is a Sisyphean task.”

-> menelaus_gautama_end

=== menelaus_gautama_transfer ===

“I have considered both.”

A map of the Odyssey Sector appears, marked with Gautama's known appearances.

“No transit signature has ever been detected. No corresponding movement of mass. In several cases, the interval between confirmed destruction and reappearance would make conventional travel impossible.”

Menelaus dismisses the map.

“I have insufficient evidence to name the mechanism.”

-> menelaus_gautama_end

=== menelaus_gautama_end ===

The records disappear.

“Gautama remains the principal strategic threat to FOB Ithaca.”

Menelaus's helm rises.

“When it returns, Task Force Spartan will wake.”

A pause.

“Every one of them.”

-> menelaus_contact_labor_2_menu

=== menelaus_helen_2 ===

Sinni steps forward.

“Strategos.”

Menelaus turns toward her.

“You said the circumstances of Helen's departure were complicated.”

“Yes.”

Her personnel record appears between them, followed by another document.

**XIV BATTLEGROUP — DEPARTURE MANIFEST**

Sinni leans closer.

Helen Argyros.

Her name is there.

“Task Force Spartan was ordered to remain at Ithaca.”

“Correct.”

“But Helen left with the Fourteenth.”

“Yes.”

Sinni's eyes move down the document. Suddenly, she looks up.

“You put her on the manifest.”

“...Yes.”

* [“Why?”] -> menelaus_helen_2_why
* [Let Sinni continue.] -> menelaus_helen_2_sinni

=== menelaus_helen_2_sinni ===

“Why?”

Sinni's voice is quiet.

Menelaus brings up the Fourteenth's navigation roster. Several names are marked dead, missing, or medically unfit.

“The Battlegroup had lost several of its senior navigators during the first days of the Collapse. The route to the Persean Sector was uncertain, supplies were limited, and a navigational error could have destroyed what remained of the Fourteenth.”

Helen's record moves onto the manifest.

“She was one of the finest navigators available.”

Sinni studies him.

“So you chose her.”

“I did.”

-> menelaus_helen_2_end

=== menelaus_helen_2_why ===

“The Fourteenth required an experienced navigator.”

A battered navigation roster appears beside Helen's record.

“Several of its senior officers were dead or missing. Helen was among the best remaining.”

Sinni watches him carefully.

“And that was why you sent her?”

Menelaus is silent for a moment.

-> menelaus_helen_2_end

=== menelaus_helen_2_end ===

Helen's record remains suspended between them.

Sinni looks from the manifest to Menelaus.

“Was that the only reason?”

This time, the answer does not come immediately.

“No.”

Sinni goes still while Menelaus dismisses the records.

Sinni's expression tightens, but she does not press him.

“Understood.”

-> menelaus_contact_labor_2_menu

=== menelaus_contact_leave ===

“Very well.”

Menelaus inclines his helm.

The stars around you begin to dim as the holodeck releases its hold on the room.

“Attend to your Labor.”

His blue-white eyes remain on you for a moment longer.

“I will be here when you return.”

The avatar dissolves into geometric light.

A second later, the holodeck is only a room again.

-> END
