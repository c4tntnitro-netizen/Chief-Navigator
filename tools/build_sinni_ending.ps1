param(
    [string]$Source =
        "graphics/source/chief_navigator_sinni_ending_v4_supplied.png",
    [string]$FullResolution =
        "graphics/source/chief_navigator_sinni_ending_v4_fullres.png",
    [string]$Runtime =
        "graphics/illustrations/chief_navigator_sinni_ending_v4.png"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root $Source
$fullResolutionPath = Join-Path $root $FullResolution
$runtimePath = Join-Path $root $Runtime

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Source image not found: $sourcePath"
}

$sourceBitmap = [System.Drawing.Bitmap]::FromFile($sourcePath)
try {
    # Preserve the supplied composition while taking the largest centered crop
    # whose integer dimensions are exactly 16:9. The source is already within
    # a fraction of a percent of that ratio, so only its outer fringe is lost.
    $scale = [Math]::Min(
        [Math]::Floor($sourceBitmap.Width / 16),
        [Math]::Floor($sourceBitmap.Height / 9))
    $cropWidth = [int]($scale * 16)
    $cropHeight = [int]($scale * 9)
    $cropX = [int][Math]::Floor(($sourceBitmap.Width - $cropWidth) / 2)
    $cropY = [int][Math]::Floor(($sourceBitmap.Height - $cropHeight) / 2)
    $crop = [System.Drawing.Rectangle]::new(
        $cropX, $cropY, $cropWidth, $cropHeight)

    $fullResolutionBitmap = $sourceBitmap.Clone(
        $crop,
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    try {
        $fullDirectory = Split-Path -Parent $fullResolutionPath
        $runtimeDirectory = Split-Path -Parent $runtimePath
        New-Item -ItemType Directory -Force `
            -Path $fullDirectory, $runtimeDirectory | Out-Null
        $fullResolutionBitmap.Save(
            $fullResolutionPath,
            [System.Drawing.Imaging.ImageFormat]::Png)

        $runtimeBitmap = [System.Drawing.Bitmap]::new(
            960,
            540,
            [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($runtimeBitmap)
            try {
                $graphics.CompositingQuality =
                    [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode =
                    [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode =
                    [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.SmoothingMode =
                    [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.DrawImage(
                    $fullResolutionBitmap,
                    [System.Drawing.Rectangle]::new(0, 0, 960, 540),
                    0,
                    0,
                    $fullResolutionBitmap.Width,
                    $fullResolutionBitmap.Height,
                    [System.Drawing.GraphicsUnit]::Pixel)
            }
            finally {
                $graphics.Dispose()
            }
            $runtimeBitmap.Save(
                $runtimePath,
                [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $runtimeBitmap.Dispose()
        }
    }
    finally {
        $fullResolutionBitmap.Dispose()
    }

    Write-Output "Source: $($sourceBitmap.Width)x$($sourceBitmap.Height)"
    Write-Output "Full-resolution 16:9 crop: ${cropWidth}x${cropHeight}"
    Write-Output "Runtime illustration: 960x540"
}
finally {
    $sourceBitmap.Dispose()
}
