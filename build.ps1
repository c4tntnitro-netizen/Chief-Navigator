param([switch]$Release)

$ErrorActionPreference = "Stop"

$modRoot = $PSScriptRoot
$parentRoot = Split-Path $modRoot -Parent
$starsectorRoot = @($parentRoot, (Split-Path $parentRoot -Parent)) |
    Where-Object { Test-Path -LiteralPath (Join-Path $_ 'starsector-core') -PathType Container } |
    Select-Object -First 1
if (-not $starsectorRoot) { throw "Cannot locate Starsector beside the source repository." }
$modsRoot = Join-Path $starsectorRoot 'mods'
$coreRoot = Join-Path $starsectorRoot "starsector-core"
$graphicsLibJar = Join-Path $modsRoot "GraphicsLib\jars\Graphics.jar"
$consoleCommandsJar = Join-Path $modsRoot "Console Commands\jars\lw_Console.jar"
$combatChatterJar = Join-Path $modsRoot "Combat Chatter\jars\CombatChatter.jar"
# Compile the isolated optional-settings bridge; LunaLib is not bundled or required to play.
$lunaLibJar = Join-Path $modsRoot "LunaLib\jars\LunaLib.jar"
$jdkRoot = Join-Path $starsectorRoot "jdk-23+7\bin"
$stagingRoot = Join-Path $modRoot ('out_build_staging\' + [Guid]::NewGuid().ToString('N'))
$outputRoot = Join-Path $stagingRoot 'classes'
$javaTmpRoot = Join-Path $stagingRoot 'java_tmp'
$stagedJarPath = Join-Path $stagingRoot 'ChiefNavigator.jar'
$jarDirectory = Join-Path $modRoot "jars"
. (Join-Path $modRoot 'tools\install_build_jar.ps1')
$modInfoPath = Join-Path $modRoot 'mod_info.json'
$modInfo = [System.IO.File]::ReadAllText($modInfoPath)
$jarPath = Join-Path $jarDirectory (Get-ChiefNavigatorBuildJarName -ModInfo $modInfo -Release:$Release)

$classpath = @(
    (Join-Path $coreRoot "starfarer.api.jar"),
    (Join-Path $coreRoot "json.jar"),
    (Join-Path $coreRoot "lwjgl.jar"),
    (Join-Path $coreRoot "lwjgl_util.jar"),
    (Join-Path $coreRoot "log4j-1.2.9.jar"),
    $graphicsLibJar,
    $consoleCommandsJar,
    $combatChatterJar,
    $lunaLibJar
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
    & (Join-Path $jdkRoot "java.exe") "-Djava.io.tmpdir=$javaTmpRoot" --source 17 -cp $nativeCsvClasspath `
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
            if ($punctuationProbe -notmatch '(?:[.?!](?:"|”)?|—(?:"|”))$') {
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

try {
    New-Item -ItemType Directory -Force -Path $outputRoot, $javaTmpRoot, $jarDirectory | Out-Null
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

    $resolvedOutput = (Resolve-Path -LiteralPath $outputRoot).Path
    $resolvedProject = (Resolve-Path -LiteralPath $modRoot).Path
    if (-not $resolvedOutput.StartsWith($resolvedProject + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Build output must remain inside the source repository."
    }
    $sourceArguments = Join-Path $stagingRoot 'sources.args'
    $quotedSources = @($sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' })
    [System.IO.File]::WriteAllLines($sourceArguments, $quotedSources,
        [System.Text.UTF8Encoding]::new($false))
    & (Join-Path $jdkRoot "javac.exe") "-J-Djava.io.tmpdir=$javaTmpRoot" -encoding UTF-8 --release 17 -cp $classpath -d $outputRoot ("@" + $sourceArguments)
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE"
    }

    Assert-RuleCommandRegistration -RegisteredPackages $settings.ruleCommandPackages `
        -CommandSources $commandSources -CompiledRoot $outputRoot

    & (Join-Path $jdkRoot "jar.exe") "-J-Djava.io.tmpdir=$javaTmpRoot" --create --file $stagedJarPath -C $outputRoot .
    if ($LASTEXITCODE -ne 0) {
        throw "jar failed with exit code $LASTEXITCODE"
    }

    Install-ChiefNavigatorBuildJar -StagedJarPath $stagedJarPath -DestinationPath $jarPath `
        -ModInfoPath $modInfoPath -ExpectedModInfo $modInfo

} finally {
    Remove-ChiefNavigatorBuildStage -StageRoot $stagingRoot -ProjectRoot $modRoot
}
Write-Host "BUILD OK -> $jarPath"
