package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.*;

/** Exercises the naming patch against serialized objects; rejects topology edits. */
public final class OdysseySystemNamesRegression {
    private static final String OWNED = "$chief_navigator_odyssey_authored_system_v1";
    private static final String MIGRATED = "$chief_navigator_system_display_names_v1";
    private interface Call { Object run(String name, Object[] args); }
    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("toString")) return type.getSimpleName();
                    return call.run(name, args);
                }));
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object unexpected(String name) {
        throw new AssertionError("Unexpected operation: " + name);
    }
    private static int writes;
    private static final class Token {
        String id, name;
        LocationAPI location;
        SectorEntityToken target;
        final List<JumpPointAPI.JumpDestination> destinations = new ArrayList<>();
        Token(String id, String name, LocationAPI location) {
            this.id = id; this.name = name; this.location = location;
        }
        <T extends SectorEntityToken> T api(Class<T> type) {
            return mock(type, (n, a) -> {
                if (n.equals("getId")) return id;
                if (n.equals("getName")) return name;
                if (n.equals("getContainingLocation")) return location;
                if (n.equals("getDestinations")) return destinations;
                if (n.equals("getTarget")) return target;
                if (n.equals("setName")) { name = (String) a[0]; writes++; return null; }
                return unexpected(n);
            });
        }
    }
    private static final class SystemState {
        String id;
        String base, name;
        final Map<String, Object> flags = new HashMap<>();
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final List<CampaignFleetAPI> fleets = new ArrayList<>();
        PlanetAPI star;
        SectorEntityToken anchor;
        final MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(a[0]));
            if (n.equals("set")) {
                check(a.length == 2 && MIGRATED.equals(a[0]), "Only a permanent naming latch may change");
                flags.put((String) a[0], a[1]); writes++; return null;
            }
            return unexpected(n);
        });
        final StarSystemAPI api = mock(StarSystemAPI.class, (n, a) -> {
            if (n.equals("getOptionalUniqueId")) return id;
            if (n.equals("getMemoryWithoutUpdate")) return memory;
            if (n.equals("getName")) return name;
            if (n.equals("getBaseName")) return base;
            if (n.equals("getStar") || n.equals("getCenter")) return star;
            if (n.equals("getHyperspaceAnchor")) return anchor;
            if (n.equals("getFleets")) return fleets;
            if (n.equals("getEntityById")) return entities.get(a[0]);
            if (n.equals("setBaseName")) {
                base = (String) a[0]; name = base + " Star System"; writes++; return null;
            }
            if (n.equals("setName")) { name = (String) a[0]; writes++; return null; }
            return unexpected(n);
        });
        SystemState(String id, String oldName, boolean owned) {
            this.id = id; base = oldName; name = oldName + " Star System";
            if (owned) flags.put(OWNED, true);
        }
    }
    private static final class Save {
        final List<StarSystemAPI> systems = new ArrayList<>();
        final List<SectorEntityToken> hyperTokens = new ArrayList<>();
        final LocationAPI hyper = mock(LocationAPI.class, (n, a) ->
                n.equals("getAllEntities") ? hyperTokens : unexpected(n));
        boolean hasHyper = true;
        CampaignFleetAPI player;
        Save() {
            Global.setFactory(mock(FactoryAPI.class, (n, a) -> unexpected("factory." + n)));
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                if (n.equals("getStarSystems")) return systems;
                if (n.equals("getHyperspace")) return hasHyper ? hyper : null;
                if (n.equals("getPlayerFleet")) return player;
                return unexpected(n);
            }));
        }
    }
    private static final class FleetState {
        String name = "Ashen Verge Drone Patrol", action = "roaming Ashen Verge";
        final FleetAssignmentDataAPI assignment = mock(FleetAssignmentDataAPI.class, (n, a) -> {
            if (n.equals("getActionText")) return action;
            if (n.equals("setActionText")) { action = (String) a[0]; writes++; return null; }
            return unexpected(n);
        });
        CampaignFleetAPI api(boolean owned, boolean isPlayer) {
            MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> n.equals("getBoolean")
                    ? owned && OdysseyPredatorScript.AUTHORED_MOBILE_DERELICT_MARKER.equals(a[0]) : unexpected(n));
            CampaignFleetAIAPI ai = mock(CampaignFleetAIAPI.class, (n, a) ->
                    n.equals("getAssignmentsCopy") ? List.of(assignment) : unexpected(n));
            return mock(CampaignFleetAPI.class, (n, a) -> {
                if (n.equals("getId")) return "chief_navigator_ashen_trial_roamer_0";
                if (n.equals("getName")) return name;
                if (n.equals("setName")) { name = (String) a[0]; writes++; return null; }
                if (n.equals("isPlayerFleet")) return isPlayer;
                if (n.equals("getMemoryWithoutUpdate")) return memory;
                if (n.equals("getAI")) return ai;
                return unexpected(n);
            });
        }
    }
    private static void renameAndPreserve(String id, String oldName, String newName, String primaryId, int routes) {
        Save save = new Save();
        SystemState system = new SystemState(id, oldName, true);
        save.systems.add(system.api);
        Token star = new Token(id + "_star", oldName, system.api);
        if (!id.equals(OdysseyExpanseSystem.DEVOURED_REACH_ID)) system.star = star.api(PlanetAPI.class);
        Token anchor = new Token("system_anchor", oldName, save.hyper);
        system.anchor = anchor.api(SectorEntityToken.class);
        Token well = new Token("saved_well", oldName + " Gravity Well", save.hyper);
        well.target = system.star;
        if (system.star != null) save.hyperTokens.add(well.api(NascentGravityWellAPI.class));
        Token unrelated = new Token("foreign", oldName + " Jump-point", save.hyper);
        save.hyperTokens.add(unrelated.api(JumpPointAPI.class));
        List<Token> endpoints = new ArrayList<>();
        List<JumpPointAPI.JumpDestination> destinations = new ArrayList<>();
        for (int i = 0; i < routes; i++) {
            String localId = i == 0 ? primaryId : "chief_navigator_cluster_"
                    + (i == 1 ? "secondary" : "tertiary") + "_local_" + id;
            Token local = new Token(localId, oldName + " Jump-point", system.api);
            JumpPointAPI localApi = local.api(JumpPointAPI.class);
            system.entities.put(localId, localApi);
            Token hyper = new Token("serialized_random_" + i, oldName + " Jump-point", save.hyper);
            JumpPointAPI hyperApi = hyper.api(JumpPointAPI.class);
            JumpPointAPI.JumpDestination inbound = new JumpPointAPI.JumpDestination(localApi, oldName);
            inbound.setMinDistFromToken(123f); inbound.setMaxDistFromToken(456f);
            hyper.destinations.add(inbound);
            local.destinations.add(new JumpPointAPI.JumpDestination(hyperApi, "Hyperspace"));
            save.hyperTokens.add(hyperApi);
            endpoints.add(local); endpoints.add(hyper); destinations.add(inbound);
        }
        FleetState owned = new FleetState(), foreign = new FleetState(), player = new FleetState();
        system.fleets.add(owned.api(true, false)); system.fleets.add(foreign.api(false, false));
        save.player = player.api(true, false); system.fleets.add(save.player);
        int originalTokenCount = save.hyperTokens.size();
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(system.base.equals(newName) && system.name.equals(newName + " Star System"), "System and base names");
        check(anchor.name.equals(newName), "Map anchor name");
        if (system.star != null) {
            check(star.name.equals(newName), "Primary star name");
            check(well.name.equals(newName + " Gravity Well"), "Existing primary well name");
        }
        for (Token endpoint : endpoints) check(endpoint.name.equals(newName + " Jump-point"), "Both route labels");
        for (int i = 0; i < destinations.size(); i++) {
            JumpPointAPI.JumpDestination dest = destinations.get(i);
            check(dest == endpoints.get(i * 2 + 1).destinations.get(0), "Destination object identity");
            check(dest.getDestination() == system.entities.get(endpoints.get(i * 2).id), "Destination target identity");
            check(dest.getLabelInInteractionDialog().equals(newName), "Saved travel-menu label");
            check(dest.getMinDistFromToken() == 123f && dest.getMaxDistFromToken() == 456f, "Arrival distance remains intact");
        }
        check(unrelated.name.equals(oldName + " Jump-point"), "Unrelated hyperspace entity untouched");
        check(foreign.name.equals("Ashen Verge Drone Patrol") && player.name.equals(foreign.name), "Foreign/player names untouched");
        check(foreign.action.equals("roaming Ashen Verge") && player.action.equals(foreign.action), "Foreign/player orders untouched");
        if (oldName.equals("Ashen Verge")) {
            check(owned.name.equals("Lacedaemon Drone Patrol") && owned.action.equals("roaming Lacedaemon"), "Owned fleet labels");
        }
        check(save.hyperTokens.size() == originalTokenCount && save.systems.get(0) == system.api, "Serialized object set preserved");
        int after = writes;
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(writes == after, "Second load performs no writes");
    }
    public static void main(String[] args) {
        renameAndPreserve(OdysseyExpanseSystem.ASHEN_VERGE_ID, "Ashen Verge", "Lacedaemon", "chief_navigator_ashen_verge_passage", 3);
        renameAndPreserve(OdysseyExpanseSystem.DEVOURED_REACH_ID, "Devoured Reach", "Anatolia",
                "chief_navigator_cluster_jump_hyper_" + OdysseyExpanseSystem.DEVOURED_REACH_ID + "_local", 1);
        renameAndPreserve(OdysseyExpanseSystem.LAST_LIGHT_ID, "Last Light", "Bouvo Olympus", "chief_navigator_odyssey_last_light_local_jump", 3);
        Save save = new Save();
        SystemState foreign = new SystemState(OdysseyExpanseSystem.ASHEN_VERGE_ID, "Ashen Verge", false);
        save.systems.add(foreign.api);
        int before = writes;
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(writes == before && foreign.base.equals("Ashen Verge"), "Unowned ID collision preserved");
        save.systems.clear();
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(writes == before, "Missing systems never created");
        SystemState partial = new SystemState(OdysseyExpanseSystem.ASHEN_VERGE_ID, "Ashen Verge", true);
        partial.name = "Custom display name";
        Token wrongType = new Token("chief_navigator_ashen_verge_passage", "Ashen Verge Jump-point", partial.api);
        partial.entities.put(wrongType.id, wrongType.api(SectorEntityToken.class));
        save.systems.add(partial.api); save.hasHyper = false;
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(partial.base.equals("Lacedaemon") && partial.name.equals("Custom display name"), "Custom full name preserved");
        check(wrongType.name.equals("Ashen Verge Jump-point"), "Wrong-type ID claim preserved");
        check(!partial.flags.containsKey(MIGRATED), "Unavailable hyperspace may finish on a later load");
        save.hasHyper = true;
        OdysseyExpanseSystem.renameAuthoredSystemLabels();
        check(Boolean.TRUE.equals(partial.flags.get(MIGRATED)), "Partial naming patch finishes without reconstructing missing state");
        Global.setSector(null); Global.setFactory(null);
        System.out.println("Odyssey system naming regression passed: labels, saved routes, ownership, partial saves and repeat loads.");
    }
}
