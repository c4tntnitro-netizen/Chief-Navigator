param(
    [string]$Source = "graphics/source/chief_navigator_ithaca_intact_generated_v1_keyed.png",
    [string]$Output = "graphics/ships/chief_navigator_ithaca_intact_generated_v1.png"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root $Source
$outputPath = Join-Path $root $Output

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Source image not found: $sourcePath"
}

$sourceBitmap = [System.Drawing.Bitmap]::FromFile($sourcePath)
try {
    $minX = $sourceBitmap.Width
    $minY = $sourceBitmap.Height
    $maxX = -1
    $maxY = -1

    for ($y = 0; $y -lt $sourceBitmap.Height; $y++) {
        for ($x = 0; $x -lt $sourceBitmap.Width; $x++) {
            if ($sourceBitmap.GetPixel($x, $y).A -gt 8) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    if ($maxX -lt $minX -or $maxY -lt $minY) {
        throw "Source image contains no visible pixels: $sourcePath"
    }

    # Include the keyed anti-aliased fringe while discarding the source canvas.
    $padding = 3
    $minX = [Math]::Max(0, $minX - $padding)
    $minY = [Math]::Max(0, $minY - $padding)
    $maxX = [Math]::Min($sourceBitmap.Width - 1, $maxX + $padding)
    $maxY = [Math]::Min($sourceBitmap.Height - 1, $maxY + $padding)

    $sourceRect = New-Object System.Drawing.Rectangle(
        $minX,
        $minY,
        ($maxX - $minX + 1),
        ($maxY - $minY + 1))

    # These dimensions match the established Vast Bulk hull coordinates,
    # collision polygon, module slots, and weapon mount locations.
    $targetWidth = 5632
    $targetHeight = 2216
    $outputBitmap = New-Object System.Drawing.Bitmap(
        $targetWidth,
        $targetHeight,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    try {
        $graphics = [System.Drawing.Graphics]::FromImage($outputBitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
            $targetRect = New-Object System.Drawing.Rectangle(0, 0, $targetWidth, $targetHeight)
            $graphics.DrawImage(
                $sourceBitmap,
                $targetRect,
                $sourceRect,
                [System.Drawing.GraphicsUnit]::Pixel)
        }
        finally {
            $graphics.Dispose()
        }

        $outputDirectory = Split-Path -Parent $outputPath
        if (-not (Test-Path -LiteralPath $outputDirectory)) {
            New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
        }
        $outputBitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $outputBitmap.Dispose()
    }

    Write-Output "Built $outputPath from exact generated station artwork."
    Write-Output "Source crop: x=$minX..$maxX, y=$minY..$maxY"
    Write-Output "Output canvas: ${targetWidth}x${targetHeight}"
}
finally {
    $sourceBitmap.Dispose()
}
