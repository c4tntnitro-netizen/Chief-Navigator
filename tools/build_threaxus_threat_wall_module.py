"""Assemble a Threat wall section from literal supplied source pixels only."""
from pathlib import Path
import hashlib, json, shutil
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
CORE=ROOT.parent.parent/'starsector-core/graphics/ships/threat'
OUT=ROOT/'graphics/source/threaxus_threat_wall_module_20261117'
NAMES=['fabricator.png','fragment_single.png','overseer.png','threat_assault_unit.png',
       'threat_hive.png','threat_skirmish.png','threat_standoff_unit.png']
SIZE=(512,224)

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'references').mkdir(exist_ok=True)
    sources=[];coords=[]
    for name in NAMES:
        path=CORE/name;shutil.copy2(path,OUT/'references'/name)
        a=np.asarray(Image.open(path).convert('RGBA'))
        sources.append(a)
        y,x=np.indices(a.shape[:2]);coords.append(np.stack([x,y],axis=-1))
    canvas=np.zeros((SIZE[1],SIZE[0],4),dtype=np.uint8)
    provenance=np.full((SIZE[1],SIZE[0],3),-1,dtype=np.int16)
    records=[]
    def paste(name,box,at,turns=0,flip=False):
        i=NAMES.index(name);x0,y0,x1,y1=box
        pixels=sources[i][y0:y1,x0:x1].copy();xy=coords[i][y0:y1,x0:x1].copy()
        if turns:pixels=np.rot90(pixels,turns);xy=np.rot90(xy,turns)
        if flip:pixels=pixels[:,::-1];xy=xy[:,::-1]
        x,y=at;h,w=pixels.shape[:2]
        assert x>=0 and y>=0 and x+w<=SIZE[0] and y+h<=SIZE[1],(name,at,(w,h))
        visible=pixels[:,:,3]>0
        canvas[y:y+h,x:x+w][visible]=pixels[visible]
        target=provenance[y:y+h,x:x+w]
        target[visible,0]=i;target[visible,1:]=xy[visible]
        records.append({'source':name,'crop':box,'position':at,'quarter_turns_ccw':turns,'mirror_x':flip})

    # A narrow longitudinal skeleton, covered by the individual armor masses.
    for x in (90,200,310):paste('threat_hive.png',(70,160,150,270),(x,112),turns=1)
    paste('fabricator.png',(34,111,346,290),(100,25))
    # End plates use opposite copies of native shoulder and fan structures.
    paste('threat_standoff_unit.png',(0,0,124,148),(16,40))
    paste('threat_standoff_unit.png',(0,0,124,148),(372,40),flip=True)
    paste('overseer.png',(6,0,142,70),(0,44),turns=1)
    paste('overseer.png',(6,0,142,70),(442,44),turns=1,flip=True)
    paste('fabricator.png',(0,88,78,190),(2,72),turns=1)
    paste('fabricator.png',(0,88,78,190),(408,72),turns=1,flip=True)
    # Heavy front ridge; retaining the source bolts, seams and socket pixels.
    paste('threat_assault_unit.png',(0,0,160,102),(74,6))
    paste('threat_assault_unit.png',(0,0,160,102),(278,6),flip=True)
    paste('threat_standoff_unit.png',(79,1,173,66),(209,8))
    paste('fabricator.png',(130,7,250,128),(195,50),turns=1)
    paste('threat_hive.png',(20,25,192,150),(170,68))
    paste('threat_skirmish.png',(1,6,81,77),(34,80))
    paste('threat_skirmish.png',(1,6,81,77),(398,80),flip=True)
    # Underside machinery and a central recessed armored throat.
    paste('threat_standoff_unit.png',(65,66,183,159),(197,120))
    for x,flip in [(44,False),(128,False),(340,True),(424,True)]:
        a=sources[1]
        paste('fragment_single.png',(0,0,a.shape[1],a.shape[0]),(x,174),flip=flip)
    # Silhouette guide only: no pixels from the Ithaca reference enter output.
    mask=Image.new('1',SIZE)
    ImageDraw.Draw(mask).polygon([(0,60),(38,18),(92,18),(108,6),(404,6),(420,18),
        (474,18),(511,60),(511,164),(476,202),(420,202),(404,217),
        (108,217),(92,202),(38,202),(0,164)],fill=1)
    keep=np.asarray(mask,dtype=bool)&(canvas[:,:,3]>0)
    canvas[~keep]=0;provenance[~keep]=-1
    final=Image.fromarray(canvas,'RGBA')
    path=OUT/'threaxus_wall_module.png';final.save(path)
    np.savez_compressed(OUT/'pixel_provenance.npz',source_xy=provenance)
    counts={}
    for i,name in enumerate(NAMES):
        used=keep&(provenance[:,:,0]==i);counts[name]=int(used.sum())
        assert counts[name]>0,'All supplied material families should contribute: '+name
        xy=provenance[used,1:]
        assert np.array_equal(canvas[used],sources[i][xy[:,1],xy[:,0]]),'A visible RGBA pixel changed'
    assert int(sum(counts.values()))==int(keep.sum())
    h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    report={'size':SIZE,'visible_bbox':final.getbbox(),'visible_pixels':int(keep.sum()),
        'source_pixels_only':True,'pixel_copy_check':'Every visible output RGBA equals its recorded source coordinate',
        'transforms':['integer crop/translation','quarter-turn transpose','horizontal mirror','binary silhouette mask'],
        'resampling':False,'blending':False,'recoloring':False,'generated_pixels':False,
        'source_pixel_counts':counts,'source_sha256':{n:h(CORE/n) for n in NAMES},
        'output_sha256':h(path),'shape_reference':'Ithaca Wall stepped/clipped-corner armor section; shape only',
        'scale_reference':'Threatxus central triangle approximately468-512px per side;512px canvas length',
        'layers':records,'integration':'Art study only; current15-module awakening mission remains separate'}
    (OUT/'audit.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ['size','visible_bbox','visible_pixels','source_pixels_only','source_pixel_counts','output_sha256']},indent=2))

if __name__=='__main__':main()
