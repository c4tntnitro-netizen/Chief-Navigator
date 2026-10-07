$ErrorActionPreference = "Stop"

$modRoot = Split-Path $PSScriptRoot -Parent
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$source = Join-Path $starsectorRoot `
    "starsector-core\graphics\ships\threat\fabricator.png"
$output = Join-Path $modRoot `
    "graphics\ships\starving_threat\chief_navigator_ungaikyo_fabricator_v1.png"

if (-not (Test-Path -LiteralPath $source)) {
    throw "Vanilla Fabricator source sprite not found: $source"
}

Add-Type -AssemblyName System.Drawing
$sourceBitmap = [System.Drawing.Bitmap]::new($source)
$result = [System.Drawing.Bitmap]::new(
    $sourceBitmap.Width,
    $sourceBitmap.Height,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

try {
    for ($y = 0; $y -lt $sourceBitmap.Height; $y++) {
        for ($x = 0; $x -lt $sourceBitmap.Width; $x++) {
            $pixel = $sourceBitmap.GetPixel($x, $y)
            if ($pixel.A -eq 0) {
                $result.SetPixel(
                    $x,
                    $y,
                    [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                continue
            }

            $luma = 0.2126 * $pixel.R `
                + 0.7152 * $pixel.G `
                + 0.0722 * $pixel.B
            $dark = 5.0 + 105.0 * [Math]::Pow($luma / 255.0, 0.82)
            $redChroma = ($pixel.R - $luma) * 0.08
            $greenChroma = ($pixel.G - $luma) * 0.05
            $blueChroma = ($pixel.B - $luma) * 0.10

            $red = [Math]::Max(0, [Math]::Min(
                255, [Math]::Round($dark * 0.86 + $redChroma)))
            $green = [Math]::Max(0, [Math]::Min(
                255, [Math]::Round($dark * 0.90 + $greenChroma)))
            $blue = [Math]::Max(0, [Math]::Min(
                255, [Math]::Round($dark * 1.02 + $blueChroma)))
            $result.SetPixel(
                $x,
                $y,
                [System.Drawing.Color]::FromArgb(
                    $pixel.A, $red, $green, $blue))
        }
    }

    $directory = Split-Path $output -Parent
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $result.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $result.Dispose()
    $sourceBitmap.Dispose()
}

Write-Host "Built Ungaikyo Fabricator sprite -> $output"
