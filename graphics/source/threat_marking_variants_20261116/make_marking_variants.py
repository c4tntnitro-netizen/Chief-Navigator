"""Small markings-only variations on the original pixel grid.

Retains the original RGBA underlay everywhere except explicitly painted armor
interiors. No geometric operation or resampling touches the source sprite.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parent
original = Image.open(root / 'original.png').convert('RGBA')
source = np.array(original)
assert original.size == (80, 92)
scale = 8
faces = [
    [(51, 11), (60, 14), (66, 19), (65, 22), (57, 18), (51, 15)],
    [(23, 17), (28, 14), (29, 16), (29, 25), (26, 28), (21, 27)],
    [(42, 40), (47, 40), (48, 47), (44, 50), (42, 47)],
    [(50, 63), (54, 61), (54, 74), (50, 78), (48, 75)],
    [(12, 73), (19, 71), (19, 78), (12, 81), (9, 82), (9, 76)],
]
allowed_image = Image.new('L', original.size)
allowed_draw = ImageDraw.Draw(allowed_image)
for face in faces:
    allowed_draw.polygon(face, fill=255)
rgb = source[:, :, :3].astype(float)
allowed = np.array(allowed_image) > 0
# Protect dark recesses/fasteners and translucent silhouette edges, even within
# a broad armor-face selection. The source itself is never blurred or moved.
allowed &= (source[:, :, 3] == 255) & (rgb.min(axis=2) > 65) & (np.ptp(rgb, axis=2) < 48)


def paint_mask(lines=(), polygons=(), rectangles=()):
    image = Image.new('L', (original.width * scale, original.height * scale))
    draw = ImageDraw.Draw(image)
    for points, width in lines:
        draw.line([(round(x * scale), round(y * scale)) for x, y in points],
                  fill=255, width=max(1, round(width * scale)), joint='curve')
    for polygon in polygons:
        draw.polygon([(round(x * scale), round(y * scale)) for x, y in polygon], fill=255)
    for x0, y0, x1, y1 in rectangles:
        draw.rectangle((round(x0 * scale), round(y0 * scale), round(x1 * scale), round(y1 * scale)), fill=255)
    mask = np.array(image.resize(original.size, Image.Resampling.LANCZOS)).astype(float) / 255
    mask[~allowed] = 0
    return mask


def apply_paint(dark, light):
    changed = source.copy()
    # Multiplicative olive-brown pigment retains the source's local shading.
    result = rgb * (1 - dark[:, :, None] * np.array([.28, .29, .35]))
    result += light[:, :, None] * np.array([18, 17, 12])
    changed[:, :, :3] = np.clip(np.rint(result), 0, 255).astype(np.uint8)
    return changed


definitions = [
    ('Offset traces', paint_mask(lines=[
        ([(53, 12.5), (56, 14), (59, 14.5)], .8),
        ([(61, 18), (64, 20)], .7),
        ([(25, 19), (25, 23), (23, 25)], .8),
        ([(44, 43), (46, 45)], .8),
        ([(51, 65), (51, 70), (50, 72)], .8),
        ([(13, 77), (16, 75)], .8),
    ]), paint_mask()),
    ('Broken bands', paint_mask(lines=[
        ([(54, 13), (58, 16)], .9),
        ([(60.5, 17.5), (64, 20)], .9),
        ([(50, 66), (52, 66)], .8),
        ([(50, 69), (52, 69)], .8),
        ([(12, 78), (15, 76.5)], .8),
    ]), paint_mask(lines=[
        ([(26, 20), (26, 22)], .9),
        ([(23, 24), (23, 25.5)], .9),
        ([(43, 42), (43, 44)], .9),
        ([(46, 43), (46, 45)], .9),
    ])),
    ('Small block accents', paint_mask(
        lines=[([(24, 21), (26, 23)], .9), ([(62, 18.5), (64, 20)], .9)],
        polygons=[[(55, 13.5), (58, 15), (57.5, 16), (54.5, 14.5)]],
        rectangles=[(45, 44, 46, 46), (50, 71, 51, 73), (13, 75, 14, 77)],
    ), paint_mask()),
]
audit = {'method': 'native-pixel markings-only edits on protected armor interiors',
         'size': original.size, 'source_sha256': hashlib.sha256((root / 'original.png').read_bytes()).hexdigest(),
         'no_source_resampling_or_geometric_edits': True, 'variants': []}
images = [original]
labels = ['Original']
for index, (name, dark, light) in enumerate(definitions, 1):
    result = apply_paint(dark, light)
    changed = np.any(result != source, axis=2)
    assert np.array_equal(result[:, :, 3], source[:, :, 3])
    assert not np.any(changed & ~allowed), 'Protected detail/silhouette changed'
    assert np.array_equal(result[~changed], source[~changed])
    assert 10 < np.count_nonzero(changed) < 180, 'Markings must remain slight'
    image = Image.fromarray(result)
    path = root / f'variant_{index:02}.png'
    image.save(path)
    images.append(image)
    labels.append(f'{index}. {name}')
    audit['variants'].append({'file': path.name, 'markings': name,
        'changed_pixels': int(np.count_nonzero(changed)),
        'changed_foreground_percent': round(100 * np.count_nonzero(changed) / np.count_nonzero(source[:, :, 3]), 3),
        'alpha_identical': True, 'all_other_pixels_exact': True,
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})

cell_width, cell_height, border, header = 80, 92, 8, 22
preview = Image.new('RGB', (cell_width * 4 + border * 5, cell_height + header + border * 2), (20, 22, 19))
draw = ImageDraw.Draw(preview)
try:
    font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 12)
except OSError:
    font = ImageFont.load_default()
for index, (image, label) in enumerate(zip(images, labels)):
    x = border + index * (cell_width + border)
    y = border
    label = 'Original' if index == 0 else f'Variant {index}'
    draw.text((x + 4, y + 2), label, font=font, fill=(224, 222, 205))
    preview.paste(image, (x, y + header), image)
preview.save(root / 'comparison_native.png')
(root / 'audit.json').write_text(json.dumps(audit, indent=2) + '\n', encoding='utf-8')
print(json.dumps(audit, indent=2))
