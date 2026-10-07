$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$hullPath = Join-Path $modRoot "data\hulls\chief_navigator_scylla_base.ship"
$designPath = Join-Path $modRoot "graphics\ships\odyssey_bosses\gautama.ship"
$hull = Get-Content -LiteralPath $hullPath -Raw | ConvertFrom-Json
$design = Get-Content -LiteralPath $designPath -Raw | ConvertFrom-Json
$spritePath = Join-Path $modRoot ($hull.spriteName -replace "/", "\")
$sprite = [System.Drawing.Bitmap]::FromFile($spritePath)

try {
    $failed = @()
    # The editor export supplies the geometry; use white vanilla THREAT flares.
    # Do not reintroduce the older fourteen scaled Fabricator exhausts.
    foreach ($field in @("width", "height", "center", "bounds", "spriteName")) {
        if (($hull.$field | ConvertTo-Json -Depth 12 -Compress) -ne
                ($design.$field | ConvertTo-Json -Depth 12 -Compress)) {
            $failed += "Gautama must use the user's exact $field."
        }
    }
    if ($sprite.Width -ne $hull.width -or $sprite.Height -ne $hull.height) {
        $failed += "Gautama's render dimensions must match the actual sprite."
    }

    if ($hull.engineSlots.Count -ne 4 -or $hull.engineSlots.Count -ne $design.engineSlots.Count) {
        $failed += "Keep all four user-authored engines."
    }
    for ($index = 0; $index -lt $hull.engineSlots.Count; $index++) {
        $slot = $hull.engineSlots[$index]
        foreach ($field in @("location", "length", "width", "angle", "contrailSize")) {
            if ($index -ge $design.engineSlots.Count -or
                    ($slot.$field | ConvertTo-Json -Compress) -ne
                    ($design.engineSlots[$index].$field | ConvertTo-Json -Compress)) {
                $failed += "Engine $($index + 1) must retain the authored $field."
            }
        }
        if ($slot.style -ne "THREAT") {
            $failed += "Engine $($index + 1) must use white vanilla THREAT flares."
        }
        # The hull pivot is measured from the PNG's bottom-left. Both center
        # and local forward displacement must be inverted for PNG y.
        $pixelX = [int][Math]::Round(
            [double]$hull.center[0] - [double]$slot.location[1])
        $pixelY = [int][Math]::Round(
            [double]$hull.height - [double]$hull.center[1] - [double]$slot.location[0])
        if ($pixelX -lt 0 -or $pixelX -ge $sprite.Width -or
                $pixelY -lt 0 -or $pixelY -ge $sprite.Height -or
                $sprite.GetPixel($pixelX, $pixelY).A -lt 96) {
            $failed += "Engine $($index + 1) is not attached to painted hull art at ($pixelX,$pixelY)."
        }
    }

    foreach ($skinId in @("chief_navigator_scylla", "chief_navigator_scylla_final",
            "chief_navigator_scylla_reincarnating")) {
        $skinPath = Join-Path $modRoot "data\hulls\skins\$skinId.skin"
        $skin = Get-Content -LiteralPath $skinPath -Raw | ConvertFrom-Json
        if ($skin.baseHullId -ne $hull.hullId -or $skin.spriteName -ne $hull.spriteName) {
            $failed += "$skinId must inherit the Gautama base hull and use its sprite."
        }
        foreach ($property in $skin.PSObject.Properties) {
            if ($property.Name -match "engine" -and
                    ($property.Name -ne "removeEngineSlots" -or @($property.Value).Count -gt 0)) {
                $failed += "$skinId overrides inherited engines through $($property.Name)."
            }
            if ($property.Name -in @("width", "height", "center") -and
                    ($property.Value | ConvertTo-Json -Compress) -ne
                    ($hull.($property.Name) | ConvertTo-Json -Compress)) {
                $failed += "$skinId changes inherited $($property.Name)."
            }
        }
    }

    if ($failed.Count -gt 0) {
        throw ("Gautama authored-engine regression: " + ($failed -join "; "))
    }
    Write-Output ("PASS: all {0} authored engines use white THREAT flares and attach to painted hull art; bounds/pivot match and all 3 skins inherit them." -f $hull.engineSlots.Count)
} finally {
    $sprite.Dispose()
}
