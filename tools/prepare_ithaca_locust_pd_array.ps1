param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$outputPath = Join-Path $PSScriptRoot `
    "..\graphics\weapons\chief_navigator_ithaca_locust_pd_array_v1.png"
$source = [System.Drawing.Bitmap]::FromFile($InputPath)
$output = New-Object System.Drawing.Bitmap(
    320,
    320,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($output)

try {
    $graphics.CompositingMode = `
        [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.CompositingQuality = `
        [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.InterpolationMode = `
        [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = `
        [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.SmoothingMode = `
        [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.DrawImage(
        $source,
        [System.Drawing.Rectangle]::new(0, 0, 320, 320),
        [System.Drawing.Rectangle]::new(0, 0, $source.Width, $source.Height),
        [System.Drawing.GraphicsUnit]::Pixel)
    $output.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $output.Dispose()
    $source.Dispose()
}

Write-Output $outputPath
