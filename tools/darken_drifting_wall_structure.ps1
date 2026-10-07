param(
    [Parameter(Mandatory = $true)]
    [string] $FrameInputPath,

    [Parameter(Mandatory = $true)]
    [string] $FrameOutputPath,

    [Parameter(Mandatory = $true)]
    [string] $IslandInputPath,

    [Parameter(Mandatory = $true)]
    [string] $IslandOutputPath,

    [ValidateRange(0.0, 1.0)]
    [float] $Brightness = 0.62
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Write-DarkenedSprite(
        [string] $InputPath,
        [string] $OutputPath,
        [float] $Multiplier) {
    $source = [System.Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $InputPath).Path)
    try {
        $output = New-Object System.Drawing.Bitmap(
            $source.Width,
            $source.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($output)
            $attributes = New-Object System.Drawing.Imaging.ImageAttributes
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.CompositingMode =
                        [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $matrix = New-Object System.Drawing.Imaging.ColorMatrix(, @(
                    [single[]]@($Multiplier, 0, 0, 0, 0),
                    [single[]]@(0, $Multiplier, 0, 0, 0),
                    [single[]]@(0, 0, $Multiplier, 0, 0),
                    [single[]]@(0, 0, 0, 1, 0),
                    [single[]]@(0, 0, 0, 0, 1)
                ))
                $attributes.SetColorMatrix($matrix)
                $destination = New-Object System.Drawing.Rectangle(
                    0, 0, $source.Width, $source.Height)
                $graphics.DrawImage(
                    $source,
                    $destination,
                    0,
                    0,
                    $source.Width,
                    $source.Height,
                    [System.Drawing.GraphicsUnit]::Pixel,
                    $attributes)
            } finally {
                if ($attributes) { $attributes.Dispose() }
                if ($graphics) { $graphics.Dispose() }
            }

            $directory = Split-Path -Parent $OutputPath
            if ($directory) {
                New-Item -ItemType Directory -Force -Path $directory | Out-Null
            }
            $output.Save(
                [System.IO.Path]::GetFullPath($OutputPath),
                [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $output.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

Write-DarkenedSprite $FrameInputPath $FrameOutputPath $Brightness
Write-DarkenedSprite $IslandInputPath $IslandOutputPath $Brightness

Write-Host "DRIFTING WALL STRUCTURE DARKENED -> $FrameOutputPath"
Write-Host "DRIFTING WALL ISLAND DARKENED -> $IslandOutputPath"
Write-Host "RGB brightness multiplier: $Brightness"
