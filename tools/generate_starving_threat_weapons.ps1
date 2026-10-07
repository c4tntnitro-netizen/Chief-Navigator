$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$modRoot = Split-Path $PSScriptRoot -Parent
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$coreRoot = Join-Path $starsectorRoot "starsector-core"
$coreWeapons = Join-Path $coreRoot "data\weapons"
$outputWeapons = Join-Path $modRoot "data\weapons"
$outputGraphics = Join-Path $modRoot "graphics\weapons\starving_threat"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$weapons = @(
    @{ Stock="swarm_launcher";       Id="chief_navigator_bone_swarm_launcher"; Name="Ossuary Swarm Launcher" },
    @{ Stock="seeker_fragment";      Id="chief_navigator_bone_seeker_fragment"; Name="Marrow Seeker" },
    @{ Stock="kinetic_fragments";    Id="chief_navigator_bone_kinetic_fragments"; Name="Bone Shard Battery" },
    @{ Stock="unstable_fragment";    Id="chief_navigator_bone_unstable_fragment"; Name="Splintered Skull" },
    @{ Stock="devouring_swarm";      Id="chief_navigator_bone_devouring_swarm"; Name="Carrion Swarm" },
    @{ Stock="voltaic_discharge";    Id="chief_navigator_bone_voltaic_discharge"; Name="Grave Spark" },
    @{ Stock="neutron_torpedo";      Id="chief_navigator_bone_neutron_torpedo"; Name="Femur Torpedo" },
    @{ Stock="light_mass_driver";    Id="chief_navigator_bone_light_mass_driver"; Name="Rib Driver" },
    @{ Stock="heavy_mass_driver";    Id="chief_navigator_bone_heavy_mass_driver"; Name="Spine Driver" },
    @{ Stock="neoferric_quadcoil";   Id="chief_navigator_bone_neoferric_quadcoil"; Name="Vertebral Quadcoil" },
    @{ Stock="voltaic_cannon";       Id="chief_navigator_bone_voltaic_cannon"; Name="Osteon Cannon" },
    @{ Stock="voidblaster";          Id="chief_navigator_bone_voidblaster"; Name="Skullblaster" }
)

New-Item -ItemType Directory -Force -Path $outputGraphics | Out-Null

$spritePattern = '"(?<key>(?:turret|hardpoint)(?:Gun|Glow)?Sprite)"\s*:\s*"(?<path>graphics/weapons/(?<file>[^"]+\.png))"'
$spriteFiles = [System.Collections.Generic.HashSet[string]]::new()

foreach ($weapon in $weapons) {
    $sourcePath = Join-Path $coreWeapons ($weapon.Stock + ".wpn")
    $text = Get-Content -LiteralPath $sourcePath -Raw
    $text = $text -replace ('"id"\s*:\s*"' + [regex]::Escape($weapon.Stock) + '"'), ('"id":"' + $weapon.Id + '"')
    if ($weapon.Stock -eq "light_mass_driver") {
        $text = $text -replace '"projectileSpecId"\s*:\s*"light_mass_driver_shot"', '"projectileSpecId":"chief_navigator_bone_light_mass_driver_shot"'
    } elseif ($weapon.Stock -eq "heavy_mass_driver") {
        $text = $text -replace '"projectileSpecId"\s*:\s*"heavy_mass_driver_shot"', '"projectileSpecId":"chief_navigator_bone_heavy_mass_driver_shot"'
    } elseif ($weapon.Stock -eq "voidblaster") {
        $text = $text -replace 'com\.fs\.starfarer\.api\.impl\.combat\.threat\.VoidblasterEffect', 'chiefnavigator.weapons.StarvingSkullblasterEffect'
    }
    $text = [regex]::Replace($text, $spritePattern, {
        param($match)
        $file = $match.Groups['file'].Value
        [void] $spriteFiles.Add($file)
        return '"' + $match.Groups['key'].Value + '":"graphics/weapons/starving_threat/' + $file + '"'
    })
    [System.IO.File]::WriteAllText(
        (Join-Path $outputWeapons ($weapon.Id + ".wpn")),
        $text,
        $utf8NoBom)
}

foreach ($file in $spriteFiles) {
    $sourcePath = Join-Path (Join-Path $coreRoot "graphics\weapons") $file
    $source = [System.Drawing.Bitmap]::FromFile($sourcePath)
    try {
        $result = New-Object System.Drawing.Bitmap($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            # A one-pixel crop, stretched back onto the original canvas, gives the old art a
            # subtly leaner silhouette without changing weapon-slot alignment or recoil offsets.
            $g = [System.Drawing.Graphics]::FromImage($result)
            try {
                $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $srcRect = New-Object System.Drawing.Rectangle(1, 1, [Math]::Max(1, $source.Width - 2), [Math]::Max(1, $source.Height - 2))
                $dstRect = New-Object System.Drawing.Rectangle(0, 0, $source.Width, $source.Height)
                $g.DrawImage($source, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
            } finally { $g.Dispose() }

            # Pale ivory lift: retain the stock values and detail, then gently desaturate and
            # raise the RGB floor so the mechanisms read as old bone rather than green metal.
            for ($y = 0; $y -lt $result.Height; $y++) {
                for ($x = 0; $x -lt $result.Width; $x++) {
                    $c = $result.GetPixel($x, $y)
                    if ($c.A -eq 0) { continue }
                    $luma = [int](0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B)
                    $r = [Math]::Min(255, [int](0.38 * $c.R + 0.42 * $luma + 52))
                    $green = [Math]::Min(255, [int](0.38 * $c.G + 0.42 * $luma + 48))
                    $b = [Math]::Min(255, [int](0.38 * $c.B + 0.42 * $luma + 34))
                    $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $green, $b))
                }
            }
            $result.Save((Join-Path $outputGraphics $file), [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $result.Dispose() }
    } finally { $source.Dispose() }
}

$csvPath = Join-Path $outputWeapons "weapon_data.csv"
$existing = @(Import-Csv -LiteralPath $csvPath | Where-Object { $_.id -notlike 'chief_navigator_bone_*' })
$stockRows = Import-Csv -LiteralPath (Join-Path $coreWeapons "weapon_data.csv")
$lookup = @{}
foreach ($row in $stockRows) { $lookup[$row.id] = $row }

$number = 200
foreach ($weapon in $weapons) {
    $row = $lookup[$weapon.Stock].PSObject.Copy()
    $row.name = $weapon.Name
    $row.id = $weapon.Id
    $row.OPs = [Math]::Max(1, [int][Math]::Ceiling(([double]$row.OPs) * 0.5))

    # Each self-contained Starving weapon keeps the tactical purpose of its
    # stock counterpart, but pays for its half-OP cost with a distinct flaw.
    switch ($weapon.Stock) {
        "light_mass_driver" {
            # Reduced cadence plus the Volatile Particle Driver's range failure.
            $row.chargedown = "1"
            $row.customAncillary = "Starved cycling increases refire delay to 1 second. Shots may also dissolve: all reach half range, but only 50% reach maximum range."
        }
        "heavy_mass_driver" {
            # Preserve its unique accurate/efficient 800-range profile while
            # making the cheap mount poor at tracking agile targets; its custom
            # shot also inherits the Volatile Particle Driver's range failure.
            $row.'turn rate' = "12"
            $row.customAncillary = "An underpowered traverse tracks slowly. Shots may also dissolve: all reach half range, but only 50% reach maximum range."
        }
        "neutron_torpedo" {
            # Preserve the singular 2,500-damage kinetic impact, but halve the
            # rack's total battle-long payload.
            $row.ammo = "1"
            $row.'proj speed' = "900"
            $row.customAncillary = "The skeletal rack carries only one torpedo, accelerated to three times the standard projectile speed."
        }
        "voltaic_cannon" {
            # Keep two opening EMP shots and their full disabling impact, but
            # make replacement charges substantially scarcer (90 seconds).
            $row.'ammo/sec' = "0.01111"
            $row.customAncillary = "Its damaged capacitor takes 90 seconds to regenerate a charge."
        }
        "voidblaster" {
            # Preserve the full stock range and charge economy. Sustained use
            # instead makes the weapon consume its host through Hammer-strength
            # feedback detonations at the mount.
            $row.customAncillary = "While firing, a Hammer-strength feedback explosion detonates at the weapon mount every 0.5 seconds, dealing high explosive damage to the firing ship."
        }
        "neoferric_quadcoil" {
            # Keep the characteristic accurate eight-round HE burst while
            # reducing sustained output: 0.7s burst + 7.3s recovery = 8s cycle.
            $row.chargedown = "7.3"
            $row.ammo = "160"
            $row.'ammo/sec' = ""
            $row.'reload size' = ""
            $row.customAncillary = "Starved feed capacitors extend the interval between bursts to 8 seconds. Carries 160 non-regenerating rounds."
        }
    }

    $row.'tech/manufacturer' = "Starving Threat"

    # These weapons exist only as authored enemy armament.  Do not let the
    # broad vanilla "threat" tag place them in faction autofit/loadout pools,
    # and never expose them through markets or post-battle salvage.
    $keptTags = @($row.tags -split ',' | ForEach-Object { $_.Trim() } |
        Where-Object {
            $_ -and $_ -ne 'codex_unlockable' -and $_ -ne 'threat' -and
            $_ -ne 'no_bp' -and $_ -ne 'no_sell' -and
            $_ -ne 'no_dealer' -and $_ -ne 'no_drop' -and
            $_ -ne 'no_drop_salvage' -and
            $_ -ne 'chief_navigator_starving_weapon'
        })
    $row.tags = (@($keptTags) + @(
        'restricted', 'no_bp', 'no_sell', 'no_dealer', 'no_drop',
        'no_drop_salvage', 'chief_navigator_starving_weapon'
    ) | Select-Object -Unique) -join ', '
    $row.number = $number++
    $existing += $row
}

function ConvertTo-StarsectorCsvCell([object] $value) {
    if ($null -eq $value -or [string]::IsNullOrEmpty([string]$value)) {
        # Starsector's CSV reader treats PowerShell's quoted empty cell ("") as a
        # literal quote instead of an empty value. Native data files leave it bare.
        return ""
    }
    return '"' + ([string]$value).Replace('"', '""') + '"'
}

$columns = @($existing[0].PSObject.Properties.Name)
$csvLines = [System.Collections.Generic.List[string]]::new()
$csvLines.Add((($columns | ForEach-Object { ConvertTo-StarsectorCsvCell $_ }) -join ','))
foreach ($row in $existing) {
    $csvLines.Add((($columns | ForEach-Object {
        ConvertTo-StarsectorCsvCell $row.$_
    }) -join ','))
}
[System.IO.File]::WriteAllLines($csvPath, $csvLines, $utf8NoBom)
Write-Output "Generated $($weapons.Count) reduced-OP bone weapons and $($spriteFiles.Count) cropped/ivory sprite edits."
