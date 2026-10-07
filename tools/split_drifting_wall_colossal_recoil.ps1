$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$sourcePath = Join-Path $modRoot "graphics\weapons\chief_navigator_drifting_wall_colossal_inert.png"
$basePath = Join-Path $modRoot "graphics\weapons\chief_navigator_drifting_wall_colossal_base_v2.png"
$barrelPath = Join-Path $modRoot "graphics\weapons\chief_navigator_drifting_wall_colossal_barrel_v2.png"

# The generated turret is already a clean, vertically authored sprite.  A
# horizontal cut at row 440 separates its long moving rail assembly from the
# fixed circular carriage without repainting or resampling either layer.
$splitRow = 440
$source = [System.Drawing.Bitmap]::FromFile($sourcePath)
$base = New-Object System.Drawing.Bitmap(
    $source.Width,
    $source.Height,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$barrel = New-Object System.Drawing.Bitmap(
    $source.Width,
    $source.Height,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

try {
    for ($y = 0; $y -lt $source.Height; $y++) {
        for ($x = 0; $x -lt $source.Width; $x++) {
            $pixel = $source.GetPixel($x, $y)
            if ($y -lt $splitRow) {
                $barrel.SetPixel($x, $y, $pixel)
            }
            else {
                $base.SetPixel($x, $y, $pixel)
            }
        }
    }

    $base.Save($basePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $barrel.Save($barrelPath, [System.Drawing.Imaging.ImageFormat]::Png)
}
finally {
    $source.Dispose()
    $base.Dispose()
    $barrel.Dispose()
}

Write-Host "WROTE $basePath"
Write-Host "WROTE $barrelPath"
