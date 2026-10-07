$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$generated = "C:\Users\myauc\.codex\generated_images\01a02111-c74f-72b1-95f4-9c5d09b3a84a\exec-338ebb1a-6281-4d10-8b81-b5854f9cb3f7.png"
$sourceDir = Join-Path $modRoot "graphics\source"
$combatDir = Join-Path $modRoot "graphics\combat"
$sourceCopy = Join-Path $sourceDir "chief_navigator_ithaca_connected_rear_wall_chroma.png"
$output = Join-Path $combatDir "chief_navigator_ithaca_connected_rear_wall.png"

New-Item -ItemType Directory -Force -Path $sourceDir, $combatDir | Out-Null
Copy-Item -LiteralPath $generated -Destination $sourceCopy -Force

$src = [System.Drawing.Bitmap]::FromFile($sourceCopy)
try {
    $rgba = New-Object System.Drawing.Bitmap(
        $src.Width,
        $src.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $minX = $src.Width
    $minY = $src.Height
    $maxX = -1
    $maxY = -1

    for ($y = 0; $y -lt $src.Height; $y++) {
        for ($x = 0; $x -lt $src.Width; $x++) {
            $c = $src.GetPixel($x, $y)
            $maxRB = [Math]::Max([int]$c.R, [int]$c.B)
            $minRB = [Math]::Min([int]$c.R, [int]$c.B)
            $chroma = $minRB - [int]$c.G
            $greenRatio = if ($minRB -gt 0) {
                [double]$c.G / [double]$minRB
            } else { 2.0 }
            $alpha = 255

            # Remove the generated #ff00ff matte and feather only its
            # anti-aliased boundary. Teal/cyan armor has a strong green
            # component and therefore cannot enter this branch.
            if (($c.R -gt 205 -and $c.B -gt 205 -and $c.G -lt 90) -or
                    ($c.R -gt 80 -and $c.B -gt 80 -and $greenRatio -lt 0.52)) {
                $alpha = 0
            } elseif ($c.R -gt 70 -and $c.B -gt 70 -and $greenRatio -lt 0.86) {
                $alpha = [Math]::Max(0, [Math]::Min(255,
                    [int](255.0 * (($greenRatio - 0.52) / 0.34))))
            }

            if ($alpha -gt 0) {
                $despill = 255 - $alpha
                $r = [Math]::Max(0, [int]$c.R - $despill)
                $b = [Math]::Max(0, [int]$c.B - $despill)
                $rgba.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(
                    $alpha, $r, [int]$c.G, $b))
                if ($alpha -gt 12) {
                    $minX = [Math]::Min($minX, $x)
                    $minY = [Math]::Min($minY, $y)
                    $maxX = [Math]::Max($maxX, $x)
                    $maxY = [Math]::Max($maxY, $y)
                }
            } else {
                $rgba.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
    }

    if ($maxX -lt $minX -or $maxY -lt $minY) {
        throw "No non-chroma pixels found in generated wall art."
    }

    $pad = 4
    $cropX = [Math]::Max(0, $minX - $pad)
    $cropY = [Math]::Max(0, $minY - $pad)
    $cropR = [Math]::Min($src.Width - 1, $maxX + $pad)
    $cropB = [Math]::Min($src.Height - 1, $maxY + $pad)
    $cropRect = [System.Drawing.Rectangle]::new(
        $cropX, $cropY, ($cropR - $cropX + 1), ($cropB - $cropY + 1))

    $targetWidth = 8000
    $targetHeight = 1680
    $final = New-Object System.Drawing.Bitmap(
        $targetWidth,
        $targetHeight,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $g = [System.Drawing.Graphics]::FromImage($final)
        try {
            $g.Clear([System.Drawing.Color]::Transparent)
            $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            # Keep a transparent safety border so filtering never clamps an
            # opaque wall pixel against the texture edge in combat.
            $dest = [System.Drawing.Rectangle]::new(
                12, 12, ($targetWidth - 24), ($targetHeight - 24))
            $g.DrawImage($rgba, $dest, $cropRect, [System.Drawing.GraphicsUnit]::Pixel)
        } finally {
            $g.Dispose()
        }
        $final.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $final.Dispose()
    }
} finally {
    if ($rgba) { $rgba.Dispose() }
    $src.Dispose()
}

Write-Host "Built $output (8000x1680 RGBA)"
