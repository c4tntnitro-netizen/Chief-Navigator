package chiefnavigator.skills;

import chiefnavigator.quest.SinniContactIntel;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.ui.*;
import java.util.*;
import peerless.api.PeerlessSkills;
import static chiefnavigator.skills.PeerlessDialogueRegression.*;

/** Training is no longer offered by Sinni, including stale callbacks on existing contacts. */
public final class SinniPeerlessRegression {
    public static void main(String[] args) {
        PeerlessDialogueRegression f=new PeerlessDialogueRegression();
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getColor")?java.awt.Color.WHITE:null));
        Global.setFactory(mock(FactoryAPI.class,(n,a)->n.equals("createMemory")?f.memory:null));
        Global.setSector(mock(SectorAPI.class,(n,a)->switch(n) {
            case "getMemoryWithoutUpdate" -> f.memory;
            case "getPlayerPerson" -> f.player;
            case "getPlayerStats" -> f.stats;
            case "getEconomy" -> mock(EconomyAPI.class,(nn,aa)->null);
            case "getScripts" -> List.of();
            default -> null;
        }));
        PersonAPI sinni=mock(PersonAPI.class,(n,a)->switch(n) {
            case "getNameString" -> "Sinni";
            case "getPortraitSprite" -> "graphics/portraits/chief_navigator_sinni_v5.png";
            case "getMemoryWithoutUpdate" -> f.memory;
            default -> null;
        });
        SinniContactIntel contact=new SinniContactIntel(sinni,mock(MarketAPI.class,(n,a)->null));
        Set<Object> buttons=new HashSet<>();int[] opened={0};
        TooltipMakerAPI[] info={null};
        info[0]=mock(TooltipMakerAPI.class,(n,a)-> {
            if(n.equals("beginImageWithText"))return info[0];
            if(n.equals("addButton")) {buttons.add(a[1]);return mock(ButtonAPI.class,(nn,aa)->null);}
            return null;
        });
        IntelUIAPI ui=mock(IntelUIAPI.class,(n,a)->{if(n.equals("showDialog"))opened[0]++;return null;});
        for(boolean withPoint:new boolean[]{false,true}) {
            f.saved.clear();if(withPoint)PeerlessSkills.awardOnce("$ship_trophy_room_peerless_isa_v1");
            Map<String,Object> before=new HashMap<>(f.saved);buttons.clear();
            contact.createSmallDescription(info[0],400f,800f);
            check(!buttons.contains("chief_navigator_sinni_contact_peerless"),"Sinni offers no training, with or without points");
            check(buttons.contains("chief_navigator_sinni_contact_directions"),"Navigation contact retained");
            contact.buttonPressConfirmed("chief_navigator_sinni_contact_peerless",ui);
            check(opened[0]==0 && f.saved.equals(before),"Retired click cannot reopen training or spend points");
        }
        System.out.println("PASS: Sinni training button removed, stale callbacks inert, navigation and Peerless balance preserved.");
    }
}
