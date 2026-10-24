# Shared by build.ps1 and its isolated installation regression. Never probe a
# destination and then reopen it with File.Copy: the exclusive stream IS the writer.
function Get-ChiefNavigatorBuildJarName {
    param([string]$ModInfo, [switch]$Release)
    $ErrorActionPreference = 'Stop'
    if ($Release) { return 'ChiefNavigator.jar' }
    $manifest = $ModInfo | ConvertFrom-Json
    $active = @($manifest.jars | ForEach-Object { ($_ -split '[\\/]')[-1] })
    foreach ($name in @('ChiefNavigator.next.jar', 'ChiefNavigator.next2.jar')) {
        if ($active -notcontains $name) { return $name }
    }
    throw 'Both development jars are selected by mod_info.json; no inactive build slot is available.'
}

function Get-ChiefNavigatorStreamHash {
    param([System.IO.Stream]$Stream)
    $hash = [System.Security.Cryptography.SHA256]::Create()
    try {
        $Stream.Position = 0
        return [BitConverter]::ToString($hash.ComputeHash($Stream))
    } finally {
        $Stream.Position = 0
        $hash.Dispose()
    }
}

function Install-ChiefNavigatorBuildJar {
    param(
        [Parameter(Mandatory = $true)][string]$StagedJarPath,
        [Parameter(Mandatory = $true)][string]$DestinationPath,
        [Parameter(Mandatory = $true)][string]$ModInfoPath,
        [Parameter(Mandatory = $true)][string]$ExpectedModInfo)
    # A caller using Continue must never fall through a failed lock acquisition.
    $ErrorActionPreference = 'Stop'
    Add-Type -AssemblyName System.IO.Compression
    $source = $null
    $destination = $null
    $manifest = $null
    try {
        if ([System.IO.Path]::GetFullPath($StagedJarPath) -eq
                [System.IO.Path]::GetFullPath($DestinationPath)) {
            throw 'The staged jar must be separate from the installed jar.'
        }
        $jarsPattern = [regex]'"jars"\s*:\s*\[[^\]]*\]'
        if (-not $jarsPattern.IsMatch($ExpectedModInfo)) {
            throw 'Could not find the jars field in mod_info.json.'
        }
        $jarRelative = 'jars/' + [System.IO.Path]::GetFileName($DestinationPath)
        $updatedModInfo = $jarsPattern.Replace(
            $ExpectedModInfo, ('"jars": ["' + $jarRelative + '"]'), 1)
        $null = $updatedModInfo | ConvertFrom-Json

        $source = [System.IO.File]::Open($StagedJarPath,
            [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read,
            [System.IO.FileShare]::Read)
        # Read every ZIP entry before acquiring or modifying any installed file.
        $archive = [System.IO.Compression.ZipArchive]::new(
            $source, [System.IO.Compression.ZipArchiveMode]::Read, $true)
        try {
            if ($archive.Entries.Count -eq 0) { throw 'The staged jar is empty.' }
            foreach ($entry in $archive.Entries) {
                $entryStream = $entry.Open()
                try { $entryStream.CopyTo([System.IO.Stream]::Null) }
                finally { $entryStream.Dispose() }
            }
        } finally { $archive.Dispose() }
        $stagedHash = Get-ChiefNavigatorStreamHash $source

        # Lock and check the manifest before opening the destination. Another
        # build or a locked manifest must not leave a partially installed jar.
        $manifest = [System.IO.File]::Open($ModInfoPath,
            [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
        $reader = [System.IO.StreamReader]::new(
            $manifest, [System.Text.Encoding]::UTF8, $true, 1024, $true)
        try { $currentModInfo = $reader.ReadToEnd() }
        finally { $reader.Dispose() }
        if ($currentModInfo -cne $ExpectedModInfo) {
            throw 'mod_info.json changed during the build; installation cancelled before overwriting the jar.'
        }
        try {
            $destination = [System.IO.File]::Open($DestinationPath,
                [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite,
                [System.IO.FileShare]::None)
        } catch {
            throw "Cannot exclusively open destination jar '$DestinationPath'. Close Starsector and retry; jar and mod_info.json were not changed. $($_.Exception.Message)"
        }

        # Keep FileShare.None continuously through overwrite, durable flush and
        # readback. A live JVM cannot keep or acquire this jar while it changes.
        $destination.SetLength(0)
        $source.CopyTo($destination)
        $destination.Flush($true)
        if ((Get-ChiefNavigatorStreamHash $destination) -cne $stagedHash) {
            throw 'Installed jar hash does not match the validated staged jar; mod_info.json was not changed.'
        }
        $manifestBytes = [System.Text.UTF8Encoding]::new($false).GetBytes($updatedModInfo)
        $manifest.Position = 0
        $manifest.SetLength(0)
        $manifest.Write($manifestBytes, 0, $manifestBytes.Length)
        $manifest.Flush($true)
    } finally {
        if ($null -ne $destination) { $destination.Dispose() }
        if ($null -ne $manifest) { $manifest.Dispose() }
        if ($null -ne $source) { $source.Dispose() }
    }
}

function Remove-ChiefNavigatorBuildStage {
    param([string]$StageRoot, [string]$ProjectRoot)
    $ErrorActionPreference = 'Stop'
    $parent = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot 'out_build_staging'))
    $target = [System.IO.Path]::GetFullPath($StageRoot)
    if (-not $target.StartsWith($parent + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing to remove a build staging directory outside the project staging root.'
    }
    if (Test-Path -LiteralPath $target) {
        $resolved = (Resolve-Path -LiteralPath $target).Path
        if (-not $resolved.StartsWith($parent + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Resolved build staging directory is outside the project staging root.'
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force -ErrorAction Stop
    }
}
