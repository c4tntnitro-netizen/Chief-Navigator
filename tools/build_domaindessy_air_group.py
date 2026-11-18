"""Build the fixed research air groups; preserve the approved carrier sprite."""
from pathlib import Path
from hashlib import sha256
import csv
import importlib.util
import json
import re
import cv2
import json5
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
CORE = next(parent / 'starsector-core' for parent in ROOT.parents
            if (parent / 'starsector-core/starfarer.api.jar').is_file()
            and (parent / 'jre/bin/java.exe').is_file())
SOURCE = ROOT / 'graphics/source/domaindessy_air_group_20261117'
SOURCE.mkdir(parents=True, exist_ok=True)
HULL = 'chief_navigator_odyssey_ith'
MOD = 'chief_navigator_domaindessy_air_group'
LIGHTNING = 'chief_navigator_lightning'
POSEIDON = 'chief_navigator_poseidon'
PD = LIGHTNING + '_cloudswarm'
RESTRICTED = 'no_bp, no_sell, no_dealer, no_drop, no_drop_salvage'

spec = importlib.util.spec_from_file_location('records', ROOT / 'tools/paginate_rules_dialogue.py')
records = importlib.util.module_from_spec(spec)
spec.loader.exec_module(records)

def read(path):
    # Core's authoring format permits hash comments and trailing commas.
    text = re.sub(r'#[^\n\r]*', '', path.read_text('utf-8'))
    return json5.loads(text)

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2)+'\n', encoding='utf-8')

def table(path):
    with path.open(newline='', encoding='utf-8-sig') as stream:
        reader = csv.DictReader(stream)
        return reader.fieldnames, list(reader)

def upsert(path, key, row, index=0):
    raw, newline = records.split_raw_records(path.read_bytes().decode('utf-8'))
    found = []
    for i, record in enumerate(raw):
        fields = records.parse_record(record) if record else []
        if len(fields) > index and fields[index] == key:
            found.append(i)
    assert len(found) <= 1, key
    value = records.serialize_record(row, newline)
    if found:
        raw[found[0]] = value
    else:
        while raw and raw[-1] == '': raw.pop()
        raw.extend([value, ''])
    path.write_bytes(newline.join(raw).encode('utf-8'))

def description(key, kind, text):
    upsert(ROOT / 'data/strings/descriptions.csv', key, [key, kind, text, '', '', '', '', ''])

configs = [('', 'broadsword_wing', 'hoplon_wing'),
           ('_interception', LIGHTNING+'_wing', 'hoplon_wing'),
           ('_siege', 'broadsword_wing', 'trident_wing'),
           ('_interception_siege', LIGHTNING+'_wing', 'trident_wing'),
           ('_poseidon', 'broadsword_wing', POSEIDON+'_wing'),
           ('_interception_poseidon', LIGHTNING+'_wing', POSEIDON+'_wing')]
for suffix, fighter, bomber in configs:
    skin = {'baseHullId': 'odyssey', 'skinHullId': HULL+suffix, 'hullName': 'Odyssey (Ithaca)',
            'descriptionId': HULL, 'tech': 'Ithaca', 'incompatibleWithBaseHull': True,
            'spriteName': f'graphics/ships/ithaca/{HULL}.png',
            # Every research profile has the same prototype acquisition restrictions.
            'tags': ['restricted', 'no_sell', 'no_dealer', 'no_autofit', 'no_bp_drop', 'chief_navigator_ithaca'],
            'removeWeaponSlots': ['WS 006'], 'removeEngineSlots': [], 'removeBuiltInMods': [],
            'fighterBays': 4, 'builtInMods': ['surveying_equipment', MOD],
            'builtInWings': [fighter, fighter, bomber, bomber], 'builtInWeapons': {}}
    write(ROOT / f'data/hulls/skins/{HULL+suffix}.skin', skin)

variant = read(CORE / 'data/variants/odyssey_Balanced.variant')
variant.update(hullId=HULL, variantId=HULL+'_Standard', displayName='Ithaca',
               wings=['broadsword_wing']*2+['hoplon_wing']*2)
for group in variant['weaponGroups']: group['weapons'].pop('WS 006', None)
variant['weaponGroups'] = [g for g in variant['weaponGroups'] if g['weapons']]
write(ROOT / f'data/variants/{HULL}_Standard.variant', variant)

# Baked, masked palette transfer; no runtime tint or change to silhouettes.
art = {}
for base, custom in [('thunder', LIGHTNING), ('trident', POSEIDON)]:
    hull = read(CORE / f'data/hulls/{base}.ship')
    stock = np.array(Image.open(CORE / hull['spriteName']).convert('RGBA'))
    rgb = stock[..., :3].astype(np.float32)
    lab = cv2.cvtColor(rgb/255, cv2.COLOR_RGB2LAB)
    L, a, b = lab.transpose(2, 0, 1)
    mask = ((stock[..., 3] > 0) & (L > 25) & (L < 88)
            & (np.hypot(a, b) > 4))
    # Retain saturated yellow lamps, bright cockpit glass and dark machinery.
    mask &= ~((rgb[..., 0] > 180) & (rgb[..., 1] > 145) & (rgb[..., 2] < 100))
    changed = lab.copy()
    changed[..., 1:] = np.array([-7.29, 1.70], np.float32)
    converted = np.rint(np.clip(cv2.cvtColor(changed, cv2.COLOR_LAB2RGB), 0, 1)*255).astype(np.uint8)
    result = stock.copy(); result[mask, :3] = converted[mask]
    assert np.array_equal(result[..., 3], stock[..., 3])
    assert np.array_equal(result[~mask], stock[~mask])
    path = ROOT / f'graphics/ships/ithaca/{custom}.png'
    Image.fromarray(result).save(path)
    Image.fromarray(stock).save(SOURCE / f'{base}_master.png')
    Image.fromarray(mask.astype(np.uint8)*255).save(SOURCE / f'{custom}_paint_mask.png')
    art[custom] = {'reference': hull['spriteName'], 'source_sha256': sha256((CORE/hull['spriteName']).read_bytes()).hexdigest(),
                   'sprite_sha256': sha256(path.read_bytes()).hexdigest(), 'alpha_exact': True, 'outside_mask_exact': True}
    write(ROOT / f'data/hulls/skins/{custom}.skin',
          {'baseHullId': base, 'skinHullId': custom,
           'hullName': 'Lightning Superiority Fighter' if base=='thunder' else 'Poseidon',
           'descriptionId': custom, 'tech': 'Ithaca', 'incompatibleWithBaseHull': True,
           'spriteName': path.relative_to(ROOT).as_posix(),
           'tags': ['no_dealer', 'no_autofit', 'no_bp_drop']})

lightning = read(CORE / 'data/variants/fighters/thunder_Fighter.variant')
lightning.update(hullId=LIGHTNING, variantId=LIGHTNING+'_Fighter', displayName='Superiority Fighter')
for group in lightning['weaponGroups']:
    for slot in group['weapons']:
        group['weapons'][slot] = {'WS 001':'ioncannon_fighter', 'WS 002':'lightdualac', 'WS 003':PD}[slot]
write(ROOT / f'data/variants/{LIGHTNING}_Fighter.variant', lightning)
poseidon = read(CORE / 'data/variants/fighters/trident_Bomber.variant')
poseidon.update(hullId=POSEIDON, variantId=POSEIDON+'_Bomber', displayName='Torpedo Bomber')
for group in poseidon['weaponGroups']:
    for slot in group['weapons']:
        if group['weapons'][slot]=='atropos_single': group['weapons'][slot]='atropos'
poseidon['weaponGroups'] = [g for g in poseidon['weaponGroups'] if g['weapons']]
write(ROOT / f'data/variants/{POSEIDON}_Bomber.variant', poseidon)

wing_fields, core_wings = table(CORE / 'data/hulls/wing_data.csv')
wing_map = {r['id']:r for r in core_wings}
for custom, base, suffix, role in [(LIGHTNING,'thunder_wing','Fighter','Superiority Fighter'),
                                  (POSEIDON,'trident_wing','Bomber','Heavy Torpedo Bomber')]:
    row = dict(wing_map[base], id=custom+'_wing', variant=custom+'_'+suffix,
               tags=('fighter, ' if base=='thunder_wing' else 'bomber, ')+RESTRICTED,
               **{'role desc':role, 'rarity':'0', 'base value':'0', 'number':''})
    fields, _ = table(ROOT / 'data/hulls/wing_data.csv')
    upsert(ROOT / 'data/hulls/wing_data.csv', row['id'], [row.get(k,'') for k in fields])

pd = read(CORE / 'data/weapons/swarmer_fighter.wpn')
pd.update(id=PD, projectileSpecId='locust', interruptibleBurst=True,
          everyFrameEffect='chiefnavigator.weapons.LightningCloudswarmEffect',
          onFireEffect='chiefnavigator.weapons.LightningCloudswarmEffect',
          hardpointAngleOffsets=[-2,1,2,-1], turretAngleOffsets=[-2,1,2,-1])
write(ROOT / f'data/weapons/{PD}.wpn', pd)
fields, weapon_rows = table(ROOT / 'data/weapons/weapon_data.csv')
row = dict(next(r for r in weapon_rows if r['id']=='chief_navigator_ithaca_locust_pd_array'))
row.update(id=PD, name='Miniature Cloudswarm PD Launcher', range='800', ammo='8',
           **{'ammo/sec':'0.8', 'reload size':'8', 'chargedown':'6', 'burst size':'8',
              'flight time':'2', 'hints':'PD, ANTI_FTR, DO_NOT_AIM, DO_NOT_CONSERVE, SYSTEM',
              'tags':'restricted, no_sell, no_dealer, no_drop, no_drop_salvage, no_bp_drop',
              'groupTag':'', 'turnRateStr':'', 'customAncillary':'Eight-round fighter-scale PD burst.', 'customAncillaryHL':''})
upsert(ROOT / 'data/weapons/weapon_data.csv', PD, [row.get(k,'') for k in fields], 1)

path = ROOT / 'data/hullmods/hull_mods.csv'; fields, _ = table(path)
row = dict.fromkeys(fields, '')
row.update(name='Domain Air Group', id=MOD, tier='0', rarity='0', **{'tech/manufacturer':'Ithaca',
           'tags':'no_build_in', 'base value':'0', 'hidden':'FALSE', 'hiddenEverywhere':'FALSE',
           'cost_frigate':'0', 'cost_dest':'0', 'cost_cruiser':'0', 'cost_capital':'0',
           'script':'chiefnavigator.hullmods.DomaindessyAirGroup',
           'desc':'Four fixed wings: two Broadswords and two Khopeshes. Interception Command Matrix replaces the Broadswords with Lightning Superiority Fighters. Siege Munitions Compiler replaces the Khopeshes with Tridents. Fortress Autoloader Core upgrades those Tridents into Poseidons, each carrying four Atropos torpedoes.',
           'short':'Research-upgraded fixed air group.', 'sprite':'graphics/hullmods/converted_hangar.png'})
upsert(path, MOD, [row[k] for k in fields], 1)
description(HULL, 'SHIP', 'An Ithaca-pattern Odyssey with four fixed fighter bays and built-in Surveying Equipment. Two Broadsword wings and two Khopesh wings form its air group. Interception Command Matrix supplies Lightning Superiority Fighters; Siege Munitions Compiler supplies Tridents; Fortress Autoloader Core upgrades the Tridents to Poseidons with double Atropos racks. The aft medium missile mount is removed. Its large energy mounts remain available for conventional weapons.')
description(LIGHTNING, 'SHIP', 'An Ithaca-refitted Thunder fielding a fighter-scale Cloudswarm PD launcher, a Light Dual Autocannon and the high-delay fighter Ion Cannon. Its guided Cloudswarm missiles intercept hostile missiles, fighters and combat drones.')
description(POSEIDON, 'SHIP', 'An Ithaca-refitted Trident carrying two double Atropos torpedo racks, for four torpedoes per bomber. It retains ordinary Trident flight, shielding and bomber behavior.')
description(PD, 'WEAPON', 'An eight-round, replenishing fighter-scale Cloudswarm launcher. Its guidance prioritizes hostile missiles, then fighters and combat drones; it cannot engage ordinary ships.')
path = ROOT / 'data/campaign/special_items.csv'
fields, items = table(path)
additions = {
    'chief_navigator_ithaca_intercept_matrix': ' On the Odyssey (Ithaca), replaces its two Broadsword wings with Lightning Superiority Fighters.',
    'chief_navigator_ithaca_munitions_compiler': ' On the Odyssey (Ithaca), replaces its two Khopesh wings with Trident bombers.',
    'chief_navigator_ithaca_autoloader_core': ' On the Odyssey (Ithaca), upgrades its Tridents into Poseidons with two double Atropos racks each; requires the Siege Munitions Compiler.',
}
for row in items:
    if row['id'] in additions:
        # Replace our existing suffix on repeat builds, preserving the Wall's text.
        key = next(k for k in fields if k.lower() in ('description', 'desc'))
        text = row[key].split(' On the Odyssey (Ithaca),')[0]
        row[key] = text + additions[row['id']]
        upsert(path, row['id'], [row.get(k,'') for k in fields], 1)
write(SOURCE / 'provenance.json', {'configs':configs, 'art':art, 'recall':False,
      'lightning_wing_size':2, 'poseidon_wing_size':2, 'poseidon_torpedoes_per_bomber':4})
print('Built six fixed air-group states, Lightning/Poseidon wings and miniature Cloudswarm.')
