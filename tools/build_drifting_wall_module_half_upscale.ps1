$ErrorActionPreference = 'Stop'

$modRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $modRoot 'graphics\source'
$shipRoot = Join-Path $modRoot 'graphics\ships'
$chromaPath = Join-Path $sourceRoot `
    'chief_navigator_drifting_wall_module_half_upscaled_v1_chroma.png'
$cleanPath = Join-Path $sourceRoot `
    'chief_navigator_drifting_wall_module_half_upscaled_v1_clean.png'
$outputPath = Join-Path $shipRoot `
    'chief_navigator_drifting_wall_module_half_upscaled_v1.png'

& (Join-Path $PSScriptRoot 'remove_magenta_chroma.ps1') `
    -InputPath $chromaPath `
    -OutputPath $cleanPath

Add-Type -AssemblyName System.Drawing

$source = [System.Drawing.Bitmap]::FromFile($cleanPath)
try {
    $output = New-Object System.Drawing.Bitmap(
        ($source.Width * 2),
        ($source.Height * 2),
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
                $source,
                [System.Drawing.Rectangle]::new(
                    0,
                    0,
                    $output.Width,
                    $output.Height),
                0,
                0,
                $source.Width,
                $source.Height,
                [System.Drawing.GraphicsUnit]::Pixel)
        } finally {
            $graphics.Dispose()
        }

        $output.Save(
            $outputPath,
            [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $output.Dispose()
    }
} finally {
    $source.Dispose()
}

Write-Output "WROTE $outputPath"
