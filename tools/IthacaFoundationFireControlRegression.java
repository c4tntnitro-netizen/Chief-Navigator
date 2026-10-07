import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

/** Verifies direct-weapon foundations receive only vanilla AI/autofire setup. */
public final class IthacaFoundationFireControlRegression {
    public static void main(String[] args) throws Exception {
        int[] resetCalls = {0};
        int[] evaluationCalls = {0};
        int[] toggleCalls = {0};
        int[] wreckToggleOffCalls = {0};
        int[] unlockCalls = {0};
        int[] forbiddenTargetCalls = {0};
        boolean[] autofiring = {false};
        boolean[] wreckAutofiring = {true};
        boolean[] liveForceDisabled = {true};
        boolean[] wreckForceDisabled = {false};
        int[] liveHealthRestores = {0};
        int[] wreckHealthRestores = {0};
        int[] wreckStopCalls = {0};

        ShipAIPlugin defaultAI = WallResearchRegression.mock(
                ShipAIPlugin.class, (name, arguments) -> {
                    if (name.equals("forceCircumstanceEvaluation")) {
                        evaluationCalls[0]++;
                    }
                    return null;
                });
        ShipAIPlugin[] installedAI = {null};
        WeaponAPI liveWeapon = WallResearchRegression.mock(
                WeaponAPI.class, (name, arguments) -> {
                    if (name.equals("getId")) {
                        return "chief_navigator_drifting_wall_colossal_inert";
                    }
                    if (name.equals("getMaxHealth")) return 100f;
                    if (name.equals("setForceDisabled")) {
                        liveForceDisabled[0] = (Boolean) arguments[0];
                    }
                    if (name.equals("setCurrHealth")) liveHealthRestores[0]++;
                    return null;
                });
        WeaponAPI wreckWeapon = WallResearchRegression.mock(
                WeaponAPI.class, (name, arguments) -> {
                    if (name.equals("getId")) {
                        return "chief_navigator_drifting_wall_colossal_destroyed";
                    }
                    if (name.equals("setForceDisabled")) {
                        wreckForceDisabled[0] = (Boolean) arguments[0];
                    }
                    if (name.equals("setCurrHealth")) wreckHealthRestores[0]++;
                    if (name.equals("stopFiring")) wreckStopCalls[0]++;
                    return null;
                });
        WeaponGroupAPI group = WallResearchRegression.mock(
                WeaponGroupAPI.class, (name, arguments) -> {
                    if (name.equals("isAutofiring")) return autofiring[0];
                    if (name.equals("getWeaponsCopy")) {
                        return Collections.singletonList(liveWeapon);
                    }
                    if (name.equals("toggleOn")) {
                        autofiring[0] = true;
                        toggleCalls[0]++;
                    }
                    return null;
                });
        WeaponGroupAPI wreckGroup = WallResearchRegression.mock(
                WeaponGroupAPI.class, (name, arguments) -> {
                    if (name.equals("isAutofiring")) {
                        return wreckAutofiring[0];
                    }
                    if (name.equals("getWeaponsCopy")) {
                        return Collections.singletonList(wreckWeapon);
                    }
                    if (name.equals("toggleOff")) {
                        wreckAutofiring[0] = false;
                        wreckToggleOffCalls[0]++;
                    }
                    return null;
                });
        ShipAPI foundation = WallResearchRegression.mock(
                ShipAPI.class, (name, arguments) -> {
                    if (name.equals("getShipAI")) return installedAI[0];
                    if (name.equals("resetDefaultAI")) {
                        installedAI[0] = defaultAI;
                        resetCalls[0]++;
                    }
                    if (name.equals("getWeaponGroupsCopy")) {
                        return Arrays.asList(group, wreckGroup);
                    }
                    if (name.equals("setControlsLocked")) {
                        WallResearchRegression.check(
                                !((Boolean) arguments[0]),
                                "Foundation controls must be unlocked");
                        unlockCalls[0]++;
                    }
                    if (name.equals("setShipTarget")
                            || name.equals("setShipAI")) {
                        forbiddenTargetCalls[0]++;
                    }
                    return null;
                });

        Class<?> support = Class.forName(
                "chiefnavigator.quest.IthacaFoundationFireControl");
        Method ensure = support.getDeclaredMethod("ensureActive", ShipAPI.class);
        ensure.setAccessible(true);
        Method restore = support.getDeclaredMethod(
                "restoreWeapon", WeaponAPI.class);
        restore.setAccessible(true);
        ensure.invoke(null, foundation);
        ensure.invoke(null, foundation);
        restore.invoke(null, liveWeapon);
        restore.invoke(null, wreckWeapon);

        WallResearchRegression.check(resetCalls[0] == 1,
                "Missing direct-parent AI must install engine default once");
        WallResearchRegression.check(toggleCalls[0] == 1 && autofiring[0],
                "Naval weapon group must be placed in vanilla autofire once");
        WallResearchRegression.check(wreckToggleOffCalls[0] == 1
                        && !wreckAutofiring[0],
                "Destroyed fixture group must stay out of autofire");
        WallResearchRegression.check(!liveForceDisabled[0]
                        && liveHealthRestores[0] == 1,
                "Live naval mount must be enabled and restored");
        WallResearchRegression.check(wreckForceDisabled[0]
                        && wreckStopCalls[0] == 1
                        && wreckHealthRestores[0] == 0,
                "Destroyed fixture must remain stopped and force-disabled");
        WallResearchRegression.check(evaluationCalls[0] == 1,
                "Newly activated vanilla AI must evaluate immediately");
        WallResearchRegression.check(unlockCalls[0] == 2,
                "Foundation controls must remain unlocked every frame");
        WallResearchRegression.check(forbiddenTargetCalls[0] == 0,
                "Activation must not install custom AI or force a target");
        System.out.println(
                "Ithaca foundation vanilla fire-control regression passed.");
    }
}
