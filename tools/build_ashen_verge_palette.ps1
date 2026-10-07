param(
    [string]$Runtime =
        "graphics/terrain/chief_navigator_ashen_verge_nebula_v1.png",
    [string]$Map =
        "graphics/terrain/chief_navigator_ashen_verge_nebula_v1_map.png"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $root -Parent) -Parent
$coreTerrain = Join-Path $starsectorRoot "starsector-core\graphics\terrain"

function Blend-Channel {
    param([double]$From, [double]$To, [double]$Amount)
    return [int][Math]::Round($From + ($To - $From) * $Amount)
}

function Get-CoralColor {
    param([double]$Tone)

    $tone = [Math]::Max(0.0, [Math]::Min(1.0, $Tone))
    if ($tone -lt 0.42) {
        $amount = $tone / 0.42
        return [System.Drawing.Color]::FromArgb(
            (Blend-Channel 21 94 $amount),
            (Blend-Channel 15 38 $amount),
            (Blend-Channel 27 43 $amount))
    }
    if ($tone -lt 0.78) {
        $amount = ($tone - 0.42) / 0.36
        return [System.Drawing.Color]::FromArgb(
            (Blend-Channel 94 190 $amount),
            (Blend-Channel 38 75 $amount),
            (Blend-Channel 43 54 $amount))
    }
    $amount = ($tone - 0.78) / 0.22
    return [System.Drawing.Color]::FromArgb(
        (Blend-Channel 190 247 $amount),
        (Blend-Channel 75 139 $amount),
        (Blend-Channel 54 83 $amount))
}

function Convert-NebulaAtlas {
    param([string]$Source, [string]$Destination)

    $sourceBitmap = [System.Drawing.Bitmap]::FromFile($Source)
    try {
        $outputBitmap = [System.Drawing.Bitmap]::new(
            $sourceBitmap.Width,
            $sourceBitmap.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            for ($x = 0; $x -lt $sourceBitmap.Width; $x++) {
                for ($y = 0; $y -lt $sourceBitmap.Height; $y++) {
                    $pixel = $sourceBitmap.GetPixel($x, $y)
                    if ($pixel.A -eq 0) {
                        $outputBitmap.SetPixel(
                            $x, $y, [System.Drawing.Color]::Transparent)
                        continue
                    }
                    $luminance = (0.2126 * $pixel.R `
                            + 0.7152 * $pixel.G `
                            + 0.0722 * $pixel.B) / 255.0
                    $tone = [Math]::Pow([Math]::Max(
                            0.0,
                            [Math]::Min(1.0, ($luminance - 0.05) / 0.72)), 0.9)
                    $coral = Get-CoralColor $tone
                    $outputBitmap.SetPixel(
                        $x,
                        $y,
                        [System.Drawing.Color]::FromArgb(
                            $pixel.A,
                            $coral.R,
                            $coral.G,
                            $coral.B))
                }
            }
            $directory = Split-Path -Parent $Destination
            New-Item -ItemType Directory -Force -Path $directory | Out-Null
            $outputBitmap.Save(
                $Destination,
                [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $outputBitmap.Dispose()
        }
    }
    finally {
        $sourceBitmap.Dispose()
    }
}

$runtimePath = Join-Path $root $Runtime
$mapPath = Join-Path $root $Map
Convert-NebulaAtlas `
    (Join-Path $coreTerrain "nebula_amber.png") `
    $runtimePath
Convert-NebulaAtlas `
    (Join-Path $coreTerrain "nebula_amber_map.png") `
    $mapPath

Write-Output "Ashen Verge nebula atlas -> $runtimePath"
Write-Output "Ashen Verge nebula map atlas -> $mapPath"
