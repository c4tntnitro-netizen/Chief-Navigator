$ErrorActionPreference = 'Stop'

$modRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $modRoot 'graphics\source'
$shipRoot = Join-Path $modRoot 'graphics\ships'
$chromaHalf = Join-Path $sourceRoot `
    'chief_navigator_drifting_wall_module_half_destroyed_v1_chroma.png'
$cleanHalf = Join-Path $sourceRoot `
    'chief_navigator_drifting_wall_module_half_destroyed_v1_clean.png'
$cleanFull = Join-Path $sourceRoot `
    'chief_navigator_drifting_wall_base_v1_clean.png'
$finalHalf = Join-Path $shipRoot `
    'chief_navigator_drifting_wall_module_half_destroyed_v1.png'
$finalBase = Join-Path $shipRoot `
    'chief_navigator_drifting_wall_base_v1.png'

& (Join-Path $PSScriptRoot 'remove_magenta_chroma.ps1') `
    -InputPath $chromaHalf `
    -OutputPath $cleanHalf

Add-Type -AssemblyName System.Drawing

function Save-NearestDouble {
    param(
        [Parameter(Mandatory = $true)]
        [System.Drawing.Bitmap]$Source,

        [Parameter(Mandatory = $true)]
        [string]$OutputPath
    )

    $output = New-Object System.Drawing.Bitmap(
        ($Source.Width * 2),
        ($Source.Height * 2),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($output)
        try {
            $graphics.CompositingMode = `
                [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.CompositingQuality = `
                [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
            $graphics.InterpolationMode = `
                [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = `
                [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.SmoothingMode = `
                [System.Drawing.Drawing2D.SmoothingMode]::None
            $graphics.DrawImage(
                $Source,
                [System.Drawing.Rectangle]::new(
                    0,
                    0,
                    $output.Width,
                    $output.Height),
                0,
                0,
                $Source.Width,
                $Source.Height,
                [System.Drawing.GraphicsUnit]::Pixel)
        } finally {
            $graphics.Dispose()
        }
        $output.Save(
            $OutputPath,
            [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $output.Dispose()
    }
}

$half = [System.Drawing.Bitmap]::FromFile($cleanHalf)
try {
    Save-NearestDouble -Source $half -OutputPath $finalHalf

    # The input is the left half. Its clean right edge is the station's
    # centerline. Remove only fully transparent columns beyond that edge so
    # the exact mirror does not create a false vertical gap at the join.
    $rightmostOpaque = -1
    for ($x = $half.Width - 1; $x -ge 0 -and $rightmostOpaque -lt 0; $x--) {
        for ($y = 0; $y -lt $half.Height; $y++) {
            if ($half.GetPixel($x, $y).A -gt 8) {
                $rightmostOpaque = $x
                break
            }
        }
    }
    if ($rightmostOpaque -lt 0) {
        throw 'The damaged half contains no opaque pixels.'
    }

    $joinHalf = $half.Clone(
        [System.Drawing.Rectangle]::new(
            0,
            0,
            ($rightmostOpaque + 1),
            $half.Height),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $mirror = $joinHalf.Clone()
        try {
        $mirror.RotateFlip([System.Drawing.RotateFlipType]::RotateNoneFlipX)
        $full = New-Object System.Drawing.Bitmap(
            ($joinHalf.Width * 2),
            $joinHalf.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($full)
            try {
                $graphics.CompositingMode = `
                    [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImageUnscaled($joinHalf, 0, 0)
                $graphics.DrawImageUnscaled($mirror, $joinHalf.Width, 0)
            } finally {
                $graphics.Dispose()
            }
            $full.Save(
                $cleanFull,
                [System.Drawing.Imaging.ImageFormat]::Png)
            Save-NearestDouble -Source $full -OutputPath $finalBase
        } finally {
            $full.Dispose()
        }
        } finally {
            $mirror.Dispose()
        }
    } finally {
        $joinHalf.Dispose()
    }
} finally {
    $half.Dispose()
}

Write-Output "WROTE $finalHalf"
Write-Output "WROTE $finalBase"
