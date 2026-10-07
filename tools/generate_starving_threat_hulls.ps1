$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $projectRoot -Parent) -Parent
$coreRoot = Join-Path $starsectorRoot "starsector-core"
$hullOutput = Join-Path $projectRoot "data\hulls"
$variantOutput = Join-Path $projectRoot "data\variants"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
Add-Type -AssemblyName System.Drawing

New-Item -ItemType Directory -Force -Path $hullOutput, $variantOutput | Out-Null

$removedSlotsByHull = @{}

function Test-WeaponSlotTouchesHull {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        $Spec,
        $Slot
    )

    if ($Slot.type -eq "SYSTEM") {
        return $true
    }

    $radius = switch ($Slot.size) {
        "LARGE" { 30 }
        "MEDIUM" { 18 }
        default { 12 }
    }
    # Weapon locations are ordinary sprite-space offsets from the hull center:
    # positive x is right and positive y is up.
    $pixelX = [int][Math]::Round([double]$Spec.center[0] + [double]$Slot.locations[0])
    $pixelY = [int][Math]::Round([double]$Spec.center[1] - [double]$Slot.locations[1])

    for ($dy = -$radius; $dy -le $radius; $dy++) {
        for ($dx = -$radius; $dx -le $radius; $dx++) {
            if (($dx * $dx + $dy * $dy) -gt ($radius * $radius)) { continue }
            $x = $pixelX + $dx
            $y = $pixelY + $dy
            if ($x -lt 0 -or $x -ge $Bitmap.Width -or $y -lt 0 -or $y -ge $Bitmap.Height) { continue }
            if ($Bitmap.GetPixel($x, $y).A -ge 96) { return $true }
        }
    }
    return $false
}

# The collision outlines intentionally cover only the large solid masses. They
# do not trace the generated sprites' ribs, cables, apertures, or empty sockets.
$hulls = @(
    @{
        Base = "assault_unit"; Id = "chief_navigator_starving_assault_unit";
        Name = "Starving Assault Unit"; Sprite = "starving_threat_assault_unit.png";
        Bounds = @(60,55, 78,0, 60,-55, 0,-70, -55,-45, -60,0, -55,45, 0,70)
    },
    @{
        Base = "fabricator_unit"; Id = "chief_navigator_starving_fabricator_unit";
        Name = "Starving Fabricator Unit"; Sprite = "starving_threat_fabricator.png";
        Bounds = @(180,90, 220,0, 170,-100, 0,-145, -170,-100, -210,0, -170,100, 0,145)
    },
    @{
        Base = "hive_unit"; Id = "chief_navigator_starving_hive_unit";
        Name = "Starving Hive Unit"; Sprite = "starving_threat_hive.png";
        Bounds = @(90,75, 105,0, 90,-75, 20,-100, -120,-45, -140,0, -120,45, 20,100)
    },
    @{
        Base = "overseer_unit"; Id = "chief_navigator_starving_overseer_unit";
        Name = "Starving Overseer Unit"; Sprite = "starving_threat_overseer.png";
        Bounds = @(45,50, 65,0, 45,-50, 0,-65, -50,-50, -60,0, -50,50, 0,65)
    },
    @{
        Base = "skirmish_unit"; Id = "chief_navigator_starving_skirmish_unit";
        Name = "Starving Skirmish Unit"; Sprite = "starving_threat_skirmish.png";
        Bounds = @(34,24, 38,-24, 0,-36, -36,-28, -38,25, 0,36)
    },
    @{
        Base = "standoff_unit"; Id = "chief_navigator_starving_standoff_unit";
        Name = "Starving Line Unit"; Sprite = "starving_threat_standoff_unit.png";
        Bounds = @(65,105, 85,0, 65,-105, 0,-115, -55,-90, -65,0, -55,90, 0,115)
    }
)

foreach ($entry in $hulls) {
    $sourcePath = Join-Path $coreRoot ("data\hulls\" + $entry.Base + ".ship")
    $targetPath = Join-Path $hullOutput ($entry.Id + ".ship")
    $spec = Get-Content -Raw -LiteralPath $sourcePath | ConvertFrom-Json
    $spec.hullId = $entry.Id
    $spec.hullName = $entry.Name
    $spec.spriteName = "graphics/ships/starving_threat/" + $entry.Sprite
    if ($entry.Base -ne "hive_unit") {
        $spec.bounds = $entry.Bounds
    }
    # Replace stock Threat Hull with the complete Starving-specific version.
    $spec.builtInMods = @($spec.builtInMods | Where-Object {
        $_ -ne "threat_hullmod" -and $_ -ne "chief_navigator_starving_threat"
    }) + @("chief_navigator_starving_threat")
    # The Hive retains the entire native geometry and fit. Pixel sampling the
    # depleted artwork must not delete any of its twelve fragment mounts.
    if ($entry.Base -eq "hive_unit") {
        $removedSlotsByHull[$entry.Id] = @()
        $json = $spec | ConvertTo-Json -Depth 30
        [System.IO.File]::WriteAllText($targetPath, $json + [Environment]::NewLine, $utf8NoBom)
        continue
    }
    $spritePath = Join-Path $projectRoot ($spec.spriteName -replace "/", "\")
    $bitmap = [System.Drawing.Bitmap]::new([string]$spritePath)
    try {
        $keptSlots = @()
        $removedSlots = @()
        foreach ($slot in @($spec.weaponSlots)) {
            if (Test-WeaponSlotTouchesHull -Bitmap $bitmap -Spec $spec -Slot $slot) {
                $keptSlots += $slot
            } else {
                $removedSlots += $slot.id
            }
        }
        $spec.weaponSlots = $keptSlots
        $removedSlotsByHull[$entry.Id] = $removedSlots
    } finally {
        $bitmap.Dispose()
    }
    $json = $spec | ConvertTo-Json -Depth 30
    [System.IO.File]::WriteAllText($targetPath, $json + [Environment]::NewLine, $utf8NoBom)
}

$variants = @(
    @{ Source = "assault_unit_Type200"; Hull = "chief_navigator_starving_assault_unit"; Id = "chief_navigator_starving_assault_Type200" },
    @{ Source = "assault_unit_Type201"; Hull = "chief_navigator_starving_assault_unit"; Id = "chief_navigator_starving_assault_Type201" },
    @{ Source = "fabricator_unit_Type450"; Hull = "chief_navigator_starving_fabricator_unit"; Id = "chief_navigator_starving_fabricator_Type450" },
    @{ Source = "hive_unit_Type350"; Hull = "chief_navigator_starving_hive_unit"; Id = "chief_navigator_starving_hive_Type350" },
    @{ Source = "overseer_unit_Type250"; Hull = "chief_navigator_starving_overseer_unit"; Id = "chief_navigator_starving_overseer_Type250" },
    @{ Source = "skirmish_unit_Type100"; Hull = "chief_navigator_starving_skirmish_unit"; Id = "chief_navigator_starving_skirmish_Type100" },
    @{ Source = "skirmish_unit_Type101"; Hull = "chief_navigator_starving_skirmish_unit"; Id = "chief_navigator_starving_skirmish_Type101" },
    @{ Source = "standoff_unit_Type300"; Hull = "chief_navigator_starving_standoff_unit"; Id = "chief_navigator_starving_standoff_Type300" },
    @{ Source = "standoff_unit_Type301"; Hull = "chief_navigator_starving_standoff_unit"; Id = "chief_navigator_starving_standoff_Type301" },
    @{ Source = "standoff_unit_Type302"; Hull = "chief_navigator_starving_standoff_unit"; Id = "chief_navigator_starving_standoff_Type302" }
)

# Fragment-dependent weapons remain excluded from other Starving roles, but
# the Hive keeps its exact native loadout and fragment economy.
$excludedFragmentWeapons = @(
    "swarm_launcher",
    "seeker_fragment",
    "kinetic_fragments",
    "unstable_fragment",
    "devouring_swarm",
    "voltaic_discharge"
)

foreach ($entry in $variants) {
    $sourcePath = Join-Path $coreRoot ("data\variants\threat\" + $entry.Source + ".variant")
    $targetPath = Join-Path $variantOutput ($entry.Id + ".variant")
    $variant = Get-Content -Raw -LiteralPath $sourcePath | ConvertFrom-Json
    $variant.hullId = $entry.Hull
    $variant.variantId = $entry.Id
    $removedSlots = @($removedSlotsByHull[$entry.Hull])
    foreach ($group in @($variant.weaponGroups)) {
        foreach ($slotId in $removedSlots) {
            $group.weapons.PSObject.Properties.Remove($slotId)
        }
        foreach ($weaponSlot in @($group.weapons.PSObject.Properties)) {
            if ($entry.Source -ne "hive_unit_Type350" -and
                    $excludedFragmentWeapons -contains [string]$weaponSlot.Value) {
                $group.weapons.PSObject.Properties.Remove($weaponSlot.Name)
            }
        }
    }
    $variant.weaponGroups = @($variant.weaponGroups | Where-Object { @($_.weapons.PSObject.Properties).Count -gt 0 })
    $json = $variant | ConvertTo-Json -Depth 30
    [System.IO.File]::WriteAllText($targetPath, $json + [Environment]::NewLine, $utf8NoBom)
}

Write-Output "Generated $($hulls.Count) Starving Threat hull specs and $($variants.Count) stock-equivalent variants."
