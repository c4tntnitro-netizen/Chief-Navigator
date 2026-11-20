from pathlib import Path
from PIL import Image
import numpy as np
import hashlib,json
p=Path(__file__).resolve().parent
raw=Image.open(p/'imagegen_raw.png').convert('RGBA')
a=np.array(raw)
template=np.array(Image.open(p.parent/'input.png').convert('RGBA'))
y,x=np.where(a[:,:,3]>=128);source_box=[int(x.min()),int(y.min()),int(x.max()+1),int(y.max()+1)]
y,x=np.where(template[:,:,3]>=128);target_box=[int(x.min()),int(y.min()),int(x.max()+1),int(y.max()+1)]
l,t,r,b=target_box
crop=np.array(raw.crop(tuple(source_box)).resize((r-l,b-t),Image.Resampling.NEAREST))
out=np.zeros_like(template);out[t:b,l:r]=crop;out[:,:,3]=template[:,:,3]
assert np.array_equal(out[:,:,3],template[:,:,3])
Image.fromarray(out).save(p/'threaxus_wall_module_clean_v2.png')
report={'edit':'Stronger built-in ImageGen reconstruction of remaining joins',
        'raw_size':raw.size,'final_size':[out.shape[1],out.shape[0]],
        'source_alpha_bbox':source_box,'native_alpha_bbox':target_box,
        'sampling':'nearest','original_native_alpha_exact':True,
        'raw_retained':True,'previous_versions_retained':True,
        'sha256':hashlib.sha256((p/'threaxus_wall_module_clean_v2.png').read_bytes()).hexdigest()}
(p/'audit.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
