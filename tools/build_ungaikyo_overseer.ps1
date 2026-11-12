param(
    [string]$Source = 'graphics/source/chief_navigator_ungaikyo_overseer_obsidian_raw_v3.png',
    [string]$Output = 'graphics/ships/starving_threat/chief_navigator_ungaikyo_overseer_obsidian_v1.png',
    [string]$Preview = 'docs/assets/ungaikyo_overseer_obsidian_preview.png'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$modsParent = Split-Path -Parent $root
$gameRoot = @($modsParent, (Split-Path -Parent $modsParent)) |
    Where-Object { Test-Path -LiteralPath (Join-Path $_ 'starsector-core') } |
    Select-Object -First 1
if (-not $gameRoot) { throw 'Cannot locate native Overseer reference.' }
$nativePath = Join-Path $gameRoot 'starsector-core/graphics/ships/threat/overseer.png'

if (-not ('UngaikyoOverseerArt' -as [type])) {
    $references = @('System.Drawing')
    if ($PSVersionTable.PSEdition -eq 'Core') {
        $references = @((Get-ChildItem (Join-Path $PSHOME 'ref/*.dll')).FullName) +
            [System.Drawing.Bitmap].Assembly.Location +
            @((Get-ChildItem (Join-Path $PSHOME 'System.Private.Windows.*.dll')).FullName)
    }
    Add-Type -ReferencedAssemblies $references -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public static class UngaikyoOverseerArt {
    private static Rectangle Bounds(Bitmap image) {
        int l=image.Width, t=image.Height, r=-1, b=-1;
        for (int y=0; y<image.Height; y++)
            for (int x=0; x<image.Width; x++)
                if (image.GetPixel(x,y).A>8) {
                    l=Math.Min(l,x); t=Math.Min(t,y);
                    r=Math.Max(r,x); b=Math.Max(b,y);
                }
        if (r<l) throw new InvalidOperationException("Empty Overseer sprite");
        return Rectangle.FromLTRB(Math.Max(0,l-1),Math.Max(0,t-1),
            Math.Min(image.Width,r+2),Math.Min(image.Height,b+2));
    }

    public static Bitmap Fit(string source, string nativePath) {
        using (Bitmap raw=new Bitmap(source))
        using (Bitmap native=new Bitmap(nativePath)) {
            if (native.Width!=148 || native.Height!=160)
                throw new InvalidOperationException("Unexpected native Overseer canvas");
            Bitmap result=new Bitmap(native.Width,native.Height,PixelFormat.Format32bppArgb);
            using (Graphics g=Graphics.FromImage(result)) {
                g.CompositingMode=CompositingMode.SourceCopy;
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                g.PixelOffsetMode=PixelOffsetMode.HighQuality;
                // Register the generated art to the native visible footprint.
                // Keep the full native canvas: its PNG pivot is (74,86).
                g.DrawImage(raw,Bounds(native),Bounds(raw),GraphicsUnit.Pixel);
            }
            return result;
        }
    }
}
'@
}

$sprite = [UngaikyoOverseerArt]::Fit((Join-Path $root $Source), $nativePath)
try {
    $outputPath = Join-Path $root $Output
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $outputPath)) | Out-Null
    $sprite.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $previewBitmap = [System.Drawing.Bitmap]::new($sprite.Width * 4, $sprite.Height * 4)
    $graphics = [System.Drawing.Graphics]::FromImage($previewBitmap)
    try {
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.DrawImage($sprite,
            [System.Drawing.Rectangle]::new(0,0,$previewBitmap.Width,$previewBitmap.Height),
            [System.Drawing.Rectangle]::new(0,0,$sprite.Width,$sprite.Height),
            [System.Drawing.GraphicsUnit]::Pixel)
        $previewPath = Join-Path $root $Preview
        [System.IO.Directory]::CreateDirectory((Split-Path -Parent $previewPath)) | Out-Null
        $previewBitmap.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $previewBitmap.Dispose() }
    Write-Output "Built $outputPath (148x160); 4x native-pixel preview: $previewPath"
} finally { $sprite.Dispose() }
