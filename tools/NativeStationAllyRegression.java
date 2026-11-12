import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.entities.BaseEntity;
import com.fs.starfarer.combat.entities.Ship;
import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;

/** Native ship cache and deployed-member ally classification; reflection is test-only. */
public final class NativeStationAllyRegression extends WallResearchRegression {
    static Unsafe unsafe() throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        return (Unsafe)f.get(null);
    }

    static final class NativeStation {
        final FleetMember member;
        final Ship ship;
        final CombatFleetManager.O0 deployed;
        NativeStation(int owner) throws Exception {
            member=(FleetMember)unsafe().allocateInstance(FleetMember.class);
            member.setOwner(owner);member.setAlly(false);
            ship=(Ship)unsafe().allocateInstance(Ship.class);
            ship.setOwner(owner);ship.setOriginalOwner(owner);ship.setAlly(false);
            ship.setFleetMember(member);
            Field data=BaseEntity.class.getDeclaredField("customData");data.setAccessible(true);
            data.set(ship,new HashMap<String,Object>());
            deployed=new CombatFleetManager.O0(member,ship);
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previous=Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class,(n,a)->null));
            org.json.JSONObject settings=new org.json.JSONObject(java.nio.file.Files.readString(
                    java.nio.file.Path.of(System.getProperty("chiefNavigator.testCore", "../../starsector-core"), "data/config/settings.json")).replaceAll("(?m)#.*$", ""));
            for (Field field:com.fs.starfarer.settings.StarfarerSettings.class.getDeclaredFields()) {
                if (field.getType()==org.json.JSONObject.class&&Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);field.set(null,settings);
                }
            }
            Class<?> controllerType=Class.forName("chiefnavigator.quest.IthacaFinalLaborBattleCreationPlugin$CenterBreachCombatPlugin");
            Constructor<?> ctor=controllerType.getDeclaredConstructor(CombatEngineAPI.class,List.class,FleetMemberAPI.class,boolean.class,boolean.class);
            ctor.setAccessible(true);
            Object controller=ctor.newInstance(mock(CombatEngineAPI.class,(n,a)->null),List.of(),null,false,false);
            Method configure=controllerType.getDeclaredMethod("markStationStructure",ShipAPI.class);
            configure.setAccessible(true);
            NativeStation parent=new NativeStation(0), module=new NativeStation(0), enemy=new NativeStation(1);
            module.ship.setParentStation(parent.ship);
            check(!parent.ship.isAlly()&&!parent.deployed.isAlly(),"Player-side station initially belongs to player command group");
            parent.ship.setAlly(true);
            check(!parent.deployed.isAlly(),"Ship-only ally flag does not update native deployed-member classification");
            parent.ship.setAlly(false);
            configure.invoke(controller,parent.ship);configure.invoke(controller,module.ship);
            configure.invoke(controller,enemy.ship);
            if (args.length>0&&args[0].equals("--baseline")) {
                check(!parent.ship.isAlly()&&!parent.member.isAlly()&&!parent.deployed.isAlly(),"Baseline reproduces player command ownership after station configuration");
                System.out.println("REPRODUCED: native station ship and deployed fleet record both remain non-allied after the original final-battle station configuration.");
                return;
            }
            for (int i=0;i<30;i++) {
                configure.invoke(controller,parent.ship);configure.invoke(controller,module.ship);
                check(parent.ship.isAlly()&&parent.member.isAlly()&&parent.deployed.isAlly(),"Parent is allied in native ship cache and deployed member");
                check(module.ship.isAlly()&&module.member.isAlly()&&module.deployed.isAlly(),"Module and native parent-based command classification are allied");
                check(parent.ship.getOwner()==0&&parent.member.getOwner()==0&&parent.ship.getOriginalOwner()==0,"Friendly side and original owner remain unchanged");
            }
            check(!enemy.ship.isAlly()&&!enemy.member.isAlly()&&!enemy.deployed.isAlly(),"Enemy-side station is never moved to allied command");
            module.ship.setParentStation(null);
            check(module.deployed.isAlly(),"Detached module keeps its own allied fleet record");
            Field ally=FleetMember.class.getDeclaredField("isAlly");
            check(Modifier.isTransient(ally.getModifiers()),"Ally classification does not persist as campaign ownership");
            System.out.println("PASS: actual native ship cache, fleet/deployed member and attached/detached module ally classification; enemy isolation, repeat calls and unchanged team ownership.");
        } finally { Global.setSettings(previous); }
    }
}
