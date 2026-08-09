$ErrorActionPreference = "Stop"

$modRoot = $PSScriptRoot
$starsectorRoot = Split-Path (Split-Path $modRoot -Parent) -Parent
$coreRoot = Join-Path $starsectorRoot "starsector-core"
$jdkRoot = Join-Path $starsectorRoot "jdk-23+7\bin"
$outputRoot = Join-Path $modRoot "out"
$jarPath = Join-Path $modRoot "jars\ChiefNavigator.jar"

$classpath = @(
    (Join-Path $coreRoot "starfarer.api.jar"),
    (Join-Path $coreRoot "json.jar"),
    (Join-Path $coreRoot "lwjgl.jar"),
    (Join-Path $coreRoot "lwjgl_util.jar"),
    (Join-Path $coreRoot "log4j-1.2.9.jar")
) -join ";"

New-Item -ItemType Directory -Force -Path $outputRoot, (Split-Path $jarPath -Parent) | Out-Null
Get-ChildItem -LiteralPath $outputRoot -Force | Remove-Item -Recurse -Force

$sources = Get-ChildItem -LiteralPath (Join-Path $modRoot "src") -Recurse -Filter "*.java" |
    Select-Object -ExpandProperty FullName

if (-not $sources) {
    throw "No Java sources found."
}

& (Join-Path $jdkRoot "javac.exe") -encoding UTF-8 --release 17 -cp $classpath -d $outputRoot $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& (Join-Path $jdkRoot "jar.exe") --create --file $jarPath -C $outputRoot .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "BUILD OK -> $jarPath"

