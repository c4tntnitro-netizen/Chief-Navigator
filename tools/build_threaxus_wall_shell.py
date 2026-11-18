"""Place the exact supplied wall sprite around the three sides of the core."""
from pathlib import Path
import csv, hashlib, io, json, math, re, shutil
import cv2
import numpy as np
from PIL import Image, ImageDraw

ID='chief_navigator_threaxus_wall'
LAUNCHER_ID='chief_navigator_threaxus_swarm_launcher'
# Pixel centers selected from visible circular pads and paired grille banks.
# The supplied image faces outward toward its bottom, hence weapon angle180.
MOUNTS=[
    ('L1',(230,181),'LARGE','BALLISTIC','TURRET','neoferric_quadcoil',190),
    ('L2',(283,181),'LARGE','BALLISTIC','TURRET','neoferric_quadcoil',190),
    ('M1',(76,128),'MEDIUM','BALLISTIC','TURRET','heavy_mass_driver',210),
    ('M2',(437,128),'MEDIUM','BALLISTIC','TURRET','heavy_mass_driver',210),
    ('H1',(137,178),'MEDIUM','MISSILE','HARDPOINT',LAUNCHER_ID,5),
    ('H2',(376,178),'MEDIUM','MISSILE','HARDPOINT',LAUNCHER_ID,5),
    ('S1',(98,107),'SMALL','BALLISTIC','TURRET','light_mass_driver',210),
    ('S2',(415,107),'SMALL','BALLISTIC','TURRET','light_mass_driver',210),
    ('S3',(198,182),'SMALL','BALLISTIC','TURRET','light_mass_driver',200),
    ('S4',(315,182),'SMALL','BALLISTIC','TURRET','light_mass_driver',200),
    ('S5',(103,146),'SMALL','ENERGY','TURRET','voltaic_discharge',360),
    ('S6',(411,146),'SMALL','ENERGY','TURRET','voltaic_discharge',360),
    ('S7',(122,24),'SMALL','ENERGY','TURRET','voltaic_discharge',360),
    ('S8',(391,24),'SMALL','ENERGY','TURRET','voltaic_discharge',360),
]

def build(root,write_json,upsert_row):
    # A wall-only registration retains every native stat and sprite. Its
    # post-advance hook exposes waiting as no-fire instead of apparent damage.
    core=root.parent.parent/'starsector-core'
    native_text=re.sub(r'#.*','',(core/'data/weapons/swarm_launcher.wpn').read_text())
    definition=json.loads(re.sub(r',\s*([}\]])',r'\1',native_text))
    definition['id']=LAUNCHER_ID
    definition['everyFrameEffect']='chiefnavigator.weapons.ThreaxusSwarmLauncherEffect'
    write_json(root/f'data/weapons/{LAUNCHER_ID}.wpn',definition)
    with (core/'data/weapons/weapon_data.csv').open(encoding='utf-8-sig',newline='') as stream:
        native=next(row for row in csv.DictReader(stream) if row.get('id')=='swarm_launcher')
    native['id']=LAUNCHER_ID
    native['tags']+=', no_bp_drop, no_sell, no_dealer, no_drop, no_drop_salvage'
    weapon_csv=root/'data/weapons/weapon_data.csv'
    weapon_header=next(csv.reader(io.StringIO(weapon_csv.read_text(encoding='utf-8-sig'))))
    upsert_row(weapon_csv,LAUNCHER_ID,[native.get(key,'') for key in weapon_header])
    art=root/'graphics/source/threaxus_wall_shell_20261117'
    supplied=art/'supplied_wall.png'
    image=Image.open(supplied).convert('RGBA');pixels=np.array(image)
    w,h=image.size;assert (w,h)==(513,225)
    origin=np.array([w/2,h/2]);center=[origin[0],h-origin[1]]
    mask=(pixels[:,:,3]>=96).astype(np.uint8)
    count,labels,stats,_=cv2.connectedComponentsWithStats(mask,8)
    component=(labels==1+np.argmax(stats[1:,cv2.CC_STAT_AREA])).astype(np.uint8)
    contours,_=cv2.findContours(component,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    outline=cv2.approxPolyDP(max(contours,key=cv2.contourArea),1.5,True).reshape(-1,2)
    local=np.column_stack([origin[1]-outline[:,1],origin[0]-outline[:,0]])
    # Match the existing native core bounds winding.
    area=lambda p:np.sum(p[:,0]*np.roll(p[:,1],-1)-np.roll(p[:,0],-1)*p[:,1])/2
    core=json.loads((root/'data/hulls/chief_navigator_threatxus.ship').read_text()) if (root/'data/hulls/chief_navigator_threatxus.ship').exists() else None
    if core is not None and area(local)*area(np.array(core['bounds']).reshape(-1,2))<0:
        local=local[::-1];outline=outline[::-1]
    radius=math.ceil(np.linalg.norm(local,axis=1).max())+3
    sprite=root/'graphics/ships/starving_threat/threaxus_wall.png'
    sprite.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(supplied,sprite)
    slots=[];poses=[];groups={}
    for sid,(px,py),size,kind,mount,weapon,arc in MOUNTS:
        assert pixels[py,px,3]>=96,(sid,'Mount is outside solid art')
        loc=[float(origin[1]-py),float(origin[0]-px)]
        slots.append({'id':sid,'type':kind,'size':size,'mount':mount,'angle':180,'arc':arc,'locations':loc})
        groups.setdefault(weapon,{})[sid]=weapon
        poses.append({'slot':sid,'pixel':[px,py],'local':loc,'size':size,'type':kind,'mount':mount,'weapon':weapon,'angle':180,'arc':arc})
    hull={'hullId':ID,'hullName':'Threat Wall','hullSize':'FRIGATE','style':'THREAT',
        'spriteName':'graphics/ships/starving_threat/threaxus_wall.png','width':w,'height':h,
        'center':center,'moduleAnchor':[0,0],'collisionRadius':radius,'shieldCenter':[0,0],
        'shieldRadius':radius,'viewOffset':0,'coversColor':'','engineSlots':[],
        'builtInMods':['fragment_swarm','threat_hullmod','reduced_explosion','always_detaches','chief_navigator_threaxus_wall_swarm'],
        'bounds':local.reshape(-1).tolist(),'weaponSlots':slots}
    write_json(root/f'data/hulls/{ID}.ship',hull)
    write_json(root/f'data/variants/{ID}_Standard.variant',{'displayName':'Wall','variantId':ID+'_Standard',
        'hullId':ID,'fluxCapacitors':20,'fluxVents':30,'hullMods':[],
        'weaponGroups':[{'autofire':True,'mode':'LINKED','weapons':v} for v in groups.values()]})
    csvpath=root/'data/hulls/ship_data.csv';header=next(csv.reader(io.StringIO(csvpath.read_text(encoding='utf-8-sig'))))
    values={'name':'Threat Wall','id':ID,'designation':'Hive Wall Module','tech/manufacturer':'Threat',
        'fleet pts':0,'hitpoints':15000,'armor rating':1000,'max flux':14000,'8/6/5/4%':25000,
        'flux dissipation':1000,'ordnance points':300,'fighter bays':0,'max speed':0,'acceleration':0,
        'deceleration':0,'max turn rate':0,'turn acceleration':0,'mass':1200,'shield type':'NONE',
        'min crew':0,'max crew':0,'cargo':0,'fuel':0,'fuel/ly':1,'max burn':0,'base value':0,
        'CR to deploy':0,'peak CR sec':720,'CR loss/sec':.25,'supplies/rec':0,'supplies/mo':0,
        'hints':'MODULE, UNBOARDABLE, HIDE_IN_CODEX','tags':'threat, hide_in_codex, no_autofit, no_dealer',
        'rarity':0,'breakProb':.5,'minPieces':2,'maxPieces':4}
    assert set(values)<=set(header)
    upsert_row(csvpath,ID,[values.get(k,'') for k in header])
    hullmodpath=root/'data/hullmods/hull_mods.csv';head=next(csv.reader(io.StringIO(hullmodpath.read_text(encoding='utf-8-sig'))))
    mod={'name':'Wall Fragment Reserve','id':'chief_navigator_threaxus_wall_swarm','tier':3,
        'rarity':0,'tech/manufacturer':'Threat','tags':'no_build_in','hidden':'TRUE','hiddenEverywhere':'TRUE',
        'script':'chiefnavigator.hullmods.ThreaxusWallSwarm','base value':0,
        'cost_frigate':0,'cost_dest':0,'cost_cruiser':0,'cost_capital':0,
        'desc':'Maintains the native fragment ammunition reserve for this wall module.',
        'short':'Two hundred fragments; native frigate regrowth multiplied by ten.'}
    upsert_row(hullmodpath,mod['id'],[mod.get(k,'') for k in head])
    upsert_row(root/'data/strings/descriptions.csv',ID,[ID,'SHIP',
        'One of Tetsu Kannon\'s three breakaway walls. Native Threat weapons occupy its visible armor sockets, while fixed launch banks release attack swarms. Destroy all three walls to awaken the core.','','','','',''])
    # Core PNG vertices identify the three faces. Offset each wall80 units
    # outward; its bottom edge and180-degree weapon bearings face away.
    points=np.array([[277.,8.],[22.,401.],[534.,401.]])
    edges=[('WALL_LEFT',points[0],points[1]),('WALL_RIGHT',points[2],points[0]),('WALL_REAR',points[1],points[2])]
    centroid=points.mean(axis=0);parent_origin=np.array([277.,276.])
    attachments=[];fits=[];layout=[]
    for sid,a,b in edges:
        tangent=(b-a)/np.linalg.norm(b-a);normal=np.array([-tangent[1],tangent[0]])
        middle=(a+b)/2
        if np.dot(normal,middle-centroid)<0:normal=-normal
        pivot=middle+80*normal
        # Rotated image-bottom vector(-sin(cw),cos(cw)) equals outward normal.
        clockwise=math.degrees(math.atan2(-normal[0],normal[1]))
        angle=-clockwise
        loc=[round(float(parent_origin[1]-pivot[1]),3),round(float(parent_origin[0]-pivot[0]),3)]
        slot={'id':sid,'type':'STATION_MODULE','size':'LARGE','mount':'HARDPOINT','angle':round(angle,5),'arc':0,'locations':loc}
        attachments.append(slot);fits.append({sid:ID+'_Standard'})
        layout.append({'slot':slot,'pixel_pivot':pivot.tolist(),'outward_pixel_normal':normal.tolist(),
            'edge':[a.tolist(),b.tolist()],'edge_length':float(np.linalg.norm(b-a)),
            'sprite_clockwise_rotation':clockwise,'weapon_outward_local_angle':180})
    polygon=np.zeros_like(mask);cv2.fillPoly(polygon,[outline],1)
    coverage=float(np.count_nonzero(polygon&component))/float(np.count_nonzero(component))
    assert coverage>.99
    write_json(art/'wall_audit.json',{'source_sha256':hashlib.sha256(supplied.read_bytes()).hexdigest(),
        'runtime_sha256':hashlib.sha256(sprite.read_bytes()).hexdigest(),'source_pixels_unchanged':True,
        'size':[w,h],'center_bottom_left':center,'collision_radius':radius,'bounds_vertices':len(outline),
        'opaque_coverage':coverage,'mounts':poses,'attachments':layout,'module_hitpoints':15000,'module_armor':1000,
        'native_fragment_capacity':200,'native_fragment_regrowth_multiplier':10,
        'native_weapon_stats_and_sprites':True,'launcher_waiting_smoke_compatibility':True})
    overlay=image.copy();draw=ImageDraw.Draw(overlay)
    draw.line([tuple(p) for p in outline]+[tuple(outline[0])],fill='#5cff80',width=1)
    for p in poses:
        px,py=p['pixel'];color='#ff8a4a' if p['size']=='LARGE' else '#c792ff' if p['size']=='MEDIUM' else '#42ceff'
        draw.ellipse((px-6,py-6,px+6,py+6),outline=color,width=1)
        draw.text((px+7,py-7),p['slot'],fill=color)
    overlay.save(art/'weapon_mounts.png')
    return attachments,fits,layout

if __name__=='__main__':
    from build_threatxus_prototype import ROOT,write_json,upsert_row
    build(ROOT,write_json,upsert_row)
