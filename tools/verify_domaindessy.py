"""Run the carrier's native RC7 loading and research/PD checks against the selected JAR."""
from pathlib import Path
import csv
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[1]
GAME = next(parent for parent in ROOT.parents
            if (parent/'starsector-core/starfarer.api.jar').is_file()
            and (parent/'jdk-23+7/bin/javac.exe').is_file())
CORE = GAME/'starsector-core'
OUT = ROOT/'out_domaindessy_check'

def fixture(relative, ids, key):
    rows = {}
    for base in [CORE, ROOT]:
        with (base/relative).open(encoding='utf-8-sig', newline='') as stream:
            records = csv.reader(stream)
            header = next(records)
            for row in records:
                if len(row) > key and row[key] in ids:
                    rows[row[key]] = row
    assert set(rows) == set(ids), (relative, set(ids)-set(rows))
    target = OUT/'native_fixture'/relative
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.writer(stream)
        writer.writerow(header)
        writer.writerows(rows.values())

fixture('data/weapons/weapon_data.csv', ['lightdualac','atropos','atropos_single',
        'ioncannon_fighter','chief_navigator_lightning_cloudswarm'], 1)
fixture('data/hulls/wing_data.csv', ['broadsword_wing','hoplon_wing','trident_wing',
        'chief_navigator_lightning_wing','chief_navigator_poseidon_wing'], 0)
jar = ROOT/json.loads((ROOT/'mod_info.json').read_text('utf-8-sig'))['jars'][0]
classpath = os.pathsep.join(str(p) for p in [jar, *CORE.glob('*.jar')])
classes = OUT/'classes'; classes.mkdir(parents=True, exist_ok=True)
sources = ['WallResearchRegression','DomaindessyAirGroupRegression',
           'NativeChargePulseLoadRegression','NativeChargePulseDamageRegression',
           'NativeDomaindessyLoadRegression','OdysseyIthRegression','NativeOdysseySkinLoadRegression']
subprocess.run([str(GAME/'jdk-23+7/bin/javac.exe'),'--release','17','-cp',classpath,
                '-d',str(classes),*(str(ROOT/'tools'/f'{name}.java') for name in sources)],
               cwd=ROOT, check=True)
for name in ['OdysseyIthRegression','NativeOdysseySkinLoadRegression']:
    result = subprocess.run([str(GAME/'jre/bin/java.exe'),'-Xverify:none',
                             '-Dlog4j.defaultInitOverride=true','-cp',
                             str(classes)+os.pathsep+classpath,name],cwd=ROOT,
                            stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (OUT/f'{name}.log').write_bytes(result.stdout)
    print(result.stdout.decode('utf-8',errors='replace'))
    result.check_returncode()
