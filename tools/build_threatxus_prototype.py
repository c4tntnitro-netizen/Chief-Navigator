"""Build a single mobile test hull from supplied art and Nexus core anchors."""
from pathlib import Path
import csv, hashlib, io, json, math, re, shutil
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
CORE = ROOT.parent.parent / 'starsector-core'
ART = ROOT / 'graphics/source/threatxus_20261116'
ID = 'chief_navigator_threatxus'
VARIANT = ID + '_Standard'
DISPLAY_NAME = 'Tetsu Kannon'

def loose_json(path):
    text=re.sub(r'#.*','',path.read_text(encoding='utf-8-sig'))
    return json.loads(re.sub(r',\s*([}\]])',r'\1',text))

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def upsert_row(path, key, fields):
    data = path.read_bytes() if path.exists() else b''
    lines = data.splitlines(keepends=True)
    newline = '\r\n' if b'\r\n' in data else '\n'
    buffer = io.StringIO(newline='')
    csv.writer(buffer, lineterminator=newline).writerow(fields)
    row = buffer.getvalue().encode('utf-8')
    matches = [i for i,line in enumerate(lines)
               if key in next(csv.reader([line.decode('utf-8-sig')]), [])]
    assert len(matches) <= 1
    if matches:
        lines[matches[0]] = row
        path.write_bytes(b''.join(lines))
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data + (newline.encode() if data and not data.endswith(b'\n') else b'') + row)

def main():
    supplied = ART / 'threatxus_supplied.png'
    image = Image.open(supplied).convert('RGBA')
    pixels = np.array(image)
    width, height = image.size
    assert (width,height) == (555,502)
    # Native Nexus core uses station1 artwork and this exact bottom-left center.
    nexus = json.loads((CORE / 'data/hulls/remnant_station2.ship').read_text())
    native_origin = np.array([nexus['center'][0], nexus['height']-nexus['center'][1]])
    origin = np.array([277.,276.])
    center = [int(origin[0]), int(height-origin[1])]
    mask = (pixels[:,:,3] >= 96).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    component = (labels == 1+np.argmax(stats[1:,cv2.CC_STAT_AREA])).astype(np.uint8)
    contours, _ = cv2.findContours(component, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    outline = cv2.approxPolyDP(max(contours,key=cv2.contourArea), 2.5, True).reshape(-1,2)
    local = np.column_stack([origin[1]-outline[:,1],origin[0]-outline[:,0]]).astype(int)
    def signed_area(points):
        return np.sum(points[:,0]*np.roll(points[:,1],-1)-np.roll(points[:,0],-1)*points[:,1])/2
    native_bounds = np.array(nexus['bounds']).reshape(-1,2)
    if signed_area(local)*signed_area(native_bounds)<0:
        local=local[::-1];outline=outline[::-1]
    radius = math.ceil(np.linalg.norm(local,axis=1).max())+5
    # Keep Nexus anchor IDs/angles, mapping attachment roles onto painted pads.
    # No STATION_MODULE slot, child hull or stationary hullmod survives.
    pads = {
        'WS 001': ((277,20),'SMALL','pdburst'),
        'WS 007': ((477,378),'SMALL','pdburst'),
        'WS 016': ((77,378),'SMALL','pdburst'),
        'WS 032': ((277,381),'MEDIUM','ionpulser'),
        'WS 033': ((183,199),'MEDIUM','heavyblaster'),
        'WS 034': ((377,199),'MEDIUM','heavyblaster'),
        'WS 035': ((105,270),'LARGE','tachyonlance'),
        'WS 036': ((208,99),'LARGE','plasma'),
        'WS 037': ((345,99),'LARGE','plasma'),
        'WS 038': ((453,270),'LARGE','tachyonlance'),
        'WS 039': ((159,414),'SMALL','pdburst'),
        'WS 040': ((398,414),'SMALL','pdburst'),
    }
    slots=[];placements=[];groups={'plasma':{},'tachyonlance':{},'medium':{},'pd':{}}
    for anchor in nexus['weaponSlots']:
        sid=anchor['id']
        if sid not in pads:continue
        (px,py),size,weapon=pads[sid]
        assert pixels[py,px,3]>=96, f'Mount {sid} off opaque art'
        x,y=anchor['locations']
        reference= [float(native_origin[0]-y),float(native_origin[1]-x)]
        projected=np.array(reference)+origin-native_origin
        slots.append({'id':sid,'type':'ENERGY','size':size,'mount':'TURRET',
                      'angle':anchor['angle'],'arc':220 if size=='SMALL' else 200,
                      'locations':[int(origin[1]-py),int(origin[0]-px)]})
        placements.append({'slot':sid,'nexus_anchor':anchor,'projected_pixel':projected.tolist(),
                           'painted_mount_pixel':[px,py],'weapon':weapon,'size':size})
        group=weapon if weapon in ['plasma','tachyonlance'] else 'pd' if size=='SMALL' else 'medium'
        groups[group][sid]=weapon
    engines=[]  # Quiet passive forward drift: no engine flares or contrails.
    sprite=ROOT/'graphics/ships/starving_threat/threaxus.png'
    sprite.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(supplied,sprite)
    from build_threaxus_wall_shell import build as build_wall_shell
    module_slots,module_fits,module_audit=build_wall_shell(ROOT,write_json,upsert_row)
    hull={'hullId':ID,'hullName':DISPLAY_NAME,'hullSize':'CAPITAL_SHIP','style':'HIGH_TECH',
          'spriteName':'graphics/ships/starving_threat/threaxus.png',
          'width':width,'height':height,'center':center,'collisionRadius':radius,
          'shieldCenter':[0,0],'shieldRadius':radius+12,'viewOffset':0,'coversColor':'',
          'builtInMods':['chief_navigator_threatxus_swarm_cloud','chief_navigator_threaxus_awakening'],
          'bounds':local.reshape(-1).tolist(),'weaponSlots':slots+module_slots,'engineSlots':engines}
    write_json(ROOT/f'data/hulls/{ID}.ship',hull)
    variant={'displayName':'Awakening','variantId':VARIANT,'hullId':ID,'goalVariant':True,
             'fluxCapacitors':40,'fluxVents':50,'hullMods':['targetingunit','hardenedshieldemitter'],
             'weaponGroups':[{'autofire':key in ['medium','pd'],'mode':'LINKED','weapons':value}
                             for key,value in groups.items()],'modules':module_fits}
    write_json(ROOT/f'data/variants/{VARIANT}.variant',variant)
    csvpath=ROOT/'data/hulls/ship_data.csv'
    header=next(csv.reader(io.StringIO(csvpath.read_text(encoding='utf-8-sig'))))
    values={'name':DISPLAY_NAME,'id':ID,'designation':'Threat Hive Ship','tech/manufacturer':'Threat',
        'system id':'chief_navigator_threatxus_fabrication','fleet pts':100,'hitpoints':40000,'armor rating':1600,
        'max flux':30000,'8/6/5/4%':25000,'flux dissipation':1200,'ordnance points':400,'fighter bays':0,
        'max speed':10,'acceleration':2,'deceleration':2,'max turn rate':6,'turn acceleration':10,
        'mass':6000,'shield type':'OMNI','shield arc':360,'shield upkeep':.8,'shield efficiency':.9,
        'min crew':0,'max crew':0,'cargo':200,'fuel':400,'fuel/ly':10,'range':40,'max burn':7,
        'base value':750000,'CR to deploy':10,'peak CR sec':720,'CR loss/sec':.25,
        'supplies/rec':100,'supplies/mo':100,'hints':'SHIP_WITH_MODULES, UNBOARDABLE, HIDE_IN_CODEX',
        'tags':'hide_in_codex, no_autofit, no_dealer','rarity':0,'breakProb':.5,'minPieces':3,'maxPieces':4}
    assert set(values)<=set(header)
    upsert_row(csvpath,ID,[values.get(key,'') for key in header])
    hullmodpath=ROOT/'data/hullmods/hull_mods.csv'
    hullmodheader=next(csv.reader(io.StringIO(hullmodpath.read_text(encoding='utf-8-sig'))))
    hullmod={'name':'Threatxus Swarm Cloud','id':'chief_navigator_threatxus_swarm_cloud','tier':3,
        'rarity':0,'tech/manufacturer':'Threat','tags':'no_build_in','base value':0,
        'hidden':'TRUE','hiddenEverywhere':'TRUE','cost_frigate':0,'cost_dest':0,'cost_cruiser':0,'cost_capital':0,
        'script':'chiefnavigator.hullmods.ThreatxusSwarmCloud',
        'desc':'A dense native fragment cloud surrounds this hull as it drifts quietly forward.',
        'short':'Dense visual fragment cloud and passive drift.'}
    upsert_row(hullmodpath,hullmod['id'],[hullmod.get(key,'') for key in hullmodheader])
    awakening={'name':'Awakening Core','id':'chief_navigator_threaxus_awakening','tier':3,
        'rarity':0,'tech/manufacturer':'Threat','tags':'no_build_in','base value':0,
        'hidden':'TRUE','hiddenEverywhere':'TRUE','cost_frigate':0,'cost_dest':0,'cost_cruiser':0,'cost_capital':0,
        'script':'chiefnavigator.hullmods.ThreaxusAwakening',
        'desc':'The protected core remains inert until every attached module is destroyed. It then awakens and pursues hostile ships.',
        'short':'Destroy every attached module to expose the living core.'}
    upsert_row(hullmodpath,awakening['id'],[awakening.get(key,'') for key in hullmodheader])
    # Independent native Assault Unit geometry; no ordinary weapons or flares.
    assault_id='chief_navigator_exodus_assault'
    assault=json.loads((CORE/'data/hulls/assault_unit.ship').read_text())
    assault.update(hullId=assault_id,hullName='Exodus Assault Unit',builtInMods=[hullmod['id']],
                   weaponSlots=[],engineSlots=[])
    write_json(ROOT/f'data/hulls/{assault_id}.ship',assault)
    with (CORE/'data/hulls/ship_data.csv').open(encoding='utf-8-sig') as f:
        assault_stats=next(row for row in csv.DictReader(f) if row['id']=='assault_unit')
    assault_stats.update({'name':'Exodus Assault Unit','id':assault_id,'designation':'Exodus Assault Unit',
        'system id':'chief_navigator_exodus_plague','ordnance points':0,'fighter bays':0,
        'hints':'UNBOARDABLE, HIDE_IN_CODEX','tags':'hide_in_codex, no_autofit, no_dealer','rarity':0})
    upsert_row(csvpath,assault_id,[assault_stats.get(key,'') for key in header])
    write_json(ROOT/f'data/variants/{assault_id}_Standard.variant',{'displayName':'Exodus',
        'variantId':assault_id+'_Standard','hullId':assault_id,'fluxCapacitors':0,'fluxVents':0,
        'hullMods':[],'weaponGroups':[]})
    system_id='chief_navigator_threatxus_fabrication'
    write_json(ROOT/f'data/shipsystems/{system_id}.system',{'id':system_id,'type':'STAT_MOD',
        'aiType':'NONE','runScriptWhileIdle':True,'statsScript':'chiefnavigator.systems.ThreatxusFabricationSystem',
        'useSound':'system_construction_swarm','outOfUsesSound':'gun_out_of_ammo'})
    systempath=ROOT/'data/shipsystems/ship_systems.csv'
    systemheader=next(csv.reader(io.StringIO(systempath.read_text(encoding='utf-8-sig'))))
    system={'name':'Fabricate Threat Units','id':system_id,'flux/second':0,'flux/use':0,
        'charge up':2,'active':1,'down':0,'cooldown':25,'tags':'offensive',
        'icon':'graphics/icons/hullsys/construction_swarm.png'}
    upsert_row(systempath,system_id,[system.get(key,'') for key in systemheader])
    upsert_row(ROOT/'data/strings/descriptions.csv',ID,[ID,'SHIP',
        f'{DISPLAY_NAME} encloses a consumed Remnant Nexus core in three armed Threat walls. Destroy every wall and the exposed hull awakens, pursuing hostile ships while fabricating Exodus Assault Units and Fabricators.',
        '', '', '', '', 'Local prototype; supplied artwork.'])
    upsert_row(ROOT/'data/strings/descriptions.csv',assault_id,[assault_id,'SHIP',
        f'A specialized Assault Unit fabricated by {DISPLAY_NAME}. Extra fragments cover its hull, and its sole attack is the twin Exodus Plague stream.','', '', '', '', 'Fabricated combat unit.'])
    upsert_row(ROOT/'data/strings/descriptions.csv',system_id,[system_id,'SHIP_SYSTEM',
        'After the core awakens, automatically assembles one unit when ready. If no Exodus Assault Unit is active or assembling, one takes priority; otherwise it builds stock Fabricators up to three, then fills remaining Assault Unit slots. Each unit takes twenty seconds to form. Assault Units attack only with Exodus Plague.',
        'Fabrication','Two-second windup; one-second activation; twenty-five-second cooldown.',
        '', 'Three Assault Units and three Fabricators maximum; building units count toward their cap.',''])
    mission=ROOT/'data/missions/chief_navigator_threatxus_test'
    mission.mkdir(parents=True,exist_ok=True)
    shutil.copy2(supplied,mission/'icon.png')
    write_json(mission/'descriptor.json',{'title':DISPLAY_NAME+' Test','difficulty':'MEDIUM',
        'icon':'icon.png','background':'graphics/backgrounds/background5.jpg'})
    (mission/'mission_text.txt').write_text(
        f'Command an Onslaught, an Eagle and two Hammerheads against {DISPLAY_NAME}.\n\n'
        'First destroy the three Threat wall modules, one along each side of the central triangle. Their visible mounts carry large, medium and small Threat guns, with Swarm Launchers in the fixed launch banks. The central hull remains protected and inert. When the final wall breaks away, the core wakes and hunts you at70 base speed, without engine flares.\n\n'
        'The awakened core retains its wide swarm cloud with a concentrated center. It fabricates specialized Assault Units and stock Fabricators. Destroy the core to halt its production, then finish its surviving units.\n\n'
        'If no Assault Unit is active or assembling, the next build is an Assault Unit. Otherwise it builds up to three Fabricators, then fills its remaining Assault Unit slots. Both types take twenty seconds to form and have separate caps of three, including unfinished builds. Assault Units attack only with Exodus Plague; point defense can intercept the stream projectiles. Fabricators retain their ordinary weapons and Construction Swarm.\n',encoding='utf-8')
    missionlist=ROOT/'data/missions/mission_list.csv'
    if not missionlist.exists():missionlist.write_text('mission,\n',encoding='utf-8')
    upsert_row(missionlist,'chief_navigator_threatxus_test',['chief_navigator_threatxus_test',''])
    overlay=image.copy();draw=ImageDraw.Draw(overlay)
    draw.line([tuple(map(int,p)) for p in outline]+[tuple(map(int,outline[0]))],fill=(70,255,110,255),width=1)
    for p in placements:
        px,py=p['painted_mount_pixel'];color={'LARGE':'#ffb65c','MEDIUM':'#e179ff','SMALL':'#5dc9ff'}[p['size']]
        draw.ellipse((px-5,py-5,px+5,py+5),outline=color,width=1)
        draw.text((px+6,py-5),p['slot'][3:],fill=color)
    for engine in engines:
        x,y=engine['location'];px,py=origin[0]-y,origin[1]-x
        draw.line([(px-4,py),(px+4,py)],fill='#ff6969',width=2)
    draw.ellipse((origin[0]-3,origin[1]-3,origin[0]+3,origin[1]+3),fill='#ffffff')
    overlay.save(ART/'collision_and_mounts.png')
    polygonmask=np.zeros_like(mask);cv2.fillPoly(polygonmask,[outline],1)
    area=int(np.count_nonzero(component));coverage=float(np.count_nonzero(polygonmask&component))/area
    assert coverage>.99
    assert np.array_equal(np.array(Image.open(sprite)),pixels)
    write_json(ART/'geometry_audit.json',{'source_sha256':hashlib.sha256(supplied.read_bytes()).hexdigest(),
        'runtime_sha256':hashlib.sha256(sprite.read_bytes()).hexdigest(),'size':image.size,'pixels_unchanged':True,
        'segmentation_alpha_threshold':96,'connected_components':count-1,'main_component_pixels':area,
        'bounds_vertices':len(outline),'contour_tolerance':2.5,'opaque_component_coverage':coverage,
        'collision_area_pixels':int(np.count_nonzero(polygonmask)),
        'collision_holes':'Native single ship outline fills enclosed holes; exterior concavities remain open.',
        'center_bottom_left':center,'center_png':origin.tolist(),'collision_radius':radius,
        'nexus_source':'data/hulls/remnant_station2.ship','nexus_native_center':nexus['center'],
        'weapon_placements':placements,'engines':engines,'single_mobile_hull':False,'mobile_core_with_modules':True,
        'display_name':DISPLAY_NAME,'attached_modules':module_audit,'dormant_until_all_modules_destroyed':True,
        'awakened_speed':70,'awakened_acceleration':20,'awakened_deceleration':15,
        'awakened_turn_rate':15,'awakened_turn_acceleration':25,
        'system':'chief_navigator_threatxus_fabrication','swarm_density_multiplier':10,
        'swarm_core_extra_members':500,'swarm_core_radius_multiplier':.5,'swarm_central_density_multiplier':3,
        'max_exodus_assault_units':3,'max_fabricators':3,'fabricator_variant':'fabricator_unit_Type450',
        'base_speed':10,'passive_forward_drift':True,'provisional_balance':True})
    print(f'Built Threatxus: {width}x{height}, {len(outline)} collision vertices, {coverage:.2%} silhouette coverage, {len(slots)} mounts, zero engines, assault/Fabricator production and1500/400-fragment clouds.')

if __name__=='__main__':main()
