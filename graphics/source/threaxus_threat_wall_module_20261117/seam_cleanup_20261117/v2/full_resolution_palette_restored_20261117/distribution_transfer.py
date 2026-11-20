"""Deterministic iterative 3-D color-distribution transfer; no spatial edits.

User explicitly requested a distribution-based recolor as a separate result.
Foreground alpha weights the CDFs; original dimensions/alpha/hidden RGB survive.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent
RGB_TO_XYZ = np.array([[.4124564, .3575761, .1804375],
                       [.2126729, .7151522, .0721750],
                       [.0193339, .1191920, .9503041]])
WHITE = np.array([.95047, 1., 1.08883])


def rgb_to_lab(rgb):
    rgb = rgb / 255.
    linear = np.where(rgb <= .04045, rgb / 12.92, ((rgb + .055) / 1.055) ** 2.4)
    xyz = linear @ RGB_TO_XYZ.T / WHITE
    delta = 6 / 29
    f = np.where(xyz > delta ** 3, np.cbrt(xyz), xyz / (3 * delta ** 2) + 4 / 29)
    return np.column_stack([116 * f[:, 1] - 16, 500 * (f[:, 0] - f[:, 1]), 200 * (f[:, 1] - f[:, 2])])


def lab_to_rgb(lab):
    fy = (lab[:, 0] + 16) / 116
    f = np.column_stack([fy + lab[:, 1] / 500, fy, fy - lab[:, 2] / 200])
    delta = 6 / 29
    xyz = np.where(f > delta, f ** 3, 3 * delta ** 2 * (f - 4 / 29)) * WHITE
    linear = xyz @ np.linalg.inv(RGB_TO_XYZ).T
    rgb = np.where(linear <= .0031308, 12.92 * linear,
                   1.055 * np.maximum(linear, 0) ** (1 / 2.4) - .055)
    return np.round(np.clip(rgb, 0, 1) * 255).astype(np.uint8)


def weighted_cdf(values, weights):
    # Group equal colors/projections: identical input pixels must recolor identically.
    unique, inverse = np.unique(values, return_inverse=True)
    mass = np.bincount(inverse, weights=weights)
    cdf = (np.cumsum(mass) - mass / 2) / np.sum(mass)
    return unique, cdf, inverse


def match_projection(values, weights, target, target_weights):
    unique, cdf, inverse = weighted_cdf(values, weights)
    target_unique, target_cdf, _ = weighted_cdf(target, target_weights)
    mapped = np.interp(cdf, target_cdf, target_unique)
    return mapped[inverse]


def describe(lab, weights):
    return {'mean': np.average(lab, axis=0, weights=weights).round(4).tolist(),
            'std': np.sqrt(np.average((lab - np.average(lab, axis=0, weights=weights)) ** 2,
                                    axis=0, weights=weights)).round(4).tolist()}


def main():
    source_image = Image.open(ROOT / 'source_original.png').convert('RGBA')
    reference_image = Image.open(ROOT / 'palette_reference.png').convert('RGBA')
    source = np.array(source_image)
    reference = np.array(reference_image)
    source_mask = source[:, :, 3] > 0
    reference_mask = reference[:, :, 3] > 0
    source_weights = source[:, :, 3][source_mask].astype(float) / 255
    reference_weights = reference[:, :, 3][reference_mask].astype(float) / 255
    # Aggregate repeated colors with their exact alpha mass. This is an exact
    # distribution compression, not image sampling or a spatial resize.
    source_colors, source_lookup = np.unique(source[:, :, :3][source_mask], axis=0, return_inverse=True)
    reference_colors, reference_lookup = np.unique(reference[:, :, :3][reference_mask], axis=0, return_inverse=True)
    source_weights = np.bincount(source_lookup, weights=source_weights)
    reference_weights = np.bincount(reference_lookup, weights=reference_weights)
    original_lab = rgb_to_lab(source_colors.astype(float))
    target_lab = rgb_to_lab(reference_colors.astype(float))
    print(f'Transferring full-resolution colors: {len(source_colors)} distinct source colors;64 CIELAB passes.', flush=True)
    transferred = original_lab.copy()
    rng = np.random.default_rng(20261117)
    for iteration in range(64):
        rotation = np.eye(3) if iteration == 0 else np.linalg.qr(rng.normal(size=(3, 3)))[0]
        projected = transferred @ rotation
        target = target_lab @ rotation
        matched = np.column_stack([match_projection(projected[:, axis], source_weights,
                                                    target[:, axis], reference_weights) for axis in range(3)])
        strength = 1. if iteration < 48 else .5
        transferred += strength * ((matched @ rotation.T) - transferred)
    transferred[:, 0] = match_projection(transferred[:, 0], source_weights, target_lab[:, 0], reference_weights)
    result = source.copy()
    output_colors = lab_to_rgb(transferred)
    result[:, :, :3][source_mask] = output_colors[source_lookup]
    output = ROOT / 'threaxus_wall_module_fullres_palette_restored.png'
    Image.fromarray(result).save(output)
    # Exact geometry/alpha and deterministic color-only mapping checks.
    assert result.shape == source.shape
    assert np.array_equal(result[:, :, 3], source[:, :, 3])
    assert np.array_equal(result[~source_mask], source[~source_mask])
    original_colors, inverse = np.unique(source[:, :, :3][source_mask], axis=0, return_inverse=True)
    packed = result[:, :, :3][source_mask].astype(np.uint32) @ np.array([65536, 256, 1], dtype=np.uint32)
    order = np.argsort(inverse, kind='stable')
    same = np.diff(inverse[order]) == 0
    assert np.all(np.diff(packed[order])[same] == 0), 'Equal source colors must receive equal output colors'
    output_lab = rgb_to_lab(output_colors.astype(float))
    quantiles = np.linspace(.005, .995, 199)

    def distance(lab):
        errors = []
        for axis in range(3):
            x, p, _ = weighted_cdf(lab[:, axis], source_weights)
            y, q, _ = weighted_cdf(target_lab[:, axis], reference_weights)
            errors.append(float(np.mean(np.abs(np.interp(quantiles, p, x) - np.interp(quantiles, q, y)))))
        return errors

    audit = {
        'method': '64 deterministic orthogonal-projection histogram matches in CIELAB (iterative distribution transfer), final luminance CDF match',
        'seed': 20261117, 'source_size': source_image.size, 'reference_size': reference_image.size,
        'output_size': source_image.size, 'alpha_identical': True, 'transparent_rgb_identical': True,
        'same_source_colors_map_identically': True, 'source_unique_foreground_colors': len(original_colors),
        'full_resolution_retained': True, 'exact_alpha_weighted_unique_color_compression': True,
        'no_resampling_or_spatial_operations': True,
        'Lab_statistics': {'original': describe(original_lab, source_weights),
                           'reference': describe(target_lab, reference_weights),
                           'output': describe(output_lab, source_weights)},
        'Lab_marginal_quantile_mean_abs_error': {'before': distance(original_lab), 'after': distance(output_lab)},
        'sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in
                   [ROOT / 'source_original.png', ROOT / 'palette_reference.png', output]},
        'runtime_integration': False,
    }
    (ROOT / 'distribution_audit.json').write_text(json.dumps(audit, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(audit, indent=2))


if __name__ == '__main__':
    main()
