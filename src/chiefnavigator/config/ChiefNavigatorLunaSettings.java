package chiefnavigator.config;

import lunalib.lunaSettings.LunaSettings;

/** Isolates optional LunaLib symbols from the campaign settings facade. */
final class ChiefNavigatorLunaSettings {
    private ChiefNavigatorLunaSettings() { }

    static Integer getTroyFleetDPLimit() {
        return LunaSettings.getInt(ChiefNavigatorSettings.MOD_ID,
                ChiefNavigatorSettings.TROY_FLEET_DP_LIMIT);
    }

    static Boolean getSanzuClouds() {
        return LunaSettings.getBoolean(ChiefNavigatorSettings.MOD_ID,
                ChiefNavigatorSettings.SANZU_CLOUDS);
    }
}
