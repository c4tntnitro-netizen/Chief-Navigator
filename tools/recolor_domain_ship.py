"""Masked Lab color transfer from the supplied survey ship; no generated pixels.

Run from any directory. Native output retains the source canvas and exact alpha.
Armor, machinery exclusions, navy paint, and extracted insignia are saved separately.
"""
from pathlib import Path
from hashlib import sha256
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'graphics/source/domain_ship_recolor_20261112'
OUT = ASSETS / 'color_transfer'
OUT.mkdir(exist_ok=True)
source_path = ASSETS / 'original_ship.png'
reference_path = ASSETS / 'survey_ship_palette_reference.png'
marking_path = ASSETS / 'domain_marking_reference.png'
source = np.array(Image.open(source_path).convert('RGBA'))
reference = np.array(Image.open(reference_path).convert('RGBA'))
marking = np.array(Image.open(marking_path).convert('RGBA'))
height, width = source.shape[:2]
assert (width, height) == (288, 384)

def lab_of(rgba):
    return cv2.cvtColor(rgba[..., :3].astype(np.float32) / 255, cv2.COLOR_RGB2LAB)

def smooth(lo, hi, values):
    t = np.clip((values - lo) / (hi - lo), 0, 1)
    return t * t * (3 - 2 * t)

def reflected(points):
    return [(width - 1 - x, y) for x, y in points]

armor_image = Image.new('L', (width, height))
draw = ImageDraw.Draw(armor_image)
central = [(136,18),(126,34),(124,49),(118,67),(112,89),(109,105),
           (102,127),(98,148),(93,169),(88,189),(84,207),(90,223),
           (107,213),(117,198),(130,185),(142,175),(154,184),
           (170,198),(181,214),(198,223),(204,208),(197,188),
           (192,166),(187,146),(183,125),(177,104),(174,87),
           (169,66),(163,48),(158,31),(151,18)]
port = [(66,27),(77,34),(80,51),(87,70),(86,94),(91,121),(86,143),
        (82,167),(77,190),(72,214),(66,234),(63,256),(60,278),
        (55,296),(52,316),(40,318),(37,300),(32,281),(27,261),
        (18,251),(24,233),(34,214),(40,195),(40,181),(33,173),
        (16,163),(15,146),(29,133),(39,110),(47,95),(49,83),
        (46,75),(51,60),(56,47),(60,33)]
lower = [(90,204),(107,211),(118,209),(132,211),(145,213),
         (158,211),(171,209),(183,212),(198,204),(206,228),
         (207,249),(195,263),(183,277),(181,295),(171,316),
         (157,317),(152,293),(136,293),(130,318),(116,317),
         (105,296),(103,278),(91,264),(80,250),(81,228)]
aft_plate = [(77,316),(92,315),(103,326),(96,341),(91,354),
             (82,367),(76,355),(78,344),(73,334)]
for polygon in [central, port, reflected(port), lower, aft_plate, reflected(aft_plate)]:
    draw.polygon(polygon, fill=255)

protected_image = Image.new('L', (width, height))
protect = ImageDraw.Draw(protected_image)
def ellipse(cx, cy, rx, ry=None):
    ry = rx if ry is None else ry
    protect.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), fill=255)
def both(cx, cy, rx, ry=None):
    ellipse(cx,cy,rx,ry); ellipse(width-1-cx,cy,rx,ry)

# Circular hardpoints, inset machinery, and their original metal rims.
ellipse(144,25,5)
protect.polygon([(138,31),(149,31),(150,51),(138,51)],fill=255)
ellipse(144,59,13,15)
ellipse(144,82,14,10)
both(131,94,8)
ellipse(144,118,19,21)
both(135,142,5)
both(72,85,11)
both(69,112,9)
both(65,130,10)
both(57,167,20,19)
both(46,201,6)
both(31,249,10)
both(53,239,5)
both(104,236,12)
both(77,254,13)
both(107,263,6)
both(120,256,7)
both(125,239,9)
both(113,281,6)
both(52,289,5)

# Pale bridge/cockpit assemblies, vents, and exposed aft engine/pipe banks.
protect.polygon([(140,153),(149,154),(157,161),(165,172),(160,186),
                 (163,202),(154,207),(133,207),(125,200),(128,184),
                 (123,175),(132,161)],fill=255)
both(126,198,9)
protect.polygon([(133,207),(153,207),(160,218),(162,236),(155,251),
                 (154,267),(162,278),(158,292),(132,293),(125,278),
                 (133,264),(134,252),(127,238),(126,222)],fill=255)
for polygon in [
    [(61,28),(74,29),(77,53),(64,54)],
    [(40,111),(49,112),(44,139),(37,140)],
    [(43,277),(63,279),(80,271),(112,281),(104,315),(89,331),(67,328),(48,309)],
    [(129,292),(159,292),(166,329),(162,356),(132,356),(121,329)],
]:
    protect.polygon(polygon,fill=255)
    protect.polygon(reflected(polygon),fill=255)

geometry = (np.array(armor_image) > 0) & (np.array(protected_image) == 0) & (source[...,3] > 0)
lab = lab_of(source)
L, a, b = lab.transpose(2,0,1)
# Spatial geometry AND warm paint chroma: neutral steel/shadows and lights stay original.
warm_paint = smooth(0.8, 5.5, a) * smooth(2.0, 9.0, b) * smooth(6, 20, L)
warm_paint *= 1 - smooth(82, 97, L)
warm_paint *= 1 - smooth(34, 54, b)  # retain saturated brass, yellow lamps and rust flecks
amount = warm_paint * geometry

ref_lab = lab_of(reference)
rL, ra, rb = ref_lab.transpose(2,0,1)
ref_teal = (reference[...,3] > 240) & (rL > 40) & (rL < 82) & (ra < -2.5) & (ra > -20) & (rb > -10) & (rb < 12)
teal_ab = np.median(ref_lab[ref_teal,1:],axis=0)
mark_lab = lab_of(marking)
mL, ma, mb = mark_lab.transpose(2,0,1)
ref_navy = (marking[...,3] > 240) & (mL > 9) & (mL < 29) & (ma > -13) & (ma < 2) & (mb > -10) & (mb < 8)
navy_ab = np.median(mark_lab[ref_navy,1:],axis=0)
source_ab = np.median(lab[amount > .75,1:],axis=0)

transferred = lab.copy()
transferred[...,1:] = teal_ab + .12 * (lab[...,1:] - source_ab)
# The teal pass preserves every source Lab lightness value and its fine texture.
teal_lab = lab + amount[...,None] * (transferred - lab)

navy_image = Image.new('L',(width,height))
navy_draw = ImageDraw.Draw(navy_image)
rail = [(77,58),(86,74),(85,102),(88,122),(82,144),(77,168),
        (72,193),(65,220),(59,220),(66,183),(69,161),(74,141),(77,118),(75,96)]
aft_navy = [(55,218),(68,217),(68,235),(63,256),(60,275),(53,283),(46,275),(49,255)]
for polygon in [rail,reflected(rail),aft_navy,reflected(aft_navy)]:
    navy_draw.polygon(polygon,fill=255)
navy_draw.polygon([(144,138),(185,177),(177,187),(144,154),(111,187),(103,177)],fill=255)
navy_amount = amount * (np.array(navy_image)/255)
navy_target = teal_lab.copy()
navy_target[...,0] = lab[...,0] * .72
navy_target[...,1:] = navy_ab + .08 * (lab[...,1:] - source_ab)
final_lab = teal_lab + navy_amount[...,None] * (navy_target - teal_lab)

def bake(color_lab):
    rgb = np.rint(np.clip(cv2.cvtColor(color_lab.astype(np.float32),cv2.COLOR_LAB2RGB),0,1)*255).astype(np.uint8)
    rgba=source.copy()
    changed=amount > 0
    rgba[changed,:3]=rgb[changed]
    return rgba

pure=bake(teal_lab)
livery=bake(final_lab)

# Extract actual gold ink from the supplied insignia crop, not newly drawn artwork.
emblem_crop=marking[53:106,25:77].copy()
gold_rgb=emblem_crop[...,:3].astype(np.float32)
gold_alpha=smooth(14,34,gold_rgb[...,0]-gold_rgb[...,2]) * smooth(5,19,gold_rgb[...,1]-gold_rgb[...,2])
gold_alpha*=smooth(75,130,gold_rgb[...,0])
emblem_crop[...,3]=np.rint(gold_alpha*255).astype(np.uint8)
emblem=Image.fromarray(emblem_crop).resize((18,18),Image.Resampling.LANCZOS)
# One existing port-side armor plate; do not lay the emblem over a hardpoint.
emblem_x,emblem_y=43,250
stamp=np.zeros((height,width,4),dtype=np.uint8)
stamp[emblem_y:emblem_y+18,emblem_x:emblem_x+18]=np.array(emblem)
stamp[...,3]=np.rint(stamp[...,3] * geometry).astype(np.uint8)
stamp_alpha=stamp[...,3:4]/255
livery[...,:3]=np.rint(livery[...,:3]*(1-stamp_alpha)+stamp[...,:3]*stamp_alpha).astype(np.uint8)
livery[...,3]=source[...,3]

Image.fromarray(pure).save(OUT/'ship_domain_color_transfer_v1.png')
Image.fromarray(livery).save(OUT/'ship_domain_color_transfer_livery_v1.png')
Image.fromarray(np.rint(amount*255).astype(np.uint8)).save(OUT/'armor_mask.png')
Image.fromarray(np.array(protected_image)).save(OUT/'machinery_protection_mask.png')
Image.fromarray(np.rint(navy_amount*255).astype(np.uint8)).save(OUT/'navy_paint_mask.png')
Image.fromarray(stamp).save(OUT/'domain_emblem_layer.png')

# Exact identity checks apply to native pixels, not a visually similar reconstruction.
assert np.array_equal(livery[...,3],source[...,3])
outside=(amount == 0) & (stamp[...,3] == 0)
assert np.array_equal(livery[outside],source[outside])
protected=np.array(protected_image)>0
assert np.array_equal(livery[protected],source[protected])
assert np.array_equal(livery[source[...,3]==0],source[source[...,3]==0])

preview=Image.new('RGB',(width*3+48,height+44),(23,27,32))
d=ImageDraw.Draw(preview)
for i,(label,pixels) in enumerate([('Original',source),('Masked color transfer',pure),('Navy + Domain stencil',livery)]):
    x=12+i*(width+12)
    d.text((x,10),label,fill=(220,228,228))
    image=Image.fromarray(pixels)
    preview.paste(image,(x,32),image.getchannel('A'))
preview.resize((preview.width*2,preview.height*2),Image.Resampling.NEAREST).save(OUT/'comparison_2x.png')
Image.fromarray(livery).resize((width*3,height*3),Image.Resampling.NEAREST).save(OUT/'livery_preview_3x.png')

report={'method':'Spatial armor masks plus warm-paint selection; reference-sampled Lab chroma transfer; source lightness retained in teal pass.',
        'source_sha256':sha256(source_path.read_bytes()).hexdigest(),'dimensions':[width,height],
        'teal_reference_Lab_ab':teal_ab.tolist(),'navy_reference_Lab_ab':navy_ab.tolist(),
        'selected_armor_pixels':int((amount>0).sum()),'navy_selected_pixels':int((navy_amount>0).sum()),
        'painted_emblem_pixels':int((stamp[...,3]>0).sum()),'alpha_exact':True,'protected_machinery_exact':True,
        'all_pixels_outside_masks_exact':True,'transparent_pixels_exact':True,'uses_image_generation':False,
        'runtime_integration':False,'rebuild_script':'tools/recolor_domain_ship.py',
        'outputs':['ship_domain_color_transfer_v1.png','ship_domain_color_transfer_livery_v1.png','armor_mask.png',
                   'machinery_protection_mask.png','navy_paint_mask.png','domain_emblem_layer.png','comparison_2x.png']}
(OUT/'provenance.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
