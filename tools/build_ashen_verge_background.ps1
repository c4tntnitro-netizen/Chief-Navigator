param(
    [string]$Source =
        "graphics/source/chief_navigator_ashen_verge_nebula_v1_supplied.png",
    [string]$Runtime =
        "graphics/backgrounds/chief_navigator_ashen_verge_nebula_v1.jpg"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root $Source
$runtimePath = Join-Path $root $Runtime

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Source image not found: $sourcePath"
}

$sourceBitmap = [System.Drawing.Bitmap]::FromFile($sourcePath)
try {
    # Starsector's system backgrounds are square. Keep the center of the
    # generated field and discard only equal outer margins when necessary.
    $cropSize = [Math]::Min($sourceBitmap.Width, $sourceBitmap.Height)
    $cropX = [int][Math]::Floor(($sourceBitmap.Width - $cropSize) / 2)
    $cropY = [int][Math]::Floor(($sourceBitmap.Height - $cropSize) / 2)

    $runtimeBitmap = [System.Drawing.Bitmap]::new(
        2048,
        2048,
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
                $sourceBitmap,
                [System.Drawing.Rectangle]::new(0, 0, 2048, 2048),
                $cropX,
                $cropY,
                $cropSize,
                $cropSize,
                [System.Drawing.GraphicsUnit]::Pixel)
        }
        finally {
            $graphics.Dispose()
        }

        $runtimeDirectory = Split-Path -Parent $runtimePath
        New-Item -ItemType Directory -Force -Path $runtimeDirectory | Out-Null

        $jpegCodec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
            Where-Object { $_.MimeType -eq "image/jpeg" } |
            Select-Object -First 1
        $encoderParameters = [System.Drawing.Imaging.EncoderParameters]::new(1)
        $qualityParameter = [System.Drawing.Imaging.EncoderParameter]::new(
            [System.Drawing.Imaging.Encoder]::Quality,
            [long]92)
        try {
            $encoderParameters.Param[0] = $qualityParameter
            $runtimeBitmap.Save($runtimePath, $jpegCodec, $encoderParameters)
        }
        finally {
            $qualityParameter.Dispose()
            $encoderParameters.Dispose()
        }
    }
    finally {
        $runtimeBitmap.Dispose()
    }

    Write-Output "Source: $($sourceBitmap.Width)x$($sourceBitmap.Height)"
    Write-Output "Ashen Verge background: 2048x2048 -> $runtimePath"
}
finally {
    $sourceBitmap.Dispose()
}
