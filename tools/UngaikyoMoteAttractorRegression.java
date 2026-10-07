package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.MoteControlScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Reproduces the real native null-attractor crash and checks each AI handoff. */
public final class UngaikyoMoteAttractorRegression {
    public static void main(String[] args) {
        SettingsAPI oldSettings = Global.getSettings();
        CombatEngineAPI oldEngine = Global.getCombatEngine();
        try {
            Vector2f position = new Vector2f(100f, 200f);
            Vector2f target = new Vector2f(600f, 200f);
            ShipAPI enemy = mock(ShipAPI.class, (name, values) -> {
                if (name.equals("getOwner")) return 0;
                if (name.equals("getLocation")) return target;
                if (name.equals("getCollisionRadius")) return 100f;
                if (name.equals("isTargetable")) return true;
                return null;
            });
            Global.setCombatEngine(mock(CombatEngineAPI.class, (name, values) -> {
                if (name.equals("getShips")) return List.of(enemy);
                throw new AssertionError("Emitter initialization must not tick gameplay or spawn anything: " + name);
            }));
            WeaponSlotAPI launch = slot(WeaponAPI.WeaponSize.SMALL, position);
            WeaponSlotAPI attractor = slot(WeaponAPI.WeaponSize.MEDIUM, position);
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class, (name, values) -> {
                if (name.equals("getHullId")) return "ziggurat";
                if (name.equals("getAllWeaponSlotsCopy")) return List.of(launch, attractor);
                return null;
            });
            StatBonus range = new StatBonus();
            MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class,
                    (name, values) -> name.equals("getSystemRangeBonus") ? range : null);
            MoteControlScript[] script = {new MoteControlScript()};
            ShipSystemAPI system = mock(ShipSystemAPI.class, (name, values) -> {
                if (name.equals("getScript")) return script[0];
                throw new AssertionError("Do not activate, replace or alter Mote Attractor: " + name);
            });
            FleetMemberAPI member = mock(FleetMemberAPI.class, (name, values) -> {
                if (name.equals("setPersonalityOverride")) {
                    check(Personalities.RECKLESS.equals(values[0]), "Keep Fearless");
                }
                return null;
            });
            int[] evaluations = {0}, activations = {0};
            ShipAPI[] mirror = {null};
            ShipAIPlugin ai = mock(ShipAIPlugin.class, (name, values) -> {
                if (name.equals("forceCircumstanceEvaluation")) {
                    check(script[0].getLockTarget(mirror[0], target) == enemy,
                            "Native lock query must be safe before first Mote Attractor update");
                    evaluations[0]++;
                }
                return null;
            });
            ShipAIPlugin[] activeAI = {null};
            mirror[0] = mock(ShipAPI.class, (name, values) -> {
                switch (name) {
                    case "getOwner": return 1;
                    case "getLocation": return position;
                    case "getHullSpec": return hull;
                    case "getMutableStats": return stats;
                    case "getSystem": return system;
                    case "getFleetMember": return member;
                    case "getShipAI": return activeAI[0];
                    case "setShipAI": activeAI[0] = (ShipAIPlugin) values[0]; break;
                    case "getChildModulesCopy": return List.of();
                    case "setDefaultAI":
                        check(values[0] == member, "Keep member-aware native AI handoff");
                        // Reproduce a new, lazily initialized native script
                        // after opening construction and every resurrection.
                        script[0] = new MoteControlScript();
                        activations[0]++;
                        break;
                    default: break;
                }
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("createDefaultShipAI")) {
                    ShipAIConfig config = (ShipAIConfig) values[1];
                    check(Personalities.RECKLESS.equals(config.personalityOverride)
                                    && config.alwaysStrafeOffensively,
                            "Fix emitter initialization without removing Fearless AI");
                    return ai;
                }
                return null;
            }));
            try {
                script[0].getLockTarget(mirror[0], target);
                throw new AssertionError("An uninitialized native script must reproduce the reported crash");
            } catch (NullPointerException expected) {
                check(expected.getMessage().contains("attractor"), "Reproduce the same null-attractor failure");
            }
            for (int i = 0; i < 8; i++) {
                // Each reconstruction produces a new unfinished native ship,
                // rather than refreshing an already installed custom AI.
                activeAI[0] = null;
                GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(mirror[0], member, 1);
                check(script[0].getLockTarget(mirror[0], target) == enemy,
                        "Native lock targeting remains functional after construction/rebuild");
            }
            check(evaluations[0] == 8 && activations[0] == 8,
                    "Initialize only once per opening/rebuild handoff, not each frame");
            System.out.println("PASS: real native null-attractor crash reproduced; opening/rebuilt Ziggurat "
                    + "mirrors initialize their unchanged Mote Attractor before Fearless AI evaluation, without gameplay ticks.");
        } finally {
            Global.setSettings(oldSettings);
            Global.setCombatEngine(oldEngine);
        }
    }

    private static WeaponSlotAPI slot(WeaponAPI.WeaponSize size, Vector2f position) {
        return mock(WeaponSlotAPI.class, (name, values) -> {
            if (name.equals("isSystemSlot")) return true;
            if (name.equals("getSlotSize")) return size;
            if (name.equals("computePosition")) return new Vector2f(position);
            return null;
        });
    }
}
