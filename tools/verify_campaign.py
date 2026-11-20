"""Run campaign regression checks against the selected release JAR."""
from pathlib import Path
import json, os, re, subprocess, sys
ROOT = Path(__file__).resolve().parents[1]
GAME = next(parent for parent in ROOT.parents
            if (parent/'starsector-core/starfarer.api.jar').is_file()
            and (parent/'jdk-23+7/bin/javac.exe').is_file())
OUT = ROOT/'out_campaign_check'
OUT.mkdir(exist_ok=True)
TESTS = ['AviciDwellerPopulationRegression', 'AviciResearchStationRegression', 'MaxWallUpgradeRegression', 'SinniSystemVignetteRegression', 'SinniVignetteCompletionRegression', 'SinniKiteRewardRegression', 'DerelictJumpAvoidanceRegression', 'FobIthacaStationRegression', 'IthacaCampaignRegression', 'CampaignLifecycleRegression', 'CampaignWorldInitializationRegression', 'FinalLaborMusicRegression', 'LaborObjectiveMarkerRegression', 'CampaignMaintenanceEventsRegression', 'CampaignPerformanceRegression', 'SinniNavigationRegression', 'SinniMapTargetRegression', 'AviciStarvingPopulationRegression', 'MonthlyIthacaStrikeRegression', 'UngaikyoCampaignRosterRegression', 'GautamaFinalRematchRegression', 'BudaiMovementRegression', 'BudaiRosterRegression', 'IthacaDefenderFriendshipRegression', 'FinalLaborSpawnRegression', 'SinniVignetteOverlapRegression', 'BudaiInteractionRegression', 'BudaiLeapCooldownRegression', 'CampaignUpkeepOnceRegression']
DEPS = ['WallResearchRegression', 'NativeChargePulseLoadRegression', 'NativeChargePulseDamageRegression', 'OnslaughtChargePulseRegression', 'AlphaOdysseyArrivalRegression', 'BudaiSalvageRewardRegression']
jar = ROOT/json.loads((ROOT/'mod_info.json').read_text('utf-8-sig'))['jars'][0]
cp = os.pathsep.join(map(str, [jar, * (GAME/'starsector-core').glob('*.jar'),
    GAME/'mods/GraphicsLib/jars/Graphics.jar', GAME/'mods/Combat Chatter/jars/CombatChatter.jar',
    GAME/'mods/Console Commands/jars/lw_Console.jar', GAME/'mods/LunaLib/jars/LunaLib.jar']))
classes = OUT/'classes'; classes.mkdir(exist_ok=True)
paths = [ROOT/'tools'/f'{n}.java' for n in dict.fromkeys(TESTS+DEPS)]
r = subprocess.run([str(GAME/'jdk-23+7/bin/javac.exe'),'--release','17','-encoding','UTF-8',
    '-cp',cp,'-sourcepath',str(ROOT/'tools'),'-d',str(classes),*map(str,paths)],
    cwd=ROOT,capture_output=True,text=True)
(OUT/'compile.log').write_text(r.stdout+r.stderr,encoding='utf-8')
if r.returncode: raise SystemExit(r.stderr)
results=[]
for n in TESTS:
    source=(ROOT/'tools'/f'{n}.java').read_text(encoding='utf-8')
    package=re.search(r'^package\s+([^;]+);',source,re.M)
    name=(package.group(1)+'.' if package else '')+n
    args=[str(GAME/'jre/bin/java.exe'),'-noverify','-Djava.awt.headless=true',
          '-cp',str(classes)+os.pathsep+cp,name]
    if n=='CampaignPerformanceRegression': args+=['--campaign-only']
    r=subprocess.run(args,cwd=ROOT,capture_output=True,text=True,timeout=120)
    (OUT/(n+'.log')).write_text(r.stdout+r.stderr,encoding='utf-8')
    results.append({'test':n,'exit':r.returncode})
    print(('PASS ' if not r.returncode else 'FAIL ')+n,flush=True)
    if r.returncode: print((r.stdout+r.stderr)[-2200:],flush=True)
(OUT/'results.json').write_text(json.dumps(results,indent=2)+'\n')
sys.exit(any(t['exit'] for t in results))
