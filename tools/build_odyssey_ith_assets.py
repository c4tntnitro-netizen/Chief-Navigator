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
CORE = next(parent / 'starsector-core' for parent in ROOT.parents
            if (parent / 'starsector-core/starfarer.api.jar').is_file()
            and (parent / 'jre/bin/java.exe').is_file())
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

import runpy
runpy.run_path(str(ROOT / 'tools/build_domaindessy_air_group.py'), run_name='__main__')
manifest = dict(art_metadata, hull=HULL, variant=VARIANT, base_geometry='odyssey',
                naval_slots=[], removed_weapon_slots=['WS 006'], fighter_bays=4,
                added_builtin_mods=['surveying_equipment', 'chief_navigator_domaindessy_air_group'],
                default_wings=['broadsword_wing']*2+['hoplon_wing']*2,
                research_air_groups='graphics/source/domaindessy_air_group_20261117/provenance.json')
(SOURCE / 'provenance.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print(json.dumps(manifest, indent=2))
