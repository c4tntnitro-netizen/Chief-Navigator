"""Build the Onslaught (ITH) refit from the supplied sprite and stock geometry."""
from pathlib import Path
from hashlib import sha256
import csv
import importlib.util
import io
import json
import re
import shutil
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
CORE = ROOT.parent.parent / 'starsector-core'
SOURCE = ROOT / 'graphics/source/onslaught_ith_20261112'
SOURCE.mkdir(parents=True, exist_ok=True)
SUPPLIED = Path(r'C:\Users\myauc\AppData\Local\Temp\codex-clipboard-3a4f1bc9-fbda-4098-96a7-7b3e5df9141e.png')
MASTER = SOURCE / 'onslaught_ith_supplied.png'
if not MASTER.exists(): shutil.copyfile(SUPPLIED, MASTER)
SPRITE = ROOT / 'graphics/ships/ithaca/chief_navigator_onslaught_ith.png'
SPRITE.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(MASTER, SPRITE)
with Image.open(SPRITE) as image:
    assert image.size == (288, 384) and image.mode == 'RGBA'
assert SPRITE.read_bytes() == MASTER.read_bytes()

HULL = 'chief_navigator_onslaught_ith'
LASER = 'chief_navigator_onslaught_ith_naval_laser'
VARIANT = HULL + '_Standard'
skin = {
    'baseHullId': 'onslaught', 'skinHullId': HULL, 'hullName': 'Onslaught (ITH)',
    'descriptionId': HULL, 'tech': 'Ithaca', 'incompatibleWithBaseHull': True,
    'spriteName': SPRITE.relative_to(ROOT).as_posix(),
    'tags': ['no_dealer', 'no_autofit', 'no_bp_drop', 'chief_navigator_ithaca'],
    'removeWeaponSlots': [], 'removeEngineSlots': [], 'removeBuiltInMods': [],
    'removeBuiltInWeapons': ['WS 016', 'WS 017'],
    'builtInWeapons': {'WS 016': LASER, 'WS 017': LASER},
}
(ROOT / f'data/hulls/skins/{HULL}.skin').write_text(json.dumps(skin, indent=2) + '\n', encoding='utf-8')
variant = json.loads((CORE / 'data/variants/onslaught/onslaught_Standard.variant').read_text('utf-8'))
variant.update({'hullId': HULL, 'variantId': VARIANT, 'displayName': 'Ithaca'})
for group in variant['weaponGroups']:
    for slot in ['WS 016', 'WS 017']:
        if slot in group['weapons']: group['weapons'][slot] = LASER
(ROOT / f'data/variants/{VARIANT}.variant').write_text(json.dumps(variant, indent=2) + '\n', encoding='utf-8')

# A dedicated built-in beam shares the naval mechanics, without fortress-sized art.
naval_width = json.loads(re.search(r'"width"\s*:\s*([0-9.]+)',
    (ROOT / 'data/weapons/chief_navigator_drifting_wall_colossal_inert.wpn').read_text('utf-8')).group(1))
weapon = {
    'id': LASER, 'specClass': 'beam',
    'everyFrameEffect': 'chiefnavigator.weapons.DriftingWallBeamEffect',
    'beamEffect': 'chiefnavigator.weapons.IthacaAbyssalBeamEffect',
    'type': 'BALLISTIC', 'size': 'LARGE', 'beamFireOnlyOnFullCharge': True,
    'turretSprite': 'graphics/fx/empty.png', 'hardpointSprite': 'graphics/fx/empty.png',
    'hardpointGlowSprite': 'graphics/weapons/chief_navigator_onslaught_ith_laser_glow.png',
    'turretOffsets': [15, 0], 'turretAngleOffsets': [0],
    'hardpointOffsets': [15, 0], 'hardpointAngleOffsets': [0],
    'fringeColor': [38,145,158,255], 'coreColor': [244,255,246,255],
    'glowColor': [104,210,202,220], 'darkCore': False, 'width': naval_width,
    'textureType': 'ROUGH', 'textureScrollSpeed': 520, 'pixelsPerTexel': 5,
    'pierceSet': ['PROJECTILE_FF','PROJECTILE_NO_FF','PROJECTILE_FIGHTER','MISSILE_FF','MISSILE_NO_FF','FIGHTER'],
    'fireSoundOne': 'chief_navigator_naval_gun_load', 'fireSoundTwo': 'chief_navigator_naval_gun_fire',
}
(ROOT / f'data/weapons/{LASER}.wpn').write_text(json.dumps(weapon, indent=2) + '\n', encoding='utf-8')

# Recolor only the small stock TPC charge overlay; the supplied hull is untouched.
glow = np.array(Image.open(CORE / 'graphics/weapons/onslaught_thermal_glow.png').convert('RGBA'))
brightness = glow[..., :3].max(axis=2).astype(np.float32) / 255
teal = np.array([104,210,202], dtype=np.float32)
glow[..., :3] = np.rint(brightness[..., None] * teal).astype(np.uint8)
Image.fromarray(glow).save(ROOT / weapon['hardpointGlowSprite'])

spec = importlib.util.spec_from_file_location('records', ROOT / 'tools/paginate_rules_dialogue.py')
records = importlib.util.module_from_spec(spec)
spec.loader.exec_module(records)
def add_csv_row(path, row_id, values):
    original, newline = records.split_raw_records(path.read_bytes().decode('utf-8'))
    serialized = records.serialize_record(values, newline)
    output, replaced = [original[0]], False
    for raw in original[1:]:
        if raw and records.parse_record(raw)[0 if path.name=='descriptions.csv' else 1] == row_id:
            output.append(serialized); replaced = True
        else: output.append(raw)
    if not replaced:
        while output and output[-1] == '': output.pop()
        output += [serialized, '']
    path.write_bytes(newline.join(output).encode('utf-8'))

csv_path = ROOT / 'data/weapons/weapon_data.csv'
rows = list(csv.DictReader(csv_path.open(encoding='utf-8', newline='')))
header = list(rows[0])
laser = next(dict(row) for row in rows if row['id'] == 'chief_navigator_drifting_wall_colossal_inert')
laser['damage/second'] = format(float(laser['damage/second']) * 0.5, 'g')
laser['range'] = '1200'
laser['energy/second'] = '900'
laser['burst delay'] = '30'
laser.update({'name': 'Ithaca Naval Laser', 'id': LASER, 'hints': 'STRIKE, DO_NOT_CONSERVE, DANGEROUS, SYSTEM',
              'groupTag': 'chief_navigator_ithaca_ship_laser', 'tech/manufacturer': 'Ithaca', 'number': '',
              'customAncillary': 'Built into the Onslaught (ITH). Receives installed Ithaca naval research upgrades.',
              'customAncillaryHL': ''})
add_csv_row(csv_path, LASER, [laser[key] for key in header])
description = ('An Ithaca-pattern Onslaught refit. Two built-in naval lasers occupy the original Thermal Pulse Cannon hardpoints; '
               'the hull, engines, defenses, and remaining weapon mounts retain the standard Onslaught layout. '
               'Defense components recovered during the expedition and installed at FOB Ithaca also improve these lasers: '
               'Colossal Traverse Macroservos, the Naval Cycling Regulator, Caged-Arc Projector, and Abyssal Focusing Lens. '
               'Each deployment uses the upgrades currently installed at Ithaca.')
add_csv_row(ROOT / 'data/strings/descriptions.csv', HULL, [HULL, 'SHIP', description, '', '', '', '', ''])
manifest = {'hull': HULL, 'variant': VARIANT, 'laser': LASER,
            'supplied_sprite_sha256': sha256(MASTER.read_bytes()).hexdigest(),
            'runtime_sprite_sha256': sha256(SPRITE.read_bytes()).hexdigest(),
            'sprite_dimensions': [288,384], 'stock_tpc_slots': ['WS 016','WS 017'],
            'base_geometry': 'onslaught', 'uses_image_generation': False,
            'laser_base_stats_from': 'chief_navigator_drifting_wall_colossal_inert',
            'laser_damage_multiplier': 0.5,
            'laser_base_range': 1200, 'laser_flux_per_second': 900, 'laser_beam_width': naval_width,
            'laser_base_cooldown': float(laser['burst delay']),
            'laser_beam_width_from': 'chief_navigator_drifting_wall_colossal_inert'}
(SOURCE / 'provenance.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
print(json.dumps(manifest, indent=2))
