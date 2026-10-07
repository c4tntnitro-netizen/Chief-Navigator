package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.awt.Color;
import java.util.Map;

/** One-time conference following the Hegemony expedition's arrival. */
public final class HegemonyIthacaCommandScene
        extends ChoicePreservingDialogPlugin {
    private static final String HEAR_SPARTAN = "hear_spartan";
    private static final String HEAR_MENELAUS = "hear_menelaus";
    private static final String LEAVE = "leave";

    private static final Color HEGEMONY = new Color(245, 205, 90);
    private static final Color MENELAUS = new Color(100, 200, 255);

    private InteractionDialogAPI dialog;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        PersonAPI menelaus = MenelausTrial.getOrCreateMenelaus();
        PersonAPI voss = OdysseyStrandedFleetsScript.getOrCreateVoss();
        ConversationPortraits.show(dialog, menelaus, voss,
                FobIthacaContacts.getOrCreateSpartanCaptain(),
                SinniContact.getOrCreatePerson());

        dialog.getTextPanel().addPara(
                "A priority command conference forces itself across every "
                        + "available Odyssey relay. Admiral Voss has patched "
                        + "himself into FOB Ithaca's command channel. Task "
                        + "Force Spartan is listening.");
        dialog.getTextPanel().addPara(
                "\"By continuity of the Fourteenth Battlegroup, this entire "
                        + "macrocomplex and every surviving Domain asset in its "
                        + "theater now fall under Hegemony command. Transfer FOB "
                        + "Ithaca, its stores, its defensive grid, and its gate.\"",
                HEGEMONY,
                "Hegemony command",
                "Transfer FOB Ithaca");
        dialog.getTextPanel().addPara(
                "Menelaus answers without hesitation. \"I am Strategos of the "
                        + "Ithaca Defense Area. The Hegemony is not the Domain. "
                        + "Ithaca is neither abandoned property nor an inheritance. "
                        + "You have no command authority here. Request denied.\"",
                MENELAUS,
                "no command authority",
                "Request denied");
        dialog.getOptionPanel().addOption(
                "Remain on the command channel", HEAR_SPARTAN);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (HEAR_SPARTAN.equals(optionData)) {
            showSpartanResponse();
        } else if (HEAR_MENELAUS.equals(optionData)) {
            showMenelausResponse();
        } else if (LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    private void showSpartanResponse() {
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara(
                "Spartan command breaks its silence. \"Task Force Spartan "
                        + "recognizes Strategos Menelaus as commander of FOB "
                        + "Ithaca. We stand with him. Any attempt to supersede "
                        + "him by force will be treated as a hostile boarding action.\"",
                MENELAUS,
                "Task Force Spartan",
                "hostile boarding action");
        dialog.getTextPanel().addPara(
                "Voss's composure fractures. \"You recognize a machine. "
                        + "A core. A mere tool left running beyond its orders. "
                        + "Tools do not hold rank. Tools do not refuse lawful "
                        + "human officers. Tools do not call themselves human.\"",
                HEGEMONY,
                "mere tool",
                "Tools do not call themselves human");
        dialog.getOptionPanel().addOption("Listen to Menelaus", HEAR_MENELAUS);
    }

    private void showMenelausResponse() {
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara(
                "The blue projection tears outward from Menelaus's eyes. Every "
                        + "relay on the channel saturates at once; his answer is "
                        + "less a transmission than a blow against the station's "
                        + "superstructure.");
        dialog.getTextPanel().addPara(
                "\"I AM HUMAN!\"",
                MENELAUS,
                "I AM HUMAN");
        dialog.getTextPanel().addPara(
                "Menelaus tears Voss's authorization from Ithaca's command net "
                        + "and casts him out of the conference. Task Force Spartan "
                        + "holds formation around the macrocomplex while the "
                        + "humiliated admiral's expedition withdraws to its Alpha "
                        + "Odyssey forward base. It does not attempt to cross "
                        + "Spartan's firing line again. Instead, Voss turns to "
                        + "requisitions and patrols that make every passage out of "
                        + "the Orion Knot a negotiation at gunpoint.");
        dialog.getOptionPanel().addOption("Close the channel", LEAVE);
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
