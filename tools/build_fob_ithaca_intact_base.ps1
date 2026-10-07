$ErrorActionPreference = 'Stop'

$modRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $modRoot `
    'graphics\ships\chief_navigator_ithaca_module_half_teal_v1.png'
$output = Join-Path $modRoot `
    'graphics\ships\chief_navigator_ithaca_intact_base_v1.png'

Add-Type -AssemblyName System.Drawing

$sourceBitmap = [System.Drawing.Bitmap]::FromFile($source)
try {
    # The approved half contains a 16-pixel centerline overlap and three
    # transparent padding rows at each vertical edge. Removing only that
    # padding yields the Drifting Wall foundation's established 5632x2216
    # combat canvas without resampling or inventing geometry.
    $halfWidth = 2816
    $topCrop = 3
    $height = 2216
    if ($sourceBitmap.Width -lt $halfWidth `
            -or $sourceBitmap.Height -lt ($topCrop + $height)) {
        throw 'The approved Ithaca half is smaller than the expected canvas.'
    }

    $left = $sourceBitmap.Clone(
        [System.Drawing.Rectangle]::new(
            0,
            $topCrop,
            $halfWidth,
            $height),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $right = $left.Clone()
        try {
            $right.RotateFlip(
                [System.Drawing.RotateFlipType]::RotateNoneFlipX)
            $full = New-Object System.Drawing.Bitmap(
                ($halfWidth * 2),
                $height,
                [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($full)
                try {
                    $graphics.CompositingMode = `
                        [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                    $graphics.DrawImageUnscaled($left, 0, 0)
                    $graphics.DrawImageUnscaled($right, $halfWidth, 0)
                } finally {
                    $graphics.Dispose()
                }
                $full.Save(
                    $output,
                    [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $full.Dispose()
            }
        } finally {
            $right.Dispose()
        }
    } finally {
        $left.Dispose()
    }
} finally {
    $sourceBitmap.Dispose()
}

Write-Output "WROTE $output (5632x2216)"
