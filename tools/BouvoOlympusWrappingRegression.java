package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Painted PNG alpha, native sprite callbacks, camera alignment and transient scope. */
public final class BouvoOlympusWrappingRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName();
                    default -> call.invoke(method.getName(), args);
                }));
    }
    static Object unexpected(String name) { throw new AssertionError("Unexpected API call: " + name); }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void close(float a, float b, String message) { check(Math.abs(a-b)<.01f, message); }

    static final class SpriteState {
        float x,y,width,height,angle,alpha; int draws;
        final SpriteAPI api;
        SpriteState(String layer, List<String> order) {
            api=mock(SpriteAPI.class,(n,a)->{
                switch(n) {
                    case "setSize": width=(float)a[0]; height=(float)a[1]; break;
                    case "setAngle": angle=(float)a[0]; break;
                    case "setAlphaMult": alpha=(float)a[0]; break;
                    case "setNormalBlend": break;
                    case "renderAtCenter": x=(float)a[0]; y=(float)a[1]; draws++; order.add(layer); break;
                    default: return unexpected(n); // Recoloring and non-image drawing are forbidden.
                }
                return null;
            });
        }
    }
    static final class Fixture {
        boolean owned=true,paused,elsewhere,dialog,menu,core,near=true,missing;
        float alpha=1,radius=450,zoom=2;
        int textureLoads;
        String starId=OdysseyExpanseSystem.LAST_LIGHT_ID+"_star";
        final List<Object> listeners=new ArrayList<>();
        final List<EveryFrameScript> scripts=new ArrayList<>();
        final List<String> order=new ArrayList<>();
        final SpriteState front=new SpriteState("front",order),rear=new SpriteState("rear",order);
        StarSystemAPI system;
        final Vector2f position=new Vector2f(2134,-723);
        final MemoryAPI memory=mock(MemoryAPI.class,(n,a)->n.equals("getBoolean")
                ? owned&&a[0].equals("$chief_navigator_odyssey_authored_system_v1"):unexpected(n));
        final PlanetAPI star=mock(PlanetAPI.class,(n,a)->switch(n){
            case "getId"->starId; case "getContainingLocation"->system;
            case "getRadius"->radius; case "getLocation"->position; default->unexpected(n);
        });
        final ViewportAPI viewport=mock(ViewportAPI.class,(n,a)->switch(n){
            case "getAlphaMult"->alpha;
            case "isNearViewport"->{check(a[0]==position,"Cull around live star");yield near;}
            case "convertWorldXtoScreenX"->((float)a[0]-100)/zoom;
            case "convertWorldYtoScreenY"->((float)a[0]+400)/zoom;
            case "convertWorldWidthToScreenWidth","convertWorldHeightToScreenHeight"->(float)a[0]/zoom;
            default->unexpected(n);
        });
        final CampaignUIAPI ui=mock(CampaignUIAPI.class,(n,a)->switch(n){
            case "isShowingDialog"->dialog; case "isShowingMenu"->menu;
            case "getCurrentCoreTab"->core?CoreUITabId.INTEL:null; default->unexpected(n);
        });
        final ListenerManagerAPI manager=mock(ListenerManagerAPI.class,(n,a)->{
            if(n.equals("removeListenerOfClass")){listeners.removeIf(v->v.getClass()==a[0]);return null;}
            if(n.equals("addListener")){check(a.length==2&&Boolean.TRUE.equals(a[1]),"Transient listener");listeners.add(a[0]);return null;}
            return unexpected(n);
        });
        Fixture(){
            system=mock(StarSystemAPI.class,(n,a)->switch(n){
                case "getOptionalUniqueId"->OdysseyExpanseSystem.LAST_LIGHT_ID;
                case "getMemoryWithoutUpdate"->memory;case "getStar"->missing?null:star;default->unexpected(n);
            });
            Global.setSector(mock(SectorAPI.class,(n,a)->{
                if(n.equals("getStarSystems"))return List.of(system);
                if(n.equals("getCurrentLocation"))return elsewhere?null:system;
                if(n.equals("getListenerManager"))return manager;
                if(n.equals("getCampaignUI"))return ui;
                if(n.equals("isPaused"))return paused;
                if(n.equals("removeTransientScriptsOfClass")){scripts.removeIf(v->v.getClass()==a[0]);return null;}
                if(n.equals("addTransientScript")){scripts.add((EveryFrameScript)a[0]);return null;}
                return unexpected(n);
            }));
            Global.setSettings(mock(SettingsAPI.class,(n,a)->{
                check(n.equals("getSprite")&&a.length==2&&a[0].equals(BouvoOlympusFilamentWrapping.SPRITE_CATEGORY),"Use registered painted assets only");
                textureLoads++;
                return a[1].equals("front")?front.api:a[1].equals("rear")?rear.api:unexpected(n);
            }));
        }
    }

    static BufferedImage asset(Path root,String layer)throws Exception{
        return ImageIO.read(root.resolve("graphics/campaign/ungaikyo/chief_navigator_bouvo_filaments_"+layer+"_v2.png").toFile());
    }
    static void assetChecks(Path root)throws Exception{
        JSONObject graphics=new JSONObject(Files.readString(root.resolve("data/config/settings.json")))
                .getJSONObject("graphics").getJSONObject(BouvoOlympusFilamentWrapping.SPRITE_CATEGORY);
        for(String layer:new String[]{"front","rear"}){
            BufferedImage image=asset(root,layer);int width=image.getWidth(),height=image.getHeight();
            check(width==height&&width>=1024,"High-resolution square alpha sprites");
            check(graphics.getString(layer).equals("graphics/campaign/ungaikyo/chief_navigator_bouvo_filaments_"+layer+"_v2.png"),"Exact sprite registration");
            int foreground=0,partial=0,center=0;
            double hole=width*.5*(1.04/BouvoOlympusFilamentWrapping.SHELL_RADIUS_MULT);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){
                int pixel=image.getRGB(x,y),alpha=pixel>>>24;
                double distance=Math.hypot(x+.5-width*.5,y+.5-height*.5);
                if(layer.equals("rear")&&distance<hole)check(alpha==0,"Rear disk opening is fully transparent");
                if(x<2||y<2||x>=width-2||y>=height-2)check(alpha==0,"No clipped image border");
                if(alpha==0)continue;
                foreground++;if(alpha<255)partial++;
                check(((pixel>>>16)&255)<=10&&((pixel>>>8)&255)<=10&&(pixel&255)<=10,"Only black pigment survives extraction");
                if(distance<width*.24)center++;
            }
            check(foreground>width*height*.005&&foreground<width*height*.10&&partial>1000,"Sparse textured strands with real antialiased transparency");
            if(layer.equals("front"))check(center>200,"Painted strands cross the star face");
        }
    }
    static void lifecycleAndRenderChecks(){
        SectorAPI oldSector=Global.getSector();SettingsAPI oldSettings=Global.getSettings();
        try{
            Fixture f=new Fixture();BouvoOlympusFilamentWrapping.install();BouvoOlympusFilamentWrapping.install();
            check(f.scripts.size()==1&&f.listeners.size()==1&&f.scripts.get(0)==f.listeners.get(0),"One shared transient instance per load");
            BouvoOlympusFilamentWrapping effect=(BouvoOlympusFilamentWrapping)f.scripts.get(0);
            effect.renderInUICoordsBelowUI(f.viewport);
            check(f.order.equals(List.of("rear","front")),"Paint rear then front");
            close(f.front.x,1017,"Star screen X");close(f.front.y,-161.5f,"Star screen Y");
            close(f.front.width,832.5f,"Correct shell scale");close(f.front.height,f.front.width,"Square sprite projection");
            close(f.front.alpha,.96f,"Front opacity");close(f.rear.alpha,.82f,"Rear opacity");
            f.zoom=4;f.radius=600;f.position.set(4334,200);f.alpha=.5f;effect.advance(20);
            effect.renderInUICoordsBelowUI(f.viewport);
            close(f.front.x,1058.5f,"Follow changed position/zoom X");close(f.front.y,150,"Follow changed position/zoom Y");
            close(f.front.width,555,"Follow real star size and zoom");close(f.front.alpha,.48f,"Viewport fade");
            check(f.textureLoads==2&&f.front.angle>0&&f.rear.angle<0,"Cache the two textures and gently counterrotate");
            float phase=effect.rotation;f.paused=true;effect.advance(20);close(effect.rotation,phase,"Pause freeze");f.paused=false;
            f.elsewhere=true;effect.advance(20);close(effect.rotation,phase,"Outside-system freeze");
            check(effect.visibleStar(f.viewport)==null,"Locality guard");f.elsewhere=false;
            f.owned=false;check(effect.visibleStar(f.viewport)==null,"Foreign system excluded");f.owned=true;
            f.starId="foreign";check(effect.visibleStar(f.viewport)==null,"Foreign star excluded");f.starId=OdysseyExpanseSystem.LAST_LIGHT_ID+"_star";
            f.missing=true;check(effect.visibleStar(f.viewport)==null,"Missing star never recreated");f.missing=false;
            f.near=false;check(effect.visibleStar(f.viewport)==null,"Offscreen culling");f.near=true;
            f.dialog=true;check(effect.visibleStar(f.viewport)==null,"Dialog guard");f.dialog=false;
            f.menu=true;check(effect.visibleStar(f.viewport)==null,"Menu guard");f.menu=false;
            f.core=true;check(effect.visibleStar(f.viewport)==null,"Core UI guard");f.core=false;
            f.alpha=0;effect.renderInUICoordsBelowUI(f.viewport);effect.renderInUICoordsAboveUIBelowTooltips(f.viewport);effect.renderInUICoordsAboveUIAndTooltips(f.viewport);
            check(f.front.draws==2&&f.rear.draws==2,"No drawing in hidden or upper UI callbacks");
            Global.setSector(null);effect.advance(1);BouvoOlympusFilamentWrapping.install();
            check(effect.visibleStar(f.viewport)==null&&!effect.isDone()&&!effect.runWhilePaused(),"Safe transient lifecycle");
        }finally{Global.setSector(oldSector);Global.setSettings(oldSettings);}
    }
    static void preview(Path root,String output)throws Exception{
        BufferedImage image=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(ImageIO.read(root.resolve("graphics/backgrounds/chief_navigator_bouvo_olympus_nebula_v1.jpg").toFile()),0,0,1024,1024,null);
            float x=512,y=512,star=165,shell=star*1.85f;
            g.setPaint(new RadialGradientPaint(x,y,star*1.3f,new float[]{0,.76f,1},new Color[]{Color.WHITE,new Color(235,243,255,180),new Color(235,243,255,0)}));
            g.fill(new Ellipse2D.Float(x-star*1.3f,y-star*1.3f,star*2.6f,star*2.6f));
            g.setColor(Color.WHITE);g.fill(new Ellipse2D.Float(x-star,y-star,star*2,star*2));
            for(String layer:new String[]{"rear","front"}){
                BufferedImage texture=asset(root,layer);g.setComposite(AlphaComposite.SrcOver.derive(layer.equals("front")?.96f:.82f));
                AffineTransform transform=new AffineTransform();transform.translate(x-shell,y-shell);transform.scale(shell*2/texture.getWidth(),shell*2/texture.getHeight());
                g.drawImage(texture,transform,null);
            }
        }finally{g.dispose();}
        ImageIO.write(image,"png",new File(output));
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);assetChecks(root);lifecycleAndRenderChecks();
        if(args.length>1)preview(root,args[1]);
        System.out.println("PASS: genuine sparse black PNG alpha, exact rear stellar opening, visible painted front strands, sprite registration, real draw callbacks and pan/zoom/fade/rotation, transient ownership/UI guards, and no world mutations or runtime tint.");
    }
}
