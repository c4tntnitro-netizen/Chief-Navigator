$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $modRoot 'graphics\source\ithaca_codex_cutouts'
$outputRoot = Join-Path $modRoot 'graphics\icons\cargo'
$previewRoot = Join-Path $modRoot 'docs\assets'
New-Item -ItemType Directory -Force -Path $outputRoot, $previewRoot | Out-Null

# Transparent cutouts of Fractal Softworks' Codex art; see the source README.
# Keep the full cutout and a small margin at the user's enlarged 96x96 size.
$icons = @(
    @{ Part = 'macroservos'; Source = 'custom.png'; Label = 'Macroservos' },
    @{ Part = 'naval_cycler'; Source = 'manual_venting.png'; Label = 'Naval cycling' },
    @{ Part = 'arc_projector'; Source = 'manual_energy_weapons.png'; Label = 'Arc projector' },
    @{ Part = 'intercept_matrix'; Source = 'game_mechanics.png'; Label = 'Interception' },
    @{ Part = 'munitions_compiler'; Source = 'manual_missiles.png'; Label = 'Munitions' },
    @{ Part = 'autoloader_core'; Source = 'manual_ballistic_weapons.png'; Label = 'Autoloader' },
    @{ Part = 'abyssal_lens'; Source = 'weapons.png'; Label = 'Abyssal lens' }
)

# Validate every source before replacing any of the active item textures.
foreach ($icon in $icons) {
    $path = Join-Path $sourceRoot ($icon.Part + '.png')
    $image = [System.Drawing.Bitmap]::new($path)
    try {
        foreach ($point in @(@(0, 0), @(($image.Width - 1), 0),
                @(0, ($image.Height - 1)), @(($image.Width - 1), ($image.Height - 1)))) {
            if ($image.GetPixel($point[0], $point[1]).A -gt 8) {
                throw ('Cutout must have transparent corners: ' + $path)
            }
        }
    } finally { $image.Dispose() }
}

foreach ($icon in $icons) {
    $sourcePath = Join-Path $sourceRoot ($icon.Part + '.png')
    $outputPath = Join-Path $outputRoot ('chief_navigator_ithaca_' + $icon.Part + '_codex_v1.png')
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    try {
        $output = [System.Drawing.Bitmap]::new(96, 96, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($output)
            $attributes = [System.Drawing.Imaging.ImageAttributes]::new()
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $attributes.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
                $scale = [Math]::Min(92.0 / $source.Width, 92.0 / $source.Height)
                $width = [int][Math]::Round($source.Width * $scale)
                $height = [int][Math]::Round($source.Height * $scale)
                $destination = [System.Drawing.Rectangle]::new(
                    [int]((96 - $width) / 2), [int]((96 - $height) / 2), $width, $height)
                $graphics.DrawImage($source, $destination,
                    0, 0, $source.Width, $source.Height,
                    [System.Drawing.GraphicsUnit]::Pixel, $attributes)
            } finally {
                $attributes.Dispose()
                $graphics.Dispose()
            }
            for ($y = 0; $y -lt $output.Height; $y++) {
                for ($x = 0; $x -lt $output.Width; $x++) {
                    if ($output.GetPixel($x, $y).A -le 8) {
                        $output.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                    }
                }
            }
            $output.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $output.Dispose() }
    } finally { $source.Dispose() }
    Write-Host ('Built ' + $outputPath)
}

$preview = [System.Drawing.Bitmap]::new(112 * $icons.Count, 264)
$graphics = [System.Drawing.Graphics]::FromImage($preview)
$font = [System.Drawing.Font]::new('Segoe UI', 9)
try {
    $graphics.Clear([System.Drawing.Color]::FromArgb(18, 24, 30))
    $light = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(235, 237, 239))
    $shade = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(202, 207, 212))
    try {
        for ($y = 136; $y -lt 264; $y += 8) {
            for ($x = 0; $x -lt $preview.Width; $x += 8) {
                $brush = if (([int]($x / 8) + [int](($y - 136) / 8)) % 2 -eq 0) { $light } else { $shade }
                $graphics.FillRectangle($brush, $x, $y, 8, 8)
            }
        }
    } finally {
        $light.Dispose()
        $shade.Dispose()
    }
    for ($index = 0; $index -lt $icons.Count; $index++) {
        $icon = $icons[$index]
        $path = Join-Path $outputRoot ('chief_navigator_ithaca_' + $icon.Part + '_codex_v1.png')
        $bitmap = [System.Drawing.Bitmap]::new($path)
        try {
            $graphics.DrawImageUnscaled($bitmap, $index * 112 + 8, 8)
            $graphics.DrawImageUnscaled($bitmap, $index * 112 + 8, 144)
        }
        finally { $bitmap.Dispose() }
        $graphics.DrawString($icon.Label, $font, [System.Drawing.Brushes]::White,
            [single]($index * 112 + 8), [single]112)
        $graphics.DrawString($icon.Label, $font, [System.Drawing.Brushes]::Black,
            [single]($index * 112 + 8), [single]248)
    }
    $preview.Save((Join-Path $previewRoot 'ithaca_codex_upgrade_icons.png'),
        [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $font.Dispose()
    $graphics.Dispose()
    $preview.Dispose()
}
