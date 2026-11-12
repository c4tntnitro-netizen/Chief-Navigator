param([string]$Output = 'docs/assets/ithaca_armed_preview_uncapped.png')

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$hull = Get-Content -Raw -LiteralPath (Join-Path $root 'data/hulls/chief_navigator_ithaca_foundation.ship') | ConvertFrom-Json
$skin = Get-Content -Raw -LiteralPath (Join-Path $root 'data/hulls/skins/chief_navigator_ithaca_intact_base.skin') | ConvertFrom-Json
$variant = Get-Content -Raw -LiteralPath (Join-Path $root 'data/variants/chief_navigator_ithaca_intact_base_Standard.variant') | ConvertFrom-Json

function Draw-CenteredSprite {
    param($Graphics, [string]$Path, [single]$X, [single]$Y,
        [single]$Angle, [single]$Width = 0, [single]$Height = 0)
    $sprite = [System.Drawing.Bitmap]::FromFile($Path)
    $state = $Graphics.Save()
    try {
        if ($Width -eq 0) { $Width = $sprite.Width }
        if ($Height -eq 0) { $Height = $sprite.Height }
        $Graphics.TranslateTransform($X, $Y)
        # Engine-local positive angles turn left in the upright hull PNG.
        $Graphics.RotateTransform(-$Angle)
        $rect = [System.Drawing.RectangleF]::new(-$Width / 2, -$Height / 2, $Width, $Height)
        # Preserve complete weapon canvases: the naval gun's transparent
        # lower margin is intentional pivot correction, not removable padding.
        $Graphics.DrawImage($sprite, $rect)
    } finally {
        $Graphics.Restore($state)
        $sprite.Dispose()
    }
}

$equipped = @{}
foreach ($group in $variant.weaponGroups) {
    foreach ($weapon in $group.weapons.PSObject.Properties) {
        $equipped[$weapon.Name] = [string]$weapon.Value
    }
}

$padding = 32
$canvas = [System.Drawing.Bitmap]::new(
    ([int]$hull.width + 2 * $padding), ([int]$hull.height + 2 * $padding),
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($canvas)
$placements = @()
try {
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.PageUnit = [System.Drawing.GraphicsUnit]::Pixel
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $base = [System.Drawing.Bitmap]::FromFile((Join-Path $root $skin.spriteName))
    try {
        # The hull PNG and weapon PNGs have different DPI metadata. Explicit
        # source/destination pixel rectangles avoid physical-size scaling.
        $graphics.DrawImage($base,
            [System.Drawing.Rectangle]::new($padding, $padding, $base.Width, $base.Height),
            [System.Drawing.Rectangle]::new(0, 0, $base.Width, $base.Height),
            [System.Drawing.GraphicsUnit]::Pixel)
    }
    finally { $base.Dispose() }

    foreach ($slot in $hull.weaponSlots) {
        if (-not $equipped.ContainsKey($slot.id)) { continue }
        $weaponId = $equipped[$slot.id]
        $spec = Get-Content -Raw -LiteralPath (Join-Path $root "data/weapons/$weaponId.wpn")
        $spriteField = if ($slot.mount -eq 'HARDPOINT') { 'hardpointSprite' } else { 'turretSprite' }
        $spriteMatch = [regex]::Match($spec, '"' + $spriteField + '"\s*:\s*"([^"]+)"')
        if (-not $spriteMatch.Success) { throw "Missing $spriteField for $weaponId" }
        $x = $hull.center[0] - $slot.locations[1]
        $y = $hull.height - $hull.center[1] - $slot.locations[0]
        $placements += [pscustomobject]@{
            Slot = $slot.id
            Weapon = $weaponId
            Sprite = $spriteMatch.Groups[1].Value
            X = $x
            Y = $y
            Angle = $slot.angle
            Layer = if ($spec -match '"renderBelowAllWeapons"\s*:\s*true') { 0 } else { 1 }
        }
    }
    if ($placements.Count -ne 12) { throw "Expected twelve current Ithaca weapons; found $($placements.Count)." }
    foreach ($mount in ($placements | Sort-Object Layer, Slot)) {
        Draw-CenteredSprite $graphics (Join-Path $root $mount.Sprite) `
            ($mount.X + $padding) ($mount.Y + $padding) $mount.Angle
    }
    $outputPath = Join-Path $root $Output
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $outputPath)) | Out-Null
    $canvas.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $placements | ConvertTo-Json | Set-Content -LiteralPath ([System.IO.Path]::ChangeExtension($outputPath, '.json'))
    Write-Output "WROTE $outputPath ($($canvas.Width)x$($canvas.Height)); twelve weapons, reactor omitted from preview."
} finally {
    $graphics.Dispose()
    $canvas.Dispose()
}
