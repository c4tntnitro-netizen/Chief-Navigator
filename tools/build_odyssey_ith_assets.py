"""Build the Ithaca Odyssey from its approved supplied sprite and stock hull geometry."""
from pathlib import Path
from hashlib import sha256
import argparse
import csv
import importlib.util
import json
import shutil
import numpy as np
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--art-only', action='store_true', help='Install the approved sprite without regenerating equipment or balance data.')
args = parser.parse_args()
ROOT = Path(__file__).resolve().parents[1]
CORE = ROOT.parent.parent / 'starsector-core'
SOURCE = ROOT / 'graphics/source/odyssey_domain_20261115'
HULL = 'chief_navigator_odyssey_ith'
LASER = HULL + '_naval_laser'
VARIANT = HULL + '_Standard'
SPRITE = ROOT / f'graphics/ships/ithaca/{HULL}.png'
MASTER = SOURCE / 'odyssey_domain_master.png'
CANVAS = SOURCE / 'odyssey_domain_supplied_canvas.png'
with Image.open(MASTER) as image:
    assert image.size == (168, 400) and image.mode == 'RGBA'
    with Image.open(CANVAS) as supplied:
        assert supplied.size == (288, 400) and supplied.mode == 'RGBA'
        assert supplied.getchannel('A').crop((0, 0, 60, 400)).getbbox() is None
        assert supplied.getchannel('A').crop((228, 0, 288, 400)).getbbox() is None
        assert image.tobytes() == supplied.crop((60, 0, 228, 400)).tobytes()
    preview = Image.new('RGBA', image.size, (18, 22, 26, 255))
    preview.alpha_composite(image)
    preview.save(SOURCE / 'preview.png')
    preview.resize((336, 800), Image.Resampling.NEAREST).save(SOURCE / 'preview_2x.png')
SPRITE.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(MASTER, SPRITE)
assert SPRITE.read_bytes() == MASTER.read_bytes()
art_metadata = {
    'source_method': 'approved user-supplied sprite; transparent horizontal padding cropped without resampling',
    'source_canvas': CANVAS.relative_to(ROOT).as_posix(),
    'source_canvas_sha256': sha256(CANVAS.read_bytes()).hexdigest(),
    'master': MASTER.relative_to(ROOT).as_posix(),
    'master_sha256': sha256(MASTER.read_bytes()).hexdigest(),
    'runtime_sprite_sha256': sha256(SPRITE.read_bytes()).hexdigest(),
    'source_dimensions': [288, 400], 'crop': [60, 0, 228, 400], 'dimensions': [168, 400],
    'source_RGBA_exact': True, 'resampling': False, 'uses_image_generation': False,
}
if args.art_only:
    provenance_path = SOURCE / 'provenance.json'
    provenance = json.loads(provenance_path.read_text('utf-8')) if provenance_path.exists() else {'hull': HULL}
    provenance.update(art_metadata)
    provenance_path.write_text(json.dumps(provenance, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(art_metadata, indent=2))
    raise SystemExit(0)

skin = {'baseHullId': 'odyssey', 'skinHullId': HULL, 'hullName': 'Odyssey (Ithaca)',
        'descriptionId': HULL, 'tech': 'Ithaca', 'incompatibleWithBaseHull': True,
        'spriteName': SPRITE.relative_to(ROOT).as_posix(),
        'tags': ['no_dealer', 'no_autofit', 'no_bp_drop', 'chief_navigator_ithaca'],
        'removeWeaponSlots': ['WS 006'], 'removeEngineSlots': [], 'removeBuiltInMods': [],
        'fighterBays': 3, 'builtInMods': ['surveying_equipment'],
        'builtInWeapons': {'WS 002': LASER, 'WS 003': LASER}}
(ROOT / f'data/hulls/skins/{HULL}.skin').write_text(json.dumps(skin, indent=2)+'\n', encoding='utf-8')
variant = json.loads((CORE / 'data/variants/odyssey_Balanced.variant').read_text('utf-8'))
variant.update(hullId=HULL, variantId=VARIANT, displayName='Ithaca')
for group in variant['weaponGroups']:
    for slot in ['WS 002', 'WS 003']:
        if slot in group['weapons']:
            group['weapons'][slot] = LASER
for group in variant['weaponGroups']:
    group['weapons'].pop('WS 006', None)
variant['weaponGroups'] = [group for group in variant['weaponGroups'] if group['weapons']]
(ROOT / f'data/variants/{VARIANT}.variant').write_text(json.dumps(variant, indent=2)+'\n', encoding='utf-8')
weapon = json.loads((ROOT / 'data/weapons/chief_navigator_onslaught_ith_naval_laser.wpn').read_text('utf-8'))
weapon.update(id=LASER, type='ENERGY', size='LARGE', turretSprite='graphics/weapons/tachyon_lance_turret_base.png',
              hardpointSprite='graphics/weapons/tachyon_lance_hardpoint_base.png',
              turretOffsets=[-3, 0], hardpointOffsets=[5, 0])
for mount in ['turret', 'hardpoint']:
    glow = np.array(Image.open(CORE / f'graphics/weapons/tachyon_lance_{mount}_glow.png').convert('RGBA'))
    glow[..., :3] = np.rint(glow[..., :3].max(axis=2)[..., None] / 255 * np.array([104, 210, 202])).astype(np.uint8)
    path = ROOT / f'graphics/weapons/{HULL}_laser_{mount}_glow.png'
    Image.fromarray(glow).save(path)
    weapon[mount+'GlowSprite'] = path.relative_to(ROOT).as_posix()
(ROOT / f'data/weapons/{LASER}.wpn').write_text(json.dumps(weapon, indent=2)+'\n', encoding='utf-8')

module_spec = importlib.util.spec_from_file_location('records', ROOT / 'tools/paginate_rules_dialogue.py')
records = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(records)
def upsert(path, key, values, index):
    raw, newline = records.split_raw_records(path.read_bytes().decode('utf-8'))
    output, found = [], False
    for record in raw:
        fields = records.parse_record(record) if record else []
        if len(fields) > index and fields[index] == key:
            output.append(records.serialize_record(values, newline)); found = True
        else:
            output.append(record)
    if not found:
        while output and output[-1] == '': output.pop()
        output += [records.serialize_record(values, newline), '']
    path.write_bytes(newline.join(output).encode('utf-8'))
csv_path = ROOT / 'data/weapons/weapon_data.csv'
with csv_path.open(newline='', encoding='utf-8') as handle:
    rows = list(csv.DictReader(handle))
stats = next(dict(row) for row in rows if row['id'] == 'chief_navigator_onslaught_ith_naval_laser')
stats.update(id=LASER, customAncillary='Built into the Odyssey (Ithaca). Receives installed Ithaca naval research upgrades.')
upsert(csv_path, LASER, [stats[key] for key in rows[0]], 1)
description = ('An Ithaca-pattern Odyssey refit. Two built-in naval lasers occupy the large energy mounts along the port broadside. '
               'The aft medium missile mount has been removed to accommodate a third fighter bay. '
               'Surveying Equipment is built in; its hull, shields, engines and other mounts retain the standard Odyssey layout. '
               'Installed Ithaca naval research and Isa\'s naval engineering package also improve its lasers.')
upsert(ROOT / 'data/strings/descriptions.csv', HULL, [HULL, 'SHIP', description, '', '', '', '', ''], 0)
manifest = {'hull': HULL, 'variant': VARIANT, 'laser': LASER, 'base_geometry': 'odyssey',
            'stock_sprite_sha256': sha256((CORE / 'graphics/ships/odyssey.png').read_bytes()).hexdigest(),
            'naval_slots': ['WS 002', 'WS 003'], 'naval_mount_size': 'LARGE', 'naval_slot_type': 'ENERGY',
            'removed_weapon_slots': ['WS 006'], 'fighter_bays': 3, 'added_builtin_mods': ['surveying_equipment'],
            'default_wings': variant['wings'], 'third_bay_empty': True,
            'stats_cloned_from': 'chief_navigator_onslaught_ith_naval_laser',
            'laser_stats': {key: stats[key] for key in ['range','damage/second','energy/second','burst delay','chargeup','chargedown','burst size']}}
manifest.update(art_metadata)
(SOURCE / 'provenance.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print(json.dumps(manifest, indent=2))
