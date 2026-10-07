package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/** Owned encounter names through Combat Chatter's native, once-only fleet intro. */
final class EncounterCombatChatter {
    static final String NAME_KEY = "$chatter_introSplash_name";
    static final String STATIC_KEY = "$chatter_introSplash_static";
    static final String SCYLLA = "Scylla Strike";
    static final String SIREN = "Siren Strike";
    static final String CYCLOPS = "Cyclops Strike";
    static final String HEAVENLY = "Heavenly Strike";
    static final String BUDAI = "Budai";
    static final String UNGAIKYO = "Ungaikyo";
    static final String HEGEMONY = "Hegemony Expeditionary Fleet";
    static final String LEAGUE = "Persean League Expeditionary Fleet";

    private EncounterCombatChatter() { }

    static void configure(CampaignFleetAPI fleet, String name) {
        if (fleet == null || name == null || fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet()) return;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(NAME_KEY, name);
        memory.set(STATIC_KEY, false);
    }
}
