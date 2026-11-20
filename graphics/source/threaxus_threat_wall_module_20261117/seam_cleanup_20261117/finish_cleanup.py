"""Normalize ImageGen, then confine its repaint to provenance seam neighborhoods."""
from pathlib import Path
import hashlib, json
import cv2
import numpy as np
from PIL import Image

OUT=Path(__file__).resolve().parent
original=np.array(Image.open(OUT/'input.png').convert('RGBA'))
raw=Image.open(OUT/'imagegen_raw.png').convert('RGBA')
rgba=np.array(raw)
ys,xs=np.where(rgba[:,:,3]>=128)
raw_box=(int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1))
ys,xs=np.where(original[:,:,3]>=128)
box=(int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1))
# Nearest sampling keeps the generated linework sharp at the intended size.
aligned=np.array(Image.fromarray(rgba).crop(raw_box).resize((box[2]-box[0],box[3]-box[1]),Image.Resampling.NEAREST))
native=np.zeros_like(original)
native[box[1]:box[3],box[0]:box[2]]=aligned
native[:,:,3]=original[:,:,3]
Image.fromarray(native).save(OUT/'imagegen_native.png')

provenance=np.load(OUT.parent/'pixel_provenance.npz')['source_xy']
assert provenance.shape[:2]==original.shape[:2]
visible=original[:,:,3]>=128
seeds=np.zeros(visible.shape,dtype=bool)
rgb=original[:,:,:3].astype(np.float32)
for axis in (0,1):
    a=(slice(None,-1),slice(None)) if axis==0 else (slice(None),slice(None,-1))
    b=(slice(1,None),slice(None)) if axis==0 else (slice(None),slice(1,None))
    pa,pb=provenance[a],provenance[b]
    discontinuity=(pa[:,:,0]!=pb[:,:,0])|(np.abs(pa[:,:,1:]-pb[:,:,1:]).sum(axis=2)!=1)
    jump=np.linalg.norm(rgb[a]-rgb[b],axis=2)
    cut=visible[a]&visible[b]&discontinuity&(jump>25)
    seeds[a]|=cut;seeds[b]|=cut
band=cv2.dilate(seeds.astype(np.uint8),np.ones((7,7),np.uint8))>0
interior=cv2.erode(visible.astype(np.uint8),np.ones((7,7),np.uint8))>0
band&=interior
mask=band.astype(np.uint8)*255
x,y,w,h=cv2.boundingRect(mask)
# NORMAL_CLONE follows the generated patch gradients. MIXED_CLONE could retain
# the original strong cut edge, which is precisely the artifact being repaired.
blend=cv2.seamlessClone(cv2.cvtColor(native[:,:,:3],cv2.COLOR_RGB2BGR),
    cv2.cvtColor(original[:,:,:3],cv2.COLOR_RGB2BGR),mask,(x+w//2,y+h//2),cv2.NORMAL_CLONE)
blend=cv2.cvtColor(blend,cv2.COLOR_BGR2RGB)
result=original.copy();result[band,:3]=blend[band]
assert np.array_equal(result[:,:,3],original[:,:,3])
assert np.array_equal(result[~band],original[~band])
Image.fromarray(mask).save(OUT/'seam_edit_mask.png')
Image.fromarray(result).save(OUT/'threaxus_wall_module_clean.png')
changed=np.any(result!=original,axis=2)
assert not np.any(changed&~band)
report={'method':'Built-in ImageGen edit + nearest native-size registration + OpenCV NORMAL_CLONE local Poisson reconstruction',
        'size':[original.shape[1],original.shape[0]],'imagegen_raw_size':list(raw.size),
        'registration_raw_alpha_bbox':raw_box,'registration_original_alpha_bbox':box,
        'seam_detection':'Source-ID/coordinate discontinuities with visible RGB jump>25;7pixel dilation;eroded silhouette guard',
        'seam_neighborhood_pixels':int(band.sum()),'changed_pixels':int(changed.sum()),
        'visible_pixels':int((original[:,:,3]>0).sum()),'outside_mask_rgba_exact':True,'original_alpha_exact':True,
        'original_pixel_only_master_preserved':True,
        'source_pixel_only':False,'note':'New RGB values exist only inside documented seam neighborhoods; this version intentionally permits generated join pixels.',
        'sha256':{n:hashlib.sha256((OUT/n).read_bytes()).hexdigest() for n in ['input.png','imagegen_raw.png','threaxus_wall_module_clean.png']},
        'references':['https://docs.opencv.org/4.12.0/df/da0/group__photo__clone.html',
                      'https://docs.opencv.org/doc/doxygen/html/db/dda/classcv_1_1detail_1_1GraphCutSeamFinder.html']}
(OUT/'audit.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
