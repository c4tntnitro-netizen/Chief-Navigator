package lunalib.lunaSettings;

/**
 * Test-only stand-in for LunaLib's settings reader, so regressions can switch
 * Chief Navigator's LunaLib settings without a running LunaLib. Never packaged.
 */
public final class LunaSettings {
    public static Boolean booleanValue = Boolean.TRUE;
    public static Integer intValue = null;
    public static boolean throwOnRead = false;

    private LunaSettings() { }

    public static Boolean getBoolean(String modId, String fieldId) {
        if (throwOnRead) throw new IllegalStateException("settings not loaded");
        return booleanValue;
    }

    public static Integer getInt(String modId, String fieldId) {
        if (throwOnRead) throw new IllegalStateException("settings not loaded");
        return intValue;
    }
}
