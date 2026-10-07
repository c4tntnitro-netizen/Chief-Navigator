$ErrorActionPreference = "Stop"

$modRoot = Split-Path $PSScriptRoot -Parent
$outputPath = Join-Path $modRoot "sounds\chief_navigator\budai_enrage_screech.wav"
$sampleRate = 44100
$duration = 3.35
$sampleCount = [int]($sampleRate * $duration)
$random = [System.Random]::new(87193)
$samples = New-Object 'System.Int16[]' $sampleCount
$lowPhase = 0.0
$formantOnePhase = 0.0
$formantTwoPhase = 0.0
$formantThreePhase = 0.0
$slowNoise = 0.0
$midNoise = 0.0
$previousNoise = 0.0

for ($i = 0; $i -lt $sampleCount; $i++) {
    $t = $i / [double]$sampleRate
    $attack = [Math]::Min(1.0, $t / 0.022)
    $release = [Math]::Min(1.0, ($duration - $t) / 0.68)
    $envelope = $attack * $release

    # Independent, inharmonic resonances make this a tearing animal/mechanical
    # roar instead of the old clean arcade-style pitch sweep.
    $rawNoise = $random.NextDouble() * 2.0 - 1.0
    $slowNoise = 0.9992 * $slowNoise + 0.0008 * $rawNoise
    $midNoise = 0.91 * $midNoise + 0.09 * $rawNoise
    $rasp = $rawNoise - 0.74 * $previousNoise
    $previousNoise = $rawNoise

    $lowFrequency = 71.0 `
        + 8.0 * [Math]::Sin(2.0 * [Math]::PI * 1.17 * $t) `
        + 240.0 * $slowNoise
    $formantOne = 463.0 `
        + 39.0 * [Math]::Sin(2.0 * [Math]::PI * 0.61 * $t + 0.3) `
        + 310.0 * $slowNoise
    $formantTwo = 997.0 `
        + 81.0 * [Math]::Sin(2.0 * [Math]::PI * 0.37 * $t + 1.7) `
        - 190.0 * $slowNoise
    $formantThree = 1867.0 `
        + 173.0 * [Math]::Sin(2.0 * [Math]::PI * 0.29 * $t + 2.4) `
        + 430.0 * $slowNoise

    $lowPhase += 2.0 * [Math]::PI * $lowFrequency / $sampleRate
    $formantOnePhase += 2.0 * [Math]::PI * $formantOne / $sampleRate
    $formantTwoPhase += 2.0 * [Math]::PI * $formantTwo / $sampleRate
    $formantThreePhase += 2.0 * [Math]::PI * $formantThree / $sampleRate

    $growl = [Math]::Sin($lowPhase)
    $growl += 0.52 * [Math]::Sin(2.73 * $lowPhase + 0.6)
    $growl += 0.24 * [Math]::Sin(5.19 * $lowPhase + 1.4)
    $metal = 0.42 * [Math]::Sin(
        $formantOnePhase + 0.8 * [Math]::Sin($lowPhase))
    $metal += 0.31 * [Math]::Sin(
        $formantTwoPhase + 0.45 * [Math]::Sin(1.61 * $lowPhase))
    $metal += 0.19 * [Math]::Sin(
        $formantThreePhase + 0.3 * [Math]::Sin(2.17 * $lowPhase))

    $convulsion = 0.76 `
        + 0.13 * [Math]::Sin(2.0 * [Math]::PI * 6.7 * $t + 0.8) `
        + 0.09 * [Math]::Sin(2.0 * [Math]::PI * 11.9 * $t + 2.1)
    $impact = [Math]::Exp(-18.0 * $t) * (1.8 * $rasp + 0.7 * $growl)
    $noiseBody = 0.72 * $rasp + 0.36 * ($rawNoise - $midNoise)
    $sample = [Math]::Tanh(
        (0.50 * $growl + $metal + $noiseBody + $impact) `
            * 1.95 * $convulsion)
    $sample *= 0.66 * $envelope
    $samples[$i] = [int16]([Math]::Round($sample * 32767.0))
}

$stream = [System.IO.File]::Open(
    $outputPath,
    [System.IO.FileMode]::Create,
    [System.IO.FileAccess]::Write,
    [System.IO.FileShare]::None)
$writer = [System.IO.BinaryWriter]::new($stream)
try {
    $dataSize = $sampleCount * 2
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
    $writer.Write([int](36 + $dataSize))
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
    $writer.Write([int]16)
    $writer.Write([int16]1)
    $writer.Write([int16]1)
    $writer.Write([int]$sampleRate)
    $writer.Write([int]($sampleRate * 2))
    $writer.Write([int16]2)
    $writer.Write([int16]16)
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
    $writer.Write([int]$dataSize)
    foreach ($sample in $samples) {
        $writer.Write($sample)
    }
} finally {
    $writer.Dispose()
    $stream.Dispose()
}

Write-Host "Generated $outputPath"
