"""Preview the native data poses at one world unit per image pixel."""
from pathlib import Path
import json,math
from PIL import Image

root=Path(__file__).resolve().parents[1]
art=root/'graphics/source/threaxus_wall_shell_20261117'
audit=json.loads((art/'wall_audit.json').read_text())
core=Image.open(root/'graphics/ships/starving_threat/threaxus.png').convert('RGBA')
wall=Image.open(art/'supplied_wall.png').convert('RGBA')
parts=[(core,0.,0.)]
for plan in audit['attachments']:
    # At core facing90, native child slot angle is the PNG's CCW rotation.
    rotated=wall.rotate(plan['slot']['angle'],resample=Image.Resampling.NEAREST,expand=True)
    x,y=plan['pixel_pivot'];parts.append((rotated,x-rotated.width/2,y-rotated.height/2))
left=math.floor(min(x for _,x,y in parts))-24;top=math.floor(min(y for _,x,y in parts))-24
right=math.ceil(max(x+im.width for im,x,y in parts))+24
bottom=math.ceil(max(y+im.height for im,x,y in parts))+24
out=Image.new('RGBA',(right-left,bottom-top))
for im,x,y in parts:out.alpha_composite(im,(round(x-left),round(y-top)))
out.save(art/'assembly_preview.png')
(art/'preview_audit.json').write_text(json.dumps({'size':out.size,'world_units_per_pixel':1,
    'parent_facing':90,'module_pose_source':'native .ship slot location/angle',
    'rotation_sampling':'nearest; preview only','input_assets_resized':False,
    'note':'Source sprite assembly preview; weapon housings are rendered by the actual game.'},indent=2)+'\n')
print(out.size)
