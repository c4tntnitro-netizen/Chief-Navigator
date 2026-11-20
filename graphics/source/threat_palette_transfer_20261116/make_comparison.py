from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parent
ai = Image.open(root / 'imagegen_recolor.png').convert('RGBA')
statistical = Image.open(root / 'distribution_transfer.png').convert('RGBA')
original = Image.open(root / 'source_original.png').convert('RGBA')
assert statistical.size == original.size == (248, 360)
assert statistical.getchannel('A').tobytes() == original.getchannel('A').tobytes()
assert ai.getchannel('A').getextrema()[0] == 0
width, height, margin, title_height = 744, 1080, 28, 78
preview = Image.new('RGB', (width * 2 + margin * 3, height + title_height + margin), (18, 20, 17))
draw = ImageDraw.Draw(preview)
try:
    font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 30)
except OSError:
    font = ImageFont.load_default()
for column, (label, im, resampling) in enumerate([
    ('ImageGen recolor', ai, Image.Resampling.LANCZOS),
    ('Distribution transfer', statistical, Image.Resampling.NEAREST),
]):
    x = margin + column * (width + margin)
    draw.text((x + 12, 19), label, fill=(227, 224, 207), font=font)
    scale = min(width / im.width, height / im.height)
    im = im.resize((round(im.width * scale), round(im.height * scale)), resampling)
    preview.paste(im, (x + (width - im.width) // 2, title_height + (height - im.height) // 2), im)
preview.save(root / 'comparison.png')
ai_original = Image.open(root / 'imagegen_recolor.png')
audit = {
    'tool': 'built-in image_gen', 'prompt': 'imagegen_recolor.prompt.txt',
    'source': 'source_original.png', 'palette_reference': 'palette_reference.png',
    'imagegen_size': ai_original.size, 'imagegen_mode': ai_original.mode,
    'imagegen_alpha_extrema': ai_original.getchannel('A').getextrema(),
    'distribution_size': statistical.size, 'distribution_alpha_exactly_preserved': True,
    'imagegen_result_is_generative_reinterpretation': True,
    'comparison_only_resizes_for_display': True, 'runtime_integration': False,
    'sha256': {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
               for name in ['imagegen_recolor.png', 'distribution_transfer.png', 'comparison.png']},
}
(root / 'comparison_audit.json').write_text(json.dumps(audit, indent=2) + '\n', encoding='utf-8')
print(json.dumps(audit, indent=2))
