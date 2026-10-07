param(
    [Parameter(Mandatory = $true)]
    [string] $InputPath,

    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$source = [System.Drawing.Bitmap]::FromFile($InputPath)
$matte = New-Object System.Drawing.Bitmap(
    $source.Width,
    $source.Height,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

$minX = $source.Width
$minY = $source.Height
$maxX = -1
$maxY = -1

try {
    for ($y = 0; $y -lt $source.Height; $y++) {
        for ($x = 0; $x -lt $source.Width; $x++) {
            $pixel = $source.GetPixel($x, $y)
            # The generated key is a mildly uneven magenta field. Classify by
            # magenta dominance instead of distance from a single corner pixel.
            $magenta = [Math]::Min(
                ([double]$pixel.R - $pixel.G),
                ([double]$pixel.B - $pixel.G))

            if ($magenta -ge 72.0) {
                $alpha = 0
            } elseif ($magenta -le 24.0) {
                $alpha = 255
            } else {
                $alpha = [int](255.0 * ((72.0 - $magenta) / 48.0))
            }

            if ($alpha -gt 0) {
                $despill = 1.0 - (0.45 * (1.0 - ($alpha / 255.0)))
                $red = [int][Math]::Max(0, [Math]::Min(255, $pixel.R * $despill))
                $blue = [int][Math]::Max(0, [Math]::Min(255, $pixel.B * $despill))
                $matte.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $red, $pixel.G, $blue))
                if ($alpha -ge 16) {
                    $minX = [Math]::Min($minX, $x)
                    $minY = [Math]::Min($minY, $y)
                    $maxX = [Math]::Max($maxX, $x)
                    $maxY = [Math]::Max($maxY, $y)
                }
            } else {
                $matte.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
    }

    if ($maxX -lt $minX -or $maxY -lt $minY) {
        throw "No opaque subject remained after chroma removal."
    }

    $padding = 12
    $left = [Math]::Max(0, $minX - $padding)
    $top = [Math]::Max(0, $minY - $padding)
    $right = [Math]::Min($matte.Width - 1, $maxX + $padding)
    $bottom = [Math]::Min($matte.Height - 1, $maxY + $padding)
    $crop = New-Object System.Drawing.Rectangle(
        $left,
        $top,
        ($right - $left + 1),
        ($bottom - $top + 1))
    $final = $matte.Clone($crop, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $parent = Split-Path $OutputPath -Parent
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
        $final.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $final.Dispose()
    }
} finally {
    $matte.Dispose()
    $source.Dispose()
}

Write-Host "BERTH SPRITE OK -> $OutputPath"
