param(
    [string]$Source = "graphics/source/chief_navigator_ithaca_intact_uncapped_v1.png",
    [string]$Output = "graphics/ships/chief_navigator_ithaca_intact_generated_v1.png",
    [switch]$WithMountCaps,
    [switch]$WithoutMountCaps,
    [ValidateRange(0, 80)]
    [int]$SmallCapReductionPercent = 40
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

    $exactSourceCanvas = $sourceBitmap.Width -eq 5632 -and $sourceBitmap.Height -eq 2216
    if ($exactSourceCanvas) {
        $minX = 0
        $minY = 0
        $maxX = $sourceBitmap.Width - 1
        $maxY = $sourceBitmap.Height - 1
    } else {
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
    # The approved runtime art includes finishing edits after the historical
    # keyed-source resize. Its archived uncapped canvas is authoritative for
    # cap placement: cloning it preserves every unrelated pixel and alpha.
    $exactCanvas = $exactSourceCanvas
    if ($exactCanvas) {
        $outputBitmap = $sourceBitmap.Clone(
            [System.Drawing.Rectangle]::new(0, 0, $targetWidth, $targetHeight),
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    } else {
        $outputBitmap = New-Object System.Drawing.Bitmap(
            $targetWidth,
            $targetHeight,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    }

    try {
        if (-not $exactCanvas) {
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
        }

        # The active wall is uncapped. Retain the old artwork as an explicit
        # opt-in for historical comparisons; the legacy exclusion still wins.
        if ($WithMountCaps -and -not $WithoutMountCaps) {
            & (Join-Path $PSScriptRoot 'build_ithaca_mount_caps.ps1') -Target $outputBitmap `
                -SmallCapReductionPercent $SmallCapReductionPercent
        }

        $outputDirectory = Split-Path -Parent $outputPath
        if (-not (Test-Path -LiteralPath $outputDirectory)) {
            New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
        }
        if ($exactCanvas -and (-not $WithMountCaps -or $WithoutMountCaps)) {
            # With no overlays, preserve the approved PNG bytes and metadata.
            [System.IO.File]::Copy($sourcePath, $outputPath, $true)
        } else {
            $outputBitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        }
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
