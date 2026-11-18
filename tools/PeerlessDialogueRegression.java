package chiefnavigator.skills;
import peerless.api.PeerlessSkills;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.*;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import data.campaign.rulecmd.ChiefNavigatorPeerlessCMD;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Legacy fixture helpers and the removed Menelaus training-menu contract. */
public final class PeerlessDialogueRegression {
    interface Call { Object call(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a)-> {
            if(m.getName().equals("equals")) return p==a[0];
            if(m.getName().equals("hashCode")) return System.identityHashCode(p);
            Object result=call.call(m.getName(),a);
            if(result!=null)return result;
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==float.class)return 0f;
            if(m.getReturnType()==int.class)return 0;
            return null;
        }));
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    final List<JSONObject> rows=new ArrayList<>();
    final Map<String,Object> saved=new HashMap<>();
    final Map<String,String> options=new LinkedHashMap<>();
    final Set<String> gold=new HashSet<>();
    final List<String> transcript=new ArrayList<>();
    final Map<String,MemoryAPI> scope=new HashMap<>();
    float skill=1f;
    String choice;
    boolean returned;
    Call extraDialog=(n,a)->null;
    Call extraSector=(n,a)->null;
    java.util.function.Consumer<String> extraScript=line->{throw new AssertionError(line);};
    final MemoryAPI memory=mock(MemoryAPI.class,(n,a)-> {
        if(n.equals("getBoolean"))return Boolean.TRUE.equals(saved.get(a[0]));
        if(n.equals("get")||n.equals("getString"))return saved.get(a[0]);
        if(n.equals("set")){if(a[0].equals(MenelausTrial.DOMAIN_EPIC)||a[0].equals(PeerlessSkills.TRAINED))check(a.length==2,"Durable memory");saved.put((String)a[0],a[1]);}
        return null;
    });
    final MutableCharacterStatsAPI stats=mock(MutableCharacterStatsAPI.class,(n,a)-> {
        if(n.equals("getSkillLevel"))return skill;
        if(n.equals("setSkillLevel")){check(a[0].equals(Skills.HELMSMANSHIP),"Only Helmsmanship");skill=(Float)a[1];}
        return null;
    });
    final PersonAPI player=mock(PersonAPI.class,(n,a)->n.equals("getStats")?stats:null);
    final OptionPanelAPI panel=mock(OptionPanelAPI.class,(n,a)-> {
        if(n.equals("clearOptions")){options.clear();}
        if(n.equals("addOption"))check(options.put((String)a[1],(String)a[0])==null,"Unique options");
        if(n.equals("setOptionColor")){check(options.containsKey(a[0]),"Color applied after option exists");gold.add((String)a[0]);}
        return null;
    });
    final TextPanelAPI text=mock(TextPanelAPI.class,(n,a)-> {
        if(n.equals("addParagraph"))transcript.add((String)a[0]);
        if(n.equals("clear"))throw new AssertionError("Preserve transcript");return null;
    });
    final InteractionDialogAPI dialog=mock(InteractionDialogAPI.class,(n,a)-> {
        if(n.equals("setOptionColor")){check(a[1]!=null,"Resolved gold color");gold.add((String)a[0]);}
        if(n.equals("getOptionPanel"))return panel;
        if(n.equals("getTextPanel"))return text;
        if(n.equals("getPlugin"))return mock(InteractionDialogPlugin.class,(n2,a2)->n2.equals("getMemoryMap")?scope:null);
        return extraDialog.call(n,a);
    });
    boolean matches(JSONObject row) {
        for(String line:row.optString("conditions").split("\\R")) {
            if(line.isBlank())continue;
            if(line.startsWith("$option == ")){if(!line.substring(11).equals(choice))return false;continue;}
            boolean neg=line.startsWith("!");String cmd=neg?line.substring(1):line;
            check(cmd.startsWith("ChiefNavigatorPeerlessCMD "),"Known condition: "+line);
            boolean value=new ChiefNavigatorPeerlessCMD().execute(row.optString("id"),null,Misc.tokenize(cmd.substring(26)),scope);
            if(value==neg)return false;
        }
        return true;
    }
    RuleAPI rule(JSONObject row) {
        return mock(RuleAPI.class,(n,a)-> {
            if(n.equals("getId"))return row.optString("id");
            if(n.equals("getTrigger"))return row.optString("trigger");
            if(n.equals("getOptions")) {
                List<Option> result=new ArrayList<>();
                for(String line:row.optString("options").split("\\R")) {
                    if(line.isBlank())continue;
                    String[] parts=line.split(":",3);Option option=new Option();
                    option.order=Float.parseFloat(parts[0]);option.id=parts[1];option.text=parts[2];result.add(option);
                }
                return result;
            }
            if(n.equals("pickText"))return row.optString("text");
            if(n.equals("getText"))return List.of(row.optString("text"));
            if(n.equals("getScriptCopy"))return List.of();
            if(n.equals("runScript"))for(String line:row.optString("script").split("\\R")) {
                if(line.isBlank())continue;
                if(line.equals("FireAll ChiefNavigatorFobIthaca")){returned=true;continue;}
                if(line.startsWith("FireAll "))FireAll.fire(null,dialog,scope,line.substring(8));
                else if(line.startsWith("SetOptionColor "))new SetOptionColor().execute(null,dialog,Misc.tokenize(line.substring(15)),scope);
                else if(line.startsWith("ChiefNavigatorPeerlessCMD "))check(new ChiefNavigatorPeerlessCMD().execute(null,dialog,Misc.tokenize(line.substring(26)),scope),"Mutation succeeded");
                else extraScript.accept(line);
            }
            return null;
        });
    }
    void choose(String id) {
        check(options.containsKey(id),"Visible choice "+id+" in "+options);
        choice=id;options.clear();gold.clear();
        check(FireBest.fire(null,dialog,scope,"DialogOptionSelected"),"Choice handled");
        check(returned||!options.isEmpty(),"No dead end");
    }
    void run() throws Exception {
        Path root=Files.isDirectory(Path.of("data"))?Path.of(""):Path.of("Chief Navigator");
        JSONArray parsed=(JSONArray)Class.forName("com.fs.starfarer.loading.G").getMethod("o00000",String.class).invoke(null,Files.readString(root.resolve("data/campaign/rules.csv")));
        for(int i=0;i<parsed.length();i++)if(parsed.getJSONObject(i).optString("id").startsWith("chiefNavigatorPeerless"))rows.add(parsed.getJSONObject(i));
        scope.put(MemKeys.GLOBAL,memory);scope.put(MemKeys.LOCAL,memory);
        RulesAPI rules=mock(RulesAPI.class,(n,a)-> {
            if(n.equals("performTokenReplacement"))return a[1];
            if(n.equals("getAllMatching")||n.equals("getBestMatching")){
                List<RuleAPI> found=rows.stream().filter(r->r.optString("trigger").equals(a[1])).filter(this::matches).map(this::rule).toList();
                if(n.equals("getAllMatching"))return found;
                check(found.size()<=1,"Unambiguous choice");return found.isEmpty()?null:found.get(0);
            }return null;
        });
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getColor")?java.awt.Color.WHITE:null));
        Global.setFactory(mock(FactoryAPI.class,(n,a)->n.equals("createMemory")?memory:null));
        Global.setSector(mock(SectorAPI.class,(n,a)-> {
            if(n.equals("getMemoryWithoutUpdate"))return memory;
            if(n.equals("getPlayerPerson"))return player;
            if(n.equals("getPlayerStats"))return stats;
            if(n.equals("getRules"))return rules;return extraSector.call(n,a);
        }));
        check(rows.isEmpty(),"Menelaus has no distinction or training rules");
        for (boolean complete : new boolean[]{false,true}) {
            saved.clear(); saved.put(MenelausTrial.FINAL_DEBRIEF_COMPLETE,complete);
            options.clear();
            check(!FireAll.fire(null,dialog,scope,"ChiefNavigatorFobIthacaCompleteOptions"),
                    "The retired section adds no menu options");
            // Native FireAll records its trigger in the fixture's shared local scope.
            Map<String,Object> before = new HashMap<>(saved);
            for(String id:List.of("chief_navigator_peerless_open","chief_navigator_peerless_receive","chief_navigator_peerless_select")) {
                choice=id;
                check(!FireBest.fire(null,dialog,scope,"DialogOptionSelected"),"Retired choices cannot reopen training");
            }
            for(String action:List.of("canReceive","hasTitle","hasPoint","canSelect","selected","receive","select")) {
                check(!new ChiefNavigatorPeerlessCMD().execute(null,dialog,Misc.tokenize(action),scope),
                        "Retired command is inert: "+action);
            }
            check(options.isEmpty() && saved.equals(before),"Removed menus cannot grant or spend points");
        }
        System.out.println("PASS: Menelaus distinction/training rules removed; stale routes and commands inert; saved state unchanged.");
    }
    public static void main(String[] args)throws Exception{new PeerlessDialogueRegression().run();}
}
