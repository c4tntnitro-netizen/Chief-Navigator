param(
    [string]$Output =
        "graphics/terrain/chief_navigator_ashen_verge_nebula_mask.png"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $root $Output
$outputDirectory = Split-Path -Parent $outputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$width = 64
$height = 48
$tileSize = 400.0
$bitmap = [System.Drawing.Bitmap]::new(
    $width,
    $height,
    [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)

try {
    for ($py = 0; $py -lt $height; $py++) {
        for ($px = 0; $px -lt $width; $px++) {
            $worldX = ($px + 0.5 - $width * 0.5) * $tileSize
            $worldY = (($height - 1 - $py) + 0.5 - $height * 0.5) `
                * $tileSize
            $radius = [Math]::Sqrt($worldX * $worldX + $worldY * $worldY)

            # Keep the primary, Ithaca, and the western final-Labor approach
            # free of campaign terrain while clouds frame the outer system.
            $centerClear = $radius -lt 3200.0
            $ithacaDx = $worldX - 4500.0
            $ithacaClear = ($ithacaDx * $ithacaDx `
                    + $worldY * $worldY) -lt (3500.0 * 3500.0)
            $westernLaneClear = $worldX -lt 5000.0 `
                -and [Math]::Abs($worldY) -lt 1500.0

            $angle = [Math]::Atan2($worldY, $worldX)
            $field = [Math]::Sin($worldX / 1900.0) `
                + 0.8 * [Math]::Cos($worldY / 1500.0) `
                + 0.7 * [Math]::Sin(($worldX + $worldY) / 2500.0) `
                + 0.75 * [Math]::Cos(3.0 * $angle - $radius / 2100.0)
            $outerBias = [Math]::Max(0.0, [Math]::Min(
                    1.0,
                    ($radius - 3600.0) / 7000.0))
            $cloud = $radius -lt 12600.0 `
                -and ($field + 1.35 * $outerBias) -gt 0.55

            if ($centerClear -or $ithacaClear -or $westernLaneClear) {
                $cloud = $false
            }
            $bitmap.SetPixel(
                $px,
                $py,
                $(if ($cloud) {
                    [System.Drawing.Color]::White
                } else {
                    [System.Drawing.Color]::Black
                }))
        }
    }

    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Ashen Verge nebula mask: ${width}x${height} -> $outputPath"
}
finally {
    $bitmap.Dispose()
}
