$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'install_build_jar.ps1')
Add-Type -AssemblyName System.IO.Compression
$projectRoot = Split-Path $PSScriptRoot -Parent
$fixtureRoot = Join-Path $projectRoot ('out_build_staging\install_test_' + [Guid]::NewGuid().ToString('N'))
$fixtureJars = Join-Path $fixtureRoot 'jars'
$fixtureManifest = Join-Path $fixtureRoot 'mod_info.json'
$fixtureDestination = Join-Path $fixtureJars 'ChiefNavigator.next.jar'
$fixtureActive = Join-Path $fixtureJars 'ChiefNavigator.next2.jar'
$fixtureStaged = Join-Path $fixtureRoot 'staged.jar'
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Assert-BuildInstall {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}
function New-FixtureJar {
    param([string]$Path, [string]$Payload)
    $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create)
    $archive = [System.IO.Compression.ZipArchive]::new(
        $stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        foreach ($name in @('META-INF/MANIFEST.MF', 'example/Fixture.class')) {
            $entry = $archive.CreateEntry($name)
            $writer = [System.IO.StreamWriter]::new($entry.Open(), $utf8)
            try { $writer.Write($Payload + ':' + $name) }
            finally { $writer.Dispose() }
        }
    } finally { $archive.Dispose() }
}
function Get-FixtureHash {
    param([string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Assert-FixtureUnchanged {
    Assert-BuildInstall ((Get-FixtureHash $fixtureDestination) -eq $originalDestination) 'Refused install changed destination bytes.'
    Assert-BuildInstall ((Get-FixtureHash $fixtureManifest) -eq $originalManifest) 'Refused install changed mod_info.json.'
    Assert-BuildInstall ((Get-FixtureHash $fixtureActive) -eq $originalActive) 'Install touched the selected active jar.'
}

try {
    $null = New-Item -ItemType Directory -Force -Path $fixtureJars
    New-FixtureJar $fixtureDestination 'old inactive jar'
    New-FixtureJar $fixtureActive 'live jar'
    New-FixtureJar $fixtureStaged 'new verified jar'
    $manifestText = '{"id":"fixture","jars":["jars/ChiefNavigator.next2.jar"]}'
    [System.IO.File]::WriteAllText($fixtureManifest, $manifestText, $utf8)
    $originalDestination = Get-FixtureHash $fixtureDestination
    $originalManifest = Get-FixtureHash $fixtureManifest
    $originalActive = Get-FixtureHash $fixtureActive
    Assert-BuildInstall ((Get-ChiefNavigatorBuildJarName $manifestText) -eq 'ChiefNavigator.next.jar') 'Prefer the inactive development slot.'
    Assert-BuildInstall ((Get-ChiefNavigatorBuildJarName ($manifestText.Replace('next2.jar', 'next.jar'))) -eq 'ChiefNavigator.next2.jar') 'Alternate when the first development slot is selected.'
    Assert-BuildInstall ((Get-ChiefNavigatorBuildJarName $manifestText -Release) -eq 'ChiefNavigator.jar') 'Release builds must retain the canonical jar.'

    # A JVM commonly keeps a shared reader. Even a caller using Continue must
    # never reach a fallback overwrite after the exclusive open is refused.
    foreach ($lockedPath in @($fixtureDestination, $fixtureManifest)) {
        $holder = [System.IO.File]::Open($lockedPath, [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::Read, [System.IO.FileShare]::Read)
        $refused = $false
        try {
            $ErrorActionPreference = 'Continue'
            try {
                Install-ChiefNavigatorBuildJar -StagedJarPath $fixtureStaged `
                    -DestinationPath $fixtureDestination -ModInfoPath $fixtureManifest `
                    -ExpectedModInfo $manifestText
            } catch { $refused = $true }
        } finally {
            $ErrorActionPreference = 'Stop'
            $holder.Dispose()
        }
        Assert-BuildInstall $refused 'An open game jar or manifest must reject installation.'
        Assert-FixtureUnchanged
    }

    $refused = $false
    try {
        Install-ChiefNavigatorBuildJar -StagedJarPath $fixtureStaged `
            -DestinationPath $fixtureDestination -ModInfoPath $fixtureManifest `
            -ExpectedModInfo ($manifestText + ' ')
    } catch { $refused = $true }
    Assert-BuildInstall $refused 'A manifest changed during compilation must reject installation.'
    Assert-FixtureUnchanged

    $invalidJar = Join-Path $fixtureRoot 'invalid.jar'
    [System.IO.File]::WriteAllText($invalidJar, 'invalid LOC header', $utf8)
    $refused = $false
    try {
        Install-ChiefNavigatorBuildJar -StagedJarPath $invalidJar `
            -DestinationPath $fixtureDestination -ModInfoPath $fixtureManifest `
            -ExpectedModInfo $manifestText
    } catch { $refused = $true }
    Assert-BuildInstall $refused 'A corrupt staged jar must reject installation.'
    Assert-FixtureUnchanged

    Install-ChiefNavigatorBuildJar -StagedJarPath $fixtureStaged `
        -DestinationPath $fixtureDestination -ModInfoPath $fixtureManifest `
        -ExpectedModInfo $manifestText
    Assert-BuildInstall ((Get-FixtureHash $fixtureDestination) -eq (Get-FixtureHash $fixtureStaged)) 'Successful install must match the staged jar byte for byte.'
    Assert-BuildInstall ((Get-FixtureHash $fixtureActive) -eq $originalActive) 'Successful installation must retain the old active jar.'
    $installedManifest = [System.IO.File]::ReadAllText($fixtureManifest) | ConvertFrom-Json
    Assert-BuildInstall ($installedManifest.jars[0] -eq 'jars/ChiefNavigator.next.jar') 'Select the verified new jar only after installation.'
    $reader = [System.IO.File]::OpenRead($fixtureDestination)
    $installedArchive = [System.IO.Compression.ZipArchive]::new(
        $reader, [System.IO.Compression.ZipArchiveMode]::Read, $false)
    try {
        Assert-BuildInstall ($installedArchive.Entries.Count -eq 2) 'Installed jar must retain every ZIP entry.'
        foreach ($entry in $installedArchive.Entries) {
            $entryReader = [System.IO.StreamReader]::new($entry.Open())
            try { Assert-BuildInstall ($entryReader.ReadToEnd() -eq ('new verified jar:' + $entry.FullName)) 'Installed ZIP entry payload changed.' }
            finally { $entryReader.Dispose() }
        }
    } finally { $installedArchive.Dispose() }
} finally {
    Remove-ChiefNavigatorBuildStage -StageRoot $fixtureRoot -ProjectRoot $projectRoot
}
Assert-BuildInstall (-not (Test-Path -LiteralPath $fixtureRoot)) 'Fixture staging cleanup failed.'
Write-Host 'PASS: inactive dev slot, canonical release, locked destination/manifest refusal under Continue, stale manifest and corrupt ZIP refusal, unchanged live jar, verified install and staging cleanup.'
