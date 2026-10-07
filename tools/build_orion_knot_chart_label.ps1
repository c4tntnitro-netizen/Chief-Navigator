$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$outputPath = Join-Path $modRoot `
    "graphics\ui\chief_navigator_persean_orion_abyss_v1.png"

$bitmap = [System.Drawing.Bitmap]::new(
    512,
    64,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$font = $null
$brush = $null
$format = $null

try {
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.TextRenderingHint = `
        [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $font = [System.Drawing.Font]::new(
        "Bahnschrift SemiCondensed",
        27,
        [System.Drawing.FontStyle]::Regular,
        [System.Drawing.GraphicsUnit]::Pixel)
    $brush = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(210, 116, 161, 174))
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center

    $bounds = [System.Drawing.RectangleF]::new(0, 0, 512, 64)
    $graphics.DrawString(
        "Persean-Orion Abyss",
        $font,
        $brush,
        $bounds,
        $format)
    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    if ($null -ne $format) { $format.Dispose() }
    if ($null -ne $brush) { $brush.Dispose() }
    if ($null -ne $font) { $font.Dispose() }
    $graphics.Dispose()
    $bitmap.Dispose()
}

Write-Host "WROTE $outputPath"
