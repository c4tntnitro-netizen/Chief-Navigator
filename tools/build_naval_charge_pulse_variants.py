"""Register hidden native weapons so every pulse length receives CSV damage setup."""
from pathlib import Path
import csv
import importlib.util
import json

ROOT = Path(__file__).resolve().parents[1]
ID = 'chief_navigator_onslaught_charge_pulse'
module = importlib.util.spec_from_file_location('records', ROOT / 'tools/paginate_rules_dialogue.py')
records = importlib.util.module_from_spec(module)
module.loader.exec_module(records)
path = ROOT / 'data/weapons/weapon_data.csv'
with path.open(newline='', encoding='utf-8') as handle:
    reader = csv.DictReader(handle)
    fields = reader.fieldnames
    base = next(row for row in reader if row['id'] == ID)
weapon = json.loads((ROOT / f'data/weapons/{ID}.wpn').read_text('utf-8'))
raw, newline = records.split_raw_records(path.read_bytes().decode('utf-8'))
for length in (150, 188, 225, 263, 338, 375, 413, 450):
    variant_id = f'{ID}_{length}'
    variant = dict(weapon, id=variant_id, projectileSpecId=f'{ID}_shot_{length}')
    (ROOT / f'data/weapons/{variant_id}.wpn').write_text(json.dumps(variant, indent=2)+'\n', encoding='utf-8')
    row = dict(base, id=variant_id)
    serialized = records.serialize_record([row[key] for key in fields], newline)
    matches = [i for i, record in enumerate(raw) if record and len(records.parse_record(record)) > 1
               and records.parse_record(record)[1] == variant_id]
    assert len(matches) <= 1
    if matches:
        raw[matches[0]] = serialized
    else:
        while raw and raw[-1] == '':
            raw.pop()
        raw.extend([serialized, ''])
path.write_bytes(newline.join(raw).encode('utf-8'))
print('Registered eight hidden length variants; original rows preserved.')
