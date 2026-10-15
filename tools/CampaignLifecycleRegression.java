import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Source-contract checks for serialized campaign-state ownership. */
public final class CampaignLifecycleRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Path findRoot() {
        Path candidate = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && candidate != null; depth++) {
            if (Files.isRegularFile(candidate.resolve(
                    "src/chiefnavigator/ChiefNavigatorModPlugin.java"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate Chief Navigator root");
    }

    private static String read(Path root, String relative) throws Exception {
        return new String(Files.readAllBytes(root.resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        check(start >= 0, "Could not locate " + signature);
        int open = source.indexOf('{', start);
        check(open >= 0, "Could not locate body for " + signature);

        int depth = 0;
        boolean inString = false;
        boolean inCharacter = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;
        for (int index = open; index < source.length(); index++) {
            char ch = source.charAt(index);
            char next = index + 1 < source.length()
                    ? source.charAt(index + 1) : '\0';

            if (inLineComment) {
                if (ch == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }
            if (inString || inCharacter) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (inString && ch == '"') {
                    inString = false;
                } else if (inCharacter && ch == '\'') {
                    inCharacter = false;
                }
                continue;
            }
            if (ch == '/' && next == '/') {
                inLineComment = true;
                index++;
                continue;
            }
            if (ch == '/' && next == '*') {
                inBlockComment = true;
                index++;
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '\'') {
                inCharacter = true;
                continue;
            }
            if (ch == '{') depth++;
            if (ch == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("Unterminated body for " + signature);
    }

    private static void checkBefore(
            String source, String earlier, String later, String message) {
        int first = source.indexOf(earlier);
        int second = source.indexOf(later);
        check(first >= 0 && second > first, message);
    }

    private static void checkStaticCreationLifecycle(
            String odyssey, String troy) {
        String odysseyEnsure = methodBody(
                odyssey, "public static StarSystemAPI ensureExists()");
        checkBefore(odysseyEnsure, "if (existing != null)",
                "return existing;",
                "Odyssey ensure must guard an existing serialized system");
        checkBefore(odysseyEnsure, "return existing;",
                "createGeneratedNebula()",
                "Odyssey ensure must preserve existing system identity");

        String troyEnsure = methodBody(
                troy, "public static StarSystemAPI ensureWaypointTroyExists()");
        checkBefore(troyEnsure, "if (existing != null)",
                "return existing;",
                "Troy ensure must guard an existing serialized system");
        checkBefore(troyEnsure, "return existing;",
                "createStarSystem(\"Waypoint Troy\")",
                "Troy ensure must preserve existing system identity");
    }

    private static void checkTroyTopology(String troy) {
        String topology = methodBody(
                troy, "private static void ensureCanonicalTopology(");
        String localCollision =
                "localClaim != null && (!(localClaim instanceof JumpPointAPI)";
        int localGuard = topology.indexOf(localCollision);
        int localExit = topology.indexOf("return;", localGuard);
        int localCreation = topology.indexOf(
                "Global.getFactory().createJumpPoint(", localGuard);
        check(localGuard >= 0 && localExit > localGuard
                        && localCreation > localExit,
                "Troy must preserve a wrong-type local ID owner");

        String hyperCollision =
                "hyperClaim != null && (!(hyperClaim instanceof JumpPointAPI)";
        int hyperGuard = topology.indexOf(hyperCollision);
        int hyperExit = topology.indexOf("return;", hyperGuard);
        int hyperCreation = topology.indexOf(
                "Global.getFactory().createJumpPoint(", hyperGuard);
        check(hyperGuard >= 0 && hyperExit > hyperGuard
                        && hyperCreation > hyperExit,
                "Troy must preserve a wrong-type hyperspace ID owner");
        check(topology.contains(
                        "if (!hasDestination(access, accessInHyperspace))"),
                "Troy must add only its missing owned destination");
        check(topology.contains(
                        "if (!hasDestination(accessInHyperspace, access))"),
                "Troy must add only its missing reciprocal destination");

        check(!topology.contains("removeEntity("),
                "Troy topology must not delete campaign entities");
        check(!topology.contains("clearDestinations("),
                "Troy topology must not clear serialized destinations");
        check(!topology.contains("autogenerateHyperspaceJumpPoints("),
                "Troy topology must not invoke global autogeneration");
        check(!topology.contains("getAutogeneratedJumpPointsInHyper("),
                "Troy must not trust transient autogenerated jump lists");
        check(!topology.contains("getAutogeneratedNascentWellsInHyper("),
                "Troy must not trust transient autogenerated well lists");
        check(!topology.contains(".clear()"),
                "Troy topology must not clear serialized collections");
    }

    private static void checkLoadLifecycle(String plugin) {
        String load = methodBody(
                plugin, "public void onGameLoad(boolean newGame)");
        String troyCreation = "TroyArrivalScript.ensureWaypointTroyExists(";
        String odysseyCreation = "OdysseyExpanseSystem.ensureExists(";
        checkBefore(load, "CampaignWorldInitialization.begin(",
                "initializeDomainCombatGuardRelations()",
                "First-install detection must precede load-time mod flags");
        String firstInstall = methodBody(load, "if (initializeWorlds)");
        check(firstInstall.contains(troyCreation)
                        && firstInstall.contains(odysseyCreation),
                "First installation must create Troy and the Orion Knot");
        String existingSavePath = load.replace(firstInstall, "");
        check(!existingSavePath.contains(troyCreation)
                        && !existingSavePath.contains(odysseyCreation),
                "Already initialized saves must omit static world recreation");

        String[] forbiddenOnLoad = {
            "cleanupLegacyPrototypeContent(",
            "retireTaskForceSpartanEncounter(",
            "ensureShowcaseEncounters(",
            "configureCluster",
            "OdysseyPredatorScript.enforceThreatContainment("
        };
        for (String forbidden : forbiddenOnLoad) {
            check(!load.contains(forbidden),
                    "onGameLoad must not recreate or clean static state: "
                            + forbidden);
        }
    }

    private static void checkPredatorOwnership(String predator) {
        String authored = methodBody(predator,
                "static boolean isAuthoredStarvingThreatFleet(");
        check(authored.contains(
                        "fleet == null || isPlayerControlledFleet(fleet)"),
                "Predator ownership must explicitly reject the player fleet");
        check(authored.contains("STARVING_THREAT_MARKER"),
                "Predator ownership must honor its explicit fleet marker");
        String[] genericHullEvidence = {
            "STARVING_THREAT_HULL_TAG",
            "getHullSpec(",
            "hasTag(",
            "getFleetData(",
            "getMembersListCopy("
        };
        for (String generic : genericHullEvidence) {
            check(!authored.contains(generic),
                    "A generic member hull cannot establish fleet ownership: "
                            + generic);
        }

        String playerGuard = methodBody(predator,
                "private static boolean isPlayerControlledFleet(");
        check(playerGuard.contains("fleet.isPlayerFleet()"),
                "Player ownership must honor the engine player flag");
        check(playerGuard.contains(
                        "fleet == Global.getSector().getPlayerFleet()"),
                "Player ownership must also use fleet identity");
    }

    private static void checkRecurringPredator(String predator) {
        String advance = methodBody(
                predator, "public void advance(float amount)");
        check(!advance.contains("retireTaskForceSpartanEncounter("),
                "Recurring maintenance must not retire Task Force Spartan");
        check(!advance.contains("enforceThreatContainment("),
                "Recurring maintenance must not delete global fleet state");
        check(!advance.contains("removeEntity("),
                "Recurring maintenance must not directly delete entities");
        check(!advance.contains("OdysseyExpanseSystem.ensureExists("),
                "Recurring maintenance must not recreate static systems");

        String budai = methodBody(
                predator, "private void ensureCharybdisHunter()");
        check(budai.contains("BUDAI_SPAWNED") && budai.contains("isBudaiDefeated()"),
                "Budai must have a one-time spawn and permanent defeat lifecycle");
        String lookup = methodBody(predator, "private static CampaignFleetAPI findBudaiFleet()");
        check(lookup.contains("isPlayerControlledFleet(fleet)"),
                "Budai's local lookup must exclude the player fleet");
    }

    private static void checkSharedFleetGuard(String troy) {
        String busy = methodBody(
                troy, "static boolean isFleetBusyForMutation(");
        check(busy.contains("isPlayerControlledFleet(fleet)"),
                "The shared mutation guard must reject the player fleet");
    }

    private static void checkRetiredRoomFacade(String veiled) {
        String ensure = methodBody(
                veiled, "public static StarSystemAPI ensureExists(");
        check(ensure.contains("return findExisting();"),
                "The retired Veiled Sun room must be find-only");
        check(!ensure.contains("generateRoom("),
                "The retired Veiled Sun room must never regenerate");
    }

    private static void checkRewardFailureLatch(String rewards) {
        String ensure = methodBody(
                rewards, "private static void ensureMainSequenceReward(");
        int latch = ensure.indexOf(
                "memory().set(MAIN_SEQUENCE_REWARD_SPAWNED, true)");
        int factory = ensure.indexOf("createEmptyFleet(");
        check(latch >= 0 && factory > latch,
                "Reward creation must latch before calling mod-sensitive "
                        + "factory code");
        check(ensure.contains("MAIN_SEQUENCE_REWARD_INVALID_V1"),
                "Partial reward construction must be quarantined");
    }

    private static void checkTopographyOwnership(String topography) {
        String install = methodBody(topography,
                "public static SinniHyperspaceTopographyEventIntel "
                        + "ensureInstalled()");
        String guard =
                "current.getClass() != HyperspaceTopographyEventIntel.class";
        int guardIndex = install.indexOf(guard);
        int clearIndex = install.indexOf("current.getFactors().clear()");
        int endIndex = install.indexOf("current.endImmediately()");
        check(guardIndex >= 0,
                "Topography replacement needs an exact-class guard");
        check(clearIndex > guardIndex && endIndex > guardIndex,
                "Unknown topography subclasses must be guarded before mutation");
        String guardedExit = install.substring(
                guardIndex, Math.min(clearIndex, endIndex));
        check(guardedExit.contains("return null;"),
                "Unknown topography subclasses must be preserved intact");
    }

    public static void main(String[] args) throws Exception {
        Path root = findRoot();
        String odyssey = read(root,
                "src/chiefnavigator/quest/OdysseyExpanseSystem.java");
        String troy = read(root,
                "src/chiefnavigator/quest/TroyArrivalScript.java");
        String plugin = read(root,
                "src/chiefnavigator/ChiefNavigatorModPlugin.java");
        String predator = read(root,
                "src/chiefnavigator/quest/OdysseyPredatorScript.java");
        String topography = read(root,
                "src/chiefnavigator/topography/"
                        + "SinniHyperspaceTopographyEventIntel.java");
        String veiled = read(root,
                "src/chiefnavigator/quest/VeiledSunRoom.java");
        String rewards = read(root,
                "src/chiefnavigator/quest/SinniVignetteRewards.java");

        checkStaticCreationLifecycle(odyssey, troy);
        checkTroyTopology(troy);
        checkLoadLifecycle(plugin);
        String creation = methodBody(plugin, "public void onNewGameAfterEconomyLoad()");
        check(!creation.contains("CampaignWorldInitialization.begin(")
                        && !creation.contains("TroyArrivalScript.ensureWaypointTroyExists()")
                        && !creation.contains("OdysseyExpanseSystem.ensureExists()"),
                "Worlds must not spawn before the engine's initial time advance");
        checkPredatorOwnership(predator);
        checkRecurringPredator(predator);
        checkSharedFleetGuard(troy);
        checkRetiredRoomFacade(veiled);
        checkRewardFailureLatch(rewards);
        checkTopographyOwnership(topography);
        System.out.println("Campaign lifecycle regression passed.");
    }
}
