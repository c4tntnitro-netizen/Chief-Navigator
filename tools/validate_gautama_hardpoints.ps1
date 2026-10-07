$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$hullPath = Join-Path $modRoot "data\hulls\chief_navigator_scylla_base.ship"
$hull = Get-Content -LiteralPath $hullPath -Raw | ConvertFrom-Json
$designPath = Join-Path $modRoot "graphics\ships\odyssey_bosses\gautama.ship"
$design = Get-Content -LiteralPath $designPath -Raw | ConvertFrom-Json
$slotMap = [ordered]@{
    "WS 001" = "WS0001"; "WS 032" = "WS0002"; "WS 010" = "WS0003"
    "WS 008" = "WS0004"; "WS 012" = "WS0005"; "WS 034" = "WS0006"
    "WS 020" = "WS0007"; "WS 033" = "WS0008"; "WS 035" = "WS0009"
}
$spritePath = Join-Path $modRoot ($hull.spriteName -replace "/", "\")
$sprite = [System.Drawing.Bitmap]::FromFile($spritePath)

try {
    $failed = @()
    foreach ($entry in $slotMap.GetEnumerator()) {
        $slot = @($hull.weaponSlots | Where-Object { $_.id -eq $entry.Key })
        $authored = @($design.weaponSlots | Where-Object { $_.id -eq $entry.Value })
        if ($slot.Count -ne 1 -or $authored.Count -ne 1) {
            throw "Missing or duplicate Gautama slot mapping: $($entry.Key) -> $($entry.Value)"
        }
        $slot = $slot[0]
        $authored = $authored[0]
        if (($slot.locations | ConvertTo-Json -Compress) -ne
                ($authored.locations | ConvertTo-Json -Compress) -or
                $slot.angle -ne $authored.angle -or $slot.arc -le 0) {
            $failed += "$($entry.Key) must use $($entry.Value)'s exact position/angle with a working arc."
        }
        # Starsector local +x is the top/forward direction of an upright hull
        # PNG, while local +y points toward the PNG's left edge.
        $pixelX = [int][Math]::Round(
            [double]$hull.center[0] - [double]$slot.locations[1])
        $pixelY = [int][Math]::Round(
            [double]$hull.height - [double]$hull.center[1] - [double]$slot.locations[0])

        $opaque = 0
        $samples = 0
        for ($dy = -6; $dy -le 6; $dy++) {
            for ($dx = -6; $dx -le 6; $dx++) {
                if ($dx * $dx + $dy * $dy -gt 36) { continue }
                $samples++
                $x = $pixelX + $dx
                $y = $pixelY + $dy
                if ($x -lt 0 -or $x -ge $sprite.Width -or $y -lt 0 -or $y -ge $sprite.Height) {
                    continue
                }
                if ($sprite.GetPixel($x, $y).A -ge 64) { $opaque++ }
            }
        }

        $coverage = $opaque / [double]$samples
        if ($coverage -lt 0.75) {
            $failed += "$($slot.id) at PNG ($pixelX,$pixelY): "
                    + "{0:P0} opaque support" -f $coverage
        }
    }

    if ($failed.Count -gt 0) {
        throw "Gautama weapon pivots do not sit on the painted hull:`n$($failed -join "`n")"
    }

    Write-Output (
        "PASS: all {0} Gautama main gun pivots match the user's marked positions/angles and align with opaque hull art." `
            -f $slotMap.Count)
} finally {
    $sprite.Dispose()
}
