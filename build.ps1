param([switch]$Release)

$ErrorActionPreference = "Stop"

$modRoot = $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$coreRoot = Join-Path $starsectorRoot "starsector-core"
$graphicsLibJar = Join-Path (Split-Path $modRoot -Parent) "GraphicsLib\jars\Graphics.jar"
$consoleCommandsJar = Join-Path (Split-Path $modRoot -Parent) "Console Commands\jars\lw_Console.jar"
$combatChatterJar = Join-Path (Split-Path $modRoot -Parent) "Combat Chatter\jars\CombatChatter.jar"
$jdkRoot = Join-Path $starsectorRoot "jdk-23+7\bin"
$outputRoot = Join-Path $modRoot "out"
$jarDirectory = Join-Path $modRoot "jars"
$jarPath = $null
$candidateNames = if ($Release) {
    @("ChiefNavigator.jar")
} else {
    @("ChiefNavigator.next.jar", "ChiefNavigator.next2.jar")
}
foreach ($candidateName in $candidateNames) {
    $candidate = Join-Path $jarDirectory $candidateName
    try {
        $probe = [System.IO.File]::Open(
            $candidate,
            [System.IO.FileMode]::OpenOrCreate,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
        $probe.Dispose()
        $jarPath = $candidate
        break
    } catch [System.IO.IOException] {
        continue
    }
}
if ($null -eq $jarPath) {
    if ($Release) {
        throw "ChiefNavigator.jar is locked. Close Starsector before building a release."
    }
    throw "Both Chief Navigator build jar slots are locked."
}

$classpath = @(
    (Join-Path $coreRoot "starfarer.api.jar"),
    (Join-Path $coreRoot "json.jar"),
    (Join-Path $coreRoot "lwjgl.jar"),
    (Join-Path $coreRoot "lwjgl_util.jar"),
    (Join-Path $coreRoot "log4j-1.2.9.jar"),
    $graphicsLibJar,
    $consoleCommandsJar,
    $combatChatterJar
) -join ";"

function Assert-RulesCsv {
    param([string]$Path)

    # Import-Csv tolerates malformed closing quotes that Starsector rejects.
    # Validate with the installed game loader before any compilation/packaging.
    $nativeCsvClasspath = @(
        (Join-Path $coreRoot "starfarer_obf.jar"),
        (Join-Path $coreRoot "fs.common_obf.jar"),
        (Join-Path $coreRoot "json.jar")
    ) -join ";"
    & (Join-Path $jdkRoot "java.exe") --source 17 -cp $nativeCsvClasspath `
        (Join-Path $modRoot "tools\RulesCsvValidation.java") $Path
    if ($LASTEXITCODE -ne 0) {
        throw "Starsector's native rules.csv validation failed."
    }

    $rows = @(Import-Csv -LiteralPath $Path)
    if ($rows.Count -eq 0) {
        throw "rules.csv contains no rule rows: $Path"
    }

    $ruleIds = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    foreach ($row in $rows) {
        if ([string]::IsNullOrWhiteSpace($row.id)) {
            throw "rules.csv contains a row with no rule id."
        }
        if (-not $ruleIds.Add($row.id)) {
            throw "rules.csv contains duplicate rule id '$($row.id)'."
        }
        if ([string]::IsNullOrWhiteSpace($row.options)) {
            continue
        }

        $optionIds = [System.Collections.Generic.HashSet[string]]::new(
            [System.StringComparer]::OrdinalIgnoreCase)
        foreach ($line in ($row.options -split "`r?`n")) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            if ($line -notmatch '^\s*\d+:([^:]+):(.*)$') {
                throw "rules.csv rule '$($row.id)' has malformed option '$line'."
            }
            $optionId = $Matches[1].Trim()
            if (-not $optionIds.Add($optionId)) {
                throw "rules.csv rule '$($row.id)' repeats option id '$optionId'."
            }
            $optionLabel = $Matches[2].Trim()
            $punctuationProbe = $optionLabel -replace '\s+\([^()]*\)$', ''
            if ($punctuationProbe -notmatch '[.?!](?:"|”)?$') {
                throw "rules.csv rule '$($row.id)' has an option without terminal punctuation: '$optionLabel'."
            }
        }
    }
}

function Assert-RuleCommandRegistration {
    param(
        [string[]]$RegisteredPackages,
        [string[]]$CommandSources,
        [string]$CompiledRoot = "")

    if (-not $CommandSources) {
        throw "No Chief Navigator rule-command sources found."
    }
    $commandNames = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::Ordinal)
    foreach ($source in $CommandSources) {
        $commandName = [System.IO.Path]::GetFileNameWithoutExtension($source)
        if (-not $commandNames.Add($commandName)) {
            throw "Rule-command name '$commandName' is ambiguous across source packages."
        }
        $packageMatch = [regex]::Match(
            [System.IO.File]::ReadAllText($source),
            '(?m)^\s*package\s+([\w.]+)\s*;')
        if (-not $packageMatch.Success) {
            throw "Rule command '$commandName' has no package declaration."
        }
        $packageName = $packageMatch.Groups[1].Value
        if ($RegisteredPackages -cnotcontains $packageName) {
            throw "Rule command '$commandName' requires '$packageName' in settings.json ruleCommandPackages."
        }
        if (-not [string]::IsNullOrWhiteSpace($CompiledRoot)) {
            $classPath = Join-Path $CompiledRoot (
                $packageName.Replace('.', '/') + '/' + $commandName + '.class')
            if (-not (Test-Path -LiteralPath $classPath -PathType Leaf)) {
                throw "Compiled rule command '$commandName' is missing: $classPath"
            }
        }
    }
}

Assert-RulesCsv (Join-Path $modRoot "data\campaign\rules.csv")

$sources = Get-ChildItem -LiteralPath (Join-Path $modRoot "src") -Recurse -Filter "*.java" |
    Select-Object -ExpandProperty FullName

if (-not $sources) {
    throw "No Java sources found."
}

$settings = Get-Content -LiteralPath (Join-Path $modRoot "data\config\settings.json") -Raw |
    ConvertFrom-Json
$commandSources = @($sources | Where-Object {
    [System.IO.Path]::GetFileName($_) -match '^ChiefNavigator.*CMD\.java$'
})
Assert-RuleCommandRegistration -RegisteredPackages $settings.ruleCommandPackages `
    -CommandSources $commandSources

New-Item -ItemType Directory -Force -Path $outputRoot, $jarDirectory | Out-Null
Get-ChildItem -LiteralPath $outputRoot -Force | Remove-Item -Recurse -Force

& (Join-Path $jdkRoot "javac.exe") -encoding UTF-8 --release 17 -cp $classpath -d $outputRoot $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

Assert-RuleCommandRegistration -RegisteredPackages $settings.ruleCommandPackages `
    -CommandSources $commandSources -CompiledRoot $outputRoot

& (Join-Path $jdkRoot "jar.exe") --create --file $jarPath -C $outputRoot .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

$jarRelative = "jars/" + [System.IO.Path]::GetFileName($jarPath)
$modInfoPath = Join-Path $modRoot "mod_info.json"
$modInfo = [System.IO.File]::ReadAllText($modInfoPath)
$jarsPattern = [regex]'"jars"\s*:\s*\[[^\]]*\]'
$jarsMatch = $jarsPattern.Match($modInfo)
if (-not $jarsMatch.Success) {
    throw "Could not find the jars field in mod_info.json."
}
$updatedModInfo = $jarsPattern.Replace(
    $modInfo,
    ('"jars": ["' + $jarRelative + '"]'),
    1)
[System.IO.File]::WriteAllText(
    $modInfoPath,
    $updatedModInfo,
    [System.Text.UTF8Encoding]::new($false))

Write-Host "BUILD OK -> $jarPath"
