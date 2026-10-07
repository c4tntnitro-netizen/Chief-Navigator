$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$wallPath = Join-Path $projectRoot "graphics\combat\ithaca_wall_complete_mapheight.png"
$apronPath = Join-Path $projectRoot "graphics\combat\ithaca_wall_mounting_apron_v2.png"
$outputPath = Join-Path $projectRoot "graphics\combat\ithaca_wall_curved_core_v1.png"

$wall = [System.Drawing.Bitmap]::new([string]$wallPath)
$apron = [System.Drawing.Bitmap]::new([string]$apronPath)
try {
    if ($wall.Height -ne 8192 -or $apron.Height -ne 8192) {
        throw "Expected both source textures to be 8192 pixels long."
    }

    # Leave enough backing beneath the corners of the rotated 960px modules.
    $outputWidth = 1940
    $output = [System.Drawing.Bitmap]::new(
        $outputWidth,
        $wall.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($output)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

        $longitudinalCenter = 4096.0
        $moduleArcHalfSpan = 2790.0
        $apronStart = 480
        $deepestBackingEdge = 1900.0
        $arcRise = 350.0
        $minimumBackingEdge = 1100.0

        for ($y = 0; $y -lt $wall.Height; $y++) {
            $localX = $longitudinalCenter - $y
            $normalized = $localX / $moduleArcHalfSpan
            $backingEdge = $deepestBackingEdge - $arcRise * $normalized * $normalized
            $backingEdge = [Math]::Max($minimumBackingEdge, [Math]::Min($deepestBackingEdge, $backingEdge))
            $rowWidth = [int][Math]::Round($backingEdge - $apronStart)
            $sourceRow = [System.Drawing.Rectangle]::new(0, $y, $apron.Width, 1)
            $destinationRow = [System.Drawing.Rectangle]::new($apronStart, $y, $rowWidth, 1)
            $graphics.DrawImage($apron, $destinationRow, $sourceRow, [System.Drawing.GraphicsUnit]::Pixel)
        }

        # Draw the original bright wall last so its pixels and seam remain exact.
        $graphics.DrawImageUnscaled($wall, 0, 0)
    } finally {
        $graphics.Dispose()
    }

    try {
        $output.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $output.Dispose()
    }
} finally {
    $wall.Dispose()
    $apron.Dispose()
}

Write-Output "Generated $outputPath"
