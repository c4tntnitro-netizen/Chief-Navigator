$ErrorActionPreference = 'Stop'

# The wider source is already square, so the shared pipeline preserves its full view.
& (Join-Path $PSScriptRoot 'build_ashen_verge_background.ps1') `
    -Source 'graphics/source/chief_navigator_bouvo_olympus_nebula_v2_wider_generated.png' `
    -Runtime 'graphics/backgrounds/chief_navigator_bouvo_olympus_nebula_v1.jpg'

Write-Output 'Bouvo Olympus background rebuilt with the wider warm-white framing.'
