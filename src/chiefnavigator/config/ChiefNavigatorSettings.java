package chiefnavigator.config;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

/** Live campaign settings; LunaLib remains optional at runtime. */
public final class ChiefNavigatorSettings {
    public static final String MOD_ID = "chief_navigator";
    public static final String TROY_FLEET_DP_LIMIT = "chiefNavigatorTroyFleetDPLimit";
    public static final int DEFAULT_TROY_FLEET_DP_LIMIT = 50;
    public static final int MIN_TROY_FLEET_DP_LIMIT = 1;
    public static final int MAX_TROY_FLEET_DP_LIMIT = 1000;

    private ChiefNavigatorSettings() { }

    /** Re-read each time so a saved LunaLib change takes effect without reloading. */
    public static float getTroyFleetDPLimit() {
        try {
            SettingsAPI settings = Global.getSettings();
            if (settings == null || settings.getModManager() == null
                    || !settings.getModManager().isModEnabled("lunalib")) {
                return DEFAULT_TROY_FLEET_DP_LIMIT;
            }
            // The separate bridge is resolved only after the enabled-mod check.
            Integer configured = ChiefNavigatorLunaSettings.getTroyFleetDPLimit();
            if (configured == null) return DEFAULT_TROY_FLEET_DP_LIMIT;
            return Math.max(MIN_TROY_FLEET_DP_LIMIT,
                    Math.min(MAX_TROY_FLEET_DP_LIMIT, configured));
        } catch (RuntimeException | LinkageError unavailable) {
            return DEFAULT_TROY_FLEET_DP_LIMIT;
        }
    }
}
