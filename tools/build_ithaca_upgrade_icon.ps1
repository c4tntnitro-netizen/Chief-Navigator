$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$icons = @(
    @{
        Source = "graphics\source\chief_navigator_ithaca_macroservos_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_macroservos_v1.png"
    },
    @{
        Source = "graphics\source\chief_navigator_ithaca_naval_cycler_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_naval_cycler_v1.png"
    },
    @{
        Source = "graphics\source\chief_navigator_ithaca_arc_projector_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_arc_projector_v1.png"
    },
    @{
        Source = "graphics\source\chief_navigator_ithaca_intercept_matrix_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_intercept_matrix_v1.png"
    },
    @{
        Source = "graphics\source\chief_navigator_ithaca_munitions_compiler_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_munitions_compiler_v1.png"
    },
    @{
        Source = "graphics\source\chief_navigator_ithaca_autoloader_core_raw_v1.png"
        Output = "graphics\icons\cargo\chief_navigator_ithaca_autoloader_core_v1.png"
    }
)

foreach ($icon in $icons) {
    $sourcePath = Join-Path $modRoot $icon.Source
    $outputPath = Join-Path $modRoot $icon.Output
    $outputDirectory = Split-Path $outputPath -Parent
    if (!(Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    try {
        $left = $source.Width
        $top = $source.Height
        $right = -1
        $bottom = -1
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                if ($source.GetPixel($x, $y).A -le 8) { continue }
                if ($x -lt $left) { $left = $x }
                if ($x -gt $right) { $right = $x }
                if ($y -lt $top) { $top = $y }
                if ($y -gt $bottom) { $bottom = $y }
            }
        }
        if ($right -lt $left -or $bottom -lt $top) {
            throw "$sourcePath contains no visible pixels."
        }

        $visibleWidth = $right - $left + 1
        $visibleHeight = $bottom - $top + 1
        $padding = [int][Math]::Ceiling(
            [Math]::Max($visibleWidth, $visibleHeight) * 0.055)
        $squareSize = [Math]::Max($visibleWidth, $visibleHeight) + 2 * $padding
        $square = [System.Drawing.Bitmap]::new(
            $squareSize,
            $squareSize,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $squareGraphics = [System.Drawing.Graphics]::FromImage($square)
            try {
                $squareGraphics.Clear([System.Drawing.Color]::Transparent)
                $x = [int](($squareSize - $visibleWidth) / 2)
                $y = [int](($squareSize - $visibleHeight) / 2)
                $sourceRect = [System.Drawing.Rectangle]::new(
                    $left, $top, $visibleWidth, $visibleHeight)
                $destinationRect = [System.Drawing.Rectangle]::new(
                    $x, $y, $visibleWidth, $visibleHeight)
                $squareGraphics.DrawImage(
                    $source,
                    $destinationRect,
                    $sourceRect,
                    [System.Drawing.GraphicsUnit]::Pixel)
            } finally {
                $squareGraphics.Dispose()
            }

            $output = [System.Drawing.Bitmap]::new(
                80,
                80,
                [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($output)
                try {
                    $graphics.Clear([System.Drawing.Color]::Transparent)
                    $graphics.CompositingMode =
                        [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                    $graphics.CompositingQuality =
                        [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                    $graphics.InterpolationMode =
                        [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                    $graphics.PixelOffsetMode =
                        [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                    $graphics.SmoothingMode =
                        [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                    $graphics.DrawImage(
                        $square,
                        [System.Drawing.Rectangle]::new(0, 0, 80, 80),
                        [System.Drawing.Rectangle]::new(
                            0, 0, $square.Width, $square.Height),
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
            $square.Dispose()
        }
    } finally {
        $source.Dispose()
    }

    Write-Host "Built $outputPath"
}
