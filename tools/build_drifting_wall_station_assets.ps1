param(
    [Parameter(Mandatory = $true)]
    [string] $FrameBerthPath,

    [Parameter(Mandatory = $true)]
    [string] $InsertSourcePath,

    [Parameter(Mandatory = $true)]
    [string] $FrameOutputPath,

    [Parameter(Mandatory = $true)]
    [string] $InsertOutputPath,

    [string] $PreviewOutputPath
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function New-TransparentBitmap([int] $width, [int] $height) {
    return New-Object System.Drawing.Bitmap(
        $width,
        $height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Configure-Graphics([System.Drawing.Graphics] $graphics) {
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
}

$frameSource = [System.Drawing.Bitmap]::FromFile($FrameBerthPath)
$insertSource = [System.Drawing.Bitmap]::FromFile($InsertSourcePath)

try {
    $frameWidth = 780
    $frameHeight = 980
    $frame = New-TransparentBitmap $frameWidth $frameHeight
    $frameGraphics = [System.Drawing.Graphics]::FromImage($frame)
    try {
        Configure-Graphics $frameGraphics
        $frameGraphics.Clear([System.Drawing.Color]::Transparent)
        $frameGraphics.DrawImage($frameSource, 0, 0, $frameWidth, $frameHeight)
    } finally {
        $frameGraphics.Dispose()
    }

    $insertWidth = 360
    $insertHeight = 610
    $insert = New-TransparentBitmap $insertWidth $insertHeight
    $insertGraphics = [System.Drawing.Graphics]::FromImage($insert)
    try {
        Configure-Graphics $insertGraphics
        $insertGraphics.Clear([System.Drawing.Color]::Transparent)
        $insertGraphics.DrawImage($insertSource, 0, 0, $insertWidth, $insertHeight)
    } finally {
        $insertGraphics.Dispose()
    }

    $canvasWidth = 4500
    $canvasHeight = 1800
    $originX = 2250
    $originY = 600
    $worldFrame = New-TransparentBitmap $canvasWidth $canvasHeight
    $worldGraphics = [System.Drawing.Graphics]::FromImage($worldFrame)
    try {
        Configure-Graphics $worldGraphics
        $worldGraphics.Clear([System.Drawing.Color]::Transparent)

        $slots = @(
            @{ X = -1620; Y = -240; Curve = 9 },
            @{ X = -1080; Y = -400; Curve = 6 },
            @{ X =  -540; Y = -500; Curve = 3 },
            @{ X =     0; Y = -540; Curve = 0 },
            @{ X =   540; Y = -500; Curve = -3 },
            @{ X =  1080; Y = -400; Curve = -6 },
            @{ X =  1620; Y = -240; Curve = -9 }
        )

        # Draw outer berths first and the center last so overlap reads as a
        # continuous load-bearing frame rather than a row of independent ships.
        foreach ($index in @(0, 6, 1, 5, 2, 4, 3)) {
            $slot = $slots[$index]
            $centerX = $originX + $slot.X
            $centerY = $originY - $slot.Y
            $worldGraphics.TranslateTransform($centerX, $centerY)
            $worldGraphics.RotateTransform(180 + $slot.Curve)
            $worldGraphics.DrawImage(
                $frame,
                [int](-$frameWidth / 2),
                [int](-$frameHeight / 2),
                $frameWidth,
                $frameHeight)
            $worldGraphics.ResetTransform()
        }
    } finally {
        $worldGraphics.Dispose()
        $frame.Dispose()
    }

    if ($PreviewOutputPath) {
        $preview = $worldFrame.Clone()
        $previewGraphics = [System.Drawing.Graphics]::FromImage($preview)
        try {
            Configure-Graphics $previewGraphics
            foreach ($index in 0..6) {
                $slot = $slots[$index]
                $centerX = $originX + $slot.X
                # Starsector seats child station modules 40 world units lower
                # than the frame compositor's nominal socket center.
                $centerY = $originY - ($slot.Y + 40)
                $previewGraphics.TranslateTransform($centerX, $centerY)
                $previewGraphics.RotateTransform(180 + $slot.Curve)
                $previewGraphics.DrawImage(
                    $insert,
                    [int](-$insertWidth / 2),
                    [int](-$insertHeight / 2),
                    $insertWidth,
                    $insertHeight)
                $previewGraphics.ResetTransform()
            }
        } finally {
            $previewGraphics.Dispose()
        }
        New-Item -ItemType Directory -Force -Path (Split-Path $PreviewOutputPath -Parent) | Out-Null
        $preview.Save($PreviewOutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $preview.Dispose()
        Write-Host "DRIFTING WALL PREVIEW OK -> $PreviewOutputPath"
    }

    # Combat facing 0 rotates conventional top-forward sprites 90 degrees.
    # Pre-rotate the composite counter-clockwise so it renders horizontally
    # while the attachment coordinates remain in their tested orientation.
    $worldFrame.RotateFlip([System.Drawing.RotateFlipType]::Rotate270FlipNone)

    foreach ($path in @($FrameOutputPath, $InsertOutputPath)) {
        New-Item -ItemType Directory -Force -Path (Split-Path $path -Parent) | Out-Null
    }
    $worldFrame.Save($FrameOutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $insert.Save($InsertOutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    Write-Host "DRIFTING WALL FRAME OK -> $FrameOutputPath"
    Write-Host "DRIFTING WALL INSERT OK -> $InsertOutputPath"
    Write-Host "Core sprite metadata: width=1800 height=4500 center=[600,2250]"
    Write-Host "Insert sprite metadata: width=360 height=610 center=[180,305]"
} finally {
    if ($worldFrame) { $worldFrame.Dispose() }
    if ($insert) { $insert.Dispose() }
    $frameSource.Dispose()
    $insertSource.Dispose()
}
