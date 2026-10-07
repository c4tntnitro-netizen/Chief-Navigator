param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,
    [string]$SourceRelativePath = 'graphics\source\chief_navigator_drifting_wall_annihilator_launcher_chroma.png',
    [string]$WeaponRelativePath = 'graphics\weapons\chief_navigator_drifting_wall_annihilator_launcher.png',
    [string]$ReferencePath = '',
    [double]$TargetMax = 210.0
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path -Parent $PSScriptRoot
$sourceOutput = Join-Path $modRoot $SourceRelativePath
$weaponOutput = Join-Path $modRoot $WeaponRelativePath

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $sourceOutput) | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $weaponOutput) | Out-Null
Copy-Item -LiteralPath $InputPath -Destination $sourceOutput -Force

$original = [System.Drawing.Bitmap]::FromFile($InputPath)
try {
    $rgba = [System.Drawing.Bitmap]::new(
        $original.Width,
        $original.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($rgba)
        try {
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.DrawImageUnscaled($original, 0, 0)
        }
        finally {
            $graphics.Dispose()
        }

        $key = $rgba.GetPixel(0, 0)
        $rect = New-Object System.Drawing.Rectangle(0, 0, $rgba.Width, $rgba.Height)
        $data = $rgba.LockBits(
            $rect,
            [System.Drawing.Imaging.ImageLockMode]::ReadWrite,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $length = [Math]::Abs($data.Stride) * $rgba.Height
            $pixels = New-Object byte[] $length
            [Runtime.InteropServices.Marshal]::Copy($data.Scan0, $pixels, 0, $length)

            $minX = $rgba.Width
            $minY = $rgba.Height
            $maxX = -1
            $maxY = -1
            for ($y = 0; $y -lt $rgba.Height; $y++) {
                $row = $y * $data.Stride
                for ($x = 0; $x -lt $rgba.Width; $x++) {
                    $index = $row + ($x * 4)
                    $blue = [double]$pixels[$index]
                    $green = [double]$pixels[$index + 1]
                    $red = [double]$pixels[$index + 2]

                    # The generator's chroma field is nominally #ff00ff but has a
                    # slight brightness gradient. Classify by magenta dominance,
                    # not exact RGB distance, so the whole matte disappears.
                    $minimumMagentaChannel = [Math]::Min($red, $blue)
                    $magentaDominance = $minimumMagentaChannel - $green
                    if (($minimumMagentaChannel -lt 60.0) -or ($magentaDominance -le 20.0)) {
                        $alpha = 1.0
                    }
                    elseif ($magentaDominance -ge 120.0) {
                        $alpha = 0.0
                    }
                    else {
                        $matte = ($magentaDominance - 20.0) / 100.0
                        $matte = $matte * $matte * (3.0 - (2.0 * $matte))
                        $alpha = 1.0 - $matte
                    }

                    if (($alpha -gt 0.0) -and ($alpha -lt 1.0)) {
                        $invAlpha = 1.0 - $alpha
                        $red = [Math]::Max(0.0, [Math]::Min(255.0, ($red - ($invAlpha * $key.R)) / $alpha))
                        $green = [Math]::Max(0.0, [Math]::Min(255.0, ($green - ($invAlpha * $key.G)) / $alpha))
                        $blue = [Math]::Max(0.0, [Math]::Min(255.0, ($blue - ($invAlpha * $key.B)) / $alpha))
                    }

                    $alphaByte = [byte][Math]::Round(255.0 * $alpha)
                    if ($alphaByte -eq 0) {
                        $red = 0.0
                        $green = 0.0
                        $blue = 0.0
                    }
                    $pixels[$index] = [byte][Math]::Round($blue)
                    $pixels[$index + 1] = [byte][Math]::Round($green)
                    $pixels[$index + 2] = [byte][Math]::Round($red)
                    $pixels[$index + 3] = $alphaByte

                    if ($alphaByte -gt 10) {
                        if ($x -lt $minX) { $minX = $x }
                        if ($y -lt $minY) { $minY = $y }
                        if ($x -gt $maxX) { $maxX = $x }
                        if ($y -gt $maxY) { $maxY = $y }
                    }
                }
            }

            [Runtime.InteropServices.Marshal]::Copy($pixels, 0, $data.Scan0, $length)
        }
        finally {
            $rgba.UnlockBits($data)
        }

        if (($maxX -lt $minX) -or ($maxY -lt $minY)) {
            throw 'The chroma-key pass did not find any launcher pixels.'
        }

        $sourcePadding = 14
        $cropX = [Math]::Max(0, $minX - $sourcePadding)
        $cropY = [Math]::Max(0, $minY - $sourcePadding)
        $cropRight = [Math]::Min($rgba.Width - 1, $maxX + $sourcePadding)
        $cropBottom = [Math]::Min($rgba.Height - 1, $maxY + $sourcePadding)
        $cropWidth = $cropRight - $cropX + 1
        $cropHeight = $cropBottom - $cropY + 1
        $cropRect = [System.Drawing.Rectangle]::new(
            [int]$cropX,
            [int]$cropY,
            [int]$cropWidth,
            [int]$cropHeight
        )

        $reference = $null
        if ($ReferencePath) {
            $reference = [System.Drawing.Bitmap]::FromFile($ReferencePath)
            $referenceMinX = $reference.Width
            $referenceMinY = $reference.Height
            $referenceMaxX = -1
            $referenceMaxY = -1
            for ($referenceY = 0; $referenceY -lt $reference.Height; $referenceY++) {
                for ($referenceX = 0; $referenceX -lt $reference.Width; $referenceX++) {
                    if ($reference.GetPixel($referenceX, $referenceY).A -gt 10) {
                        if ($referenceX -lt $referenceMinX) { $referenceMinX = $referenceX }
                        if ($referenceY -lt $referenceMinY) { $referenceMinY = $referenceY }
                        if ($referenceX -gt $referenceMaxX) { $referenceMaxX = $referenceX }
                        if ($referenceY -gt $referenceMaxY) { $referenceMaxY = $referenceY }
                    }
                }
            }
            if (($referenceMaxX -lt $referenceMinX) -or ($referenceMaxY -lt $referenceMinY)) {
                throw "Reference sprite has no visible pixels: $ReferencePath"
            }
            $targetWidth = $reference.Width
            $targetHeight = $reference.Height
            $destination = [System.Drawing.Rectangle]::new(
                $referenceMinX,
                $referenceMinY,
                $referenceMaxX - $referenceMinX + 1,
                $referenceMaxY - $referenceMinY + 1
            )
        }
        else {
            $scale = $TargetMax / [Math]::Max($cropRect.Width, $cropRect.Height)
            $targetWidth = [Math]::Max(1, [int][Math]::Round($cropRect.Width * $scale))
            $targetHeight = [Math]::Max(1, [int][Math]::Round($cropRect.Height * $scale))
            $destination = [System.Drawing.Rectangle]::new(0, 0, $targetWidth, $targetHeight)
        }
        $result = [System.Drawing.Bitmap]::new(
            $targetWidth,
            $targetHeight,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($result)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.DrawImage($rgba, $destination, $cropRect, [System.Drawing.GraphicsUnit]::Pixel)
            }
            finally {
                $graphics.Dispose()
            }

            $result.Save($weaponOutput, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $result.Dispose()
            if ($null -ne $reference) { $reference.Dispose() }
        }
    }
    finally {
        $rgba.Dispose()
    }
}
finally {
    $original.Dispose()
}

Write-Output "Source: $sourceOutput"
Write-Output "Weapon: $weaponOutput"
Write-Output "Size: ${targetWidth}x${targetHeight}"
